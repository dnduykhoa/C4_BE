package j2ee_backend.nhom05.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    
    @NotBlank(message = "Email hoặc số điện thoại không được để trống")
    private String emailOrPhone;
    
    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    private boolean rememberMe = false;
}
