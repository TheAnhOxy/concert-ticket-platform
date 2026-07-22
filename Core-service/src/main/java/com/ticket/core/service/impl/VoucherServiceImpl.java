package com.ticket.core.service.impl;

import com.ticket.core.entity.Voucher;
import com.ticket.core.enums.DiscountType;
import com.ticket.core.repository.VoucherRepository;
import com.ticket.core.service.VoucherService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;

    @Override
    @Transactional
    public BigDecimal applyAndDeductVoucher(String code, BigDecimal orderAmount) {
        // 1. Tìm voucher theo mã
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại!"));

        // 2. Kiểm tra trạng thái voucher (phải là ACTIVE)
        if (!"ACTIVE".equalsIgnoreCase(voucher.getStatus())) {
            throw new RuntimeException("Mã giảm giá hiện không hoạt động!");
        }

        LocalDateTime now = LocalDateTime.now();

        // 3. Kiểm tra thời gian hiệu lực (dùng startTime và endTime)
        if (now.isBefore(voucher.getStartTime()) || now.isAfter(voucher.getEndTime())) {
            throw new RuntimeException("Mã giảm giá đã hết hạn hoặc chưa đến thời gian sử dụng!");
        }

        // 4. Kiểm tra giá trị đơn hàng tối thiểu (Min_Order_Value) trước khi trừ lượt
        if (voucher.getMinOrderValue() != null && orderAmount.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new RuntimeException("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã này (" + voucher.getMinOrderValue() + " VNĐ)!");
        }

        // 5. Tính toán số tiền được giảm trước
        BigDecimal discountAmount = BigDecimal.ZERO;

        // Vì discountType là Enum nên dùng so sánh trực tiếp hoặc switch-case
        if (DiscountType.PERCENTAGE.equals(voucher.getDiscountType())) {
            // Giảm theo % và dùng HALF_UP để làm tròn, tránh ArithmeticException khi chia vô hạn
            discountAmount = orderAmount.multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            // Nếu có giới hạn mức giảm tối đa (Max Discount)
            if (voucher.getMaxDiscountAmount() != null && discountAmount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                discountAmount = voucher.getMaxDiscountAmount();
            }
        } else if (DiscountType.FIXED_AMOUNT.equals(voucher.getDiscountType())) {
            discountAmount = voucher.getDiscountValue();
        }

        // Đảm bảo số tiền giảm không vượt quá tổng giá trị đơn hàng
        if (discountAmount.compareTo(orderAmount) > 0) {
            discountAmount = orderAmount;
        }

        // 6. Thực hiện trừ lượt nguyên tử ở database sau khi đã pass tất cả các bước validate
        int updatedRows = voucherRepository.decreaseVoucherUsage(code, now);
        if (updatedRows == 0) {
            throw new RuntimeException("Rất tiếc, mã giảm giá đã hết lượt sử dụng!");
        }

        return discountAmount;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<Voucher> getAllVouchers() {
        return voucherRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Voucher getVoucherById(Long id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mã giảm giá có ID: " + id));
    }

    @Override
    @Transactional
    public Voucher createVoucher(Voucher voucher) {
        if (voucher.getStatus() == null) {
            voucher.setStatus("ACTIVE");
        }
        return voucherRepository.save(voucher);
    }

    @Override
    @Transactional
    public Voucher updateVoucher(Long id, Voucher details) {
        Voucher existing = getVoucherById(id);
        existing.setCode(details.getCode());
        existing.setDiscountType(details.getDiscountType());
        existing.setDiscountValue(details.getDiscountValue());
        existing.setMinOrderValue(details.getMinOrderValue());
        existing.setMaxDiscountAmount(details.getMaxDiscountAmount());
        existing.setUsageLimit(details.getUsageLimit());
        existing.setStartTime(details.getStartTime());
        existing.setEndTime(details.getEndTime());
        existing.setStatus(details.getStatus());
        return voucherRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteVoucher(Long id) {
        Voucher existing = getVoucherById(id);
        voucherRepository.delete(existing);
    }
}