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
            String paymentDueAtStr = node.get("paymentDueAt").asText();

            LocalDateTime paymentDueAt = LocalDateTime.parse(paymentDueAtStr);
            Instant executionTime = paymentDueAt.atZone(ZoneId.systemDefault()).toInstant();

            taskScheduler.schedule(() -> expireBookingAndRefundInventory(bookingCode, ticketCategoryId, quantity), executionTime);

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

            log.warn("[AUTO-CANCEL] Đơn hàng {} đã quá hạn 1 phút. Gọi Core Service hoàn {} vé thành công!", bookingCode, quantity);
        }
    }
}