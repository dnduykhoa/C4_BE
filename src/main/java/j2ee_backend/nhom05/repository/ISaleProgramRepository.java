package j2ee_backend.nhom05.repository;

import j2ee_backend.nhom05.model.SaleProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ISaleProgramRepository extends JpaRepository<SaleProgram, Long> {

    List<SaleProgram> findAllByOrderByCreatedAtDesc();

    List<SaleProgram> findByIsActiveTrueOrderByStartDateDesc();

    // Lấy các sale program đang hoạt động và áp dụng cho 1 sản phẩm cụ thể
    // Dùng EXISTS thay vì JOIN + DISTINCT để tránh lỗi SQL Server với NVARCHAR(MAX) / NTEXT
    @Query("SELECT sp FROM SaleProgram sp WHERE EXISTS (" +
           "SELECT 1 FROM sp.products p WHERE p.id = :productId) " +
           "AND sp.isActive = true " +
           "AND sp.startDate <= :now AND sp.endDate >= :now")
    List<SaleProgram> findActiveByProductId(
        @Param("productId") Long productId,
        @Param("now") LocalDateTime now
    );
}
