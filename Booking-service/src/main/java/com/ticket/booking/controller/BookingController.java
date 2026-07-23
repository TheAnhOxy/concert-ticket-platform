package com.ticket.booking.controller;

import com.ticket.booking.dto.request.BookingRequest;
import com.ticket.booking.dto.response.BookingResponse;
import com.ticket.booking.service.BookingService;
// Giả định bạn có class ApiResponse chung trong project, nếu chưa có bạn có thể tự định nghĩa hoặc dùng chung gói
import com.ticket.booking.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        BookingResponse response = bookingService.createBooking(request);

        ApiResponse apiResponse = ApiResponse.builder()
                .status(HttpStatus.CREATED.value())
                .message("Giữ chỗ thành công! Vui lòng thanh toán trong vòng 10 phút.")
                .data(response)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse> getBookingsByUserId(@PathVariable Long userId) {
        java.util.List<BookingResponse> response = bookingService.getBookingsByUserId(userId);
        ApiResponse apiResponse = ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách đơn đặt vé thành công!")
                .data(response)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @PutMapping("/{bookingId}/status")
    public ResponseEntity<ApiResponse> updateBookingStatus(
            @PathVariable Long bookingId,
            @RequestParam("status") String status) {
        BookingResponse response = bookingService.updateBookingStatus(bookingId, status);
        ApiResponse apiResponse = ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Cập nhật trạng thái đơn hàng thành công!")
                .data(response)
                .build();
        return ResponseEntity.ok(apiResponse);
    }
}