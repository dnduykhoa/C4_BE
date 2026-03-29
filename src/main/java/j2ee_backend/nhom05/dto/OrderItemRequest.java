package j2ee_backend.nhom05.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemRequest {

    @NotNull(message = "Thiếu productId")
    private Long productId;

    private Long variantId;

    @NotNull(message = "Thiếu quantity")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;
}
