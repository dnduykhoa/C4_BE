package j2ee_backend.nhom05.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.dto.pcbuilder.PcBuildSelection;
import j2ee_backend.nhom05.dto.pcbuilder.PcBuilderRequestDto;
import j2ee_backend.nhom05.service.PcBuilderService;

@RestController
@RequestMapping("/api/products/pc-builder")
@CrossOrigin(origins = "*")
public class PcBuilderController {

    @Autowired
    private PcBuilderService pcBuilderService;

    @GetMapping("/slots")
    public ResponseEntity<?> getSlots() {
        try {
            return ResponseEntity.ok(new ApiResponse("Lấy danh sách slot build PC thành công", pcBuilderService.getSlots()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/options")
    public ResponseEntity<?> getOptions(
            @RequestParam String slot,
            PcBuildSelection selection) {
        try {
            return ResponseEntity.ok()
                .header("X-API-Deprecated", "true")
                .header("X-API-Replacement", "POST /api/products/pc-builder/options")
                .body(new ApiResponse(
                    "Lấy danh sách linh kiện theo slot thành công",
                    pcBuilderService.getOptions(slot, selection)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(PcBuildSelection selection) {
        try {
            return ResponseEntity.ok()
                    .header("X-API-Deprecated", "true")
                    .header("X-API-Replacement", "POST /api/products/pc-builder/summary")
                    .body(new ApiResponse(
                            "Đánh giá build PC thành công",
                            pcBuilderService.getSummary(selection)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PostMapping("/options")
    public ResponseEntity<?> getOptionsPost(@RequestBody(required = false) PcBuilderRequestDto request) {
        try {
            PcBuilderRequestDto safeRequest = request != null ? request : new PcBuilderRequestDto();
            return ResponseEntity.ok(new ApiResponse(
                    "Lấy danh sách linh kiện theo slot thành công",
                    pcBuilderService.getOptions(safeRequest.getSlot(), safeRequest.getSelection())));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PostMapping("/summary")
    public ResponseEntity<?> getSummaryPost(@RequestBody(required = false) PcBuilderRequestDto request) {
        try {
            PcBuilderRequestDto safeRequest = request != null ? request : new PcBuilderRequestDto();
            return ResponseEntity.ok(new ApiResponse(
                    "Đánh giá build PC thành công",
                    pcBuilderService.getSummary(safeRequest.getSelection())));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PostMapping("/checkout-preview")
    public ResponseEntity<?> getCheckoutPreview(@RequestBody(required = false) PcBuilderRequestDto request) {
        try {
            PcBuilderRequestDto safeRequest = request != null ? request : new PcBuilderRequestDto();
            return ResponseEntity.ok(new ApiResponse(
                    "Chuẩn hóa build thành cart item thành công",
                    pcBuilderService.getCheckoutPreview(safeRequest.getSelection())));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }
}
