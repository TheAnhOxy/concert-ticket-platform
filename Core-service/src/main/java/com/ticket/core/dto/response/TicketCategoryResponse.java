package com.ticket.core.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TicketCategoryResponse {
    private Long id;
    private Long concertId;
    private String name;
    private BigDecimal price;
    private Integer totalQuantity;
    private Integer availableQuantity;
    private String status;
}
