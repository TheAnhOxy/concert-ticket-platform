package com.ticket.payment.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.payment.entity.Payment;
import com.ticket.payment.enums.PaymentStatus;
import com.ticket.payment.repository.PaymentRepository;
import com.ticket.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public void processBookingCreatedEvent(String eventMessage) {
        log.info("[Payment Service] Nhận thông điệp giữ chỗ từ Kafka: {}", eventMessage);
        try {
            JsonNode node = objectMapper.readTree(eventMessage);
            if (node == null || !node.has("bookingCode")) {
                log.warn("[Payment Service] Bỏ qua tin nhắn cũ không có bookingCode: {}", eventMessage);
                return;
            }
            String bookingCode = node.get("bookingCode").asText();
            Long userId = node.has("userId") ? node.get("userId").asLong() : 1L;
            BigDecimal finalAmount = node.has("finalAmount") ? new BigDecimal(node.get("finalAmount").asText()) : BigDecimal.ZERO;

            // Kiểm tra xem đơn hàng đã được tạo Payment chưa (đảm bảo tính idempotent)
            paymentRepository.findByBookingReference(bookingCode).ifPresentOrElse(
                existing -> log.info("[Payment Service] Đơn đặt vé {} đã có bản ghi thanh toán.", bookingCode),
                () -> {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("contactEmail", node.has("contactEmail") ? node.get("contactEmail").asText() : "user@gmail.com");
                    metadata.put("contactName", node.has("contactName") ? node.get("contactName").asText() : "Khách hàng");

                    Payment payment = Payment.builder()
                            .bookingReference(bookingCode)
                            .userId(userId)
                            .amount(finalAmount)
                            .paymentMethod("MOCK")
                            .status(PaymentStatus.PENDING)
                            .callbackData(metadata)
                            .build();
                    paymentRepository.save(payment);
                    log.info("[Payment Service] Tạo mới bản ghi thanh toán PENDING cho đơn hàng: {}", bookingCode);
                }
            );
        } catch (Exception e) {
            log.error("[Payment Service] Lỗi xử lý sự kiện Booking Created: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi xử lý sự kiện Booking Created, sẽ kích hoạt DLQ/Retry", e);
        }
    }

    @Override
    @Transactional
    public Payment handleMockPayment(String bookingReference, String paymentStatus) {
        log.info("[Payment Service] Xử lý thanh toán Mock cho đơn: {}, Trạng thái: {}", bookingReference, paymentStatus);
        
        Payment payment = paymentRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bản ghi thanh toán của đơn hàng: " + bookingReference));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("[Payment Service] Đơn hàng {} đã hoàn thành thanh toán trước đó với trạng thái: {}", bookingReference, payment.getStatus());
            return payment;
        }

        Map<String, Object> existingMetadata = payment.getCallbackData();
        String contactEmail = existingMetadata != null && existingMetadata.containsKey("contactEmail") ? (String) existingMetadata.get("contactEmail") : "user@gmail.com";
        String contactName = existingMetadata != null && existingMetadata.containsKey("contactName") ? (String) existingMetadata.get("contactName") : "Khách hàng";

        PaymentStatus targetStatus = "SUCCESS".equalsIgnoreCase(paymentStatus) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
        payment.setStatus(targetStatus);
        payment.setTransactionId("TXN-MOCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        
        Map<String, Object> callbackMap = new HashMap<>();
        callbackMap.put("paymentMethod", "MOCK");
        callbackMap.put("status", targetStatus.name());
        callbackMap.put("processedAt", java.time.LocalDateTime.now().toString());
        callbackMap.put("contactEmail", contactEmail);
        callbackMap.put("contactName", contactName);
        payment.setCallbackData(callbackMap);

        Payment saved = paymentRepository.save(payment);

        // Bắn sự kiện thanh toán lên Kafka cho các service khác lắng nghe
        try {
            Map<String, Object> paymentEvent = new HashMap<>();
            paymentEvent.put("bookingCode", bookingReference);
            paymentEvent.put("status", targetStatus.name());
            paymentEvent.put("transactionId", payment.getTransactionId());
            paymentEvent.put("amount", payment.getAmount());
            paymentEvent.put("contactEmail", contactEmail);
            paymentEvent.put("contactName", contactName);
            paymentEvent.put("userId", payment.getUserId());

            String message = objectMapper.writeValueAsString(paymentEvent);
            kafkaTemplate.send("payment-events-topic", message);
            log.info("[Payment Service] Bắn sự kiện thanh toán thành công lên Kafka cho đơn: {}", bookingReference);
        } catch (Exception e) {
            log.error("[Payment Service] Lỗi bắn sự kiện thanh toán lên Kafka: {}", e.getMessage());
            throw new RuntimeException("Lỗi truyền tin thanh toán qua Kafka", e);
        }

        return saved;
    }
}
