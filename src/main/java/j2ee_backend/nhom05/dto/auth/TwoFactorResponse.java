package j2ee_backend.nhom05.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TwoFactorResponse {
    private String message;
    private boolean requiresTwoFactor;
    private String emailOrPhone; // Để frontend biết gửi code về đâu
}
