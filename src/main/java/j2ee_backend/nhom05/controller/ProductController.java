package j2ee_backend.nhom05.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.dto.ProductComparisonResponse;
import j2ee_backend.nhom05.model.Brand;
import j2ee_backend.nhom05.model.Category;
import j2ee_backend.nhom05.model.Product;
import j2ee_backend.nhom05.model.ProductMedia;
import j2ee_backend.nhom05.model.ProductStatus;
import j2ee_backend.nhom05.repository.IBrandRepository;
import j2ee_backend.nhom05.repository.ICategoryRepository;
import j2ee_backend.nhom05.service.ProductMediaService;
import j2ee_backend.nhom05.service.ProductService;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductMediaService productMediaService;

    @Autowired
    private ICategoryRepository categoryRepository;

    @Autowired
    private IBrandRepository brandRepository;

    // Lấy tất cả sản phẩm
    @GetMapping("")
    public ResponseEntity<?> getAllProducts() {
        try {
            List<Product> products = productService.getAllProducts();
            return ResponseEntity.ok(new ApiResponse("Lấy danh sách sản phẩm thành công", products));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Lấy sản phẩm theo ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        try {
            Product product = productService.getProductById(id).orElse(null);
            if (product == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse("Không tìm thấy sản phẩm", null));
            }
            return ResponseEntity.ok(new ApiResponse("Lấy thông tin sản phẩm thành công", product));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Thêm sản phẩm mới (với đầy đủ thông tin và media)
    @PostMapping("/add")
    public ResponseEntity<?> createProduct(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String boxContents,
            @RequestParam BigDecimal price,
            @RequestParam Integer stockQuantity,
            @RequestParam Long categoryId,
            @RequestParam Long brandId,
            @RequestParam(defaultValue = "ACTIVE") ProductStatus status,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        try {
            // Tạo sản phẩm mới
            Product product = new Product();
            product.setName(name);
            product.setDescription(description);
            product.setBoxContents(boxContents);
            product.setPrice(price);
            product.setStockQuantity(stockQuantity);
            product.setStatus(status);

            // Set category
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));
            product.setCategory(category);

            // Set brand
            Brand brand = brandRepository.findById(brandId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu"));
            product.setBrand(brand);

            // Lưu sản phẩm trước
            Product newProduct = productService.createProduct(product);

            // Upload media nếu có
            if (files != null && files.length > 0) {
                productMediaService.uploadProductMedia(newProduct.getId(), files, true);
            }

            // Tải lại product từ DB để response bao gồm media vừa upload
            Product savedProduct = productService.getProductById(newProduct.getId())
                    .orElse(newProduct);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse("Thêm sản phẩm thành công", savedProduct));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Cập nhật sản phẩm (với hoặc không có media)
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String boxContents,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(required = false) Integer stockQuantity,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "deleteMediaIds", required = false) List<Long> deleteMediaIds) {
        try {
            // Lấy sản phẩm hiện tại
            Product existingProduct = productService.getProductById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

            // Cập nhật các trường được gửi lên (chỉ update field nào có giá trị)
            if (name != null) {
                existingProduct.setName(name);
            }

            if (description != null) {
                existingProduct.setDescription(description);
            }

            if (boxContents != null) {
                existingProduct.setBoxContents(boxContents);
            }

            if (price != null) {
                existingProduct.setPrice(price);
            }

            if (stockQuantity != null) {
                existingProduct.setStockQuantity(stockQuantity);
            }

            if (status != null) {
                existingProduct.setStatus(status);
            }

            if (categoryId != null) {
                Category category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));
                existingProduct.setCategory(category);
            }

            if (brandId != null) {
                Brand brand = brandRepository.findById(brandId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu"));
                existingProduct.setBrand(brand);
            }

            // Xóa media cũ nếu có danh sách deleteMediaIds
            if (deleteMediaIds != null && !deleteMediaIds.isEmpty()) {
                for (Long mediaId : deleteMediaIds) {
                    productMediaService.deleteParentProductMedia(id, mediaId);
                }
            }

            // Upload media mới nếu có
            if (files != null && files.length > 0) {
                productMediaService.uploadProductMedia(id, files, false);
            }

            // Lưu product
            Product updatedProduct = productService.updateProduct(id, existingProduct);

            return ResponseEntity.ok(new ApiResponse("Cập nhật sản phẩm thành công", updatedProduct));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Ngừng kinh doanh sản phẩm (xóa mềm - chuyển isActive = false)

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            Product product = productService.deleteProduct(id);
            return ResponseEntity.ok(new ApiResponse("Đã ngừng kinh doanh sản phẩm", product));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Tìm kiếm sản phẩm theo tên
    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(@RequestParam String name) {
        try {
            List<Product> products = productService.searchProductsByName(name);
            return ResponseEntity.ok(new ApiResponse("Tìm kiếm thành công", products));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Lấy sản phẩm theo danh mục
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?> getProductsByCategory(@PathVariable Long categoryId) {
        try {
            List<Product> products = productService.getProductsByCategory(categoryId);
            return ResponseEntity.ok(new ApiResponse("Lấy sản phẩm theo danh mục thành công", products));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Lấy sản phẩm cùng loại để gợi ý so sánh
    @GetMapping("/{productId}/same-type")
    public ResponseEntity<?> getSameTypeProducts(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "10") Integer limit) {
        try {
            List<Product> products = productService.getSameTypeProducts(productId, limit);
            return ResponseEntity.ok(new ApiResponse("Lấy danh sách sản phẩm cùng loại thành công", products));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // So sánh nhiều sản phẩm cùng loại theo thông số kỹ thuật
    @GetMapping("/compare")
    public ResponseEntity<?> compareProducts(@RequestParam List<Long> ids) {
        try {
            ProductComparisonResponse comparison = productService.compareProducts(ids);
            return ResponseEntity.ok(new ApiResponse("So sánh sản phẩm cùng loại thành công", comparison));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Lấy sản phẩm theo nhiều categoryId (dùng cho multi-select danh mục con)
    @GetMapping("/categories")
    public ResponseEntity<?> getProductsByCategories(@RequestParam List<Long> ids) {
        try {
            List<Product> products = productService.getProductsByCategories(ids);
            return ResponseEntity.ok(new ApiResponse("Lấy sản phẩm theo nhiều danh mục thành công", products));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Lấy sản phẩm theo danh mục cha và tất cả danh mục con
    @GetMapping("/category/{categoryId}/with-children")
    public ResponseEntity<?> getProductsByCategoryTree(@PathVariable Long categoryId) {
        try {
            List<Product> products = productService.getProductsByCategoryTree(categoryId);
            return ResponseEntity.ok(new ApiResponse("Lấy sản phẩm theo cây danh mục thành công", products));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Lấy sản phẩm theo thương hiệu
    @GetMapping("/brand/{brandId}")
    public ResponseEntity<?> getProductsByBrand(@PathVariable Long brandId) {
        try {
            List<Product> products = productService.getProductsByBrand(brandId);
            return ResponseEntity.ok(new ApiResponse("Lấy sản phẩm theo thương hiệu thành công", products));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Lấy sản phẩm đang hoạt động (status = ACTIVE)
    @GetMapping("/active")
    public ResponseEntity<?> getActiveProducts() {
        try {
            List<Product> products = productService.getActiveProducts();
            return ResponseEntity.ok(new ApiResponse("Lấy sản phẩm đang hoạt động thành công", products));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Lấy sản phẩm hàng mới về (status = NEW_ARRIVAL)
    @GetMapping("/new-arrival")
    public ResponseEntity<?> getNewArrivalProducts() {
        try {
            List<Product> products = productService.getNewArrivalProducts();
            return ResponseEntity.ok(new ApiResponse("Lấy sản phẩm hàng mới về thành công", products));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Lấy sản phẩm hàng sắp về (status = OUT_OF_STOCK)
    @GetMapping("/out-of-stock")
    public ResponseEntity<?> getOutOfStockProducts() {
        try {
            List<Product> products = productService.getOutOfStockProducts();
            return ResponseEntity.ok(new ApiResponse("Lấy sản phẩm hàng sắp về thành công", products));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Lấy sản phẩm ngưng kinh doanh (status = INACTIVE)
    @GetMapping("/inactive")
    public ResponseEntity<?> getInactiveProducts() {
        try {
            List<Product> products = productService.getInactiveProducts();
            return ResponseEntity.ok(new ApiResponse("Lấy sản phẩm ngừng kinh doanh thành công", products));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Lấy sản phẩm theo khoảng giá
    @GetMapping("/price-range")
    public ResponseEntity<?> getProductsByPriceRange(@RequestParam BigDecimal min, @RequestParam BigDecimal max) {
        try {
            List<Product> products = productService.getProductsByPriceRange(min, max);
            return ResponseEntity.ok(new ApiResponse("Lấy sản phẩm theo khoảng giá thành công", products));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Lấy tất cả media của sản phẩm
    @GetMapping("/{productId}/media")
    public ResponseEntity<?> getProductMedia(@PathVariable Long productId) {
        try {
            List<ProductMedia> mediaList = productMediaService.getProductMedia(productId);
            return ResponseEntity.ok(new ApiResponse("Lấy media thành công", mediaList));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Upload media cho sản phẩm cha (hỗ trợ upload theo lô nhỏ từ frontend)
    @PostMapping("/{productId}/media/upload")
    public ResponseEntity<?> uploadProductMedia(
            @PathVariable Long productId,
            @RequestParam(value = "files") MultipartFile[] files,
            @RequestParam(defaultValue = "true") boolean isPrimary) {
        try {
            List<ProductMedia> uploaded = productMediaService.uploadProductMedia(productId, files, isPrimary);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse("Upload media sản phẩm thành công", uploaded));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Xóa một media theo ID
    @DeleteMapping("/{productId}/media/{mediaId}")
    public ResponseEntity<?> deleteMedia(@PathVariable Long productId, @PathVariable Long mediaId) {
        try {
            productMediaService.deleteParentProductMedia(productId, mediaId);
            return ResponseEntity.ok(new ApiResponse("Xóa media thành công", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Đặt một media làm ảnh chính
    @PutMapping("/{productId}/media/{mediaId}/set-primary")
    public ResponseEntity<?> setPrimaryMedia(@PathVariable Long productId, @PathVariable Long mediaId) {
        try {
            ProductMedia media = productMediaService.setPrimaryMedia(productId, mediaId);
            return ResponseEntity.ok(new ApiResponse("Đặt ảnh chính thành công", media));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Bật/tắt trạng thái hoạt động của sản phẩm (ACTIVE <-> INACTIVE)
    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<?> toggleActive(@PathVariable Long id) {
        try {
            Product product = productService.getProductById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
            ProductStatus newStatus = product.getStatus() == ProductStatus.ACTIVE
                    ? ProductStatus.INACTIVE
                    : ProductStatus.ACTIVE;
            product.setStatus(newStatus);
            Product updated = productService.updateProduct(id, product);
            String statusLabel = updated.getStatus() == ProductStatus.ACTIVE ? "kích hoạt" : "vô hiệu hóa";
            return ResponseEntity.ok(new ApiResponse("Đã " + statusLabel + " sản phẩm", updated));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Đánh dấu hàng sắp về (status = OUT_OF_STOCK)
    @PatchMapping("/{id}/out-of-stock")
    public ResponseEntity<?> markOutOfStock(@PathVariable Long id) {
        try {
            Product product = productService.markOutOfStock(id);
            return ResponseEntity.ok(new ApiResponse("Đã đánh dấu sản phẩm hàng sắp về", product));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Đánh dấu hàng mới về (status = NEW_ARRIVAL)
    @PatchMapping("/{id}/new-arrival")
    public ResponseEntity<?> markNewArrival(@PathVariable Long id) {
        try {
            Product product = productService.markNewArrival(id);
            return ResponseEntity.ok(new ApiResponse("Đã đánh dấu sản phẩm hàng mới về", product));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Kích hoạt lại sản phẩm (status = ACTIVE)
    @PatchMapping("/{id}/restore")
    public ResponseEntity<?> restoreProduct(@PathVariable Long id) {
        try {
            Product product = productService.restoreProduct(id);
            return ResponseEntity.ok(new ApiResponse("Đã kích hoạt lại sản phẩm", product));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Lọc sản phẩm kết hợp (danh mục + thương hiệu + khoảng giá + tên)
    @GetMapping("/filter")
    public ResponseEntity<?> filterProducts(
            @RequestParam(required = false) List<Long> categoryIds,
            @RequestParam(required = false) List<Long> brandIds,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String name) {
        try {
            List<Product> products = productService.filterProducts(categoryIds, brandIds, minPrice, maxPrice, name);
            return ResponseEntity.ok(new ApiResponse("Lọc sản phẩm thành công", products));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }
}
