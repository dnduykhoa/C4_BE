package j2ee_backend.nhom05.dto.auth;

import java.time.LocalDateTime;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrustedDeviceResponse {
    private Long sessionId;
    private String ownerUsername;
    private String ownerEmail;
    private Set<String> ownerRoles;
    private String deviceName;
    private String ipAddress;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastSeenAt;
}