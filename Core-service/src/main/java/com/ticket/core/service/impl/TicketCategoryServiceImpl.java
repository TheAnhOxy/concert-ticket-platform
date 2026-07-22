package com.ticket.core.service.impl;

import com.ticket.core.repository.TicketCategoryRepository;
import com.ticket.core.service.TicketCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketCategoryServiceImpl implements TicketCategoryService {

    private final TicketCategoryRepository ticketCategoryRepository;

    @Override
    public Integer getAvailableQuantity(Long id) {
        Integer qty = ticketCategoryRepository.getAvailableQuantity(id);
        return qty != null ? qty : 0;
    }

    @Override
    @Transactional
    public void deductQuantity(Long id, int quantity) {
        int updatedRows = ticketCategoryRepository.deductQuantity(id, quantity);
        if (updatedRows == 0) {
            // Nếu updatedRows == 0 nghĩa là không tìm thấy ID hoặc availableQuantity < quantity
            log.error("Không đủ vé trong kho hoặc không tìm thấy TicketCategory ID: {}", id);
            throw new RuntimeException("Số lượng vé không đủ để thực hiện giao dịch!");
        }
        log.info("Đã trừ {} vé cho TicketCategory ID: {}", quantity, id);
    }

    @Override
    @Transactional
    public void refundQuantity(Long id, int quantity) {
        ticketCategoryRepository.refundQuantity(id, quantity);
        log.info("Đã hoàn {} vé cho TicketCategory ID: {}", quantity, id);
    }
}