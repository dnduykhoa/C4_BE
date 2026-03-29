package j2ee_backend.nhom05.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import j2ee_backend.nhom05.config.RoleAccess;
import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.dto.ProductQuestionAnswerRequest;
import j2ee_backend.nhom05.dto.ProductQuestionCreateRequest;
import j2ee_backend.nhom05.service.ProductQuestionService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ProductQuestionController {

    @Autowired
    private ProductQuestionService productQuestionService;

    @GetMapping("/products/{productId}/questions")
    public ResponseEntity<?> getProductQuestions(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            return ResponseEntity.ok(
                new ApiResponse(
                    "Lấy danh sách hỏi đáp thành công",
                    productQuestionService.getQuestionsByProduct(productId, userDetails)
                )
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PostMapping("/products/{productId}/questions")
    public ResponseEntity<?> createQuestion(
            @PathVariable Long productId,
            @Valid @RequestBody ProductQuestionCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse(
                    "Đặt câu hỏi thành công",
                    productQuestionService.askQuestion(productId, request.getQuestion(), userDetails)
                ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/admin/product-questions")
    public ResponseEntity<?> getAdminQuestions(
            @RequestParam(required = false) Boolean answered,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (!RoleAccess.isBackoffice(userDetails)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse("Bạn không có quyền truy cập", null));
            }

            return ResponseEntity.ok(
                new ApiResponse(
                    "Lấy danh sách hỏi đáp thành công",
                    productQuestionService.getAdminQuestions(answered)
                )
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PatchMapping("/admin/product-questions/{id}/answer")
    public ResponseEntity<?> answerQuestion(
            @PathVariable Long id,
            @Valid @RequestBody ProductQuestionAnswerRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (!RoleAccess.hasAnyRole(userDetails, "STAFF")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse("Chỉ Staff được phản hồi hỏi đáp", null));
            }

            return ResponseEntity.ok(
                new ApiResponse(
                    "Phản hồi câu hỏi thành công",
                    productQuestionService.answerQuestion(id, request.getAnswer(), userDetails)
                )
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
}
