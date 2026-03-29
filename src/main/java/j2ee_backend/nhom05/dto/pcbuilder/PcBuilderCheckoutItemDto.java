package j2ee_backend.nhom05.dto.pcbuilder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PcBuilderCheckoutItemDto {
    private Long productId;
    private Long variantId;
    private Integer quantity;
}
