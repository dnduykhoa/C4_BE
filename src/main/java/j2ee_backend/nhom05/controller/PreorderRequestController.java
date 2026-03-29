package j2ee_backend.nhom05.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.dto.PreorderRegistrationRequest;
import j2ee_backend.nhom05.service.PreorderRequestService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/preorders")
@CrossOrigin(origins = "*")
public class PreorderRequestController {

    @Autowired
    private PreorderRequestService preorderRequestService;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody PreorderRegistrationRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("Đăng ký chờ hàng thành công", preorderRequestService.createRequest(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
}