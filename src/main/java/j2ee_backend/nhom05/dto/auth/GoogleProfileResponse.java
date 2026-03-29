package j2ee_backend.nhom05.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleProfileResponse {
    private String sub;
    private String email;
    private boolean emailVerified;
    private String name;
    private String picture;
}