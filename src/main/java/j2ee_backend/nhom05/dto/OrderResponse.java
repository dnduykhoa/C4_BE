package j2ee_backend.nhom05.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {

    private Long id;
    private String orderCode;
    private Long userId;
    private String fullName;
    private String phone;
    private String email;
    private String shippingAddress;
    private String note;
    private String paymentMethod;
    private String status;
    private BigDecimal totalAmount;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private String momoUrl;   // Chỉ có giá trị khi paymentMethod = MOMO
    private LocalDateTime paymentDeadline; // Deadline thanh toán (chỉ có khi MOMO chờ thanh toán)

    // Thông tin giảm giá
    private BigDecimal originalAmount;      // Tổng tiền gốc trước khi giảm
    private BigDecimal saleDiscount;        // Số tiền giảm từ sale program
    private BigDecimal voucherDiscount;     // Số tiền giảm từ voucher
    private String appliedVoucherCode;      // Mã voucher đã áp dụng

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private String productImageUrl; // URL ảnh đại diện sản phẩm
        private Long variantId;
        private String variantSku;
        private String variantName;
        private String variantDisplayName;
        private String variantImageUrl; // URL ảnh của biến thể (nếu biến thể có ảnh riêng)
        private String displayImageUrl; // URL ảnh cuối cùng để hiển thị (ưu tiên biến thể)
        private String imageUrl;
        private List<String> variantOptions;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
        private Boolean reviewed; // true nếu user đã đánh giá order item này
    }
}
