package j2ee_backend.nhom05.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import j2ee_backend.nhom05.dto.CartResponse;
import j2ee_backend.nhom05.model.Cart;
import j2ee_backend.nhom05.model.CartItem;
import j2ee_backend.nhom05.model.Product;
import j2ee_backend.nhom05.model.ProductStatus;
import j2ee_backend.nhom05.model.ProductVariant;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.repository.ICartItemRepository;
import j2ee_backend.nhom05.repository.ICartRepository;
import j2ee_backend.nhom05.repository.IProductRepository;
import j2ee_backend.nhom05.repository.IProductVariantRepository;
import j2ee_backend.nhom05.repository.IUserRepository;

/**
 * Test Case – Chức năng Giỏ hàng (White-box)
 * Tuyến đường: a→c→i | a→b→e→i | a→b→d→g→i | a→b→d→f→h
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("4.3.3 - Test Case: Chức năng Giỏ hàng")
class CartServiceTest {

    @Mock private ICartRepository           cartRepository;
    @Mock private ICartItemRepository       cartItemRepository;
    @Mock private IProductRepository        productRepository;
    @Mock private IProductVariantRepository productVariantRepository;
    @Mock private IUserRepository           userRepository;

    private CartService cartService;

    // Dùng chung: cart id=100, user id=1
    private Cart  cart;
    private User  user;

    @BeforeEach
    void setUp() {
        cartService = new CartService();
        ReflectionTestUtils.setField(cartService, "cartRepository",           cartRepository);
        ReflectionTestUtils.setField(cartService, "cartItemRepository",       cartItemRepository);
        ReflectionTestUtils.setField(cartService, "productRepository",        productRepository);
        ReflectionTestUtils.setField(cartService, "productVariantRepository", productVariantRepository);
        ReflectionTestUtils.setField(cartService, "userRepository",           userRepository);

        user = new User();
        user.setId(1L);

        cart = new Cart();
        cart.setId(100L);
        cart.setUser(user);
        cart.setItems(new ArrayList<>());
    }

    /** Tạo Product với status và stockQuantity cho trước */
    private Product product(ProductStatus status, int stock) {
        Product p = new Product();
        p.setId(10L);
        p.setName("Product Test");
        p.setPrice(BigDecimal.valueOf(500_000));
        p.setStatus(status);
        p.setStockQuantity(stock);
        return p;
    }

    /** Mock đầy đủ cho buildCartResponse (gọi sau khi save) */
    private void mockBuildCartResponse(List<CartItem> itemsInCart) {
        when(cartRepository.findById(100L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllByCartId(100L)).thenReturn(itemsInCart);
    }

    // -----------------------------------------------------------------------
    // GH-WB-01
    // SP không có biến thể (hasActiveVariants=False), variantId=null
    // → Thêm sản phẩm cha vào giỏ, trả về 200
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("GH-WB-01: SP không có biến thể – thêm SP cha vào giỏ thành công (200)")
    void GHWB01_addToCart_noVariants_addsParentProduct() {
        Product p = product(ProductStatus.ACTIVE, 10);

        when(productRepository.findById(10L)).thenReturn(Optional.of(p));
        when(productVariantRepository.existsByProductIdAndIsActiveTrue(10L)).thenReturn(false); // hasActiveVariants = False
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartAndProductAndVariant(100L, 10L, null))
                .thenReturn(Optional.empty()); // chưa có cart_item

        CartItem savedItem = new CartItem();
        savedItem.setId(1L);
        savedItem.setProduct(p);
        savedItem.setQuantity(1);
        savedItem.setCart(cart);
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(savedItem);

        mockBuildCartResponse(List.of(savedItem));

        CartResponse result = cartService.addToCart(1L, 10L, null, 1);

        assertNotNull(result);

        // Xác nhận cart_item mới được tạo và lưu với SP cha
        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertNull(captor.getValue().getVariant());
        assertEquals(p, captor.getValue().getProduct());
        assertEquals(1, captor.getValue().getQuantity());
    }

    // -----------------------------------------------------------------------
    // GH-WB-02:
    // SP có biến thể (hasActiveVariants=True), variantId=15L (hợp lệ)
    // → Số lượng cart_item tăng thêm 2
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("GH-WB-02: SP có biến thể, chọn variantId hợp lệ – số lượng cart_item tăng thêm 2")
    void GHWB02_addToCart_withValidVariant_quantityIncreases() {
        Product p = product(ProductStatus.ACTIVE, 20);

        ProductVariant variant = new ProductVariant();
        variant.setId(15L);
        variant.setIsActive(true);
        variant.setStockQuantity(10);
        variant.setPrice(BigDecimal.valueOf(600_000));

        // CartItem đã tồn tại với quantity = 3
        CartItem existing = new CartItem();
        existing.setId(5L);
        existing.setProduct(p);
        existing.setVariant(variant);
        existing.setQuantity(3);
        existing.setCart(cart);

        when(productRepository.findById(10L)).thenReturn(Optional.of(p));
        when(productVariantRepository.existsByProductIdAndIsActiveTrue(10L)).thenReturn(true); // hasActiveVariants = True
        when(productVariantRepository.findByIdAndProductId(15L, 10L)).thenReturn(Optional.of(variant));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartAndProductAndVariant(100L, 10L, 15L))
                .thenReturn(Optional.of(existing)); // đã có cart_item
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(existing);

        mockBuildCartResponse(List.of(existing));

        cartService.addToCart(1L, 10L, 15L, 2); // thêm 2

        // Xác nhận quantity được cập nhật thành 3 + 2 = 5
        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertEquals(5, captor.getValue().getQuantity());
    }

    // -----------------------------------------------------------------------
    // GH-WB-03
    // SP có biến thể (hasActiveVariants=True), variantId=null, parentAvailable=True (C=False)
    // → SP cha được thêm vào giỏ thành công (200)
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("GH-WB-03: SP có biến thể, không chọn variant, SP cha vẫn bán lẻ – thêm SP cha thành công (200)")
    void GHWB03_addToCart_hasVariants_noVariantId_parentAvailable_success() {
        // parentAvailable = True → ACTIVE + stock > 0
        Product p = product(ProductStatus.ACTIVE, 5);

        when(productRepository.findById(10L)).thenReturn(Optional.of(p));
        when(productVariantRepository.existsByProductIdAndIsActiveTrue(10L)).thenReturn(true); // hasActiveVariants=True
        // variantId=null, parentAvailable=True → IF condition = true&&true&&false = false → không throw
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartAndProductAndVariant(100L, 10L, null))
                .thenReturn(Optional.empty());

        CartItem savedItem = new CartItem();
        savedItem.setId(2L);
        savedItem.setProduct(p);
        savedItem.setQuantity(1);
        savedItem.setCart(cart);
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(savedItem);

        mockBuildCartResponse(List.of(savedItem));

        CartResponse result = cartService.addToCart(1L, 10L, null, 1);

        assertNotNull(result);
        verify(cartItemRepository).save(any(CartItem.class)); // SP cha được lưu vào giỏ
    }

    // -----------------------------------------------------------------------
    // GH-WB-04: Tuyến a → b → d → f → h
    // SP có biến thể (hasActiveVariants=True), variantId=null, parentAvailable=False (C=True)
    // → Ngắt tại C, báo lỗi 400: "Vui lòng chọn biến thể"
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("GH-WB-04: SP có biến thể, không chọn variant, SP cha không bán lẻ – ném ngoại lệ 400")
    void GHWB04_addToCart_hasVariants_noVariantId_parentUnavailable_throwsException() {
        // parentAvailable = False → INACTIVE (không purchasable, không preorderable)
        Product p = product(ProductStatus.INACTIVE, 0);

        when(productRepository.findById(10L)).thenReturn(Optional.of(p));
        when(productVariantRepository.existsByProductIdAndIsActiveTrue(10L)).thenReturn(true); // hasActiveVariants=True
        // variantId=null, parentAvailable=False → IF condition = true&&true&&true = true → THROW

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cartService.addToCart(1L, 10L, null, 1));

        assertTrue(ex.getMessage().contains("Vui lòng chọn biến thể"),
                "Phải báo chọn biến thể, thực tế: " + ex.getMessage());

        verify(cartItemRepository, never()).save(any());
    }
}
