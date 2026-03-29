package j2ee_backend.nhom05.dto;

import j2ee_backend.nhom05.model.ConditionType;
import j2ee_backend.nhom05.model.DiscountType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
public class SaleProgramResponse {

    private Long id;
    private String name;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    private List<ConditionResponse> conditions;
    private Set<Long> productIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    public static class ConditionResponse {
        private Long id;
        private ConditionType conditionType;
        private String conditionValue;
        private String description;
    }
}
