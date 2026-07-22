package com.ticket.payment.service;

import com.ticket.payment.entity.Payment;

public interface PaymentService {
    void processBookingCreatedEvent(String eventMessage);
    Payment handleMockPayment(String bookingReference, String paymentStatus);
}
