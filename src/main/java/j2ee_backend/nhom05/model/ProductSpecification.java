package j2ee_backend.nhom05.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;

/**
 * EAV Value — Giá trị thuộc tính của sản phẩm.
 * specKey/specValue vẫn giữ để tương thích với dữ liệu cũ.
 * attributeDefinition là liên kết chuẩn hóa với AttributeDefinition.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "product_specifications")
public class ProductSpecification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // EAV: Entity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;

    // EAV: Attribute — liên kết chuẩn hóa (nullable để tương thích dữ liệu cũ)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attr_def_id")
    @ToString.Exclude
    private AttributeDefinition attributeDefinition;

    // EAV: Attribute key dạng chuỗi tự do (legacy / fallback khi không có attr_def_id)
    @Column(name = "spec_key", length = 100, columnDefinition = "NVARCHAR(100)")
    private String specKey; // VD: "CPU", "RAM", "Mainboard"

    // EAV: Value dạng chuỗi (dùng cho DataType.STRING / DataType.LIST / DataType.BOOLEAN)
    @Column(name = "spec_value", columnDefinition = "NVARCHAR(500)")
    private String specValue; // VD: "AMD Ryzen 5 5600X", "8GB DDR4"

    // EAV: Value dạng số — dùng cho DataType.NUMBER, hỗ trợ lọc/so sánh (>= 8 GB RAM)
    @Column(name = "value_number", precision = 18, scale = 4)
    private BigDecimal valueNumber; // VD: 8 (GB), 15.6 (inch), 5000 (mAh)

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    /**
     * Trả về giá trị hiển thị: ưu tiên specValue, nếu null thì dùng valueNumber.
     */
    public String getDisplayValue() {
        if (specValue != null && !specValue.isBlank()) return specValue;
        if (valueNumber != null) {
            String unit = (attributeDefinition != null && attributeDefinition.getUnit() != null)
                ? " " + attributeDefinition.getUnit() : "";
            return valueNumber.stripTrailingZeros().toPlainString() + unit;
        }
        return "";
    }
}
