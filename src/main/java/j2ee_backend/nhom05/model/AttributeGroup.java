package j2ee_backend.nhom05.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Nhóm thuộc tính sản phẩm - EAV: phân loại các AttributeDefinition
 * Ví dụ: "Hiệu năng", "Màn hình", "Kết nối", "Thiết kế", "Pin & Sạc"
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "attribute_groups")
public class AttributeGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    @NotBlank(message = "Tên nhóm thuộc tính không được để trống")
    private String name; // VD: "Hiệu năng", "Màn hình", "Kết nối"

    @Column(name = "description", columnDefinition = "NVARCHAR(500)")
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // Danh sách các định nghĩa thuộc tính trong nhóm này
    @OneToMany(mappedBy = "attributeGroup", cascade = CascadeType.ALL)
    @ToString.Exclude
    @JsonIgnore
    private List<AttributeDefinition> attributeDefinitions = new ArrayList<>();
}
