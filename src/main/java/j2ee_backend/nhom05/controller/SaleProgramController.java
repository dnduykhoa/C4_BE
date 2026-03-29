package j2ee_backend.nhom05.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import j2ee_backend.nhom05.config.RoleAccess;
import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.dto.SaleProgramRequest;
import j2ee_backend.nhom05.dto.SaleProgramResponse;
import j2ee_backend.nhom05.service.SaleProgramService;

@RestController
@RequestMapping("/api/sale-programs")
@CrossOrigin(origins = "*")
public class SaleProgramController {

    @Autowired
    private SaleProgramService saleProgramService;

    // ── Public: Lấy danh sách sale đang hoạt động ─────────────────────────────

    @GetMapping("/active")
    public ResponseEntity<?> getActive() {
        try {
            List<SaleProgramResponse> list = saleProgramService.getActive();
            return ResponseEntity.ok(new ApiResponse("Lấy danh sách chương trình sale thành công", list));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(new ApiResponse("OK", saleProgramService.getById(id)));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }

    // ── Admin: Quản lý chương trình sale ──────────────────────────────────────

    @GetMapping
    public ResponseEntity<?> getAll(@AuthenticationPrincipal UserDetails userDetails) {
        if (!isAdminOrManager(userDetails)) return forbidden();
        try {
            return ResponseEntity.ok(new ApiResponse("OK", saleProgramService.getAll()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody SaleProgramRequest request) {
        if (!isAdminOrManager(userDetails)) return forbidden();
        try {
            SaleProgramResponse res = saleProgramService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse("Tạo chương trình sale thành công", res));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody SaleProgramRequest request) {
        if (!isAdminOrManager(userDetails)) return forbidden();
        try {
            return ResponseEntity.ok(new ApiResponse("Cập nhật chương trình sale thành công", saleProgramService.update(id, request)));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (!isAdminOrManager(userDetails)) return forbidden();
        try {
            saleProgramService.delete(id);
            return ResponseEntity.ok(new ApiResponse("Xoá chương trình sale thành công", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggle(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (!isAdminOrManager(userDetails)) return forbidden();
        try {
            return ResponseEntity.ok(new ApiResponse("Cập nhật trạng thái thành công", saleProgramService.toggleActive(id)));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(e.getMessage(), null));
        }
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private boolean isAdminOrManager(UserDetails userDetails) {
        return RoleAccess.hasAnyRole(userDetails, "ADMIN", "MANAGER");
    }

    private ResponseEntity<ApiResponse> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse("Bạn không có quyền thực hiện thao tác này", null));
    }
}
