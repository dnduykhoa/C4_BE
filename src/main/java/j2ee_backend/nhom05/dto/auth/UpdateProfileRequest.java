package j2ee_backend.nhom05.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Data
public class UpdateProfileRequest {
    
    private String fullName;
    
    @Email(message = "Email không hợp lệ")
    private String email;
    
    @Pattern(
        regexp = "^(\\+84|84|0)(3[2-9]|5[25689]|7[06-9]|8[1-9]|9[0-9])\\d{7}$",
        message = "Số điện thoại Việt Nam không hợp lệ (VD: 0912345678 hoặc +84912345678)"
    )
    private String phone;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;
}
