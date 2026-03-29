package j2ee_backend.nhom05.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import j2ee_backend.nhom05.model.ProductMedia;

@Repository
public interface IProductMediaRepository extends JpaRepository<ProductMedia, Long> {
    List<ProductMedia> findByProductId(Long productId);
    List<ProductMedia> findByProductIdAndVariantIsNull(Long productId);
    List<ProductMedia> findByProductIdAndVariantId(Long productId, Long variantId);
    void deleteByProductId(Long productId);
    void deleteByProductIdAndVariantId(Long productId, Long variantId);

    // Lấy displayOrder lớn nhất của sản phẩm (để tiếp tục đánh số)
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(MAX(m.displayOrder), -1) FROM ProductMedia m WHERE m.product.id = :productId")
    int findMaxDisplayOrderByProductId(Long productId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(MAX(m.displayOrder), -1) FROM ProductMedia m WHERE m.product.id = :productId AND m.variant.id = :variantId")
    int findMaxDisplayOrderByProductIdAndVariantId(Long productId, Long variantId);
}
