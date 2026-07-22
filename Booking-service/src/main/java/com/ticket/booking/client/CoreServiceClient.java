package com.ticket.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

// Trỏ URL về cổng 8082 của core-service (thay đổi linh hoạt qua config/eureka)
@FeignClient(name = "core-service", url = "${core.service.url:http://localhost:8082}")
public interface CoreServiceClient {

    //  API lấy số lượng tồn kho (để cache lại vào Redis)
    // Lấy số lượng tồn kho (Đổi Integer thành String để tránh lỗi parse JSON primitive)
    @GetMapping("/ticket-categories/{id}/available-quantity")
    String getAvailableQuantity(@PathVariable("id") Long ticketCategoryId);

    //  API trừ kho (Core Service sẽ xử lý Pessimistic Lock dưới DB của nó)
    @PostMapping("/ticket-categories/{id}/deduct")
    void deductQuantity(@PathVariable("id") Long ticketCategoryId, @RequestParam("quantity") int quantity);

    //  hoàn kho (Core Service cộng lại vé khi đơn hết hạn)
    @PostMapping("/ticket-categories/{id}/refund")
    void refundQuantity(@PathVariable("id") Long ticketCategoryId, @RequestParam("quantity") int quantity);
}