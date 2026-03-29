package j2ee_backend.nhom05.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import j2ee_backend.nhom05.config.RoleAccess;
import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.service.PreorderRequestService;

@RestController
@RequestMapping("/api/admin/preorders")
@CrossOrigin(origins = "*")
public class AdminPreorderRequestController {

    @Autowired
    private PreorderRequestService preorderRequestService;

    @GetMapping
    public ResponseEntity<?> getAll(@AuthenticationPrincipal UserDetails userDetails) {
        if (!RoleAccess.hasAnyRole(userDetails, "ADMIN", "MANAGER", "STAFF")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse("Bạn không có quyền thực hiện thao tác này", null));
        }

        return ResponseEntity.ok(
            new ApiResponse("Lấy danh sách khách chờ hàng thành công", preorderRequestService.getAllRequests())
        );
    }
}