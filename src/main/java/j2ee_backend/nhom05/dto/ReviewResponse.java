package j2ee_backend.nhom05.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewResponse {

    private Long id;
    private Long userId;
    private String username;
    private Long productId;
    private String productName;
    private Long variantId;
    private String variantSku;
    private Long orderItemId;
    private Integer rating;
    private String comment;
    private List<String> imageUrls;
    private boolean hidden;
    private LocalDateTime createdAt;
    private String reply;
    private LocalDateTime repliedAt;
}
