package j2ee_backend.nhom05.controller;

import java.util.List;
import java.util.Map;

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
import org.springframework.web.bind.annotation.RestController;

import j2ee_backend.nhom05.config.RoleAccess;
import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.dto.OrderRequest;
import j2ee_backend.nhom05.dto.OrderResponse;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.service.MomoService;
import j2ee_backend.nhom05.service.OrderService;
import j2ee_backend.nhom05.service.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private MomoService momoService;

    @Autowired
    private VnpayService vnpayService;

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    // ── POST /api/orders ──────────────────────────────────────────────────────
    // Tạo đơn hàng từ giỏ hàng (yêu cầu đăng nhập) - CUSTOMER ONLY
    @PostMapping
    public ResponseEntity<?> createOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OrderRequest request,
            HttpServletRequest httpRequest) {
        // Check if user is backoffice (Admin/Manager/Staff cannot create orders)
        if (RoleAccess.hasAnyRole(userDetails, "ADMIN", "MANAGER", "STAFF")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse("Chỉ khách hàng có thể tạo đơn hàng", null));
        }

        try {
            Long userId = ((User) userDetails).getId();
            OrderResponse order = orderService.createOrder(userId, request);

            // Nếu chọn VNPAY → tạo URL thanh toán VNPay
            if ("VNPAY".equalsIgnoreCase(request.getPaymentMethod())) {
                String ip = resolveClientIp(httpRequest);
                String vnpayUrl = vnpayService.createPaymentUrl(
                    order.getOrderCode(), order.getTotalAmount(), ip);
                order.setVnpayUrl(vnpayUrl);
            }

            // Nếu chọn MOMO → tạo URL thanh toán MoMo
            if ("MOMO".equalsIgnoreCase(request.getPaymentMethod())) {
                String momoUrl = momoService.createPaymentUrl(
                    order.getOrderCode(), order.getTotalAmount());
                order.setMomoUrl(momoUrl);
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("Đặt hàng thành công", order));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // ── GET /api/orders/my ────────────────────────────────────────────────────
    // Lấy danh sách đơn hàng của user hiện tại
    @GetMapping("/my")
    public ResponseEntity<?> getMyOrders(@AuthenticationPrincipal UserDetails userDetails) {
        if (RoleAccess.hasAnyRole(userDetails, "ADMIN", "MANAGER", "STAFF")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse("Trang đơn hàng chỉ dành cho khách hàng", null));
        }
        try {
            Long userId = ((User) userDetails).getId();
            List<OrderResponse> orders = orderService.getOrdersByUser(userId);
            return ResponseEntity.ok(new ApiResponse("Lấy danh sách đơn hàng thành công", orders));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // ── GET /api/orders/{id} ──────────────────────────────────────────────────
    // Lấy chi tiết đơn hàng (user chỉ xem đơn của mình, ADMIN xem tất cả)
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = ((User) userDetails).getId();
            boolean canViewAll = RoleAccess.hasAnyRole(userDetails, "ADMIN", "MANAGER", "STAFF");
            OrderResponse order = orderService.getOrderById(id, userId, canViewAll);
            return ResponseEntity.ok(new ApiResponse("Lấy thông tin đơn hàng thành công", order));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // ── POST /api/orders/{id}/cancel ──────────────────────────────────────────
    // User tự huỷ đơn hàng (chỉ khi đơn đang PENDING)
    // Body (optional): { "cancelReason": "..." }
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = ((User) userDetails).getId();
            String cancelReason = (body != null) ? body.get("cancelReason") : null;
            OrderResponse order = orderService.cancelOrder(id, userId, cancelReason);
            return ResponseEntity.ok(new ApiResponse("Huỷ đơn hàng thành công", order));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // ── POST /api/orders/{id}/retry-payment ──────────────────────────────────
    // User thanh toán lại đơn online còn hạn
    @PostMapping("/{id}/retry-payment")
    public ResponseEntity<?> retryPayment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        try {
            Long userId = ((User) userDetails).getId();
            OrderResponse order = orderService.retryPayment(id, userId);

            // Tạo lại URL thanh toán theo phương thức đơn hàng
            if ("VNPAY".equalsIgnoreCase(order.getPaymentMethod())) {
                String ip = resolveClientIp(httpRequest);
                String vnpayUrl = vnpayService.createPaymentUrl(
                    order.getOrderCode(), order.getTotalAmount(), ip);
                order.setVnpayUrl(vnpayUrl);
            }
            if ("MOMO".equalsIgnoreCase(order.getPaymentMethod())) {
                String momoUrl = momoService.createPaymentUrl(
                    order.getOrderCode(), order.getTotalAmount());
                order.setMomoUrl(momoUrl);
            }

            return ResponseEntity.ok(new ApiResponse("Tạo lại thanh toán thành công", order));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // ── GET /api/orders  (ADMIN/MANAGER/STAFF) ──────────────────────────────
    @GetMapping
    public ResponseEntity<?> getAllOrders(@AuthenticationPrincipal UserDetails userDetails) {
        if (!RoleAccess.hasAnyRole(userDetails, "ADMIN", "MANAGER", "STAFF")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse("Bạn không có quyền thực hiện thao tác này", null));
        }
        try {
            List<OrderResponse> orders = orderService.getAllOrders();
            return ResponseEntity.ok(new ApiResponse("Lấy tất cả đơn hàng thành công", orders));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // ── PATCH /api/orders/{id}/status  (ADMIN/MANAGER/STAFF) ─────────────────
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (!RoleAccess.hasAnyRole(userDetails, "ADMIN", "MANAGER", "STAFF")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse("Bạn không có quyền thực hiện thao tác này", null));
        }
        try {
            String newStatus = body.get("status");
            String cancelReason = body.get("cancelReason");
            if (newStatus == null || newStatus.isBlank()) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse("Thiếu trường 'status'", null));
            }
            OrderResponse order = orderService.updateOrderStatus(id, newStatus, cancelReason);
            return ResponseEntity.ok(new ApiResponse("Cập nhật trạng thái thành công", order));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
}

