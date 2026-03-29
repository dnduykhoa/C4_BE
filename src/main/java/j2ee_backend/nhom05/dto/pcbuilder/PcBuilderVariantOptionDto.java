package j2ee_backend.nhom05.dto.pcbuilder;

import java.math.BigDecimal;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PcBuilderVariantOptionDto {
    private Long variantId;
    private String label;
    private BigDecimal price;
    private Integer stockQuantity;
    private Map<String, String> keySpecs;
}
