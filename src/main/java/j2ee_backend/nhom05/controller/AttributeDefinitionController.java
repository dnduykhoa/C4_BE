package j2ee_backend.nhom05.controller;

import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.dto.AttributeDefinitionRequest;
import j2ee_backend.nhom05.model.AttributeDefinition;
import j2ee_backend.nhom05.service.AttributeDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API quản lý định nghĩa thuộc tính EAV (AttributeDefinition)
 *
 * GET    /api/attribute-definitions               → Tất cả thuộc tính
 * GET    /api/attribute-definitions/active        → Đang hoạt động
 * GET    /api/attribute-definitions/filterable    → Dùng được để lọc sản phẩm
 * GET    /api/attribute-definitions/by-group/{groupId} → Theo nhóm
 * GET    /api/attribute-definitions/{id}          → Chi tiết
 * GET    /api/attribute-definitions/key/{attrKey} → Tra cứu theo key
 * POST   /api/attribute-definitions/add           → Thêm mới
 * PUT    /api/attribute-definitions/update/{id}   → Cập nhật
 * DELETE /api/attribute-definitions/delete/{id}   → Xóa
 */
@RestController
@RequestMapping("/api/attribute-definitions")
@CrossOrigin(origins = "*")
public class AttributeDefinitionController {

    @Autowired
    private AttributeDefinitionService attributeDefinitionService;

    @GetMapping("")
    public ResponseEntity<?> getAll() {
        try {
            return ResponseEntity.ok(new ApiResponse("Lấy danh sách thuộc tính thành công",
                attributeDefinitionService.getAllDefinitions()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActive() {
        try {
            return ResponseEntity.ok(new ApiResponse("Lấy thuộc tính đang hoạt động thành công",
                attributeDefinitionService.getActiveDefinitions()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/filterable")
    public ResponseEntity<?> getFilterable() {
        try {
            return ResponseEntity.ok(new ApiResponse("Lấy thuộc tính có thể lọc thành công",
                attributeDefinitionService.getFilterableDefinitions()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/by-group/{groupId}")
    public ResponseEntity<?> getByGroup(@PathVariable Long groupId) {
        try {
            return ResponseEntity.ok(new ApiResponse("Lấy thuộc tính theo nhóm thành công",
                attributeDefinitionService.getDefinitionsByGroup(groupId)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            AttributeDefinition def = attributeDefinitionService.getDefinitionById(id).orElse(null);
            if (def == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("Không tìm thấy thuộc tính", null));
            }
            return ResponseEntity.ok(new ApiResponse("Lấy thuộc tính thành công", def));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/key/{attrKey}")
    public ResponseEntity<?> getByKey(@PathVariable String attrKey) {
        try {
            AttributeDefinition def = attributeDefinitionService.getDefinitionByKey(attrKey).orElse(null);
            if (def == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("Không tìm thấy thuộc tính với key: " + attrKey, null));
            }
            return ResponseEntity.ok(new ApiResponse("Lấy thuộc tính thành công", def));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> create(@RequestBody AttributeDefinitionRequest request) {
        try {
            AttributeDefinition def = request.toEntity();
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("Thêm thuộc tính thành công",
                    attributeDefinitionService.createDefinition(def, request.getGroupId())));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody AttributeDefinitionRequest request) {
        try {
            AttributeDefinition def = request.toEntity();
            return ResponseEntity.ok(new ApiResponse("Cập nhật thuộc tính thành công",
                attributeDefinitionService.updateDefinition(id, def, request.getGroupId())));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            attributeDefinitionService.deleteDefinition(id);
            return ResponseEntity.ok(new ApiResponse("Xóa thuộc tính thành công", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
}
