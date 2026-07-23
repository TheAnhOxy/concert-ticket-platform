package com.ticket.booking.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.booking.client.CoreServiceClient;
import com.ticket.booking.entity.Booking;
import com.ticket.booking.enums.BookingStatus;
import com.ticket.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

    private final BookingRepository bookingRepository;
    private final CoreServiceClient coreServiceClient; // Dùng Feign thay thế
    private final StringRedisTemplate redisTemplate;
    private final TaskScheduler taskScheduler;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "booking-events-topic", groupId = "booking-expiration-group")
    public void consumeBookingEvent(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String bookingCode = node.get("bookingCode").asText();
            Long ticketCategoryId = node.get("ticketCategoryId").asLong();
            int quantity = node.get("quantity").asInt();

            Instant executionTime;
            if (node.has("paymentDueAtEpoch")) {
                executionTime = Instant.ofEpochMilli(node.get("paymentDueAtEpoch").asLong());
            } else {
                String paymentDueAtStr = node.get("paymentDueAt").asText();
                LocalDateTime paymentDueAt = LocalDateTime.parse(paymentDueAtStr);
                executionTime = paymentDueAt.atZone(ZoneId.systemDefault()).toInstant();
            }

            // Nếu thời gian hết hạn đã trôi qua ở quá khứ, ta thực hiện kiểm tra ngay lập tức để dọn dẹp
            if (executionTime.isBefore(Instant.now())) {
                expireBookingAndRefundInventory(bookingCode, ticketCategoryId, quantity);
            } else {
                taskScheduler.schedule(() -> expireBookingAndRefundInventory(bookingCode, ticketCategoryId, quantity), executionTime);
            }

        } catch (Exception e) {
            log.error("[Kafka Consumer] Lỗi xử lý event: {}", e.getMessage());
        }
    }

    @Transactional
    public void expireBookingAndRefundInventory(String bookingCode, Long ticketCategoryId, int quantity) {
        log.info("[Task] Đang kiểm tra trạng thái thanh toán của đơn hàng: {}", bookingCode);

        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (booking.getStatus() == BookingStatus.WAITING_PAYMENT) {
            // 1. Đổi status nội bộ
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);

            // 2. Gọi API sang Core Service hoàn vé vào DB
            try {
                coreServiceClient.refundQuantity(ticketCategoryId, quantity);
            } catch (Exception e) {
                log.error("Lỗi khi gọi Core Service để hoàn vé cho đơn {}: {}", bookingCode, e.getMessage());
                // Ở môi trường thực tế, nếu call API lỗi, bạn cần bắn vào Dead Letter Queue (DLQ) để xử lý đền bù (saga)
            }

            // 3. Hoàn vé trên Redis
            String redisKey = "inventory:ticket:" + ticketCategoryId;
            redisTemplate.opsForValue().increment(redisKey, quantity);

            // 4. Giải phóng ghế trên Core Service
            if (booking.getSeatIds() != null && !booking.getSeatIds().trim().isEmpty()) {
                try {
                    java.util.List<Long> seatIds = java.util.Arrays.stream(booking.getSeatIds().split(","))
                            .map(Long::parseLong)
                            .collect(java.util.stream.Collectors.toList());
                    coreServiceClient.releaseSeats(seatIds);
                } catch (Exception e) {
                    log.error("Lỗi khi gọi Core Service để giải phóng ghế cho đơn {}: {}", bookingCode, e.getMessage());
                }
            }

            log.warn("[AUTO-CANCEL] Đơn hàng {} đã quá hạn 1 phút. Gọi Core Service hoàn {} vé và giải phóng ghế thành công!", bookingCode, quantity);
        }
    }

    @KafkaListener(topics = "payment-events-topic", groupId = "booking-payment-group")
    @Transactional
    public void consumePaymentEvent(String message) {
        log.info("[Kafka Consumer Debug] Nhận tin nhắn thô từ payment-events-topic: {}", message);
        try {
            JsonNode node = objectMapper.readTree(message);
            String bookingCode = node.get("bookingCode").asText();
            String paymentStatus = node.get("status").asText(); // SUCCESS or FAILED

            log.info("[Kafka Consumer] Nhận sự kiện thanh toán cho đơn {}: {}", bookingCode, paymentStatus);

            Booking booking = bookingRepository.findByBookingCode(bookingCode)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt vé: " + bookingCode));

            if (booking.getStatus() == BookingStatus.WAITING_PAYMENT) {
                if ("SUCCESS".equalsIgnoreCase(paymentStatus)) {
                    booking.setStatus(BookingStatus.PAID);
                    booking.setPaidAmount(booking.getFinalAmount());
                    bookingRepository.save(booking);

                    // Xác nhận ghế thành BOOKED dưới Core-service
                    if (booking.getSeatIds() != null && !booking.getSeatIds().trim().isEmpty()) {
                        java.util.List<Long> seatIds = java.util.Arrays.stream(booking.getSeatIds().split(","))
                                .map(Long::parseLong)
                                .collect(java.util.stream.Collectors.toList());
                        coreServiceClient.confirmSeats(seatIds);
                    }
                    log.info("[Payment Consumer] Đơn hàng {} đã được chuyển sang trạng thái PAID và xác nhận ghế!", bookingCode);
                } else {
                    booking.setStatus(BookingStatus.CANCELLED);
                    bookingRepository.save(booking);

                    // Giải phóng ghế
                    if (booking.getSeatIds() != null && !booking.getSeatIds().trim().isEmpty()) {
                        java.util.List<Long> seatIds = java.util.Arrays.stream(booking.getSeatIds().split(","))
                                .map(Long::parseLong)
                                .collect(java.util.stream.Collectors.toList());
                        coreServiceClient.releaseSeats(seatIds);
                    }

                    // Lấy ticketCategoryId và quantity từ bookingItems
                    com.ticket.booking.entity.BookingItem item = booking.getBookingItems().stream().findFirst()
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy hạng vé!"));

                    // Hoàn trả kho
                    coreServiceClient.refundQuantity(item.getTicketCategoryId(), item.getQuantity());
                    String redisKey = "inventory:ticket:" + item.getTicketCategoryId();
                    redisTemplate.opsForValue().increment(redisKey, item.getQuantity());
                    log.warn("[Payment Consumer] Đơn hàng {} thanh toán thất bại. Đã hủy đơn, giải phóng ghế và hoàn kho!", bookingCode);
                }
            }
        } catch (Exception e) {
            log.error("[Kafka Consumer] Lỗi xử lý payment event: {}", e.getMessage());
        }
    }
}