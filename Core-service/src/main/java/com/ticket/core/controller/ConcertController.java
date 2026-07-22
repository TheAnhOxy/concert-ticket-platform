package com.ticket.core.controller;

import com.ticket.core.dto.request.ConcertRequest;
import com.ticket.core.dto.response.ApiResponse;
import com.ticket.core.dto.response.ConcertResponse;
import com.ticket.core.service.ConcertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/concerts")
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertService concertService;

    @PostMapping
    public ResponseEntity<ApiResponse> createConcert(@RequestBody ConcertRequest request) {
        ConcertResponse response = concertService.createConcert(request);
        ApiResponse apiResponse = ApiResponse.builder()
                .status(HttpStatus.CREATED.value())
                .message("Tạo Concert thành công!")
                .data(response)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getConcertById(@PathVariable Long id) {
        ConcertResponse response = concertService.getConcertById(id);
        ApiResponse apiResponse = ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Lấy Concert thành công!")
                .data(response)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getAllConcerts() {
        List<ConcertResponse> response = concertService.getAllConcerts();
        ApiResponse apiResponse = ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách Concert thành công!")
                .data(response)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateConcert(@PathVariable Long id, @RequestBody ConcertRequest request) {
        ConcertResponse response = concertService.updateConcert(id, request);
        ApiResponse apiResponse = ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Cập nhật Concert thành công!")
                .data(response)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteConcert(@PathVariable Long id) {
        concertService.deleteConcert(id);
        ApiResponse apiResponse = ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Xóa Concert thành công!")
                .build();
        return ResponseEntity.ok(apiResponse);
    }
}
