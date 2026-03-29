package j2ee_backend.nhom05.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "carousel_slides")
public class CarouselSlide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image", length = 1000)
    private String image;

    // IMAGE hoặc VIDEO
    @Column(name = "media_type", length = 10)
    private String mediaType = "IMAGE";

    @Column(name = "badge", length = 100, columnDefinition = "NVARCHAR(100)")
    private String badge;

    @Column(name = "title", length = 500, columnDefinition = "NVARCHAR(500)")
    private String title;

    @Column(name = "subtitle", columnDefinition = "NTEXT")
    private String subtitle;

    @Column(name = "button_text", length = 100, columnDefinition = "NVARCHAR(100)")
    private String buttonText;

    @Column(name = "button_link", length = 500)
    private String buttonLink;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    // Thời gian hiển thị mỗi slide (ms), mặc định 4 giây
    @Column(name = "interval_ms")
    private Integer intervalMs = 4000;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
