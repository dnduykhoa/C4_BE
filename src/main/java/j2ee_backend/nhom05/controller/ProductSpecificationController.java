package j2ee_backend.nhom05.controller;

import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.model.Product;
import j2ee_backend.nhom05.model.ProductSpecification;
import j2ee_backend.nhom05.repository.IProductRepository;
import j2ee_backend.nhom05.service.ProductSpecificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * API quản lý EAV Value (ProductSpecification)
 *
 * GET    /api/products/{productId}/specifications              → Tất cả thuộc tính của sản phẩm
 * GET    /api/products/{productId}/specifications/{id}        → Chi tiết một thuộc tính
 * POST   /api/products/{productId}/specifications/add         → Thêm thuộc tính cho sản phẩm
 * PUT    /api/products/{productId}/specifications/update/{id} → Cập nhật giá trị
 * DELETE /api/products/{productId}/specifications/delete/{id} → Xóa một thuộc tính
 * DELETE /api/products/{productId}/specifications/clear       → Xóa tất cả thuộc tính
 *
 * GET    /api/products/filter/by-spec   → Lọc sản phẩm theo giá trị số
 *         ?attrKey=ram&minValue=8&maxValue=32
 */
@RestController
@CrossOrigin(origins = "*")
public class ProductSpecificationController {

    @Autowired
    private ProductSpecificationService specService;

    @Autowired
    private IProductRepository productRepository;

    // ------------------------------------------------------------------
    // CRUD thuộc tính của sản phẩm
    // ------------------------------------------------------------------

    @GetMapping("/api/products/{productId}/specifications")
    public ResponseEntity<?> getSpecs(@PathVariable Long productId) {
        try {
            return ResponseEntity.ok(new ApiResponse("Lấy thuộc tính sản phẩm thành công",
                specService.getSpecsByProduct(productId)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/api/products/{productId}/specifications/{id}")
    public ResponseEntity<?> getSpecById(@PathVariable Long productId, @PathVariable Long id) {
        try {
            ProductSpecification spec = specService.getSpecById(id).orElse(null);
            if (spec == null || !spec.getProduct().getId().equals(productId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("Không tìm thấy thuộc tính", null));
            }
            return ResponseEntity.ok(new ApiResponse("Lấy thuộc tính thành công", spec));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    /**
     * Thêm thuộc tính cho sản phẩm.
     *
     * Cách 1 — EAV chuẩn hóa (khuyến nghị):
     *   POST .../add?attrDefId=1&specValue=AMD+Ryzen+5+5600X
     *   POST .../add?attrDefId=2&valueNumber=8&displayOrder=2
     *
     * Cách 2 — Tự do (legacy/fallback):
     *   POST .../add?specKey=CPU&specValue=AMD+Ryzen+5+5600X
     */
    @PostMapping("/api/products/{productId}/specifications/add")
    public ResponseEntity<?> addSpec(
            @PathVariable Long productId,
            @RequestParam(required = false) Long attrDefId,
            @RequestParam(required = false) String specKey,
            @RequestParam(required = false) String specValue,
            @RequestParam(required = false) BigDecimal valueNumber,
            @RequestParam(defaultValue = "0") Integer displayOrder) {
        try {
            if (attrDefId == null && (specKey == null || specKey.isBlank())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("Phải cung cấp attrDefId hoặc specKey", null));
            }
            ProductSpecification spec = specService.addSpec(
                productId, attrDefId, specKey, specValue, valueNumber, displayOrder);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("Thêm thuộc tính sản phẩm thành công", spec));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PutMapping("/api/products/{productId}/specifications/update/{id}")
    public ResponseEntity<?> updateSpec(
            @PathVariable Long productId,
            @PathVariable Long id,
            @RequestParam(required = false) String specValue,
            @RequestParam(required = false) BigDecimal valueNumber,
            @RequestParam(required = false) Integer displayOrder) {
        try {
            ProductSpecification spec = specService.updateSpec(id, specValue, valueNumber, displayOrder);
            return ResponseEntity.ok(new ApiResponse("Cập nhật thuộc tính sản phẩm thành công", spec));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @DeleteMapping("/api/products/{productId}/specifications/delete/{id}")
    public ResponseEntity<?> deleteSpec(@PathVariable Long productId, @PathVariable Long id) {
        try {
            specService.deleteSpec(id);
            return ResponseEntity.ok(new ApiResponse("Xóa thuộc tính sản phẩm thành công", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @DeleteMapping("/api/products/{productId}/specifications/clear")
    public ResponseEntity<?> clearSpecs(@PathVariable Long productId) {
        try {
            specService.deleteAllSpecsByProduct(productId);
            return ResponseEntity.ok(new ApiResponse("Đã xóa tất cả thuộc tính của sản phẩm", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // ------------------------------------------------------------------
    // Lọc sản phẩm theo giá trị số EAV
    // ------------------------------------------------------------------

    /**
     * Lọc sản phẩm theo giá trị số của thuộc tính.
     * VD: GET /api/products/filter/by-spec?attrKey=ram&minValue=8
     *     → Trả về sản phẩm có RAM >= 8 GB
     */
    @GetMapping("/api/products/filter/by-spec")
    public ResponseEntity<?> filterBySpec(
            @RequestParam String attrKey,
            @RequestParam(required = false) BigDecimal minValue,
            @RequestParam(required = false) BigDecimal maxValue) {
        try {
            List<Long> productIds = specService.filterProductIdsByNumericSpec(attrKey, minValue, maxValue);
            List<Product> products = productRepository.findAllById(productIds);
            return ResponseEntity.ok(new ApiResponse(
                "Lọc sản phẩm theo " + attrKey + " thành công (" + products.size() + " kết quả)", products));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
}
