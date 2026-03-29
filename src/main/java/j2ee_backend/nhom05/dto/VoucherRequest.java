package j2ee_backend.nhom05.dto;

import j2ee_backend.nhom05.model.DiscountType;
import j2ee_backend.nhom05.model.VoucherType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VoucherRequest {

    private String code;
    private String name;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;

    // Giới hạn mức giảm tối đa (cho PERCENTAGE; null = không giới hạn)
    private BigDecimal maxDiscountAmount;

    // Giá trị đơn hàng tối thiểu để dùng voucher (null = không yêu cầu)
    private BigDecimal minOrderAmount;

    private VoucherType voucherType;

    // Số lần dùng tối đa (null = không giới hạn)
    private Integer maxUsageCount;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive = true;
}
