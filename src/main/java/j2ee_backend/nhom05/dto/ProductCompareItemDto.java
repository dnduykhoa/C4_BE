package j2ee_backend.nhom05.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCompareItemDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stockQuantity;
    private String status;
    private String brandName;
    private String imageUrl;
}
