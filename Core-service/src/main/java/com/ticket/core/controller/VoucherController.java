package com.ticket.core.controller;

import com.ticket.core.dto.response.ApiResponse;
import com.ticket.core.entity.Voucher;
import com.ticket.core.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    // ==========================================
    // INTERNAL MICROSERVICE COMMUNICATION ENDPOINTS (Do NOT wrap in ApiResponse)
    // ==========================================
    @PostMapping("/apply")
    public BigDecimal applyAndDeductVoucher(
            @RequestParam("code") String code,
            @RequestParam("orderAmount") BigDecimal orderAmount) {
        return voucherService.applyAndDeductVoucher(code, orderAmount);
    }

    // ==========================================
    // CRUD PUBLIC/OPERATIONAL APIS (Wrap in ApiResponse)
    // ==========================================
    @PostMapping
    public ResponseEntity<ApiResponse> createVoucher(@RequestBody Voucher voucher) {
        Voucher created = voucherService.createVoucher(voucher);
        ApiResponse apiResponse = ApiResponse.builder()
                .status(HttpStatus.CREATED.value())
                .message("Tạo mã giảm giá thành công!")
                .data(created)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getVoucherById(@PathVariable Long id) {
        Voucher voucher = voucherService.getVoucherById(id);
        ApiResponse apiResponse = ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Lấy thông tin mã giảm giá thành công!")
                .data(voucher)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getAllVouchers() {
        List<Voucher> vouchers = voucherService.getAllVouchers();
        ApiResponse apiResponse = ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách mã giảm giá thành công!")
                .data(vouchers)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateVoucher(@PathVariable Long id, @RequestBody Voucher details) {
        Voucher updated = voucherService.updateVoucher(id, details);
        ApiResponse apiResponse = ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Cập nhật thông tin mã giảm giá thành công!")
                .data(updated)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteVoucher(@PathVariable Long id) {
        voucherService.deleteVoucher(id);
        ApiResponse apiResponse = ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Xóa mã giảm giá thành công!")
                .build();
        return ResponseEntity.ok(apiResponse);
    }
}
