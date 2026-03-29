package j2ee_backend.nhom05.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VoucherValidateRequest {
    private String voucherCode;
    private BigDecimal orderAmount; // Tổng đơn hàng trước khi áp voucher
}
