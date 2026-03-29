package j2ee_backend.nhom05.controller;

import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.model.AttributeGroup;
import j2ee_backend.nhom05.service.AttributeGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

/**
 * API quản lý nhóm thuộc tính (EAV - AttributeGroup)
 *
 * GET    /api/attribute-groups            → Tất cả nhóm
 * GET    /api/attribute-groups/active     → Nhóm đang hoạt động
 * GET    /api/attribute-groups/{id}       → Chi tiết nhóm
 * POST   /api/attribute-groups/add        → Thêm nhóm mới
 * PUT    /api/attribute-groups/update/{id}→ Cập nhật nhóm
 * DELETE /api/attribute-groups/delete/{id}→ Xóa nhóm
 */
@RestController
@RequestMapping("/api/attribute-groups")
@CrossOrigin(origins = "*")
public class AttributeGroupController {

    @Autowired
    private AttributeGroupService attributeGroupService;

    @GetMapping("")
    public ResponseEntity<?> getAll() {
        try {
            return ResponseEntity.ok(new ApiResponse("Lấy danh sách nhóm thuộc tính thành công",
                attributeGroupService.getAllGroups()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActive() {
        try {
            return ResponseEntity.ok(new ApiResponse("Lấy nhóm thuộc tính đang hoạt động thành công",
                attributeGroupService.getActiveGroups()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            AttributeGroup group = attributeGroupService.getGroupById(id).orElse(null);
            if (group == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("Không tìm thấy nhóm thuộc tính", null));
            }
            return ResponseEntity.ok(new ApiResponse("Lấy nhóm thuộc tính thành công", group));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> create(@Valid @RequestBody AttributeGroup group) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("Thêm nhóm thuộc tính thành công",
                    attributeGroupService.createGroup(group)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody AttributeGroup group) {
        try {
            return ResponseEntity.ok(new ApiResponse("Cập nhật nhóm thuộc tính thành công",
                attributeGroupService.updateGroup(id, group)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            attributeGroupService.deleteGroup(id);
            return ResponseEntity.ok(new ApiResponse("Xóa nhóm thuộc tính thành công", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
}
