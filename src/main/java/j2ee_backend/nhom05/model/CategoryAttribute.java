package j2ee_backend.nhom05.model;

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

/**
 * Liên kết Category ↔ AttributeDefinition
 * Xác định thuộc tính nào áp dụng cho danh mục nào.
 * VD: Danh mục "Laptop" yêu cầu RAM, CPU, SSD; "Điện thoại" yêu cầu RAM, Pin, Camera.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "category_attributes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"category_id", "attr_def_id"})
})
public class CategoryAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Danh mục áp dụng thuộc tính này
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private Category category;

    // Định nghĩa thuộc tính được áp dụng
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attr_def_id", nullable = false)
    @ToString.Exclude
    @JsonIgnoreProperties({"categoryAttributes", "productSpecifications"})
    private AttributeDefinition attributeDefinition;

    // Thuộc tính bắt buộc với danh mục này (ghi đè is_required của AttributeDefinition)
    @Column(name = "is_required")
    private Boolean isRequired = false;

    // Thứ tự hiển thị trong danh mục này
    @Column(name = "display_order")
    private Integer displayOrder = 0;
}
