package com.ticket.booking.entity;


import com.ticket.booking.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "bookings")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 30)
    private String bookingCode;

    @Column(name = "user_id", nullable = true)
    private Long userId;

    @Column(nullable = false)
    private Long concertId;

    private BigDecimal totalAmount;
    private BigDecimal paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookingStatus status;

    @Column(length = 30)
    private String paymentMethod;

    private LocalDateTime paymentDueAt;

    private String contactName;
    private String contactEmail;
    private String contactPhone;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> priceSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> promotionSnapshot;

    /** Danh sách các vé/hạng vé được đặt trong đơn hàng */
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BookingItem> bookingItems;

    /** Danh sách các giữ chỗ tạm thời cho đơn hàng */
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TicketReservation> ticketReservations;
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<VoucherReservation> voucherReservations;
    @Version
    private Long version;

    @CreationTimestamp
    private LocalDateTime createdAt;
}