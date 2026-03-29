package j2ee_backend.nhom05.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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

import j2ee_backend.nhom05.config.RoleAccess;
import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.dto.CartItemRequest;
import j2ee_backend.nhom05.dto.CartResponse;
import j2ee_backend.nhom05.dto.UpdateCartItemRequest;
import j2ee_backend.nhom05.service.CartService;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
public class CartController {

    @Autowired
    private CartService cartService;

    // Helper method: Check if user is backoffice (Admin/Manager/Staff)
    private ResponseEntity<?> checkCustomerOnly(UserDetails userDetails) {
        if (RoleAccess.hasAnyRole(userDetails, "ADMIN", "MANAGER", "STAFF")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse("Chỉ khách hàng có thể truy cập giỏ hàng", null));
        }
        return null;
    }

    // GET /api/cart?userId={userId}
    // Lấy giỏ hàng của user (tạo mới nếu chưa có)
    @GetMapping("")
    public ResponseEntity<?> getCart(@AuthenticationPrincipal UserDetails userDetails,
                                     @RequestParam Long userId) {
        // Check if user is backoffice
        ResponseEntity<?> customerCheck = checkCustomerOnly(userDetails);
        if (customerCheck != null) return customerCheck;

        try {
            CartResponse cartResponse = cartService.getCartByUserId(userId);
            return ResponseEntity.ok(new ApiResponse("Lấy giỏ hàng thành công", cartResponse));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // POST /api/cart/items?userId={userId}
    // Thêm sản phẩm vào giỏ hàng
    @PostMapping("/items")
    public ResponseEntity<?> addToCart(@AuthenticationPrincipal UserDetails userDetails,
                                       @RequestParam Long userId,
                                       @RequestBody CartItemRequest request) {
        // Check if user is backoffice
        ResponseEntity<?> customerCheck = checkCustomerOnly(userDetails);
        if (customerCheck != null) return customerCheck;

        try {
            CartResponse cartResponse = cartService.addToCart(
                userId,
                request.getProductId(),
                request.getVariantId(),
                request.getQuantity()
            );
            return ResponseEntity.ok(new ApiResponse("Thêm vào giỏ hàng thành công", cartResponse));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // PUT /api/cart/items/{itemId}?userId={userId}
    // Cập nhật số lượng sản phẩm trong giỏ hàng, body: { "quantity": N }
    @PutMapping("/items/{itemId}")
    public ResponseEntity<?> updateCartItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long itemId,
            @RequestParam Long userId,
            @RequestBody UpdateCartItemRequest request) {
        // Check if user is backoffice
        ResponseEntity<?> customerCheck = checkCustomerOnly(userDetails);
        if (customerCheck != null) return customerCheck;

        try {
            CartResponse cartResponse = cartService.updateCartItem(userId, itemId, request.getQuantity());
            return ResponseEntity.ok(new ApiResponse("Cập nhật giỏ hàng thành công", cartResponse));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // DELETE /api/cart/items/{itemId}?userId={userId}
    // Xóa một sản phẩm khỏi giỏ hàng
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> removeCartItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long itemId,
            @RequestParam Long userId) {
        // Check if user is backoffice
        ResponseEntity<?> customerCheck = checkCustomerOnly(userDetails);
        if (customerCheck != null) return customerCheck;

        try {
            CartResponse cartResponse = cartService.removeCartItem(userId, itemId);
            return ResponseEntity.ok(new ApiResponse("Xóa sản phẩm khỏi giỏ hàng thành công", cartResponse));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // DELETE /api/cart?userId={userId}
    // Xóa toàn bộ giỏ hàng
    @DeleteMapping("")
    public ResponseEntity<?> clearCart(@AuthenticationPrincipal UserDetails userDetails,
                                       @RequestParam Long userId) {
        // Check if user is backoffice
        ResponseEntity<?> customerCheck = checkCustomerOnly(userDetails);
        if (customerCheck != null) return customerCheck;

        try {
            cartService.clearCart(userId);
            return ResponseEntity.ok(new ApiResponse("Đã xóa toàn bộ giỏ hàng", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // GET /api/cart/validate?userId={userId}
    // Kiểm tra giỏ hàng trước khi thanh toán
    @GetMapping("/validate")
    public ResponseEntity<?> validateCart(@RequestParam Long userId) {
        try {
            List<String> errors = cartService.validateCartForCheckout(userId);
            if (errors.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse("Giỏ hàng hợp lệ, có thể thanh toán", null));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("Giỏ hàng có sản phẩm không thể thanh toán", errors));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
}
