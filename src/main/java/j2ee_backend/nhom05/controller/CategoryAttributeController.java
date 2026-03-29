package j2ee_backend.nhom05.controller;

import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.model.CategoryAttribute;
import j2ee_backend.nhom05.service.CategoryAttributeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API quản lý liên kết Category ↔ AttributeDefinition (EAV)
 *
 * GET    /api/category-attributes/by-category/{categoryId}  → Thuộc tính áp dụng cho danh mục
 * POST   /api/category-attributes/assign                     → Gán thuộc tính vào danh mục
 * PUT    /api/category-attributes/update/{id}               → Cập nhật is_required / display_order
 * DELETE /api/category-attributes/remove/{id}               → Gỡ thuộc tính (theo id liên kết)
 * DELETE /api/category-attributes/remove?categoryId=&attrDefId= → Gỡ theo cặp khóa
 */
@RestController
@RequestMapping("/api/category-attributes")
@CrossOrigin(origins = "*")
public class CategoryAttributeController {

    @Autowired
    private CategoryAttributeService categoryAttributeService;

    @GetMapping("/by-category/{categoryId}")
    public ResponseEntity<?> getByCategory(@PathVariable Long categoryId) {
        try {
            return ResponseEntity.ok(new ApiResponse("Lấy thuộc tính của danh mục thành công",
                categoryAttributeService.getAttributesByCategory(categoryId)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    /**
     * Gán thuộc tính vào danh mục.
     * Body: { "categoryId": 2, "attrDefId": 1, "isRequired": true, "displayOrder": 1 }
     */
    @PostMapping("/assign")
    public ResponseEntity<?> assign(
            @RequestParam Long categoryId,
            @RequestParam Long attrDefId,
            @RequestParam(defaultValue = "false") Boolean isRequired,
            @RequestParam(defaultValue = "0") Integer displayOrder) {
        try {
            CategoryAttribute ca = categoryAttributeService.assignAttribute(
                categoryId, attrDefId, isRequired, displayOrder);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("Gán thuộc tính vào danh mục thành công", ca));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean isRequired,
            @RequestParam(required = false) Integer displayOrder) {
        try {
            CategoryAttribute ca = categoryAttributeService.updateAssignment(id, isRequired, displayOrder);
            return ResponseEntity.ok(new ApiResponse("Cập nhật liên kết thành công", ca));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @DeleteMapping("/remove/{id}")
    public ResponseEntity<?> removeById(@PathVariable Long id) {
        try {
            categoryAttributeService.removeAttribute(id);
            return ResponseEntity.ok(new ApiResponse("Gỡ thuộc tính khỏi danh mục thành công", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @DeleteMapping("/remove")
    public ResponseEntity<?> removeByKeys(
            @RequestParam Long categoryId,
            @RequestParam Long attrDefId) {
        try {
            categoryAttributeService.removeAttributeByCategoryAndDef(categoryId, attrDefId);
            return ResponseEntity.ok(new ApiResponse("Gỡ thuộc tính khỏi danh mục thành công", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
}
