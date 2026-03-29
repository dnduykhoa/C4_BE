package j2ee_backend.nhom05.controller;

import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.dto.CarouselSlideRequest;
import j2ee_backend.nhom05.model.CarouselSlide;
import j2ee_backend.nhom05.service.CarouselSlideService;
import j2ee_backend.nhom05.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/carousel")
@CrossOrigin(origins = "*")
public class CarouselSlideController {

    @Autowired
    private CarouselSlideService service;

    @Autowired
    private FileStorageService fileStorageService;

    // POST /api/carousel/upload — upload file ảnh hoặc video
    @PostMapping("/upload")
    public ResponseEntity<?> uploadMedia(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse("File không hợp lệ", null));
            }
            boolean isImage = fileStorageService.isImageFile(file);
            boolean isVideo = fileStorageService.isVideoFile(file);
            if (!isImage && !isVideo) {
                return ResponseEntity.badRequest().body(new ApiResponse("Chỉ chấp nhận file hình ảnh hoặc video", null));
            }
            String filePath = fileStorageService.storeFile(file, "carousel");
            String mediaType = isVideo ? "VIDEO" : "IMAGE";
            return ResponseEntity.ok(new ApiResponse("Upload thành công", Map.of(
                "url", "/images/" + filePath,
                "mediaType", mediaType
            )));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // GET /api/carousel — public, trả về slides đang active (cho HomePage)
    @GetMapping
    public ResponseEntity<?> getActiveSlides() {
        List<CarouselSlide> slides = service.getActiveSlides();
        return ResponseEntity.ok(new ApiResponse("Thành công", slides));
    }

    // GET /api/carousel/all — admin, trả về tất cả slides
    @GetMapping("/all")
    public ResponseEntity<?> getAllSlides() {
        List<CarouselSlide> slides = service.getAllSlides();
        return ResponseEntity.ok(new ApiResponse("Thành công", slides));
    }

    // POST /api/carousel — admin, tạo slide mới
    @PostMapping
    public ResponseEntity<?> create(@RequestBody CarouselSlideRequest req) {
        try {
            CarouselSlide slide = service.create(req);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse("Tạo slide thành công", slide));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // PUT /api/carousel/{id} — admin, cập nhật slide
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody CarouselSlideRequest req) {
        try {
            CarouselSlide slide = service.update(id, req);
            return ResponseEntity.ok(new ApiResponse("Cập nhật slide thành công", slide));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // DELETE /api/carousel/{id} — admin, xóa slide
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(new ApiResponse("Xóa slide thành công", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }
}
