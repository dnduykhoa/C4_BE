package j2ee_backend.nhom05.repository;

import j2ee_backend.nhom05.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ICategoryRepository extends JpaRepository<Category, Long> {
    
    // Lấy tất cả danh mục gốc (không có parent)
    List<Category> findByParentIsNull();
    
    // Lấy danh mục con của một danh mục
    List<Category> findByParentId(Long parentId);
    
    // Lấy danh mục đang hoạt động
    List<Category> findByIsActiveTrue();
    
    // Tìm danh mục theo tên
    List<Category> findByNameContainingIgnoreCase(String name);
}
