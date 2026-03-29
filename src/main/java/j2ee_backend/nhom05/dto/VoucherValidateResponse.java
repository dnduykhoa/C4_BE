package j2ee_backend.nhom05.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoucherValidateResponse {
    private boolean valid;
    private String message;
    private BigDecimal discountAmount;  // Số tiền được giảm
    private BigDecimal finalAmount;     // Tổng tiền sau khi giảm
    private String voucherCode;
}
