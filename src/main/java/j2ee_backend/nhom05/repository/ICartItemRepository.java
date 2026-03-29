package j2ee_backend.nhom05.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import j2ee_backend.nhom05.model.CartItem;

@Repository
public interface ICartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    List<CartItem> findAllByCartId(Long cartId);

    @Query("""
        SELECT ci FROM CartItem ci
        WHERE ci.cart.id = :cartId
          AND ci.product.id = :productId
          AND ((:variantId IS NULL AND ci.variant IS NULL)
            OR (:variantId IS NOT NULL AND ci.variant.id = :variantId))
        """)
    Optional<CartItem> findByCartAndProductAndVariant(
        @Param("cartId") Long cartId,
        @Param("productId") Long productId,
        @Param("variantId") Long variantId
    );
}
