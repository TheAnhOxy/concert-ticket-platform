package com.ticket.core.controller;

import com.ticket.core.dto.request.TicketCategoryRequest;
import com.ticket.core.dto.response.ApiResponse;
import com.ticket.core.dto.response.TicketCategoryResponse;
import com.ticket.core.service.TicketCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/ticket-categories")
@RequiredArgsConstructor
public class TicketCategoryController {

    private final TicketCategoryService ticketCategoryService;

    // =========================================================================
    // INTERNAL MICROSERVICE COMMUNICATION ENDPOINTS (Do NOT wrap in ApiResponse)
    // =========================================================================

    // 1. API lấy số lượng tồn kho
    @GetMapping("/{id}/available-quantity")
    public ResponseEntity<Integer> getAvailableQuantity(@PathVariable("id") Long id) {
        Integer quantity = ticketCategoryService.getAvailableQuantity(id);
        return ResponseEntity.ok(quantity);
    }

    // 2. API trừ kho
    @PostMapping("/{id}/deduct")
    public ResponseEntity<String> deductQuantity(
            @PathVariable("id") Long id,
            @RequestParam("quantity") int quantity) {

        ticketCategoryService.deductQuantity(id, quantity);
        return ResponseEntity.ok("Trừ kho thành công");
    }

    // 3. API hoàn kho
    @PostMapping("/{id}/refund")
    public ResponseEntity<String> refundQuantity(
            @PathVariable("id") Long id,
            @RequestParam("quantity") int quantity) {

        ticketCategoryService.refundQuantity(id, quantity);
        return ResponseEntity.ok("Hoàn vé thành công");
    }

    // 4. API lấy giá vé
    @GetMapping("/{id}/price")
    public ResponseEntity<BigDecimal> getPrice(@PathVariable("id") Long id) {
        BigDecimal price = ticketCategoryService.getPrice(id);
        return ResponseEntity.ok(price);
    }

    // =========================================================================
    // CRUD PUBLIC/OPERATIONAL APIS (Wrap in ApiResponse)
    // =========================================================================

    // CRUD 5. Tạo Ticket Category
    @PostMapping
    public ResponseEntity<ApiResponse> createTicketCategory(@RequestBody TicketCategoryRequest request) {
        TicketCategoryResponse response = ticketCategoryService.createTicketCategory(request);
        ApiResponse apiResponse = ApiResponse.builder()
                .status(HttpStatus.CREATED.value())
                .message("Tạo Hạng vé thành công!")
                .data(response)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    // CRUD 6. Lấy Ticket Category theo ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getTicketCategoryById(@PathVariable Long id) {
        TicketCategoryResponse response = ticketCategoryService.getTicketCategoryById(id);
        ApiResponse apiResponse = ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Lấy chi tiết Hạng vé thành công!")
                .data(response)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    // CRUD 7. Lấy danh sách Ticket Category
    @GetMapping
    public ResponseEntity<ApiResponse> getAllTicketCategories() {
        List<TicketCategoryResponse> response = ticketCategoryService.getAllTicketCategories();
        ApiResponse apiResponse = ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách Hạng vé thành công!")
                .data(response)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    // CRUD 8. Cập nhật Ticket Category
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateTicketCategory(@PathVariable Long id, @RequestBody TicketCategoryRequest request) {
        TicketCategoryResponse response = ticketCategoryService.updateTicketCategory(id, request);
        ApiResponse apiResponse = ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Cập nhật Hạng vé thành công!")
                .data(response)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    // CRUD 9. Xóa Ticket Category
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteTicketCategory(@PathVariable Long id) {
        ticketCategoryService.deleteTicketCategory(id);
        ApiResponse apiResponse = ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Xóa Hạng vé thành công!")
                .build();
        return ResponseEntity.ok(apiResponse);
    }
}