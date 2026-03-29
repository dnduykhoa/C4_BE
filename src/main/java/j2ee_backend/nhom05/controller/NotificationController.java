package j2ee_backend.nhom05.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/my")
    public ResponseEntity<?> getMyNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = ((User) userDetails).getId();
            return ResponseEntity.ok(new ApiResponse(
                "Lấy danh sách thông báo thành công",
                notificationService.getMyNotifications(userId)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/my/unread")
    public ResponseEntity<?> getMyUnreadNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = ((User) userDetails).getId();
            return ResponseEntity.ok(new ApiResponse(
                "Lấy thông báo chưa đọc thành công",
                notificationService.getMyUnreadNotifications(userId)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/my/unread-count")
    public ResponseEntity<?> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = ((User) userDetails).getId();
            return ResponseEntity.ok(new ApiResponse(
                "Lấy số thông báo chưa đọc thành công",
                Map.of("count", notificationService.getUnreadCount(userId))
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = ((User) userDetails).getId();
            return ResponseEntity.ok(new ApiResponse(
                "Đã đánh dấu đã đọc",
                notificationService.markAsRead(id, userId)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PatchMapping("/my/read-all")
    public ResponseEntity<?> markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = ((User) userDetails).getId();
            notificationService.markAllAsRead(userId);
            return ResponseEntity.ok(new ApiResponse("Đã đánh dấu tất cả thông báo là đã đọc", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
}
