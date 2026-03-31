package j2ee_backend.nhom05.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailOrderItem {
    private String imageUrl;
    private String productName;
    private String variantName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}
