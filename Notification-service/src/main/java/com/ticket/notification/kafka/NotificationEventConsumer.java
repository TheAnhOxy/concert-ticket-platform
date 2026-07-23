package com.ticket.notification.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = "-dlt"
    )
    @KafkaListener(topics = "booking-events-topic", groupId = "notification-group")
    public void consumeBookingEvent(String message) {
        log.info("[Notification Consumer] Nhận sự kiện đặt giữ chỗ từ Kafka: {}", message);
        try {
            JsonNode node = objectMapper.readTree(message);
            if (node == null || !node.has("bookingCode")) {
                log.warn("[Notification Consumer] Bỏ qua tin nhắn cũ không có bookingCode: {}", message);
                return;
            }
            String bookingCode = node.get("bookingCode").asText();
            Long userId = node.has("userId") ? node.get("userId").asLong() : 1L;
            String contactEmail = node.has("contactEmail") ? node.get("contactEmail").asText() : "user@gmail.com";
            String contactName = node.has("contactName") ? node.get("contactName").asText() : "Khách hàng";
            BigDecimal finalAmount = node.has("finalAmount") ? new BigDecimal(node.get("finalAmount").asText()) : BigDecimal.ZERO;

            notificationService.sendBookingCreatedNotification(bookingCode, userId, contactEmail, contactName, finalAmount);
        } catch (Exception e) {
            log.error("[Notification Consumer] Lỗi xử lý sự kiện đặt giữ chỗ: {}", e.getMessage(), e);
            throw new RuntimeException("Kích hoạt retry/DLQ", e);
        }
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = "-dlt"
    )
    @KafkaListener(topics = "payment-events-topic", groupId = "notification-payment-group")
    public void consumePaymentEvent(String message) {
        log.info("[Notification Consumer] Nhận sự kiện thanh toán từ Kafka: {}", message);
        try {
            JsonNode node = objectMapper.readTree(message);
            String bookingCode = node.get("bookingCode").asText();
            String status = node.get("status").asText(); // SUCCESS or FAILED
            BigDecimal amount = new BigDecimal(node.get("amount").asText());
            Long userId = node.get("userId").asLong();
            String contactEmail = node.has("contactEmail") ? node.get("contactEmail").asText() : "user@gmail.com";
            String contactName = node.has("contactName") ? node.get("contactName").asText() : "Khách hàng";

            if ("SUCCESS".equalsIgnoreCase(status)) {
                notificationService.sendPaymentSuccessNotification(bookingCode, userId, contactEmail, contactName, amount);
            } else {
                notificationService.sendPaymentFailedNotification(bookingCode, userId, contactEmail, contactName, amount);
            }
        } catch (Exception e) {
            log.error("[Notification Consumer] Lỗi xử lý sự kiện thanh toán: {}", e.getMessage());
            throw new RuntimeException("Kích hoạt retry/DLQ", e);
        }
    }

    @DltHandler
    public void handleDltMessage(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("[Notification Consumer DLQ] Nhận tin nhắn lỗi từ DLQ của topic {}. Nội dung: {}", topic, message);
    }
}
