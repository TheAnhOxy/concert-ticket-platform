package com.ticket.booking.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BookingResponse {
    private Long id;
    private String bookingCode;      // Đã sửa từ bookingReference thành bookingCode
    private Long userId;
    private Long concertId;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private BigDecimal paidAmount;
    private String status;           // Trạng thái đơn hàng (đưa về String hoặc giữ enum tùy ý)
    private List<Long> seatIds;
    private String paymentMethod;
    private LocalDateTime paymentDueAt;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private LocalDateTime createdAt;
}