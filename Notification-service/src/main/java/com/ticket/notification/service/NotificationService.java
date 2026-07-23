package com.ticket.notification.service;

import java.math.BigDecimal;

public interface NotificationService {
    void sendBookingCreatedNotification(String bookingCode, Long userId, String contactEmail, String contactName, BigDecimal amount);
    void sendPaymentSuccessNotification(String bookingCode, Long userId, String contactEmail, String contactName, BigDecimal amount);
    void sendPaymentFailedNotification(String bookingCode, Long userId, String contactEmail, String contactName, BigDecimal amount);
}
