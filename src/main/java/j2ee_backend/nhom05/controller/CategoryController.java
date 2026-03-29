package j2ee_backend.nhom05.controller;

import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.model.Category;
import j2ee_backend.nhom05.dto.CategoryRequest;
import j2ee_backend.nhom05.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategoryController {
    
    @Autowired
    private CategoryService categoryService;
    
    // Lấy tất cả danh mục GỐC (có cấu trúc cây)
    @GetMapping("")
    public ResponseEntity<?> getAllCategories() {
        try {
            List<Category> categories = categoryService.getAllCategories();
            return ResponseEntity.ok(new ApiResponse("Lấy danh sách danh mục thành công", categories));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
    
    // Lấy danh mục theo ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable Long id) {
        try {
            Category category = categoryService.getCategoryById(id).orElse(null);
            if (category == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("Không tìm thấy danh mục", null));
            }
            return ResponseEntity.ok(new ApiResponse("Lấy thông tin danh mục thành công", category));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
    
    // Lấy danh mục gốc (menu chính)
    @GetMapping("/root")
    public ResponseEntity<?> getRootCategories() {
        try {
            List<Category> categories = categoryService.getRootCategories();
            return ResponseEntity.ok(new ApiResponse("Lấy danh mục gốc thành công", categories));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
    
    // Lấy danh mục con
    @GetMapping("/{id}/children")
    public ResponseEntity<?> getChildCategories(@PathVariable Long id) {
        try {
            List<Category> categories = categoryService.getChildCategories(id);
            return ResponseEntity.ok(new ApiResponse("Lấy danh mục con thành công", categories));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
    
    // Lấy danh mục đang hoạt động
    @GetMapping("/active")
    public ResponseEntity<?> getActiveCategories() {
        try {
            List<Category> categories = categoryService.getActiveCategories();
            return ResponseEntity.ok(new ApiResponse("Lấy danh mục đang hoạt động thành công", categories));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
    
    // Thêm danh mục mới
    @PostMapping("/add")
    public ResponseEntity<?> createCategory(@RequestBody CategoryRequest request) {
        try {
            Category category = categoryService.createCategory(
                request.getName(),
                request.getDescription(),
                request.getDisplayOrder(),
                request.getIsActive(),
                request.getParentId()
            );
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("Thêm danh mục thành công", category));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
    
    // Cập nhật danh mục
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody CategoryRequest request) {
        try {
            Category category = categoryService.updateCategory(
                id,
                request.getName(),
                request.getDescription(),
                request.getDisplayOrder(),
                request.getIsActive(),
                request.getParentId()
            );
            return ResponseEntity.ok(new ApiResponse("Cập nhật danh mục thành công", category));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
    
    // Xóa danh mục
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.ok(new ApiResponse("Xóa danh mục thành công", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
    
    // Tìm kiếm danh mục
    @GetMapping("/search")
    public ResponseEntity<?> searchCategories(@RequestParam String name) {
        try {
            List<Category> categories = categoryService.searchCategories(name);
            return ResponseEntity.ok(new ApiResponse("Tìm kiếm thành công", categories));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
}
