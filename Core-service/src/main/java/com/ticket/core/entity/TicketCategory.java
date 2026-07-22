package com.ticket.core.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ticket_categories")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @Column(nullable = false, length = 100)
    private String name; // VIP, Standard...

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    // Số lượng còn lại (Tồn kho thực tế)
    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;
    @Column(nullable = false, length = 20)
    private String status; // ACTIVE, INACTIVE
}