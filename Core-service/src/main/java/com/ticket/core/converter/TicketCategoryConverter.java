package com.ticket.core.converter;

import com.ticket.core.dto.request.TicketCategoryRequest;
import com.ticket.core.dto.response.TicketCategoryResponse;
import com.ticket.core.entity.TicketCategory;
import com.ticket.core.entity.Concert;
import org.springframework.stereotype.Component;

@Component
public class TicketCategoryConverter {

    public TicketCategory toEntity(TicketCategoryRequest request) {
        if (request == null) {
            return null;
        }
        TicketCategory entity = new TicketCategory();
        entity.setName(request.getName());
        entity.setPrice(request.getPrice());
        entity.setTotalQuantity(request.getTotalQuantity());
        entity.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        return entity;
    }

    public TicketCategoryResponse toResponse(TicketCategory entity) {
        if (entity == null) {
            return null;
        }
        TicketCategoryResponse response = new TicketCategoryResponse();
        response.setId(entity.getId());
        if (entity.getConcert() != null) {
            response.setConcertId(entity.getConcert().getId());
        }
        response.setName(entity.getName());
        response.setPrice(entity.getPrice());
        response.setTotalQuantity(entity.getTotalQuantity());
        response.setAvailableQuantity(entity.getAvailableQuantity());
        response.setStatus(entity.getStatus());
        return response;
    }
}
