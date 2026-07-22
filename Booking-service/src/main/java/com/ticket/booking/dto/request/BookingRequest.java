package com.ticket.booking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BookingRequest {
    @NotNull(message = "User ID không được để trống")
    private Long userId;

    @NotNull(message = "Concert ID không được để trống")
    private Long concertId;

    private String voucherCode;

    private Long voucherId;

    @NotNull(message = "Ticket Category ID không được để trống")
    private Long ticketCategoryId;

    @NotNull(message = "Số lượng vé không được để trống")
    @Min(value = 1, message = "Số lượng vé mua tối thiểu là 1")
    private Integer quantity;

    @NotNull(message = "Đơn giá không được để trống")
    private BigDecimal unitPrice;

    @NotNull(message = "Tổng tiền chưa giảm không được để trống")
    private BigDecimal totalAmount;

    private BigDecimal discountAmount = BigDecimal.ZERO;

    @NotNull(message = "Số tiền thanh toán cuối cùng không được để trống")
    private BigDecimal finalAmount;

    @NotBlank(message = "Idempotency Key là bắt buộc để chống duplicate request")
    private String idempotencyKey;

    // --- BỔ SUNG CÁC TRƯỜNG THÔNG TIN LIÊN HỆ VÀ THANH TOÁN ---
    @NotBlank(message = "Tên người liên hệ không được để trống")
    private String contactName;

    @NotBlank(message = "Email liên hệ không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String contactEmail;

    @NotBlank(message = "Số điện thoại liên hệ không được để trống")
    private String contactPhone;

    private String paymentMethod; // VD: "MOCK", "VNPAY", "MOMO"
    private List<Long> seatIds;
}