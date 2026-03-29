package j2ee_backend.nhom05.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class Verify2FARequest {
    
    @NotBlank(message = "Email hoặc số điện thoại không được để trống")
    private String emailOrPhone;
    
    @NotBlank(message = "Mã xác thực không được để trống")
    private String code;

    private boolean rememberMe = false;
}
