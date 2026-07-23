package com.ticket.notification.service.impl;

import com.ticket.notification.entity.Notification;
import com.ticket.notification.enums.NotificationType;
import com.ticket.notification.repository.NotificationRepository;
import com.ticket.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void sendBookingCreatedNotification(String bookingCode, Long userId, String contactEmail, String contactName, BigDecimal amount) {
        String title = "Giữ chỗ thành công - Đơn hàng " + bookingCode;
        String message = String.format("Chào %s, bạn đã giữ chỗ thành công cho đơn đặt vé %s. Số tiền cần thanh toán: %s VNĐ. Vui lòng hoàn tất thanh toán trước khi đơn hàng hết hạn.",
                contactName, bookingCode, amount);

        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(NotificationType.BOOKING_SUCCESS)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
        log.info("[Notification Service] Đã gửi thông báo giữ chỗ qua email {} (mô phỏng). Nội dung: {}", contactEmail, message);
    }

    @Override
    @Transactional
    public void sendPaymentSuccessNotification(String bookingCode, Long userId, String contactEmail, String contactName, BigDecimal amount) {
        String title = "Thanh toán thành công - Đơn hàng " + bookingCode;
        String message = String.format("Chúc mừng %s, đơn hàng %s đã được thanh toán thành công số tiền %s VNĐ! Vé của bạn đã được xuất chính thức.",
                contactName, bookingCode, amount);

        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(NotificationType.PAYMENT_SUCCESS)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
        log.info("[Notification Service] Đã gửi thông báo thanh toán thành công qua email {} (mô phỏng). Nội dung: {}", contactEmail, message);
    }

    @Override
    @Transactional
    public void sendPaymentFailedNotification(String bookingCode, Long userId, String contactEmail, String contactName, BigDecimal amount) {
        String title = "Thanh toán thất bại/Hủy bỏ - Đơn hàng " + bookingCode;
        String message = String.format("Chào %s, đơn hàng %s của bạn đã bị hủy bỏ hoặc thanh toán thất bại. Hạng vé và ghế bạn chọn đã được hoàn trả lại hệ thống.",
                contactName, bookingCode);

        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(NotificationType.BOOKING_EXPIRED)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
        log.info("[Notification Service] Đã gửi thông báo hủy/thất bại qua email {} (mô phỏng). Nội dung: {}", contactEmail, message);
    }
}
