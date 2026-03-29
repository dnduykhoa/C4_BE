package j2ee_backend.nhom05.repository;

import j2ee_backend.nhom05.model.ProductSpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface IProductSpecificationRepository extends JpaRepository<ProductSpecification, Long> {

    List<ProductSpecification> findByProductIdOrderByDisplayOrderAsc(Long productId);

  List<ProductSpecification> findByProductIdInOrderByDisplayOrderAsc(List<Long> productIds);

    List<ProductSpecification> findByAttributeDefinitionId(Long attrDefId);

    @Modifying
    @Query("DELETE FROM ProductSpecification s WHERE s.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);

    /**
     * Lọc sản phẩm theo giá trị số của thuộc tính.
     * VD: attrKey="ram", minValue=8, maxValue=null → tìm sản phẩm RAM >= 8 GB
     */
    @Query("""
        SELECT DISTINCT s.product.id
        FROM ProductSpecification s
        WHERE s.specKey = :attrKey
          AND s.valueNumber IS NOT NULL
          AND (:minValue IS NULL OR s.valueNumber >= :minValue)
          AND (:maxValue IS NULL OR s.valueNumber <= :maxValue)
        """)
    List<Long> findProductIdsByNumericSpec(@Param("attrKey") String attrKey,
                                            @Param("minValue") BigDecimal minValue,
                                            @Param("maxValue") BigDecimal maxValue);
}
