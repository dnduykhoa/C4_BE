package j2ee_backend.nhom05.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import j2ee_backend.nhom05.config.DataTypeConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Định nghĩa thuộc tính - EAV: phần "Attribute" được chuẩn hóa
 * Ví dụ: RAM (key="ram", type=NUMBER, unit="GB"), CPU (key="cpu", type=STRING)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "attribute_definitions", uniqueConstraints = {
    @UniqueConstraint(columnNames = "attr_key")
})
public class AttributeDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    @NotBlank(message = "Tên thuộc tính không được để trống")
    private String name; // VD: "RAM", "CPU", "Kích thước màn hình"

    @Column(name = "attr_key", nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    @NotBlank(message = "Khóa thuộc tính không được để trống")
    private String attrKey; // VD: "ram", "cpu", "screen_size" — dùng làm specKey chuẩn hóa

    @Convert(converter = DataTypeConverter.class)
    @Column(name = "data_type", nullable = false, length = 20)
    private DataType dataType = DataType.STRING; // STRING | NUMBER | BOOLEAN | LIST

    @Column(name = "unit", length = 50, columnDefinition = "NVARCHAR(50)")
    private String unit; // VD: "GB", "GHz", "inch", "mAh", "W", "MP" — null nếu không có đơn vị

    @Column(name = "is_filterable")
    private Boolean isFilterable = false; // Có dùng để lọc sản phẩm trên frontend không

    @Column(name = "is_required")
    private Boolean isRequired = false; // Thuộc tính bắt buộc nhập khi tạo sản phẩm

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // Nhóm thuộc tính chứa định nghĩa này
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    @ToString.Exclude
    private AttributeGroup attributeGroup;

    // Các liên kết với danh mục
    @OneToMany(mappedBy = "attributeDefinition", cascade = CascadeType.ALL)
    @ToString.Exclude
    @JsonIgnore
    private List<CategoryAttribute> categoryAttributes = new ArrayList<>();

    // Các giá trị thuộc tính trên sản phẩm
    @OneToMany(mappedBy = "attributeDefinition", cascade = CascadeType.ALL)
    @ToString.Exclude
    @JsonIgnore
    private List<ProductSpecification> productSpecifications = new ArrayList<>();

    public enum DataType {
        STRING,  // Chuỗi văn bản  — VD: "AMD Ryzen 5 5600X"
        NUMBER,  // Số — VD: 8 (GB), 15.6 (inch), 5000 (mAh)
        BOOLEAN, // Đúng/Sai — VD: Có hỗ trợ 5G (true/false)
        LIST     // Danh sách — VD: màu sắc "Đen, Bạc, Vàng"
    }
}
