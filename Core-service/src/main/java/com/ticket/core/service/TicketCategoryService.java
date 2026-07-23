package com.ticket.core.service;

import com.ticket.core.dto.request.TicketCategoryRequest;
import com.ticket.core.dto.response.TicketCategoryResponse;

import java.math.BigDecimal;
import java.util.List;

public interface TicketCategoryService {
    Integer getAvailableQuantity(Long id);
    void deductQuantity(Long id, int quantity);
    void refundQuantity(Long id, int quantity);
    BigDecimal getPrice(Long id);

    TicketCategoryResponse createTicketCategory(TicketCategoryRequest request);
    TicketCategoryResponse getTicketCategoryById(Long id);
    List<TicketCategoryResponse> getAllTicketCategories();
    TicketCategoryResponse updateTicketCategory(Long id, TicketCategoryRequest request);
    void deleteTicketCategory(Long id);
}