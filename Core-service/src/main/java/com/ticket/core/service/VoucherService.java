package com.ticket.core.service;

import java.math.BigDecimal;

import com.ticket.core.entity.Voucher;
import java.util.List;

public interface VoucherService {

    BigDecimal applyAndDeductVoucher(String code, BigDecimal orderAmount);

    List<Voucher> getAllVouchers();

    Voucher getVoucherById(Long id);

    Voucher createVoucher(Voucher voucher);

    Voucher updateVoucher(Long id, Voucher voucher);

    void deleteVoucher(Long id);
}
