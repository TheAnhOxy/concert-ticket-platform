package com.ticket.core.service;

public interface TicketCategoryService {
    Integer getAvailableQuantity(Long id);
    void deductQuantity(Long id, int quantity);
    void refundQuantity(Long id, int quantity);
}