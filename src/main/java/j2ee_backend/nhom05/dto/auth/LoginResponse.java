package j2ee_backend.nhom05.dto.auth;

import java.time.LocalDate;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String message;
    private String token;
    private String refreshToken;
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private LocalDate birthDate;
    private Set<String> roles;
}
