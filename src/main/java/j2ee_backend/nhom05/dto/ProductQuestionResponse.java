package j2ee_backend.nhom05.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductQuestionResponse {
    private Long id;
    private Long productId;
    private String productName;
    private Long customerId;
    private String customerName;
    private String question;
    private String answer;
    private Boolean answered;
    private LocalDateTime askedAt;
    private LocalDateTime answeredAt;
    private Long answeredById;
    private String answeredByName;
}
