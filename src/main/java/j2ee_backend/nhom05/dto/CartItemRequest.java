package j2ee_backend.nhom05.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItemRequest {
    private Long productId;
    private Long variantId;
    private Integer quantity;
}
