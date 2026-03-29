package j2ee_backend.nhom05.dto;

import j2ee_backend.nhom05.model.ConditionType;
import j2ee_backend.nhom05.model.DiscountType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
public class SaleProgramRequest {

    private String name;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive = true;

    // IDs sản phẩm áp dụng sale
    private Set<Long> productIds;

    // Danh sách điều kiện
    private List<ConditionRequest> conditions;

    @Data
    public static class ConditionRequest {
        private ConditionType conditionType;
        private String conditionValue;
        private String description;
    }
}
