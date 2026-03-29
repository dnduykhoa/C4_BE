package j2ee_backend.nhom05.repository;

import j2ee_backend.nhom05.model.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IProductReviewRepository extends JpaRepository<ProductReview, Long> {

    boolean existsByUserIdAndOrderItemId(Long userId, Long orderItemId);

    // Công khai: chỉ lấy đánh giá chưa bị ẩn
    List<ProductReview> findByProductIdAndVariantIdIsNullAndHiddenFalseOrderByCreatedAtDesc(Long productId);

    // Lấy review theo product và variant
    List<ProductReview> findByProductIdAndVariantIdAndHiddenFalseOrderByCreatedAtDesc(Long productId, Long variantId);

    @Query("SELECT r.orderItem.id FROM ProductReview r WHERE r.user.id = :userId AND r.orderItem.id IN :itemIds")
    List<Long> findReviewedOrderItemIds(@Param("userId") Long userId, @Param("itemIds") List<Long> itemIds);

    // Lấy điểm trung bình của sản phẩm CHA (không có variant)
    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM ProductReview r WHERE r.product.id = :productId AND r.variant IS NULL AND r.hidden = false")
    Double findAverageRatingByProductId(@Param("productId") Long productId);

    // Lấy điểm trung bình của variant cụ thể
    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM ProductReview r WHERE r.product.id = :productId AND r.variant.id = :variantId AND r.hidden = false")
    Double findAverageRatingByProductIdAndVariantId(@Param("productId") Long productId, @Param("variantId") Long variantId);

    // Đếm số lượng đánh giá của sản phẩm CHA (không có variant)
    long countByProductIdAndVariantIsNullAndHiddenFalse(Long productId);

    // Đếm số lượng đánh giá của variant cụ thể
    long countByProductIdAndVariantIdAndHiddenFalse(Long productId, Long variantId);

    @Query("SELECT r FROM ProductReview r WHERE " +
           "(:keyword IS NULL OR LOWER(r.product.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(r.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(r.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:rating IS NULL OR r.rating = :rating) " +
           "ORDER BY r.createdAt DESC")
    List<ProductReview> findAllWithFilters(@Param("keyword") String keyword, @Param("rating") Integer rating);
}
