package com.ticket.payment.controller;

import com.ticket.payment.dto.response.ApiResponse;
import com.ticket.payment.entity.Payment;
import com.ticket.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/pay")
    public ResponseEntity<ApiResponse> pay(
            @RequestParam("bookingReference") String bookingReference,
            @RequestParam("status") String status) {
        Payment payment = paymentService.handleMockPayment(bookingReference, status);
        
        ApiResponse apiResponse = ApiResponse.builder()
                .status(200)
                .message("Xử lý thanh toán đơn đặt giữ chỗ thành công!")
                .data(payment)
                .build();
                
        return ResponseEntity.ok(apiResponse);
    }
}
