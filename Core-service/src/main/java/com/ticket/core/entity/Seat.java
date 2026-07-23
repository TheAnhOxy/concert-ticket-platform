package com.ticket.core.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_category_id", nullable = false)
    private Long ticketCategoryId;

    @Column(name = "seat_number", nullable = false, length = 20)
    private String seatNumber;

    @Column(nullable = false, length = 20)
    private String status; // AVAILABLE, RESERVED, BOOKED
}
