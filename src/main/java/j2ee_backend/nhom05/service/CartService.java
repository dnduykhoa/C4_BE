package j2ee_backend.nhom05.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import j2ee_backend.nhom05.dto.CartResponse;
import j2ee_backend.nhom05.dto.CartResponse.CartItemResponse;
import j2ee_backend.nhom05.model.Cart;
import j2ee_backend.nhom05.model.CartItem;
import j2ee_backend.nhom05.model.Product;
import j2ee_backend.nhom05.model.ProductMedia;
import j2ee_backend.nhom05.model.ProductStatus;
import j2ee_backend.nhom05.model.ProductVariant;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.repository.ICartItemRepository;
import j2ee_backend.nhom05.repository.ICartRepository;
import j2ee_backend.nhom05.repository.IProductRepository;
import j2ee_backend.nhom05.repository.IProductVariantRepository;
import j2ee_backend.nhom05.repository.IUserRepository;

@Service
public class CartService {

    private static final int PREORDER_AVAILABLE_STOCK = 9999;

    @Autowired
    private ICartRepository cartRepository;

    @Autowired
    private ICartItemRepository cartItemRepository;

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IProductVariantRepository productVariantRepository;

    @Autowired
    private IUserRepository userRepository;

    // Lấy hoặc tạo mới giỏ hàng cho user
    @Transactional
    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));
            Cart cart = new Cart();
            cart.setUser(user);
            return cartRepository.save(cart);
        });
    }

    // Thêm sản phẩm vào giỏ hàng (nếu đã có thì tăng số lượng)
    @Transactional
    public CartResponse addToCart(Long userId, Long productId, Long variantId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Số lượng phải lớn hơn 0");
        }

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + productId));

        boolean hasActiveVariants = productVariantRepository.existsByProductIdAndIsActiveTrue(productId);
        if (hasActiveVariants && variantId == null && !isParentProductAvailableForCart(product)) {
            throw new RuntimeException("Sản phẩm này có biến thể. Vui lòng chọn biến thể trước khi thêm vào giỏ hàng");
        }

        ProductVariant variant = null;
        if (variantId != null) {
            variant = productVariantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể phù hợp"));
            if (!isVariantAvailableForCart(variant)) {
                throw new RuntimeException("Biến thể không còn hoạt động");
            }
        } else if (!isParentProductAvailableForCart(product)) {
            throw new RuntimeException("Sản phẩm không còn hoạt động");
        }

        boolean preorder = isPreorderItem(product, variant);

        int availableStock = getAvailableStock(product, variant);
        if (!preorder && availableStock <= 0) {
            throw new RuntimeException("Sản phẩm '" + product.getName() + "' Hàng sắp về");
        }

        Cart cart = getOrCreateCart(userId);

        // Kiểm tra nếu sản phẩm đã có trong giỏ hàng
        CartItem cartItem = cartItemRepository
            .findByCartAndProductAndVariant(cart.getId(), productId, variantId)
            .orElse(null);

        if (cartItem != null) {
            int newQuantity = cartItem.getQuantity() + quantity;
            if (!preorder && newQuantity > availableStock) {
                newQuantity = availableStock;
            }
            cartItem.setQuantity(newQuantity);
        } else {
            cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setVariant(variant);
            int actualQuantity = preorder ? quantity : Math.min(quantity, availableStock);
            cartItem.setQuantity(actualQuantity);
            cart.getItems().add(cartItem);
        }

        cartItemRepository.save(cartItem);
        return buildCartResponse(cart);
    }

    // Cập nhật số lượng sản phẩm trong giỏ hàng
    @Transactional
    public CartResponse updateCartItem(Long userId, Long cartItemId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Số lượng phải lớn hơn 0");
        }

        CartItem cartItem = cartItemRepository.findById(cartItemId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng"));

        if (!cartItem.getCart().getUser().getId().equals(userId)) {
            throw new RuntimeException("Không có quyền chỉnh sửa giỏ hàng này");
        }

        Product product = cartItem.getProduct();
        ProductVariant variant = cartItem.getVariant();
        boolean preorder = isPreorderItem(product, variant);
        int availableStock = getAvailableStock(product, variant);
        int actualQuantity = preorder ? quantity : Math.min(quantity, availableStock);
        if (!preorder && actualQuantity <= 0) {
            throw new RuntimeException("Sản phẩm '" + product.getName() + "' Hàng sắp về");
        }

        cartItem.setQuantity(actualQuantity);
        cartItemRepository.save(cartItem);

        Cart cart = getOrCreateCart(userId);
        return buildCartResponse(cart);
    }

    // Xóa một sản phẩm khỏi giỏ hàng
    @Transactional
    public CartResponse removeCartItem(Long userId, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng"));

        if (!cartItem.getCart().getUser().getId().equals(userId)) {
            throw new RuntimeException("Không có quyền chỉnh sửa giỏ hàng này");
        }

        Long cartId = cartItem.getCart().getId();
        cartItemRepository.delete(cartItem);

        Cart persistedCart = cartRepository.findById(cartId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

        return buildCartResponse(persistedCart);
    }

    // Xóa toàn bộ giỏ hàng
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));
        cartRepository.delete(cart); // cascade CascadeType.ALL xóa cả cart_items
    }

    // Lấy thông tin giỏ hàng
    @Transactional(readOnly = true)
    public CartResponse getCartByUserId(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return buildCartResponse(cart);
    }

    // Kiểm tra giỏ hàng trước khi thanh toán — trả về danh sách lỗi nếu có
    @Transactional(readOnly = true)
    public List<String> validateCartForCheckout(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

        List<String> errors = new ArrayList<>();

        if (cart.getItems().isEmpty()) {
            errors.add("Giỏ hàng trống");
            return errors;
        }

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            ProductVariant variant = item.getVariant();

            if (variant != null) {
                if (!isVariantAvailableForCart(variant)) {
                    errors.add("Biến thể của sản phẩm '" + product.getName() + "' hiện không còn bán");
                    continue;
                }
                if (!isPreorderItem(product, variant) && item.getQuantity() > variant.getStockQuantity()) {
                    errors.add("Biến thể của sản phẩm '" + product.getName() + "' chỉ còn "
                        + variant.getStockQuantity() + " sản phẩm trong kho");
                }
            } else {
                if (!isParentProductAvailableForCart(product)) {
                    errors.add("Sản phẩm '" + product.getName() + "' hiện không còn bán");
                    continue;
                }
                if (!isPreorderItem(product, null) && item.getQuantity() > product.getStockQuantity()) {
                    errors.add("Sản phẩm '" + product.getName() + "' chỉ còn " + product.getStockQuantity() + " sản phẩm trong kho");
                }
            }
        }

        return errors;
    }

    // Build CartResponse DTO từ Cart entity
    private CartResponse buildCartResponse(Cart cart) {
        // Tải lại giỏ hàng mới nhất từ DB để đồng bộ trạng thái
        cart = cartRepository.findById(cart.getId())
            .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));
        List<CartItem> cartItems = cartItemRepository.findAllByCartId(cart.getId());

        List<CartItemResponse> itemResponses = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            ProductVariant variant = item.getVariant();
            boolean preorder = isPreorderItem(product, variant);

            boolean inStock = preorder || (variant != null
                ? isVariantPurchasable(variant)
                : isParentProductPurchasable(product));
            int availableStock = preorder ? PREORDER_AVAILABLE_STOCK : getAvailableStock(product, variant);

            BigDecimal unitPrice = variant != null ? variant.getPrice() : product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            String productImageUrl = resolvePrimaryImageUrl(product != null ? product.getMedia() : null);
            String variantImageUrl = resolvePrimaryImageUrl(variant != null ? variant.getMedia() : null);
            String displayImageUrl = variantImageUrl != null ? variantImageUrl : productImageUrl;
            List<String> variantOptions = buildVariantOptions(variant);
            String variantDisplayName = resolveVariantDisplayName(variant, variantOptions);

            CartItemResponse responseItem = new CartItemResponse();
            responseItem.setId(item.getId());
            responseItem.setProduct(product);
            responseItem.setProductName(product != null ? product.getName() : null);
            responseItem.setProductImageUrl(productImageUrl);
            responseItem.setVariantId(variant != null ? variant.getId() : null);
            responseItem.setVariantSku(variant != null ? variant.getSku() : null);
            responseItem.setVariantName(variantDisplayName);
            responseItem.setVariantDisplayName(variantDisplayName);
            responseItem.setVariantImageUrl(variantImageUrl);
            responseItem.setDisplayImageUrl(displayImageUrl);
            responseItem.setImageUrl(displayImageUrl);
            responseItem.setVariantOptions(variantOptions);
            responseItem.setQuantity(item.getQuantity());
            responseItem.setUnitPrice(unitPrice);
            responseItem.setSubtotal(subtotal);
            responseItem.setInStock(inStock);
            responseItem.setAvailableStock(availableStock);
            responseItem.setPreorder(preorder);

            itemResponses.add(responseItem);
        }

        return new CartResponse(
            cart.getId(),
            cart.getUser().getId(),
            itemResponses,
            itemResponses.size(),
            totalAmount
        );
    }

    private int getAvailableStock(Product product, ProductVariant variant) {
        if (variant != null) {
            Integer stock = variant.getStockQuantity();
            return stock == null ? 0 : stock;
        }
        Integer stock = product.getStockQuantity();
        return stock == null ? 0 : stock;
    }

    private boolean isParentProductPurchasable(Product product) {
        ProductStatus status = product.getStatus();
        return (status == ProductStatus.ACTIVE || status == ProductStatus.NEW_ARRIVAL)
            && getAvailableStock(product, null) > 0;
    }

    private boolean isParentProductPreorderable(Product product) {
        return product.getStatus() == ProductStatus.OUT_OF_STOCK;
    }

    private boolean isParentProductAvailableForCart(Product product) {
        return isParentProductPurchasable(product) || isParentProductPreorderable(product);
    }

    private boolean isVariantPurchasable(ProductVariant variant) {
        Integer stock = variant.getStockQuantity();
        return Boolean.TRUE.equals(variant.getIsActive()) && stock != null && stock > 0;
    }

    private boolean isVariantPreorderable(ProductVariant variant) {
        Integer stock = variant.getStockQuantity();
        return Boolean.TRUE.equals(variant.getIsActive()) && stock != null && stock <= 0;
    }

    private boolean isVariantAvailableForCart(ProductVariant variant) {
        return isVariantPurchasable(variant) || isVariantPreorderable(variant);
    }

    private boolean isPreorderItem(Product product, ProductVariant variant) {
        if (variant != null) {
            return isVariantPreorderable(variant);
        }
        return isParentProductPreorderable(product);
    }

    private List<String> buildVariantOptions(ProductVariant variant) {
        if (variant == null || variant.getValues() == null) {
            return null;
        }
        return variant.getValues().stream()
            .map(v -> {
                String key = v.getAttributeDefinition() != null ? v.getAttributeDefinition().getName() : v.getAttrKey();
                String value = v.getDisplayValue();
                return (key != null ? key : "") + ": " + value;
            })
            .collect(Collectors.toList());
    }

    private String resolveVariantDisplayName(ProductVariant variant, List<String> variantOptions) {
        if (variant == null) {
            return null;
        }
        String sku = variant.getSku();
        if (sku != null && !sku.isBlank()) {
            return sku.trim();
        }
        if (variantOptions != null && !variantOptions.isEmpty()) {
            return String.join(" / ", variantOptions);
        }
        return null;
    }

    private String resolvePrimaryImageUrl(List<ProductMedia> mediaList) {
        if (mediaList == null || mediaList.isEmpty()) {
            return null;
        }
        return mediaList.stream()
            .filter(m -> "IMAGE".equals(m.getMediaType()) && Boolean.TRUE.equals(m.getIsPrimary()))
            .map(ProductMedia::getMediaUrl)
            .findFirst()
            .orElseGet(() -> mediaList.stream()
                .filter(m -> "IMAGE".equals(m.getMediaType()))
                .map(ProductMedia::getMediaUrl)
                .findFirst()
                .orElse(null));
    }
}
