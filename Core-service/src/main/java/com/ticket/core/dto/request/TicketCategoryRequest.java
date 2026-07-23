package com.ticket.core.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TicketCategoryRequest {
    private Long concertId;
    private String name;
    private BigDecimal price;
    private Integer totalQuantity;
    private String status; // ACTIVE, INACTIVE
    private java.util.List<String> seatNumbers;
}
