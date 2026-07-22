package com.ticket.core.service;

import com.ticket.core.entity.Voucher;
import com.ticket.core.enums.DiscountType;
import com.ticket.core.repository.VoucherRepository;
import com.ticket.core.service.impl.VoucherServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoucherServiceImplTest {

    @Mock
    private VoucherRepository voucherRepository;

    @InjectMocks
    private VoucherServiceImpl voucherService;

    private Voucher sampleVoucher;

    @BeforeEach
    void setUp() {
        sampleVoucher = Voucher.builder()
                .id(1L)
                .code("SUMMER2026")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("20.00")) // 20%
                .minOrderValue(new BigDecimal("200000.00")) // Đơn tối thiểu 200,000
                .maxDiscountAmount(new BigDecimal("100000.00")) // Giảm tối đa 100,000
                .usageLimit(50)
                .startTime(LocalDateTime.now().minusDays(1))
                .endTime(LocalDateTime.now().plusDays(10))
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("1. Thất bại - Không tìm thấy mã giảm giá trong CSDL")
    void applyVoucher_VoucherNotFound_ThrowsException() {
        when(voucherRepository.findByCode("INVALID_CODE")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                voucherService.applyAndDeductVoucher("INVALID_CODE", new BigDecimal("500000"))
        );

        assertEquals("Mã giảm giá không tồn tại!", exception.getMessage());
        verify(voucherRepository, never()).decreaseVoucherUsage(anyString(), any());
    }

    @Test
    @DisplayName("2. Thất bại - Mã giảm giá đang bị khóa (INACTIVE)")
    void applyVoucher_InactiveVoucher_ThrowsException() {
        sampleVoucher.setStatus("INACTIVE");
        when(voucherRepository.findByCode("SUMMER2026")).thenReturn(Optional.of(sampleVoucher));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                voucherService.applyAndDeductVoucher("SUMMER2026", new BigDecimal("500000"))
        );

        assertEquals("Mã giảm giá hiện không hoạt động!", exception.getMessage());
        verify(voucherRepository, never()).decreaseVoucherUsage(anyString(), any());
    }

    @Test
    @DisplayName("3. Thất bại - Mã giảm giá chưa đến thời gian áp dụng")
    void applyVoucher_NotStartedYet_ThrowsException() {
        sampleVoucher.setStartTime(LocalDateTime.now().plusDays(2)); // Bắt đầu sau 2 ngày
        when(voucherRepository.findByCode("SUMMER2026")).thenReturn(Optional.of(sampleVoucher));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                voucherService.applyAndDeductVoucher("SUMMER2026", new BigDecimal("500000"))
        );

        assertEquals("Mã giảm giá đã hết hạn hoặc chưa đến thời gian sử dụng!", exception.getMessage());
    }

    @Test
    @DisplayName("4. Thất bại - Mã giảm giá đã hết hạn sử dụng")
    void applyVoucher_Expired_ThrowsException() {
        sampleVoucher.setEndTime(LocalDateTime.now().minusDays(1)); // Hết hạn ngày hôm qua
        when(voucherRepository.findByCode("SUMMER2026")).thenReturn(Optional.of(sampleVoucher));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                voucherService.applyAndDeductVoucher("SUMMER2026", new BigDecimal("500000"))
        );

        assertEquals("Mã giảm giá đã hết hạn hoặc chưa đến thời gian sử dụng!", exception.getMessage());
    }

    @Test
    @DisplayName("5. Thất bại - Giá trị đơn hàng chưa đạt giá trị tối thiểu (Min Order Value)")
    void applyVoucher_MinOrderValueNotMet_ThrowsException() {
        when(voucherRepository.findByCode("SUMMER2026")).thenReturn(Optional.of(sampleVoucher));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                voucherService.applyAndDeductVoucher("SUMMER2026", new BigDecimal("100000.00"))
        );

        assertTrue(exception.getMessage().contains("Đơn hàng chưa đạt giá trị tối thiểu"));
    }

    @Test
    @DisplayName("6. Thành công - Giảm theo phần trăm (%) tiêu chuẩn (20% của 400.000 = 80.000)")
    void applyVoucher_Percentage_Success() {
        when(voucherRepository.findByCode("SUMMER2026")).thenReturn(Optional.of(sampleVoucher));
        when(voucherRepository.decreaseVoucherUsage(eq("SUMMER2026"), any(LocalDateTime.class))).thenReturn(1);

        BigDecimal discount = voucherService.applyAndDeductVoucher("SUMMER2026", new BigDecimal("400000.00"));

        assertEquals(new BigDecimal("80000.00"), discount);
        verify(voucherRepository, times(1)).decreaseVoucherUsage(eq("SUMMER2026"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("7. Thành công - Giảm theo phần trăm (%) nhưng bị chạm trần tối đa (Max Discount)")
    void applyVoucher_Percentage_CappedByMaxDiscount() {
        when(voucherRepository.findByCode("SUMMER2026")).thenReturn(Optional.of(sampleVoucher));
        when(voucherRepository.decreaseVoucherUsage(eq("SUMMER2026"), any(LocalDateTime.class))).thenReturn(1);

        BigDecimal discount = voucherService.applyAndDeductVoucher("SUMMER2026", new BigDecimal("1000000.00"));

        assertEquals(new BigDecimal("100000.00"), discount);
    }

    @Test
    @DisplayName("8. Thành công - Giảm theo số tiền cố định (FIXED_AMOUNT)")
    void applyVoucher_FixedAmount_Success() {
        sampleVoucher.setDiscountType(DiscountType.FIXED_AMOUNT);
        sampleVoucher.setDiscountValue(new BigDecimal("50000.00"));

        when(voucherRepository.findByCode("SUMMER2026")).thenReturn(Optional.of(sampleVoucher));
        when(voucherRepository.decreaseVoucherUsage(eq("SUMMER2026"), any(LocalDateTime.class))).thenReturn(1);

        BigDecimal discount = voucherService.applyAndDeductVoucher("SUMMER2026", new BigDecimal("300000.00"));

        assertEquals(new BigDecimal("50000.00"), discount);
    }

    @Test
    @DisplayName("9. Thành công - Mức giảm lớn hơn tổng đơn hàng -> Giảm bằng đúng giá trị đơn hàng")
    void applyVoucher_DiscountExceedsOrderAmount_CappedAtOrderAmount() {
        sampleVoucher.setDiscountType(DiscountType.FIXED_AMOUNT);
        sampleVoucher.setDiscountValue(new BigDecimal("500000.00"));
        sampleVoucher.setMinOrderValue(BigDecimal.ZERO);

        when(voucherRepository.findByCode("SUMMER2026")).thenReturn(Optional.of(sampleVoucher));
        when(voucherRepository.decreaseVoucherUsage(eq("SUMMER2026"), any(LocalDateTime.class))).thenReturn(1);

        BigDecimal discount = voucherService.applyAndDeductVoucher("SUMMER2026", new BigDecimal("300000.00"));

        assertEquals(new BigDecimal("300000.00"), discount);
    }

    @Test
    @DisplayName("10. Thất bại - Mã giảm giá đã hết lượt sử dụng (decreaseVoucherUsage = 0)")
    void applyVoucher_UsageLimitExceeded_ThrowsException() {
        when(voucherRepository.findByCode("SUMMER2026")).thenReturn(Optional.of(sampleVoucher));
        when(voucherRepository.decreaseVoucherUsage(eq("SUMMER2026"), any(LocalDateTime.class))).thenReturn(0);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                voucherService.applyAndDeductVoucher("SUMMER2026", new BigDecimal("500000.00"))
        );

        assertEquals("Rất tiếc, mã giảm giá đã hết lượt sử dụng!", exception.getMessage());
    }

  
    @Test
    @DisplayName("11. Tạo mới Voucher thành công (Tự động gán status ACTIVE nếu null)")
    void createVoucher_DefaultStatusActive() {
        Voucher newVoucher = Voucher.builder().code("NEWVOUCHER").build();
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(i -> i.getArguments()[0]);

        Voucher created = voucherService.createVoucher(newVoucher);

        assertNotNull(created);
        assertEquals("ACTIVE", created.getStatus());
        verify(voucherRepository).save(newVoucher);
    }

    @Test
    @DisplayName("12. Lấy Voucher theo ID thành công")
    void getVoucherById_Success() {
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(sampleVoucher));

        Voucher result = voucherService.getVoucherById(1L);

        assertNotNull(result);
        assertEquals("SUMMER2026", result.getCode());
    }

    @Test
    @DisplayName("13. Lấy Voucher theo ID thất bại - Không tìm thấy ID")
    void getVoucherById_NotFound_ThrowsException() {
        when(voucherRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                voucherService.getVoucherById(99L)
        );

        assertTrue(exception.getMessage().contains("Không tìm thấy mã giảm giá có ID: 99"));
    }

    @Test
    @DisplayName("14. Lấy toàn bộ danh sách Voucher thành công")
    void getAllVouchers_Success() {
        when(voucherRepository.findAll()).thenReturn(List.of(sampleVoucher));

        List<Voucher> list = voucherService.getAllVouchers();

        assertEquals(1, list.size());
        verify(voucherRepository).findAll();
    }

    @Test
    @DisplayName("15. Xóa Voucher thành công")
    void deleteVoucher_Success() {
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(sampleVoucher));
        doNothing().when(voucherRepository).delete(sampleVoucher);

        assertDoesNotThrow(() -> voucherService.deleteVoucher(1L));

        verify(voucherRepository).delete(sampleVoucher);
    }
}
