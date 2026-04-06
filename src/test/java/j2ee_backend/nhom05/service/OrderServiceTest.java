package j2ee_backend.nhom05.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import j2ee_backend.nhom05.controller.OrderController;
import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.dto.OrderItemRequest;
import j2ee_backend.nhom05.dto.OrderRequest;
import j2ee_backend.nhom05.dto.OrderResponse;
import j2ee_backend.nhom05.model.Product;
import j2ee_backend.nhom05.model.ProductStatus;
import j2ee_backend.nhom05.model.Role;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.repository.ICartRepository;
import j2ee_backend.nhom05.repository.IOrderRepository;
import j2ee_backend.nhom05.repository.IProductRepository;
import j2ee_backend.nhom05.repository.IProductVariantRepository;
import j2ee_backend.nhom05.repository.IUserRepository;
import j2ee_backend.nhom05.repository.IProductReviewRepository;

/**
 * 4.4.3 - Test Case: Chức năng Đặt hàng (White-box)
 * Tuyến đường: a | b-c | b-d-e | b-d-f-g-h
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("4.4.3 - Test Case: Chức năng Đặt hàng")
class OrderServiceTest {

    // ── Service mocks ─────────────────────────────────────────────────────────
    @Mock private IOrderRepository          orderRepository;
    @Mock private ICartRepository           cartRepository;
    @Mock private IProductRepository        productRepository;
    @Mock private IProductVariantRepository productVariantRepository;
    @Mock private IUserRepository           userRepository;
    @Mock private IProductReviewRepository  reviewRepository;
    @Mock private SaleProgramService        saleProgramService;
    @Mock private OrderPaymentEmailService  orderPaymentEmailService;

    // ── Controller mocks ──────────────────────────────────────────────────────
    @Mock private OrderService       orderService;
    @Mock private VnpayService       vnpayService;
    @Mock private MomoService        momoService;
    @Mock private HttpServletRequest httpRequest;

    private OrderService    realOrderService;
    private OrderController orderController;

    @BeforeEach
    void setUp() {
        // Service thật (dùng cho DH-WB-02, DH-WB-03)
        realOrderService = new OrderService();
        ReflectionTestUtils.setField(realOrderService, "orderRepository",          orderRepository);
        ReflectionTestUtils.setField(realOrderService, "cartRepository",           cartRepository);
        ReflectionTestUtils.setField(realOrderService, "productRepository",        productRepository);
        ReflectionTestUtils.setField(realOrderService, "productVariantRepository", productVariantRepository);
        ReflectionTestUtils.setField(realOrderService, "userRepository",           userRepository);
        ReflectionTestUtils.setField(realOrderService, "reviewRepository",         reviewRepository);
        ReflectionTestUtils.setField(realOrderService, "saleProgramService",       saleProgramService);
        ReflectionTestUtils.setField(realOrderService, "orderPaymentEmailService", orderPaymentEmailService);

        // Controller với orderService mock (dùng cho DH-WB-01, DH-WB-04)
        orderController = new OrderController();
        ReflectionTestUtils.setField(orderController, "orderService", orderService);
        ReflectionTestUtils.setField(orderController, "vnpayService",  vnpayService);
        ReflectionTestUtils.setField(orderController, "momoService",   momoService);
    }

    /** Tạo User với role cho trước */
    private User userWithRole(String roleName) {
        Role role = new Role();
        role.setId(1L);
        role.setName(roleName);
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        User user = new User();
        user.setId(1L);
        user.setActive(true);
        user.setRoles(roles);
        return user;
    }

    /** Tạo OrderRequest VNPAY cơ bản */
    private OrderRequest vnpayRequest() {
        OrderRequest req = new OrderRequest();
        req.setFullName("Test User");
        req.setPhone("0912345678");
        req.setEmail("user@example.com");
        req.setShippingAddress("123 Street");
        req.setPaymentMethod("VNPAY");
        return req;
    }

    /** Tạo OrderRequest với paymentMethod và quantity cho trước */
    private OrderRequest baseRequest(String paymentMethod, int quantity) {
        OrderRequest req = new OrderRequest();
        req.setFullName("Test User");
        req.setPhone("0912345678");
        req.setEmail("user@example.com");
        req.setShippingAddress("123 Street");
        req.setPaymentMethod(paymentMethod);
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(10L);
        item.setQuantity(quantity);
        req.setItems(List.of(item));
        return req;
    }

    /** Tạo Product ACTIVE với stockQuantity cho trước */
    private Product activeProduct(int stock) {
        Product p = new Product();
        p.setId(10L);
        p.setName("Laptop Test");
        p.setStatus(ProductStatus.ACTIVE);
        p.setStockQuantity(stock);
        p.setPrice(BigDecimal.valueOf(15_000_000));
        return p;
    }

    // -----------------------------------------------------------------------
    // DH-WB-01
    // Tài khoản ADMIN cố gắng tạo đơn → Lỗi 403 Forbidden
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("DH-WB-01: Tài khoản ADMIN tạo đơn hàng")
    void DHWB01_adminCreateOrder_returns403() {
        User adminUser = userWithRole("ADMIN");

        ResponseEntity<?> response = orderController.createOrder(adminUser, vnpayRequest(), httpRequest);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        ApiResponse body = (ApiResponse) response.getBody();
        assertNotNull(body);
        assertEquals("Chỉ khách hàng có thể tạo đơn hàng", body.getMessage());
        verify(orderService, never()).createOrder(anyLong(), any());
    }

    // -----------------------------------------------------------------------
    // DH-WB-02
    // USER, quantity = 9999 vượt tồn kho → Lỗi 400 tồn kho
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("DH-WB-02: Số lượng vượt tồn kho")
    void DHWB02_exceedStock_throwsException() {
        User activeUser = new User();
        activeUser.setId(1L);
        activeUser.setActive(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(activeProduct(100)));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> realOrderService.createOrder(1L, baseRequest("CASH", 9999)));

        assertTrue(ex.getMessage().contains("chỉ còn") && ex.getMessage().contains("sản phẩm trong kho"),
                "Phải báo lỗi tồn kho, thực tế: " + ex.getMessage());
        verify(orderRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // DH-WB-03
    // USER, SP hợp lệ, Payment = "ABC" (sai PTTT) → Lỗi 400 phương thức TT
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("DH-WB-03: Phương thức thanh toán không hợp lệ")
    void DHWB03_invalidPaymentMethod_throwsException() {
        User activeUser = new User();
        activeUser.setId(1L);
        activeUser.setActive(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(activeProduct(500)));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> realOrderService.createOrder(1L, baseRequest("ABC", 1)));

        assertTrue(ex.getMessage().contains("Phương thức thanh toán không hợp lệ"),
                "Phải báo lỗi PTTT, thực tế: " + ex.getMessage());
        verify(orderRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // DH-WB-04
    // USER, giỏ hàng có SP, Payment = "VNPAY"
    // → HTTP 201, tạo đơn, xóa giỏ hàng (trong service), sinh URL VNPAY
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("DH-WB-04: USER đặt hàng VNPAY")
    void DHWB04_userCreateOrderVnpay_returns201WithVnpayUrl() {
        User regularUser = userWithRole("USER");

        OrderResponse mockOrderResponse = new OrderResponse();
        mockOrderResponse.setOrderCode("ORD-TEST-001");
        mockOrderResponse.setTotalAmount(BigDecimal.valueOf(5_000_000));

        when(orderService.createOrder(eq(1L), any(OrderRequest.class))).thenReturn(mockOrderResponse);
        when(vnpayService.createPaymentUrl(eq("ORD-TEST-001"), eq(BigDecimal.valueOf(5_000_000)), anyString()))
                .thenReturn("https://vnpay.vn/pay?token=test123");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        ResponseEntity<?> response = orderController.createOrder(regularUser, vnpayRequest(), httpRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        ApiResponse body = (ApiResponse) response.getBody();
        assertNotNull(body);
        assertEquals("Đặt hàng thành công", body.getMessage());

        OrderResponse orderData = (OrderResponse) body.getData();
        assertNotNull(orderData);
        assertEquals("ORD-TEST-001", orderData.getOrderCode());
        assertEquals("https://vnpay.vn/pay?token=test123", orderData.getVnpayUrl());

        verify(orderService).createOrder(eq(1L), any(OrderRequest.class));
        verify(vnpayService).createPaymentUrl(eq("ORD-TEST-001"), eq(BigDecimal.valueOf(5_000_000)), anyString());
    }
}
