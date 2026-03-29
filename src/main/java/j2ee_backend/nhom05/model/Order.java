package j2ee_backend.nhom05.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", length = 20, unique = true)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(name = "full_name", length = 100, columnDefinition = "NVARCHAR(100)")
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "shipping_address", columnDefinition = "NVARCHAR(500)")
    private String shippingAddress;

    @Column(name = "note", columnDefinition = "NVARCHAR(MAX)")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    // Tổng tiền gốc (trước khi trừ giảm giá)
    @Column(name = "original_amount", precision = 18, scale = 2)
    private BigDecimal originalAmount;

    // Số tiền giảm từ chương trình sale
    @Column(name = "sale_discount", precision = 18, scale = 2)
    private BigDecimal saleDiscount = BigDecimal.ZERO;

    // Số tiền giảm từ voucher
    @Column(name = "voucher_discount", precision = 18, scale = 2)
    private BigDecimal voucherDiscount = BigDecimal.ZERO;

    // Mã voucher đã áp dụng (lưu để tham chiếu)
    @Column(name = "applied_voucher_code", length = 50)
    private String appliedVoucherCode;

    // Voucher đã áp dụng
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_voucher_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Voucher appliedVoucher;

    // Tổng tiền thực tế (sau khi trừ tất cả giảm giá)
    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", columnDefinition = "NVARCHAR(500)")
    private String cancelReason;

    @Column(name = "payment_deadline")
    private LocalDateTime paymentDeadline;

    @Column(name = "payment_retry_count")
    private Integer paymentRetryCount = 0;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (orderCode == null || orderCode.isBlank()) {
            orderCode = generateOrderCode();
        }
        if (saleDiscount == null) saleDiscount = BigDecimal.ZERO;
        if (voucherDiscount == null) voucherDiscount = BigDecimal.ZERO;
    }

    private String generateOrderCode() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return datePart + sb;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
