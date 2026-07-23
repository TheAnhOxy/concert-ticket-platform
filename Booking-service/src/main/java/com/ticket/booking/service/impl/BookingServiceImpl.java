package com.ticket.booking.service.impl;

import com.ticket.booking.client.CoreServiceClient;
import com.ticket.booking.converter.BookingConverter;
import com.ticket.booking.dto.request.BookingRequest;
import com.ticket.booking.dto.response.BookingResponse;
import com.ticket.booking.entity.Booking;
import com.ticket.booking.entity.BookingItem;
import com.ticket.booking.entity.TicketReservation;
import com.ticket.booking.enums.BookingStatus;
import com.ticket.booking.repository.BookingItemRepository;
import com.ticket.booking.repository.BookingRepository;
import com.ticket.booking.repository.TicketReservationRepository;
import com.ticket.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final TicketReservationRepository ticketReservationRepository;
    private final BookingConverter bookingConverter;

    private final CoreServiceClient coreServiceClient;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {

        // ==========================================
        // 1. IDEMPOTENCY CHECK (Chống Duplicate Booking)
        // ==========================================
        Optional<Booking> existingBooking = bookingRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingBooking.isPresent()) {
            log.info("[Idempotency] Trả về đơn hàng cũ cho key: {}", request.getIdempotencyKey());
            return bookingConverter.toResponseDto(existingBooking.get());
        }

        // KIỂM TRA FAST-FAIL VOUCHER ĐÃ SỬ DỤNG CHƯA TRƯỚC KHI THỰC HIỆN CÁC THAO TÁC CÓ TRẠNG THÁI
        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            List<String> statusStrings = List.of(
                    BookingStatus.PAID.name(),
                    BookingStatus.WAITING_PAYMENT.name()
            );
            boolean hasUsed = bookingRepository.existsByUserIdAndVoucherCodeAndStatusIn(
                    request.getUserId(),
                    request.getVoucherCode().trim(),
                    statusStrings
            );
            if (hasUsed) {
                throw new RuntimeException("Bạn đã sử dụng mã giảm giá này rồi!");
            }
        }

        // Kiểm tra khớp số ghế với số vé đặt
        if (request.getSeatIds() != null && !request.getSeatIds().isEmpty()) {
            if (request.getSeatIds().size() != request.getQuantity()) {
                throw new RuntimeException("Số lượng ghế chọn (" + request.getSeatIds().size() + ") không khớp với số lượng vé đặt (" + request.getQuantity() + ")!");
            }
        }

        // ==========================================
        // 2. REDISSON LOCK (Chống System instability & Overselling)
        // ==========================================
        String lockKey = "lock:ticket_category:" + request.getTicketCategoryId();
        RLock lock = redissonClient.getLock(lockKey);

        RLock seatMultiLock = null;
        if (request.getSeatIds() != null && !request.getSeatIds().isEmpty()) {
            RLock[] locks = request.getSeatIds().stream()
                    .map(seatId -> redissonClient.getLock("lock:seat:" + seatId))
                    .toArray(RLock[]::new);
            seatMultiLock = redissonClient.getMultiLock(locks);
        }

        boolean isSeatLocked = false;
        boolean seatsReserved = false;
        boolean inventoryDeducted = false;

        try {
            boolean isLocked = lock.tryLock(3, 5, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new RuntimeException("Hệ thống đang xử lý lượng lớn giao dịch, vui lòng thử lại sau!");
            }

            if (seatMultiLock != null) {
                isSeatLocked = seatMultiLock.tryLock(3, 5, TimeUnit.SECONDS);
                if (!isSeatLocked) {
                    lock.unlock();
                    throw new RuntimeException("Một số ghế bạn chọn đang được giao dịch bởi người khác, vui lòng chọn lại!");
                }
            }

            // ==========================================
            // 3. FAST-CHECK TỒN KHO REDIS & GIỮ GHẾ TRƯỚC (Tránh rò rỉ kho vé)
            // ==========================================
            int availableQuantity = checkInventory(request.getTicketCategoryId());
            if (availableQuantity < request.getQuantity()) {
                throw new RuntimeException("Rất tiếc, số lượng vé trong kho không đủ đáp ứng!");
            }

            // Giữ chỗ ghế cứng trên Core-service trước để check trùng ghế
            if (request.getSeatIds() != null && !request.getSeatIds().isEmpty()) {
                coreServiceClient.reserveSeats(request.getSeatIds(), request.getTicketCategoryId());
                seatsReserved = true;
            }

            // Sau khi giữ ghế thành công mới thực hiện trừ kho vé
            deductInventory(request.getTicketCategoryId(), request.getQuantity());
            inventoryDeducted = true;

            // ==========================================
            // 4. KHỞI TẠO ĐƠN HÀNG (TIMEOUT 10 PHÚT)
            // ==========================================
            String bookingCode = "BKG-" + System.currentTimeMillis();
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

            // Lấy đơn giá thực tế từ Core Service
            BigDecimal unitPrice = coreServiceClient.getPrice(request.getTicketCategoryId());
            if (unitPrice == null) {
                throw new RuntimeException("Không tìm thấy đơn giá cho hạng vé này!");
            }
            BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

            Booking booking = bookingConverter.toBookingEntity(request, bookingCode, totalAmount, totalAmount, expiresAt);
            applyVoucher(request, booking);
            bookingRepository.save(booking);
            
            BookingItem item = bookingConverter.toBookingItemEntity(booking, request, unitPrice);
            bookingItemRepository.save(item);

            TicketReservation reservation = bookingConverter.toTicketReservationEntity(booking, request, expiresAt);
            ticketReservationRepository.save(reservation);

            // ==========================================
            // 5. BẮN EVENT KAFKA HỦY ĐƠN & HOÀN KHO
            // ==========================================
            String kafkaPayload = "";
            try {
                Map<String, Object> eventPayload = new java.util.HashMap<>();
                eventPayload.put("bookingCode", bookingCode);
                eventPayload.put("userId", booking.getUserId());
                eventPayload.put("ticketCategoryId", request.getTicketCategoryId());
                eventPayload.put("quantity", request.getQuantity());
                eventPayload.put("totalAmount", booking.getTotalAmount());
                eventPayload.put("finalAmount", booking.getFinalAmount());
                eventPayload.put("status", "WAITING_PAYMENT");
                eventPayload.put("paymentDueAt", expiresAt.toString());
                eventPayload.put("paymentDueAtEpoch", expiresAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
                eventPayload.put("contactEmail", booking.getContactEmail());
                eventPayload.put("contactName", booking.getContactName());
                kafkaPayload = objectMapper.writeValueAsString(eventPayload);
            } catch (Exception e) {
                log.error("Lỗi serialize event payload: {}", e.getMessage());
                kafkaPayload = String.format(
                        "{\"bookingCode\": \"%s\", \"ticketCategoryId\": %d, \"quantity\": %d, \"status\": \"WAITING_PAYMENT\", \"paymentDueAt\": \"%s\"}",
                        bookingCode, request.getTicketCategoryId(), request.getQuantity(), expiresAt
                );
            }
            kafkaTemplate.send("booking-events-topic", kafkaPayload);
            log.info("[Kafka Producer] Bắn event giữ chỗ 10 phút cho đơn hàng: {}", bookingCode);

            return bookingConverter.toResponseDto(booking);

        } catch (InterruptedException e) {
            compensateBookingFailure(request, seatsReserved, inventoryDeducted);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lỗi xử lý luồng.");
        } catch (Exception e) {
            compensateBookingFailure(request, seatsReserved, inventoryDeducted);
            throw e;
        } finally {
            if (seatMultiLock != null && isSeatLocked) {
                try {
                    seatMultiLock.unlock();
                } catch (Exception e) {
                    log.warn("Lỗi giải phóng khóa ghế: {}", e.getMessage());
                }
            }
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void compensateBookingFailure(BookingRequest request, boolean seatsReserved, boolean inventoryDeducted) {
        if (inventoryDeducted) {
            try {
                coreServiceClient.refundQuantity(request.getTicketCategoryId(), request.getQuantity());
                String redisKey = "inventory:ticket:" + request.getTicketCategoryId();
                redisTemplate.opsForValue().increment(redisKey, request.getQuantity());
            } catch (Exception e) {
                log.error("[Saga Compensation] Lỗi hoàn kho vé: {}", e.getMessage());
            }
        }
        if (seatsReserved && request.getSeatIds() != null && !request.getSeatIds().isEmpty()) {
            try {
                coreServiceClient.releaseSeats(request.getSeatIds());
            } catch (Exception e) {
                log.error("[Saga Compensation] Lỗi hoàn trả ghế: {}", e.getMessage());
            }
        }
    }

    // --- CÁC HÀM HỖ TRỢ XỬ LÝ KHO QUA OPENFEIGN ---

    private int checkInventory(Long ticketCategoryId) {
        String redisKey = "inventory:ticket:" + ticketCategoryId;
        String cachedQty = redisTemplate.opsForValue().get(redisKey);

        if (cachedQty != null) {
            return Integer.parseInt(cachedQty);
        }

        // Gọi OpenFeign (lúc này trả về String)
        String response = coreServiceClient.getAvailableQuantity(ticketCategoryId);

        // Parse String thành Integer an toàn
        Integer dbQty = 0;
        if (response != null && !response.trim().isEmpty()) {
            try {
                dbQty = Integer.parseInt(response.trim());
            } catch (NumberFormatException e) {
                log.error("Lỗi parse số lượng vé từ core-service. Data nhận được: {}", response);
                throw new RuntimeException("Dữ liệu kho vé không hợp lệ!");
            }
        }

        redisTemplate.opsForValue().set(redisKey, String.valueOf(dbQty));
        return dbQty;
    }

    private void deductInventory(Long ticketCategoryId, int quantity) {
        try {
            // Gọi HTTP sang Core Service để thực hiện transaction trừ DB
            coreServiceClient.deductQuantity(ticketCategoryId, quantity);

            // Core xử lý OK thì Booking mới trừ Redis Cache
            String redisKey = "inventory:ticket:" + ticketCategoryId;
            redisTemplate.opsForValue().decrement(redisKey, quantity);
            log.info("Đã trừ {} vé khỏi kho cho hạng vé {}", quantity, ticketCategoryId);
        } catch (Exception e) {
            log.error("Lỗi khi gọi Core Service để trừ kho: {}", e.getMessage());
            throw new RuntimeException("Trừ kho DB thất bại (có thể do đã hết vé)!");
        }
    }
    private void applyVoucher(BookingRequest request, Booking booking) {
        if (request.getVoucherCode() == null || request.getVoucherCode().trim().isEmpty()) {
            booking.setFinalAmount(booking.getTotalAmount()); // Không có mã -> Trả đủ
            return;
        }

        String voucherCode = request.getVoucherCode().trim();

        // 1. CHỐNG SPAMMING (Chuyển Enum status sang List<String> để khớp với Native Query)
        List<String> statusStrings = List.of(
                BookingStatus.PAID.name(),
                BookingStatus.WAITING_PAYMENT.name()
        );

        boolean hasUsed = bookingRepository.existsByUserIdAndVoucherCodeAndStatusIn(
                request.getUserId(),
                voucherCode,
                statusStrings
        );
        if (hasUsed) {
            throw new RuntimeException("Bạn đã sử dụng mã giảm giá này rồi!");
        }

        // 2. GỌI CORE-SERVICE ĐỂ THẨM ĐỊNH & TRỪ KHO VOUCHER
        try {
            BigDecimal discountAmount = coreServiceClient.applyAndDeductVoucher(
                    voucherCode,
                    booking.getTotalAmount()
            );

            // 3. TÍNH TOÁN LẠI FINAL AMOUNT TẠI BACKEND
            BigDecimal finalAmount = booking.getTotalAmount().subtract(discountAmount);
            if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
                finalAmount = BigDecimal.ZERO; // Không cho âm tiền
            }

            // Lưu vết thông tin voucher dưới dạng Map<String, Object> cho JSONB
            Map<String, Object> promoSnapshot = new java.util.HashMap<>();
            promoSnapshot.put("voucherCode", voucherCode);
            promoSnapshot.put("discountAmount", discountAmount);

            booking.setPromotionSnapshot(promoSnapshot);
            booking.setFinalAmount(finalAmount);

            log.info("Áp dụng mã {} thành công. Giảm {} VNĐ", voucherCode, discountAmount);

        } catch (feign.FeignException e) {
            String errorMsg = e.contentUTF8();
            log.error("Lỗi Feign khi áp dụng voucher: {}", errorMsg);
            throw new RuntimeException("Mã giảm giá lỗi: " + (errorMsg != null && !errorMsg.isEmpty() ? errorMsg : e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi áp dụng voucher: {}", e.getMessage());
            throw new RuntimeException("Mã giảm giá không hợp lệ, chưa đạt điều kiện hoặc đã hết lượt sử dụng! Chi tiết: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<BookingResponse> getBookingsByUserId(Long userId) {
        return bookingRepository.findByUserId(userId).stream()
                .map(bookingConverter::toResponseDto)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional
    public BookingResponse updateBookingStatus(Long bookingId, String statusStr) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt vé với ID: " + bookingId));

        BookingStatus oldStatus = booking.getStatus();
        BookingStatus newStatus = BookingStatus.valueOf(statusStr.toUpperCase());

        if (oldStatus == newStatus) {
            return bookingConverter.toResponseDto(booking);
        }

        booking.setStatus(newStatus);
        Booking saved = bookingRepository.save(booking);

        java.util.List<Long> seatIds = parseSeatIds(booking.getSeatIds());

        // Lấy ticketCategoryId và quantity từ bookingItems
        BookingItem item = booking.getBookingItems().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Đơn đặt vé không hợp lệ (không tìm thấy thông tin hạng vé)!"));
        Long ticketCategoryId = item.getTicketCategoryId();
        int quantity = item.getQuantity();

        if (newStatus == BookingStatus.PAID) {
            // Xác nhận ghế thành BOOKED
            if (!seatIds.isEmpty()) {
                try {
                    coreServiceClient.confirmSeats(seatIds);
                } catch (Exception e) {
                    log.error("Lỗi khi xác nhận ghế sang BOOKED: {}", e.getMessage());
                }
            }
        } else if (newStatus == BookingStatus.CANCELLED || newStatus == BookingStatus.EXPIRED) {
            // Hủy/Hết hạn -> Giải phóng ghế và hoàn trả kho vé (nếu trạng thái cũ là WAITING_PAYMENT)
            if (oldStatus == BookingStatus.WAITING_PAYMENT) {
                if (!seatIds.isEmpty()) {
                    try {
                        coreServiceClient.releaseSeats(seatIds);
                    } catch (Exception e) {
                        log.error("Lỗi khi giải phóng ghế: {}", e.getMessage());
                    }
                }
                try {
                    coreServiceClient.refundQuantity(ticketCategoryId, quantity);
                    String redisKey = "inventory:ticket:" + ticketCategoryId;
                    redisTemplate.opsForValue().increment(redisKey, quantity);
                } catch (Exception e) {
                    log.error("Lỗi khi hoàn kho vé: {}", e.getMessage());
                }
            }
        }

        return bookingConverter.toResponseDto(saved);
    }

    private java.util.List<Long> parseSeatIds(String seatIdsStr) {
        if (seatIdsStr == null || seatIdsStr.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return java.util.Arrays.stream(seatIdsStr.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .collect(java.util.stream.Collectors.toList());
    }
}