package j2ee_backend.nhom05.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.service.AuthSessionService;

@RestController
@RequestMapping("/api/admin/trusted-devices")
public class AdminAuthSessionController {

    private final AuthSessionService authSessionService;

    public AdminAuthSessionController(AuthSessionService authSessionService) {
        this.authSessionService = authSessionService;
    }

    @GetMapping
    public ResponseEntity<?> getTrustedDevices(@AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        return ResponseEntity.ok(new ApiResponse(
                "Lấy danh sách thiết bị tin cậy thành công",
                authSessionService.listTrustedDevicesForActor(user)));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<?> revokeTrustedDevice(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long sessionId) {
        User user = (User) userDetails;
        authSessionService.revokeTrustedDeviceForActor(user, sessionId);
        return ResponseEntity.ok(new ApiResponse("Thu hồi thiết bị thành công", null));
    }
}