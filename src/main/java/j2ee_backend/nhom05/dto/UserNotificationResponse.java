package j2ee_backend.nhom05.dto;

import java.time.LocalDateTime;

import j2ee_backend.nhom05.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserNotificationResponse {
    private Long id;
    private NotificationType type;
    private String title;
    private String content;
    private String referenceUrl;
    private Boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
