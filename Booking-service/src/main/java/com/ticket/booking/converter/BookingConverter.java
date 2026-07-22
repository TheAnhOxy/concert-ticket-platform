package com.ticket.booking.converter;

import com.ticket.booking.dto.request.BookingRequest;
import com.ticket.booking.dto.response.BookingResponse;
import com.ticket.booking.entity.Booking;
import com.ticket.booking.entity.BookingItem;
import com.ticket.booking.entity.TicketReservation;
import com.ticket.booking.enums.BookingStatus;
import com.ticket.booking.enums.ReservationStatus;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class BookingConverter {

    private final ModelMapper modelMapper;

    public BookingResponse toResponseDto(Booking booking) {
        if (booking == null) {
            return null;
        }

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())
                .userId(booking.getUserId())
                .concertId(booking.getConcertId())
                .totalAmount(booking.getTotalAmount())
                .paidAmount(booking.getPaidAmount())
                .status(booking.getStatus() != null ? booking.getStatus().name() : null)
                .paymentMethod(booking.getPaymentMethod())
                .paymentDueAt(booking.getPaymentDueAt())
                .contactName(booking.getContactName())
                .contactEmail(booking.getContactEmail())
                .contactPhone(booking.getContactPhone())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    public Booking toBookingEntity(BookingRequest request, String bookingCode, LocalDateTime expiresAt) {
        return Booking.builder()
                .bookingCode(bookingCode)
                .idempotencyKey(request.getIdempotencyKey())
                .userId(request.getUserId())
                .concertId(request.getConcertId())
                .totalAmount(request.getTotalAmount())
                .finalAmount(request.getFinalAmount())
                .paidAmount(BigDecimal.ZERO)
                .status(BookingStatus.WAITING_PAYMENT)
                .paymentMethod(request.getPaymentMethod())
                .paymentDueAt(expiresAt)
                .expiresAt(expiresAt) // <--- Gán thêm giá trị này để khớp cột expires_at trong DB
                .contactName(request.getContactName())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .build();
    }

    public BookingItem toBookingItemEntity(Booking booking, BookingRequest request) {
        return BookingItem.builder()
                .booking(booking)
                .ticketCategoryId(request.getTicketCategoryId())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .subtotal(request.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity())))
                .build();
    }

    public TicketReservation toTicketReservationEntity(Booking booking, BookingRequest request, LocalDateTime expiresAt) {
        return TicketReservation.builder()
                .booking(booking)
                .ticketCategoryId(request.getTicketCategoryId())
                .quantity(request.getQuantity())
                .reservedUntil(expiresAt)
                .status(ReservationStatus.RESERVED)
                .build();
    }
}