package j2ee_backend.nhom05.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import j2ee_backend.nhom05.model.Product;
import j2ee_backend.nhom05.model.ProductStatus;

@Repository
public interface IProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    
    // Tìm sản phẩm theo tên (tìm kiếm không phân biệt hoa thường)
    List<Product> findByNameContainingIgnoreCase(String name);
    
    // Tìm sản phẩm theo danh mục
    List<Product> findByCategoryId(Long categoryId);
    
    // Đếm số sản phẩm theo danh mục
    long countByCategoryId(Long categoryId);
    
    // Đếm số sản phẩm theo thương hiệu
    long countByBrandId(Long brandId);
    
    // Tìm sản phẩm theo thương hiệu
    List<Product> findByBrandId(Long brandId);
    
    // Tìm sản phẩm theo danh sách categoryId (dùng cho lọc theo cây danh mục)
    List<Product> findByCategoryIdIn(List<Long> categoryIds);

    // Tìm sản phẩm cùng loại (cùng category), loại trừ sản phẩm hiện tại
    List<Product> findByCategoryIdAndIdNot(Long categoryId, Long productId);

    // Tìm sản phẩm theo trạng thái
    List<Product> findByStatus(ProductStatus status);

    List<Product> findByStatusAndNewArrivalAtBefore(ProductStatus status, LocalDateTime threshold);
    
    // Tìm sản phẩm theo khoảng giá
    List<Product> findByPriceBetween(java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice);
}
