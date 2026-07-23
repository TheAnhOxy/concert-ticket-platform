package com.ticket.core.controller;

import com.ticket.core.dto.response.ApiResponse;
import com.ticket.core.entity.Seat;
import com.ticket.core.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    // =========================================================================
    // INTERNAL MICROSERVICE COMMUNICATION ENDPOINTS (Do NOT wrap in ApiResponse)
    // =========================================================================

    @PostMapping("/reserve")
    public ResponseEntity<String> reserveSeats(
            @RequestParam("seatIds") List<Long> seatIds,
            @RequestParam("ticketCategoryId") Long ticketCategoryId) {
        seatService.reserveSeats(seatIds, ticketCategoryId);
        return ResponseEntity.ok("Giữ chỗ các ghế thành công!");
    }

    @PostMapping("/release")
    public ResponseEntity<String> releaseSeats(@RequestParam("seatIds") List<Long> seatIds) {
        seatService.releaseSeats(seatIds);
        return ResponseEntity.ok("Giải phóng các ghế thành công!");
    }

    @PostMapping("/confirm")
    public ResponseEntity<String> confirmSeats(@RequestParam("seatIds") List<Long> seatIds) {
        seatService.confirmSeats(seatIds);
        return ResponseEntity.ok("Xác nhận các ghế thành công!");
    }

    // =========================================================================
    // CRUD PUBLIC/OPERATIONAL APIS (Wrap in ApiResponse)
    // =========================================================================

    @GetMapping("/category/{ticketCategoryId}")
    public ResponseEntity<ApiResponse> getSeatsByCategoryId(@PathVariable Long ticketCategoryId) {
        List<Seat> seats = seatService.getSeatsByCategoryId(ticketCategoryId);
        ApiResponse apiResponse = ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách ghế thành công!")
                .data(seats)
                .build();
        return ResponseEntity.ok(apiResponse);
    }
}
