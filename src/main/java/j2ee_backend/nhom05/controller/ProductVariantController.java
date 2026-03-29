package j2ee_backend.nhom05.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.dto.ProductVariantRequest;
import j2ee_backend.nhom05.dto.VariantResolveRequest;
import j2ee_backend.nhom05.model.ProductMedia;
import j2ee_backend.nhom05.model.ProductVariant;
import j2ee_backend.nhom05.service.ProductMediaService;
import j2ee_backend.nhom05.service.ProductVariantService;

@RestController
@RequestMapping("/api/products/{productId}/variants")
@CrossOrigin(origins = "*")
public class ProductVariantController {

    @Autowired
    private ProductVariantService productVariantService;

    @Autowired
    private ProductMediaService productMediaService;

    @GetMapping("")
    public ResponseEntity<?> getByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "false") boolean onlyActive) {
        try {
            return ResponseEntity.ok(new ApiResponse("Lấy danh sách biến thể thành công",
                productVariantService.getVariantsByProduct(productId, onlyActive)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/{variantId}")
    public ResponseEntity<?> getById(@PathVariable Long productId, @PathVariable Long variantId) {
        try {
            ProductVariant variant = productVariantService.getVariantById(productId, variantId).orElse(null);
            if (variant == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("Không tìm thấy biến thể", null));
            }
            return ResponseEntity.ok(new ApiResponse("Lấy biến thể thành công", variant));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> create(@PathVariable Long productId, @RequestBody ProductVariantRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("Tạo biến thể thành công", productVariantService.createVariant(productId, request)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PutMapping("/update/{variantId}")
    public ResponseEntity<?> update(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @RequestBody ProductVariantRequest request) {
        try {
            return ResponseEntity.ok(new ApiResponse("Cập nhật biến thể thành công",
                productVariantService.updateVariant(productId, variantId, request)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @DeleteMapping("/delete/{variantId}")
    public ResponseEntity<?> delete(@PathVariable Long productId, @PathVariable Long variantId) {
        try {
            productVariantService.deleteVariant(productId, variantId);
            return ResponseEntity.ok(new ApiResponse("Xóa biến thể thành công", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/{variantId}/media")
    public ResponseEntity<?> getVariantMedia(@PathVariable Long productId, @PathVariable Long variantId) {
        try {
            return ResponseEntity.ok(new ApiResponse("Lấy media biến thể thành công",
                productMediaService.getVariantMedia(productId, variantId)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PostMapping("/{variantId}/media/upload")
    public ResponseEntity<?> uploadVariantMedia(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @RequestParam(value = "files") MultipartFile[] files,
            @RequestParam(defaultValue = "false") boolean isPrimary) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("Upload media biến thể thành công",
                    productMediaService.uploadProductMedia(productId, variantId, files, isPrimary)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @DeleteMapping("/{variantId}/media/{mediaId}")
    public ResponseEntity<?> deleteVariantMedia(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @PathVariable Long mediaId) {
        try {
            ProductMedia media = productMediaService.getVariantMedia(productId, variantId)
                .stream()
                .filter(m -> m.getId().equals(mediaId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy media biến thể"));
            productMediaService.deleteProductMedia(media.getId());
            return ResponseEntity.ok(new ApiResponse("Xóa media biến thể thành công", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PutMapping("/{variantId}/media/{mediaId}/set-primary")
    public ResponseEntity<?> setPrimaryVariantMedia(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @PathVariable Long mediaId) {
        try {
            productMediaService.getVariantMedia(productId, variantId)
                .stream()
                .filter(m -> m.getId().equals(mediaId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy media biến thể"));
            ProductMedia media = productMediaService.setPrimaryMedia(productId, mediaId);
            return ResponseEntity.ok(new ApiResponse("Đặt ảnh chính cho biến thể thành công", media));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/options")
    public ResponseEntity<?> getOptions(@PathVariable Long productId) {
        try {
            Map<String, java.util.List<String>> options = productVariantService.getVariantOptions(productId);
            return ResponseEntity.ok(new ApiResponse("Lấy danh sách lựa chọn biến thể thành công", options));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PostMapping("/resolve")
    public ResponseEntity<?> resolve(@PathVariable Long productId, @RequestBody VariantResolveRequest request) {
        try {
            ProductVariant variant = productVariantService.resolveVariant(productId, request.getSelections()).orElse(null);
            if (variant == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("Không tìm thấy biến thể phù hợp", null));
            }
            return ResponseEntity.ok(new ApiResponse("Resolve biến thể thành công", variant));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
}
