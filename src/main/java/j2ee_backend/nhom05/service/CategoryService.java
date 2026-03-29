package j2ee_backend.nhom05.service;

import j2ee_backend.nhom05.model.Category;
import j2ee_backend.nhom05.repository.ICategoryRepository;
import j2ee_backend.nhom05.repository.IProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {
    
    @Autowired
    private ICategoryRepository categoryRepository;
    
    @Autowired
    private IProductRepository productRepository;
    
    // Lấy tất cả danh mục
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
    
    // Lấy danh mục theo ID
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }
    
    // Lấy tất cả danh mục gốc
    public List<Category> getRootCategories() {
        return categoryRepository.findByParentIsNull();
    }
    
    // Lấy danh mục con
    public List<Category> getChildCategories(Long parentId) {
        return categoryRepository.findByParentId(parentId);
    }
    
    // Lấy danh mục đang hoạt động
    public List<Category> getActiveCategories() {
        return categoryRepository.findByIsActiveTrue();
    }
    
    // Tạo danh mục mới
    public Category createCategory(String name, String description, Integer displayOrder, Boolean isActive, Long parentId) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setDisplayOrder(displayOrder);
        category.setIsActive(isActive);
        
        if (parentId != null) {
            Category parent = categoryRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục cha với ID: " + parentId));
            category.setParent(parent);
        }
        
        return categoryRepository.save(category);
    }
    
    // Tạo danh mục mới (giữ lại để tương thích)
    public Category createCategory(Category category) {
        return categoryRepository.save(category);
    }
    
    // Cập nhật danh mục
    public Category updateCategory(Long id, String name, String description, Integer displayOrder, Boolean isActive, Long parentId) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID: " + id));
        
        category.setName(name);
        category.setDescription(description);
        category.setDisplayOrder(displayOrder);
        category.setIsActive(isActive);
        
        if (parentId != null) {
            Category parent = categoryRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục cha với ID: " + parentId));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }
        
        return categoryRepository.save(category);
    }
    
    // Cập nhật danh mục (giữ lại để tương thích)
    public Category updateCategory(Long id, Category categoryDetails) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID: " + id));
        
        category.setName(categoryDetails.getName());
        category.setDescription(categoryDetails.getDescription());
        category.setDisplayOrder(categoryDetails.getDisplayOrder());
        category.setIsActive(categoryDetails.getIsActive());
        category.setParent(categoryDetails.getParent());
        
        return categoryRepository.save(category);
    }
    
    // Xóa danh mục
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID: " + id));
        
        // Kiểm tra xem có danh mục con không
        List<Category> childCategories = categoryRepository.findByParentId(id);
        if (!childCategories.isEmpty()) {
            throw new RuntimeException("Không thể xóa danh mục này vì còn " + childCategories.size() + " danh mục con. Vui lòng xóa các danh mục con trước.");
        }
        
        // Kiểm tra xem có sản phẩm nào thuộc danh mục này không
        long productCount = productRepository.countByCategoryId(id);
        if (productCount > 0) {
            throw new RuntimeException("Không thể xóa danh mục này vì còn " + productCount + " sản phẩm. Vui lòng chuyển hoặc xóa các sản phẩm trước.");
        }
        
        categoryRepository.delete(category);
    }
    
    // Tìm kiếm danh mục theo tên
    public List<Category> searchCategories(String name) {
        return categoryRepository.findByNameContainingIgnoreCase(name);
    }
}
