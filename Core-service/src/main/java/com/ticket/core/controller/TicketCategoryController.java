package com.ticket.core.controller;

import com.ticket.core.service.TicketCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ticket-categories")
@RequiredArgsConstructor
public class TicketCategoryController {

    private final TicketCategoryService ticketCategoryService;

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
}