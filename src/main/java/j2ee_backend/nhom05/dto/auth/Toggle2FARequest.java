package j2ee_backend.nhom05.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class Toggle2FARequest {
    
    @NotNull(message = "Trạng thái 2FA không được để trống")
    private Boolean enabled;
}
