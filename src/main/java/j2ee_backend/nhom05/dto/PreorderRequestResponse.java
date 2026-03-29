package j2ee_backend.nhom05.dto;

import java.time.LocalDateTime;

import j2ee_backend.nhom05.model.PreorderRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PreorderRequestResponse {
    private Long id;
    private Long productId;
    private String productName;
    private Long variantId;
    private String variantName;
    private String customerName;
    private String phone;
    private String email;
    private Integer desiredQuantity;
    private PreorderRequestStatus status;
    private Integer queuePosition;
    private LocalDateTime createdAt;
    private LocalDateTime notifiedAt;
}