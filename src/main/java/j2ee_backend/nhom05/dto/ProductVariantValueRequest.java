package j2ee_backend.nhom05.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductVariantValueRequest {
    private Long attrDefId;
    private String attrKey;
    private String attrValue;
    private BigDecimal valueNumber;
    private Integer displayOrder;
}
