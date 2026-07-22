package com.ticket.payment.kafka;

import com.ticket.payment.service.PaymentService;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final PaymentService paymentService;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = "-dlt"
    )
    @KafkaListener(topics = "booking-events-topic", groupId = "payment-group")
    public void consumeBookingEvent(String message) {
        log.info("[Payment Consumer] Nhận sự kiện đặt vé từ Kafka: {}", message);
        paymentService.processBookingCreatedEvent(message);
    }

    @DltHandler
    public void handleDltMessage(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("[Payment Consumer DLQ] LỖI NGHIÊM TRỌNG - Tin nhắn chuyển vào DLQ của topic {}. Nội dung: {}", topic, message);
    }
}
