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

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (booking.getPromotionSnapshot() != null) {
            Object discount = booking.getPromotionSnapshot().get("discountAmount");
            if (discount != null) {
                if (discount instanceof BigDecimal) {
                    discountAmount = (BigDecimal) discount;
                } else {
                    try {
                        discountAmount = new BigDecimal(discount.toString());
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }

        java.util.List<Long> seatIdsList = null;
        if (booking.getSeatIds() != null && !booking.getSeatIds().trim().isEmpty()) {
            seatIdsList = java.util.Arrays.stream(booking.getSeatIds().split(","))
                    .map(Long::parseLong)
                    .collect(java.util.stream.Collectors.toList());
        }

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())
                .userId(booking.getUserId())
                .concertId(booking.getConcertId())
                .totalAmount(booking.getTotalAmount())
                .discountAmount(discountAmount)
                .finalAmount(booking.getFinalAmount())
                .paidAmount(booking.getPaidAmount())
                .status(booking.getStatus() != null ? booking.getStatus().name() : null)
                .seatIds(seatIdsList)
                .paymentMethod(booking.getPaymentMethod())
                .paymentDueAt(booking.getPaymentDueAt())
                .contactName(booking.getContactName())
                .contactEmail(booking.getContactEmail())
                .contactPhone(booking.getContactPhone())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    public Booking toBookingEntity(BookingRequest request, String bookingCode, BigDecimal totalAmount, BigDecimal finalAmount, LocalDateTime expiresAt) {
        String seatIdsStr = null;
        if (request.getSeatIds() != null && !request.getSeatIds().isEmpty()) {
            seatIdsStr = request.getSeatIds().stream()
                    .map(Object::toString)
                    .collect(java.util.stream.Collectors.joining(","));
        }

        return Booking.builder()
                .bookingCode(bookingCode)
                .idempotencyKey(request.getIdempotencyKey())
                .userId(request.getUserId())
                .concertId(request.getConcertId())
                .totalAmount(totalAmount)
                .finalAmount(finalAmount)
                .paidAmount(BigDecimal.ZERO)
                .status(BookingStatus.WAITING_PAYMENT)
                .seatIds(seatIdsStr)
                .paymentMethod(request.getPaymentMethod())
                .paymentDueAt(expiresAt)
                .expiresAt(expiresAt) // <--- Gán thêm giá trị này để khớp cột expires_at trong DB
                .contactName(request.getContactName())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .build();
    }

    public BookingItem toBookingItemEntity(Booking booking, BookingRequest request, BigDecimal unitPrice) {
        return BookingItem.builder()
                .booking(booking)
                .ticketCategoryId(request.getTicketCategoryId())
                .quantity(request.getQuantity())
                .unitPrice(unitPrice)
                .subtotal(unitPrice.multiply(BigDecimal.valueOf(request.getQuantity())))
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