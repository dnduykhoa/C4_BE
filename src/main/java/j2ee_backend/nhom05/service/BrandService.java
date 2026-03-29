package j2ee_backend.nhom05.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import j2ee_backend.nhom05.model.Brand;
import j2ee_backend.nhom05.model.Category;
import j2ee_backend.nhom05.repository.IBrandRepository;
import j2ee_backend.nhom05.repository.ICategoryRepository;
import j2ee_backend.nhom05.repository.IProductRepository;

@Service
public class BrandService {

    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private IBrandRepository brandRepository;

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private ICategoryRepository categoryRepository;
    
    // Lấy tất cả brand
    public List<Brand> getAllBrands() {
        return brandRepository.findAll();
    }
    
    // Lấy brand theo ID
    public Optional<Brand> getBrandById(Long id) {
        return brandRepository.findById(id);
    }
    
    // Lấy brand đang hoạt động
    public List<Brand> getActiveBrands() {
        return brandRepository.findByIsActiveTrue();
    }

    // Lấy brand theo danh mục (cha + tất cả con)
    public List<Brand> getBrandsByCategory(Long categoryId) {
        List<Long> categoryIds = new ArrayList<>();
        categoryIds.add(categoryId);
        List<Category> children = categoryRepository.findByParentId(categoryId);
        for (Category child : children) {
            categoryIds.add(child.getId());
        }
        return brandRepository.findActiveBrandsByCategoryIds(categoryIds);
    }
    
    // Tạo brand mới (có thể kèm file logo)
    public Brand createBrand(Brand brand, MultipartFile logoFile) {
        if (logoFile != null && !logoFile.isEmpty()) {
            if (!fileStorageService.isImageFile(logoFile)) {
                throw new RuntimeException("File logo phải là hình ảnh (jpg, png, webp,...)");
            }
            String path = fileStorageService.storeFile(logoFile, "brands");
            brand.setLogoUrl("/images/" + path);
        }
        return brandRepository.save(brand);
    }
    
    // Cập nhật brand (có thể kèm file logo mới)
    public Brand updateBrand(Long id, Brand brandDetails, MultipartFile logoFile) {
        Brand brand = brandRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu với ID: " + id));
        
        brand.setName(brandDetails.getName());
        brand.setDescription(brandDetails.getDescription());
        brand.setDisplayOrder(brandDetails.getDisplayOrder());
        brand.setIsActive(brandDetails.getIsActive());

        if (logoFile != null && !logoFile.isEmpty()) {
            if (!fileStorageService.isImageFile(logoFile)) {
                throw new RuntimeException("File logo phải là hình ảnh (jpg, png, webp,...)");
            }
            // Xóa logo cũ nếu là file local
            if (brand.getLogoUrl() != null && brand.getLogoUrl().startsWith("/images/")) {
                fileStorageService.deleteFile(brand.getLogoUrl().substring("/images/".length()));
            }
            String path = fileStorageService.storeFile(logoFile, "brands");
            brand.setLogoUrl("/images/" + path);
        }
        
        return brandRepository.save(brand);
    }
    
    // Xóa brand
    public void deleteBrand(Long id) {
        Brand brand = brandRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu với ID: " + id));
        
        // Kiểm tra xem có sản phẩm nào thuộc thương hiệu này không
        long productCount = productRepository.countByBrandId(id);
        if (productCount > 0) {
            throw new RuntimeException("Không thể xóa thương hiệu này vì còn " + productCount + " sản phẩm. Vui lòng chuyển hoặc xóa các sản phẩm trước.");
        }

        // Xóa file logo local
        if (brand.getLogoUrl() != null && brand.getLogoUrl().startsWith("/images/")) {
            fileStorageService.deleteFile(brand.getLogoUrl().substring("/images/".length()));
        }
        
        brandRepository.delete(brand);
    }
    
    // Tìm kiếm brand theo tên
    public List<Brand> searchBrands(String name) {
        return brandRepository.findByNameContainingIgnoreCase(name);
    }
}
