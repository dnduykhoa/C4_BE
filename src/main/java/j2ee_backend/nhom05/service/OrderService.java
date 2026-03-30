package j2ee_backend.nhom05.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import j2ee_backend.nhom05.dto.OrderItemRequest;
import j2ee_backend.nhom05.dto.OrderRequest;
import j2ee_backend.nhom05.dto.OrderResponse;
import j2ee_backend.nhom05.dto.OrderResponse.OrderItemResponse;
import j2ee_backend.nhom05.model.Cart;
import j2ee_backend.nhom05.model.CartItem;
import j2ee_backend.nhom05.model.Order;
import j2ee_backend.nhom05.model.OrderItem;
import j2ee_backend.nhom05.model.OrderStatus;
import j2ee_backend.nhom05.model.PaymentMethod;
import j2ee_backend.nhom05.model.Product;
import j2ee_backend.nhom05.model.ProductMedia;
import j2ee_backend.nhom05.model.ProductSpecification;
import j2ee_backend.nhom05.model.ProductStatus;
import j2ee_backend.nhom05.model.ProductVariant;
import j2ee_backend.nhom05.model.ProductVariantValue;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.model.Voucher;
import j2ee_backend.nhom05.repository.ICartRepository;
import j2ee_backend.nhom05.repository.IOrderRepository;
import j2ee_backend.nhom05.repository.IProductRepository;
import j2ee_backend.nhom05.repository.IProductReviewRepository;
import j2ee_backend.nhom05.repository.IProductVariantRepository;
import j2ee_backend.nhom05.repository.IUserRepository;
import j2ee_backend.nhom05.repository.IVoucherRepository;
import j2ee_backend.nhom05.validator.PhoneValidator;

@Service
public class OrderService {

    @Autowired
    private IOrderRepository orderRepository;

    @Autowired
    private ICartRepository cartRepository;

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IProductVariantRepository productVariantRepository;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IVoucherRepository voucherRepository;

    @Autowired
    private IProductReviewRepository reviewRepository;

    @Autowired
    private SaleProgramService saleProgramService;

    @Autowired
    private VoucherService voucherService;

    private static class OrderLineSource {
        private final Product product;
        private final ProductVariant variant;
        private final int quantity;

        private OrderLineSource(Product product, ProductVariant variant, int quantity) {
            this.product = product;
            this.variant = variant;
            this.quantity = quantity;
        }
    }

    /**
     * Tạo đơn hàng từ giỏ hàng hiện tại của user.
     * Sau khi tạo thành công: trừ tồn kho và xoá giỏ hàng.
     */
    @Transactional
    public OrderResponse createOrder(Long userId, OrderRequest request) {
        // 1. Lấy user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // 2. Resolve nguồn item: request.items (mua ngay) hoặc cart hiện tại
        List<OrderLineSource> orderLines = new ArrayList<>();
        boolean useCartSource = request.getItems() == null || request.getItems().isEmpty();
        Cart cart = null;

        if (!useCartSource) {
            for (OrderItemRequest item : request.getItems()) {
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(
                                () -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + item.getProductId()));

                ProductVariant variant = null;
                if (item.getVariantId() != null) {
                    variant = productVariantRepository.findByIdAndProductId(item.getVariantId(), product.getId())
                            .orElseThrow(() -> new RuntimeException(
                                    "Không tìm thấy biến thể hợp lệ cho sản phẩm '" + product.getName() + "'"));
                }

                Integer requestedQty = item.getQuantity();
                int qty = requestedQty != null ? requestedQty : 0;
                if (qty <= 0) {
                    throw new RuntimeException("Số lượng sản phẩm phải lớn hơn 0");
                }

                orderLines.add(new OrderLineSource(product, variant, qty));
            }
        } else {
            cart = cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Giỏ hàng trống, không thể đặt hàng"));

            if (cart.getItems() == null || cart.getItems().isEmpty()) {
                throw new RuntimeException("Giỏ hàng trống, không thể đặt hàng");
            }

            for (CartItem cartItem : cart.getItems()) {
                orderLines.add(new OrderLineSource(
                        cartItem.getProduct(),
                        cartItem.getVariant(),
                        cartItem.getQuantity()));
            }
        }

        // 3. Validate từng dòng sản phẩm
        for (OrderLineSource line : orderLines) {
            Product product = line.product;
            ProductVariant variant = line.variant;
            if (variant != null) {
                if (!isVariantAvailableForOrder(variant)) {
                    throw new RuntimeException("Biến thể của sản phẩm '" + product.getName() + "' hiện không còn bán");
                }
                if (!isVariantPreorderable(variant) && line.quantity > variant.getStockQuantity()) {
                    throw new RuntimeException("Biến thể của sản phẩm '" + product.getName()
                            + "' chỉ còn " + variant.getStockQuantity() + " sản phẩm trong kho");
                }
            } else {
                if (!isProductAvailableForOrder(product)) {
                    throw new RuntimeException("Sản phẩm '" + product.getName() + "' hiện không còn bán");
                }
                if (!isProductPreorderable(product) && line.quantity > product.getStockQuantity()) {
                    throw new RuntimeException("Sản phẩm '" + product.getName()
                            + "' chỉ còn " + product.getStockQuantity() + " sản phẩm trong kho");
                }
            }
        }

        // 4. Parse paymentMethod
        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Phương thức thanh toán không hợp lệ: " + request.getPaymentMethod());
        }

        // 5. Tạo Order
        Order order = new Order();
        order.setUser(user);
        order.setFullName(request.getFullName());
        order.setPhone(PhoneValidator.normalize(request.getPhone()));
        order.setEmail(request.getEmail());
        order.setShippingAddress(request.getShippingAddress());
        order.setNote(request.getNote());
        order.setPaymentMethod(paymentMethod);
        order.setStatus(OrderStatus.PENDING);
        if (paymentMethod == PaymentMethod.MOMO || paymentMethod == PaymentMethod.VNPAY) {
            order.setPaymentDeadline(LocalDateTime.now().plusMinutes(30));
        }

        // 6. Tạo OrderItem, tính tổng tiền gốc và danh sách dòng để tính sale
        BigDecimal originalAmount = BigDecimal.ZERO;
        int totalQuantity = 0;
        List<OrderItem> orderItems = new ArrayList<>();
        List<SaleProgramService.OrderLineInfo> saleLines = new ArrayList<>();

        for (OrderLineSource line : orderLines) {
            Product product = line.product;
            ProductVariant variant = line.variant;
            BigDecimal unitPrice = variant != null ? variant.getPrice() : product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(line.quantity));
            originalAmount = originalAmount.add(subtotal);
            totalQuantity += line.quantity;

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setVariant(variant);
            orderItem.setQuantity(line.quantity);
            orderItem.setUnitPrice(unitPrice);
            orderItem.setSubtotal(subtotal);
            orderItems.add(orderItem);

            saleLines.add(new SaleProgramService.OrderLineInfo(product.getId(), unitPrice, line.quantity));

            // Trừ tồn kho nếu đây không phải đơn đặt trước
            if (!isPreorderLine(product, variant)) {
                deductStock(product, variant, line.quantity);
            }
        }

        // 7. Tính giảm giá từ sale program
        BigDecimal saleDiscount = saleProgramService.calculateSaleDiscount(
                saleLines,
                paymentMethod.name(),
                originalAmount,
                totalQuantity);

        // 8. Tính giảm giá từ voucher (áp trên số tiền sau khi đã trừ sale)
        BigDecimal afterSaleAmount = originalAmount.subtract(saleDiscount).max(BigDecimal.ZERO);
        BigDecimal voucherDiscount = BigDecimal.ZERO;
        String voucherCode = request.getVoucherCode();
        Voucher appliedVoucher = null;

        if (voucherCode != null && !voucherCode.isBlank()) {
            voucherDiscount = voucherService.calculateDiscount(voucherCode.trim(), afterSaleAmount, userId);
            appliedVoucher = voucherRepository.findByCodeIgnoreCase(voucherCode.trim()).orElse(null);
        }

        // 9. Tổng tiền cuối = tiền sau sale - giảm voucher
        BigDecimal totalAmount = afterSaleAmount.subtract(voucherDiscount).max(BigDecimal.ZERO);

        order.setOriginalAmount(originalAmount);
        order.setSaleDiscount(saleDiscount);
        order.setVoucherDiscount(voucherDiscount);
        order.setAppliedVoucherCode(appliedVoucher != null ? appliedVoucher.getCode() : null);
        order.setAppliedVoucher(appliedVoucher);
        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        // 10. Đánh dấu voucher đã được sử dụng
        if (appliedVoucher != null) {
            voucherService.markUsed(appliedVoucher.getCode(), user, savedOrder);
        }

        // 11. Nếu lấy dữ liệu từ giỏ hàng thì xoá giỏ sau khi đặt thành công
        if (useCartSource && cart != null) {
            cartRepository.delete(cart);
        }

        return buildOrderResponse(savedOrder);
    }

    /**
     * Lấy danh sách đơn hàng của user.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // Batch-check reviewed order items cho các đơn DELIVERED
        List<Long> deliveredItemIds = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .flatMap(o -> o.getItems().stream())
                .map(OrderItem::getId)
                .collect(Collectors.toList());
        Set<Long> reviewedItemIds = deliveredItemIds.isEmpty()
                ? Collections.emptySet()
                : new HashSet<>(reviewRepository.findReviewedOrderItemIds(userId, deliveredItemIds));

        List<OrderResponse> result = new ArrayList<>();
        for (Order order : orders) {
            result.add(buildOrderResponse(order, reviewedItemIds));
        }
        return result;
    }

    /**
     * Lấy chi tiết một đơn hàng (chỉ user sở hữu hoặc ADMIN mới xem được).
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId, boolean isAdmin) {
        Order order;
        if (isAdmin) {
            order = orderRepository.findByIdWithItems(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
            return buildOrderResponse(order);
        } else {
            order = orderRepository.findByIdAndUserId(orderId, userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng hoặc bạn không có quyền xem"));

            // Check reviewed status cho user
            Set<Long> reviewedItemIds = Collections.emptySet();
            if (order.getStatus() == OrderStatus.DELIVERED) {
                List<Long> itemIds = order.getItems().stream()
                        .map(OrderItem::getId)
                        .collect(Collectors.toList());
                reviewedItemIds = new HashSet<>(reviewRepository.findReviewedOrderItemIds(userId, itemIds));
            }
            return buildOrderResponse(order, reviewedItemIds);
        }
    }

    /**
     * Admin: lấy tất cả đơn hàng, sắp xếp mới nhất trước.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        List<OrderResponse> result = new ArrayList<>();
        for (Order order : orders) {
            result.add(buildOrderResponse(order));
        }
        return result;
    }

    /**
     * Admin cập nhật trạng thái đơn hàng.
     * Nếu chuyển sang CANCELLED: hoàn tồn kho + hoàn voucher.
     */
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, String newStatus, String cancelReason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        OrderStatus newOrderStatus;
        try {
            newOrderStatus = OrderStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Trạng thái không hợp lệ: " + newStatus);
        }

        OrderStatus currentStatus = order.getStatus();

        if (newOrderStatus == OrderStatus.CANCELLED
                && currentStatus != OrderStatus.CANCELLED
                && currentStatus != OrderStatus.DELIVERED) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                ProductVariant variant = item.getVariant();
                restoreStock(product, variant, item.getQuantity());
            }
            order.setCancelledAt(LocalDateTime.now());
            order.setCancelReason(
                    (cancelReason != null && !cancelReason.isBlank()) ? cancelReason : "Admin huỷ đơn hàng");
            // Hoàn lại voucher về trạng thái chưa dùng
            voucherService.releaseVoucher(orderId);
        }

        order.setStatus(newOrderStatus);
        Order savedOrder = orderRepository.save(order);

        return buildOrderResponse(savedOrder);
    }

    /**
     * User huỷ đơn hàng (chỉ khi còn PENDING).
     */
    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long userId, String cancelReason) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể huỷ đơn hàng đang ở trạng thái chờ xác nhận");
        }
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            ProductVariant variant = item.getVariant();
            restoreStock(product, variant, item.getQuantity());
        }
        // Hoàn lại voucher
        voucherService.releaseVoucher(orderId);

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelReason(cancelReason != null && !cancelReason.isBlank()
                ? cancelReason
                : "Người dùng huỷ đơn");
        Order savedOrder = orderRepository.save(order);

        return buildOrderResponse(savedOrder);
    }

    // ── Payment callbacks ─────────────────────────────────────────────────────

    @Transactional
    public void confirmMomoPayment(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderCode));
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setPaymentDeadline(null);
            orderRepository.save(order);

            try {
                cartRepository.findByUserId(order.getUser().getId())
                        .ifPresent(cart -> cartRepository.delete(cart));
            } catch (Exception ignored) {
            }

        }
    }

    @Transactional
    public void cancelMomoPayment(String orderCode) {
        orderRepository.findByOrderCode(orderCode).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setPaymentDeadline(LocalDateTime.now().plusMinutes(30));
                orderRepository.save(order);
            }
        });
    }

    @Transactional
    public void confirmVnpayPayment(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderCode));
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setPaymentDeadline(null);
            orderRepository.save(order);

            try {
                cartRepository.findByUserId(order.getUser().getId())
                        .ifPresent(cart -> cartRepository.delete(cart));
            } catch (Exception ignored) {
            }
        }
    }

    @Transactional
    public void cancelVnpayPayment(String orderCode) {
        orderRepository.findByOrderCode(orderCode).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setPaymentDeadline(LocalDateTime.now().plusMinutes(30));
                orderRepository.save(order);
            }
        });
    }

    private static final int MAX_PAYMENT_RETRY = 3;

    @Transactional
    public OrderResponse retryPayment(Long orderId, Long userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Đơn hàng không ở trạng thái chờ xác nhận");
        }

        if (order.getPaymentMethod() != PaymentMethod.MOMO && order.getPaymentMethod() != PaymentMethod.VNPAY) {
            throw new RuntimeException("Đơn hàng này không áp dụng thanh toán lại online");
        }

        if (order.getPaymentDeadline() == null || order.getPaymentDeadline().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Đơn hàng đã hết hạn thanh toán");
        }

        Integer savedRetryCount = order.getPaymentRetryCount();
        int retryCount = savedRetryCount != null ? savedRetryCount : 0;
        if (retryCount >= MAX_PAYMENT_RETRY) {
            for (OrderItem item : order.getItems()) {
                restoreStock(item.getProduct(), item.getVariant(), item.getQuantity());
            }
            voucherService.releaseVoucher(orderId);
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelledAt(LocalDateTime.now());
            order.setCancelReason("Huỷ tự động do vượt quá " + MAX_PAYMENT_RETRY + " lần thanh toán lại");
            order.setPaymentDeadline(null);
            orderRepository.save(order);
            throw new RuntimeException("Đơn hàng đã bị huỷ do vượt quá " + MAX_PAYMENT_RETRY + " lần thanh toán lại");
        }

        order.setPaymentRetryCount(retryCount + 1);
        order.setPaymentDeadline(LocalDateTime.now().plusMinutes(30));
        return buildOrderResponse(orderRepository.save(order));
    }

    /**
     * Tự động huỷ các đơn online chưa thanh toán khi quá hạn deadline.
     */
    @Transactional
    public void expireUnpaidOrders() {
        List<Long> expiredIds = orderRepository.findExpiredUnpaidOrderIds(
                OrderStatus.PENDING,
            List.of(PaymentMethod.MOMO, PaymentMethod.VNPAY),
                LocalDateTime.now());

        for (Long orderId : expiredIds) {
            Order order = orderRepository.findByIdWithItems(orderId).orElse(null);
            if (order == null)
                continue;

            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                ProductVariant variant = item.getVariant();
                restoreStock(product, variant, item.getQuantity());
            }

            voucherService.releaseVoucher(orderId);

            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelledAt(LocalDateTime.now());
            order.setCancelReason("Hết hạn thanh toán online");
            order.setPaymentDeadline(null);
            orderRepository.save(order);
        }
    }

    // ── Stock helpers ─────────────────────────────────────────────────────────

    private void deductStock(Product product, ProductVariant variant, int qty) {
        if (variant != null) {
            int newStock = variant.getStockQuantity() - qty;
            variant.setStockQuantity(newStock);
        } else {
            int newStock = product.getStockQuantity() - qty;
            product.setStockQuantity(newStock);
            if (newStock <= 0) {
                product.setStatus(ProductStatus.OUT_OF_STOCK);
            }
            productRepository.save(product);
        }
    }

    private void restoreStock(Product product, ProductVariant variant, int qty) {
        if (variant != null) {
            int newStock = variant.getStockQuantity() + qty;
            variant.setStockQuantity(newStock);
        } else {
            int newStock = product.getStockQuantity() + qty;
            product.setStockQuantity(newStock);
            if (product.getStatus() == ProductStatus.OUT_OF_STOCK && newStock > 0) {
                product.setStatus(ProductStatus.ACTIVE);
            }
            productRepository.save(product);
        }
    }

    private boolean isProductPurchasable(Product product) {
        ProductStatus status = product.getStatus();
        return (status == ProductStatus.ACTIVE || status == ProductStatus.NEW_ARRIVAL)
                && product.getStockQuantity() != null
                && product.getStockQuantity() > 0;
    }

    private boolean isProductPreorderable(Product product) {
        return product.getStatus() == ProductStatus.OUT_OF_STOCK;
    }

    private boolean isProductAvailableForOrder(Product product) {
        return isProductPurchasable(product) || isProductPreorderable(product);
    }

    private boolean isVariantPurchasable(ProductVariant variant) {
        return Boolean.TRUE.equals(variant.getIsActive())
                && variant.getStockQuantity() != null
                && variant.getStockQuantity() > 0;
    }

    private boolean isVariantPreorderable(ProductVariant variant) {
        return Boolean.TRUE.equals(variant.getIsActive())
                && variant.getStockQuantity() != null
                && variant.getStockQuantity() <= 0;
    }

    private boolean isVariantAvailableForOrder(ProductVariant variant) {
        return isVariantPurchasable(variant) || isVariantPreorderable(variant);
    }

    private boolean isPreorderLine(Product product, ProductVariant variant) {
        if (variant != null) {
            return isVariantPreorderable(variant);
        }
        return isProductPreorderable(product);
    }

    // ── Build response ────────────────────────────────────────────────────────

    private String resolvePrimaryImageUrl(List<ProductMedia> mediaList) {
        if (mediaList == null || mediaList.isEmpty()) {
            return null;
        }
        return mediaList.stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsPrimary()) && "IMAGE".equals(m.getMediaType()))
                .findFirst()
                .map(ProductMedia::getMediaUrl)
                .orElse(mediaList.stream()
                        .filter(m -> "IMAGE".equals(m.getMediaType()))
                        .findFirst()
                        .map(ProductMedia::getMediaUrl)
                        .orElse(null));
    }

    private List<String> buildVariantOptions(ProductVariant variant) {
        if (variant == null || variant.getValues() == null) {
            return null;
        }
        return variant.getValues().stream()
                .map(v -> {
                    String key = v.getAttributeDefinition() != null
                            ? v.getAttributeDefinition().getName()
                            : v.getAttrKey();
                    return (key != null ? key : "") + ": " + v.getDisplayValue();
                })
                .collect(Collectors.toList());
    }

    private String normalizeAttrKey(String key) {
        if (key == null) {
            return null;
        }
        String trimmed = key.trim().toLowerCase();
        return trimmed.isBlank() ? null : trimmed;
    }

    private List<String> buildParentVariantOptions(Product product) {
        if (product == null || product.getVariants() == null || product.getVariants().isEmpty()) {
            return null;
        }

        Set<Long> variantDefIds = new HashSet<>();
        Set<String> variantAttrKeys = new HashSet<>();

        for (ProductVariant productVariant : product.getVariants()) {
            List<ProductVariantValue> values = productVariant.getValues();
            if (values == null || values.isEmpty()) {
                continue;
            }

            for (ProductVariantValue value : values) {
                if (value.getAttributeDefinition() != null && value.getAttributeDefinition().getId() != null) {
                    variantDefIds.add(value.getAttributeDefinition().getId());
                }

                String fromValueAttrKey = normalizeAttrKey(value.getAttrKey());
                if (fromValueAttrKey != null) {
                    variantAttrKeys.add(fromValueAttrKey);
                }

                String fromDefAttrKey = value.getAttributeDefinition() != null
                        ? normalizeAttrKey(value.getAttributeDefinition().getAttrKey())
                        : null;
                if (fromDefAttrKey != null) {
                    variantAttrKeys.add(fromDefAttrKey);
                }
            }
        }

        if (variantDefIds.isEmpty() && variantAttrKeys.isEmpty()) {
            return null;
        }

        List<ProductSpecification> specifications = product.getSpecifications();
        if (specifications == null || specifications.isEmpty()) {
            return null;
        }

        List<String> parentOptions = new ArrayList<>();

        for (ProductSpecification spec : specifications) {
            Long specDefId = spec.getAttributeDefinition() != null ? spec.getAttributeDefinition().getId() : null;
            String specAttrKey = spec.getAttributeDefinition() != null
                    ? normalizeAttrKey(spec.getAttributeDefinition().getAttrKey())
                    : null;
            String specKey = normalizeAttrKey(spec.getSpecKey());

            boolean isVariantDimension = (specDefId != null && variantDefIds.contains(specDefId))
                    || (specAttrKey != null && variantAttrKeys.contains(specAttrKey))
                    || (specKey != null && variantAttrKeys.contains(specKey));

            if (!isVariantDimension) {
                continue;
            }

            String label = spec.getAttributeDefinition() != null && spec.getAttributeDefinition().getName() != null
                    ? spec.getAttributeDefinition().getName().trim()
                    : (spec.getSpecKey() != null ? spec.getSpecKey().trim() : "");
            String displayValue = spec.getDisplayValue() != null ? spec.getDisplayValue().trim() : "";

            if (label.isBlank() || displayValue.isBlank()) {
                continue;
            }

            String option = label + ": " + displayValue;
            if (!parentOptions.contains(option)) {
                parentOptions.add(option);
            }
        }

        return parentOptions.isEmpty() ? null : parentOptions;
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

    private OrderResponse buildOrderResponse(Order order, Set<Long> reviewedItemIds) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            ProductVariant variant = item.getVariant();

            String productImageUrl = resolvePrimaryImageUrl(product != null ? product.getMedia() : null);
            String variantImageUrl = resolvePrimaryImageUrl(variant != null ? variant.getMedia() : null);
            String displayImageUrl = variantImageUrl != null ? variantImageUrl : productImageUrl;
            List<String> variantOptions = buildVariantOptions(variant);
            if ((variantOptions == null || variantOptions.isEmpty()) && variant == null) {
                variantOptions = buildParentVariantOptions(product);
            }
            String variantDisplayName = resolveVariantDisplayName(variant, variantOptions);
            if ((variantDisplayName == null || variantDisplayName.isBlank())
                    && variantOptions != null && !variantOptions.isEmpty()) {
                variantDisplayName = String.join(" / ", variantOptions);
            }

            OrderItemResponse itemResp = new OrderItemResponse();
            itemResp.setId(item.getId());
            itemResp.setProductId(product != null ? product.getId() : null);
            itemResp.setProductName(product != null ? product.getName() : null);
            itemResp.setProductImageUrl(productImageUrl);
            itemResp.setVariantId(variant != null ? variant.getId() : null);
            itemResp.setVariantSku(variant != null ? variant.getSku() : null);
            itemResp.setVariantName(variantDisplayName);
            itemResp.setVariantDisplayName(variantDisplayName);
            itemResp.setVariantImageUrl(variantImageUrl);
            itemResp.setDisplayImageUrl(displayImageUrl);
            itemResp.setImageUrl(displayImageUrl);
            itemResp.setVariantOptions(variantOptions);
            itemResp.setQuantity(item.getQuantity());
            itemResp.setUnitPrice(item.getUnitPrice());
            itemResp.setSubtotal(item.getSubtotal());
            itemResp.setReviewed(reviewedItemIds.contains(item.getId()));
            itemResponses.add(itemResp);
        }

        // Dùng setter thay vì all-args constructor để tránh phụ thuộc vào thứ tự field
        OrderResponse resp = new OrderResponse();
        resp.setId(order.getId());
        resp.setOrderCode(order.getOrderCode());
        resp.setUserId(order.getUser().getId());
        resp.setFullName(order.getFullName());
        resp.setPhone(order.getPhone());
        resp.setEmail(order.getEmail());
        resp.setShippingAddress(order.getShippingAddress());
        resp.setNote(order.getNote());
        resp.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
        resp.setStatus(order.getStatus() != null ? order.getStatus().name() : null);
        resp.setOriginalAmount(order.getOriginalAmount());
        resp.setSaleDiscount(order.getSaleDiscount() != null ? order.getSaleDiscount() : BigDecimal.ZERO);
        resp.setVoucherDiscount(order.getVoucherDiscount() != null ? order.getVoucherDiscount() : BigDecimal.ZERO);
        resp.setAppliedVoucherCode(order.getAppliedVoucherCode());
        resp.setTotalAmount(order.getTotalAmount());
        resp.setItems(itemResponses);
        resp.setCreatedAt(order.getCreatedAt());
        resp.setUpdatedAt(order.getUpdatedAt());
        resp.setCancelledAt(order.getCancelledAt());
        resp.setCancelReason(order.getCancelReason());
        resp.setMomoUrl(null);    // được gán bởi controller nếu cần
        resp.setPaymentDeadline(order.getPaymentDeadline());
        return resp;
    }

    private OrderResponse buildOrderResponse(Order order) {
        return buildOrderResponse(order, Collections.emptySet());
    }
}
