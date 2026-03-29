package j2ee_backend.nhom05.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import j2ee_backend.nhom05.model.ProductVariantValue;

@Repository
public interface IProductVariantValueRepository extends JpaRepository<ProductVariantValue, Long> {
    List<ProductVariantValue> findByVariantIdOrderByDisplayOrderAsc(Long variantId);
    List<ProductVariantValue> findByVariantIdInOrderByDisplayOrderAsc(List<Long> variantIds);
}
