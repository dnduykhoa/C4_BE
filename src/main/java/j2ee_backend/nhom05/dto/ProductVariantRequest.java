package j2ee_backend.nhom05.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductVariantRequest {
    private String sku;
    private BigDecimal price;
    private Integer stockQuantity;
    private Boolean isActive;
    private Integer displayOrder;
    private List<ProductVariantValueRequest> values;
}
