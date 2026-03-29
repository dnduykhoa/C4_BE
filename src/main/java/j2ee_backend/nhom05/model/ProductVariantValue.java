package j2ee_backend.nhom05.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "product_variant_values", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"variant_id", "attr_key"})
})
public class ProductVariantValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attr_def_id")
    @ToString.Exclude
    @JsonIgnoreProperties({"categoryAttributes", "productSpecifications"})
    private AttributeDefinition attributeDefinition;

    @Column(name = "attr_key", nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String attrKey;

    @Column(name = "attr_value", length = 200, columnDefinition = "NVARCHAR(200)")
    private String attrValue;

    @Column(name = "value_number", precision = 18, scale = 4)
    private BigDecimal valueNumber;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    public String getDisplayValue() {
        if (attrValue != null && !attrValue.isBlank()) return attrValue;
        if (valueNumber != null) {
            String unit = (attributeDefinition != null && attributeDefinition.getUnit() != null)
                ? " " + attributeDefinition.getUnit() : "";
            return valueNumber.stripTrailingZeros().toPlainString() + unit;
        }
        return "";
    }
}
