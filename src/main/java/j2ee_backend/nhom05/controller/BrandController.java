package j2ee_backend.nhom05.controller;

import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.model.Brand;
import j2ee_backend.nhom05.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/brands")
@CrossOrigin(origins = "*")
public class BrandController {
    
    @Autowired
    private BrandService brandService;
    
    // Lấy tất cả brand
    @GetMapping("")
    public ResponseEntity<?> getAllBrands() {
        try {
            List<Brand> brands = brandService.getAllBrands();
            return ResponseEntity.ok(new ApiResponse("Lấy danh sách thương hiệu thành công", brands));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
    
    // Lấy brand theo ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getBrandById(@PathVariable Long id) {
        try {
            Brand brand = brandService.getBrandById(id).orElse(null);
            if (brand == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("Không tìm thấy thương hiệu", null));
            }
            return ResponseEntity.ok(new ApiResponse("Lấy thông tin thương hiệu thành công", brand));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
    
    // Lấy brand đang hoạt động
    @GetMapping("/active")
    public ResponseEntity<?> getActiveBrands() {
        try {
            List<Brand> brands = brandService.getActiveBrands();
            return ResponseEntity.ok(new ApiResponse("Lấy thương hiệu đang hoạt động thành công", brands));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Lấy brand theo danh mục cha (bao gồm cả danh mục con)
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?> getBrandsByCategory(@PathVariable Long categoryId) {
        try {
            List<Brand> brands = brandService.getBrandsByCategory(categoryId);
            return ResponseEntity.ok(new ApiResponse("Lấy thương hiệu theo danh mục thành công", brands));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
    
    // Thêm brand mới (multipart/form-data)
    @PostMapping("/add")
    public ResponseEntity<?> createBrand(
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "displayOrder", defaultValue = "0") Integer displayOrder,
            @RequestParam(value = "isActive", defaultValue = "true") Boolean isActive,
            @RequestParam(value = "logo", required = false) MultipartFile logo) {
        try {
            Brand brand = new Brand();
            brand.setName(name);
            brand.setDescription(description);
            brand.setDisplayOrder(displayOrder);
            brand.setIsActive(isActive);
            Brand newBrand = brandService.createBrand(brand, logo);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("Thêm thương hiệu thành công", newBrand));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
    
    // Cập nhật brand (multipart/form-data)
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateBrand(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "displayOrder", defaultValue = "0") Integer displayOrder,
            @RequestParam(value = "isActive", defaultValue = "true") Boolean isActive,
            @RequestParam(value = "logo", required = false) MultipartFile logo) {
        try {
            Brand brandDetails = new Brand();
            brandDetails.setName(name);
            brandDetails.setDescription(description);
            brandDetails.setDisplayOrder(displayOrder);
            brandDetails.setIsActive(isActive);
            Brand updatedBrand = brandService.updateBrand(id, brandDetails, logo);
            return ResponseEntity.ok(new ApiResponse("Cập nhật thương hiệu thành công", updatedBrand));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
    
    // Xóa brand
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteBrand(@PathVariable Long id) {
        try {
            brandService.deleteBrand(id);
            return ResponseEntity.ok(new ApiResponse("Xóa thương hiệu thành công", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
    
    // Tìm kiếm brand
    @GetMapping("/search")
    public ResponseEntity<?> searchBrands(@RequestParam String name) {
        try {
            List<Brand> brands = brandService.searchBrands(name);
            return ResponseEntity.ok(new ApiResponse("Tìm kiếm thành công", brands));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
}
