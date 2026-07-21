package com.ticket.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "voucher_reservations")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class VoucherReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "voucher_id", nullable = false)
    private Long voucherId; // Tham chiếu logic sang Core Service

    @Column(nullable = false)
    private LocalDateTime reservedUntil;

    @Column(nullable = false, length = 20)
    private String status; // RESERVED, EXPIRED, CONFIRMED

    @CreationTimestamp
    private LocalDateTime createdAt;
}