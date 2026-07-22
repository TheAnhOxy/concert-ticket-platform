package com.ticket.booking.service.impl;

import com.ticket.booking.client.CoreServiceClient;
import com.ticket.booking.converter.BookingConverter;
import com.ticket.booking.dto.request.BookingRequest;
import com.ticket.booking.dto.response.BookingResponse;
import com.ticket.booking.entity.Booking;
import com.ticket.booking.entity.BookingItem;
import com.ticket.booking.entity.TicketReservation;
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

import java.time.LocalDateTime;
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

        // ==========================================
        // 2. REDISSON LOCK (Chống System instability & Overselling)
        // ==========================================
        String lockKey = "lock:ticket_category:" + request.getTicketCategoryId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(3, 5, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new RuntimeException("Hệ thống đang xử lý lượng lớn giao dịch, vui lòng thử lại sau!");
            }

            // ==========================================
            // 3. FAST-CHECK TỒN KHO REDIS & TRỪ DB CORE (Chống Overselling)
            // ==========================================
            int availableQuantity = checkInventory(request.getTicketCategoryId());
            if (availableQuantity < request.getQuantity()) {
                throw new RuntimeException("Rất tiếc, số lượng vé trong kho không đủ đáp ứng!");
            }

            deductInventory(request.getTicketCategoryId(), request.getQuantity());

            // ==========================================
            // 4. KHỞI TẠO ĐƠN HÀNG (TIMEOUT 10 PHÚT)
            // ==========================================
            String bookingCode = "BKG-" + System.currentTimeMillis();
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(2);

            Booking booking = bookingConverter.toBookingEntity(request, bookingCode, expiresAt);
            bookingRepository.save(booking);

            BookingItem item = bookingConverter.toBookingItemEntity(booking, request);
            bookingItemRepository.save(item);

            TicketReservation reservation = bookingConverter.toTicketReservationEntity(booking, request, expiresAt);
            ticketReservationRepository.save(reservation);

            // ==========================================
            // 5. BẮN EVENT KAFKA HỦY ĐƠN & HOÀN KHO
            // ==========================================
            String kafkaPayload = String.format(
                    "{\"bookingCode\": \"%s\", \"ticketCategoryId\": %d, \"quantity\": %d, \"status\": \"WAITING_PAYMENT\", \"paymentDueAt\": \"%s\"}",
                    bookingCode, request.getTicketCategoryId(), request.getQuantity(), expiresAt
            );
            kafkaTemplate.send("booking-events-topic", kafkaPayload);
            log.info("[Kafka Producer] Bắn event giữ chỗ 10 phút cho đơn hàng: {}", bookingCode);

            return bookingConverter.toResponseDto(booking);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lỗi xử lý luồng.");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
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
}