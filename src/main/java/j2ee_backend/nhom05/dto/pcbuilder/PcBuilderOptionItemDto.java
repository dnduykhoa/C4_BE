package j2ee_backend.nhom05.dto.pcbuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PcBuilderOptionItemDto {
    private String slotKey;
    private Long productId;
    private Long variantId;
    private Integer quantity;
    private String name;
    private String variantLabel;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private BigDecimal price;
    private Integer stockQuantity;
    private String brandName;
    private String categoryName;
    private Boolean hasVariants;
    private Long defaultVariantId;
    private List<PcBuilderVariantOptionDto> availableVariants;
    private PcBuilderCompatibilityDto compatibility;
    private Map<String, String> keySpecs;
}
