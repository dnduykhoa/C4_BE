package j2ee_backend.nhom05.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import j2ee_backend.nhom05.model.Order;
import j2ee_backend.nhom05.model.OrderStatus;
import j2ee_backend.nhom05.model.PaymentMethod;

@Repository
public interface IOrderRepository extends JpaRepository<Order, Long> {

    // Tìm theo mã đơn hàng (ví dụ: 20260312GPH8F2)
    Optional<Order> findByOrderCode(String orderCode);

    // Lấy tất cả đơn hàng của một user, sắp xếp mới nhất trước
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Lấy đơn hàng theo id và userId (đảm bảo chỉ xem đơn của mình)
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    // Lấy đơn hàng theo trạng thái (dành cho admin)
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    // Lấy tất cả đơn hàng, sắp xếp mới nhất trước (admin)
    List<Order> findAllByOrderByCreatedAtDesc();

    // Đếm số đơn hàng của user
    long countByUserId(Long userId);

    // Tìm đơn hàng theo id để xử lý (items/products sẽ lazy-load trong @Transactional)
    // Không dùng JOIN FETCH + DISTINCT để tránh lỗi SQL Server với cột ntext (products.description)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    // Lấy ID các đơn PENDING thanh toán online đã quá hạn (không JOIN để tránh lỗi DISTINCT ntext trên SQL Server)
    @Query("SELECT o.id FROM Order o " +
           "WHERE o.status = :pendingStatus " +
           "AND o.paymentMethod IN :onlineMethods " +
           "AND o.paymentDeadline IS NOT NULL " +
           "AND o.paymentDeadline < :now")
    List<Long> findExpiredUnpaidOrderIds(
        @Param("pendingStatus") OrderStatus pendingStatus,
        @Param("onlineMethods") List<PaymentMethod> onlineMethods,
        @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Order o SET o.appliedVoucher = null WHERE o.appliedVoucher.id = :voucherId")
    int clearAppliedVoucherReferences(@Param("voucherId") Long voucherId);

    // Đếm tổng số lượng sản phẩm cha đã bán (không gồm các dòng mua theo biến thể)
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.product.id = :productId AND oi.variant IS NULL AND oi.order.status = j2ee_backend.nhom05.model.OrderStatus.DELIVERED")
    long countSoldQuantityByProductId(@Param("productId") Long productId);

    // Đếm tổng số lượng đã bán theo biến thể (chỉ đơn DELIVERED)
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.variant.id = :variantId AND oi.order.status = j2ee_backend.nhom05.model.OrderStatus.DELIVERED")
    long countSoldQuantityByVariantId(@Param("variantId") Long variantId);
}
