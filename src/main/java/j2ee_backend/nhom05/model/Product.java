package j2ee_backend.nhom05.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "products")
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200, columnDefinition = "NVARCHAR(200)")
    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    @Column(name = "description", columnDefinition = "NTEXT")
    private String description;

    @Column(name = "box_contents", length = 500, columnDefinition = "NVARCHAR(500)")
    private String boxContents; // Bộ sản phẩm gồm gì (VD: Cây lấy sim, Cáp Type C, Hộp, Sách hướng dẫn)

    @Column(name = "price", nullable = false, precision = 18, scale = 2)
    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    @Min(value = 0, message = "Số lượng không được âm")
    private Integer stockQuantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @NotFound(action = NotFoundAction.IGNORE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    @NotFound(action = NotFoundAction.IGNORE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Brand brand;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ProductMedia> media = new ArrayList<>();

    public List<ProductMedia> getMedia() {
        if (media == null) return new ArrayList<>();
        return media.stream()
            .filter(m -> m.getVariant() == null)
            .toList();
    }

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ProductSpecification> specifications = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ProductVariant> variants = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "new_arrival_at")
    private LocalDateTime newArrivalAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ProductStatus status = ProductStatus.ACTIVE;

    @Transient
    private long soldCount = 0;

    @Transient
    private ReviewSummary reviewSummary;

    @JsonProperty("isActive")
    public boolean getIsActive() {
        return this.status == ProductStatus.ACTIVE;
    }

    public static class ReviewSummary {
        public final double averageRating;
        public final long totalReviews;

        public ReviewSummary(double averageRating, long totalReviews) {
            this.averageRating = averageRating;
            this.totalReviews = totalReviews;
        }
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Phương thức lấy ảnh chính
    public ProductMedia getPrimaryImage() {
        return media.stream()
            .filter(m -> "IMAGE".equals(m.getMediaType()) && Boolean.TRUE.equals(m.getIsPrimary()))
            .findFirst()
            .orElse(null);
    }
    
    // Phương thức lấy tất cả ảnh phụ
    public List<ProductMedia> getSecondaryImages() {
        return media.stream()
            .filter(m -> "IMAGE".equals(m.getMediaType()) && !Boolean.TRUE.equals(m.getIsPrimary()))
            .toList();
    }
    
    // Phương thức lấy tất cả ảnh (chính + phụ)
    public List<ProductMedia> getAllImages() {
        return media.stream()
            .filter(m -> "IMAGE".equals(m.getMediaType()))
            .toList();
    }
    
    // Phương thức lấy tất cả video
    public List<ProductMedia> getVideos() {
        return media.stream()
            .filter(m -> "VIDEO".equals(m.getMediaType()))
            .toList();
    }
}
