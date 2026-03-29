package j2ee_backend.nhom05.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import j2ee_backend.nhom05.model.Product;
import j2ee_backend.nhom05.model.ProductMedia;
import j2ee_backend.nhom05.model.ProductVariant;
import j2ee_backend.nhom05.repository.IProductMediaRepository;
import j2ee_backend.nhom05.repository.IProductRepository;
import j2ee_backend.nhom05.repository.IProductVariantRepository;

@Service
public class ProductMediaService {
    
    @Autowired
    private IProductMediaRepository productMediaRepository;
    
    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IProductVariantRepository productVariantRepository;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    /**
     * Upload media cho sản phẩm
     */
    @Transactional
    public List<ProductMedia> uploadProductMedia(Long productId, MultipartFile[] files, boolean isPrimary) {
        return uploadProductMedia(productId, null, files, isPrimary);
    }

    /**
     * Upload media cho sản phẩm hoặc biến thể
     */
    @Transactional
    public List<ProductMedia> uploadProductMedia(Long productId, Long variantId, MultipartFile[] files, boolean isPrimary) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        ProductVariant variant = null;
        if (variantId != null) {
            variant = productVariantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể"));
        }
        
        List<ProductMedia> uploadedMedia = new ArrayList<>();

        // Lấy displayOrder tiếp theo từ media hiện có
        int nextOrder = variantId == null
            ? productMediaRepository.findMaxDisplayOrderByProductId(productId) + 1
            : productMediaRepository.findMaxDisplayOrderByProductIdAndVariantId(productId, variantId) + 1;
        // Kiểm tra đã có primary image trong scope hiện tại chưa
        List<ProductMedia> existingMedia = variantId == null
            ? productMediaRepository.findByProductIdAndVariantIsNull(productId)
            : productMediaRepository.findByProductIdAndVariantId(productId, variantId);
        boolean alreadyHasPrimary = existingMedia
            .stream().anyMatch(m -> Boolean.TRUE.equals(m.getIsPrimary()));

        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];

            if (file == null || file.isEmpty()) {
                continue; // Bỏ qua file rỗng
            }

            // Xác định loại media
            String mediaType;
            if (fileStorageService.isImageFile(file)) {
                mediaType = "IMAGE";
            } else if (fileStorageService.isVideoFile(file)) {
                mediaType = "VIDEO";
            } else {
                throw new RuntimeException("File '" + file.getOriginalFilename() + "' không hợp lệ. Chỉ chấp nhận hình ảnh hoặc video");
            }

            // Upload file và lấy đường dẫn
            String filePath = fileStorageService.storeFile(file, "products");

            // Tạo ProductMedia
            ProductMedia media = new ProductMedia();
            media.setProduct(product);
            media.setVariant(variant);
            media.setMediaUrl("/images/" + filePath);
            media.setMediaType(mediaType);
            // Chỉ file đầu tiên của lô này được set primary nếu chưa có primary
            media.setIsPrimary(isPrimary && i == 0 && !alreadyHasPrimary);
            media.setDisplayOrder(nextOrder + i);

            uploadedMedia.add(productMediaRepository.save(media));
        }
        
        return uploadedMedia;
    }
    
    /**
     * Xóa media của sản phẩm
     */
    @Transactional
    public void deleteProductMedia(Long mediaId) {
        ProductMedia media = productMediaRepository.findById(mediaId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy media"));
        
        // Xóa file vật lý
        String filePath = media.getMediaUrl().replace("/images/", "");
        fileStorageService.deleteFile(filePath);
        
        // Xóa record trong DB
        productMediaRepository.delete(media);
    }

    /**
     * Xóa media thuộc phạm vi sản phẩm cha (không phải media biến thể)
     */
    @Transactional
    public void deleteParentProductMedia(Long productId, Long mediaId) {
        ProductMedia media = productMediaRepository.findById(mediaId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy media"));

        if (!media.getProduct().getId().equals(productId)) {
            throw new RuntimeException("Media không thuộc sản phẩm này");
        }

        if (media.getVariant() != null) {
            throw new RuntimeException("Media này thuộc biến thể, không thể xóa ở phạm vi sản phẩm cha");
        }

        String filePath = media.getMediaUrl().replace("/images/", "");
        fileStorageService.deleteFile(filePath);
        productMediaRepository.delete(media);
    }
    
    /**
     * Lấy tất cả media của sản phẩm
     */
    public List<ProductMedia> getProductMedia(Long productId) {
        return productMediaRepository.findByProductIdAndVariantIsNull(productId);
    }

    /**
     * Lấy media của một biến thể
     */
    public List<ProductMedia> getVariantMedia(Long productId, Long variantId) {
        productVariantRepository.findByIdAndProductId(variantId, productId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể"));
        return productMediaRepository.findByProductIdAndVariantId(productId, variantId);
    }
    
    /**
     * Xóa tất cả media của sản phẩm
     */
    @Transactional
    public void deleteAllProductMedia(Long productId) {
        List<ProductMedia> mediaList = productMediaRepository.findByProductId(productId);
        
        for (ProductMedia media : mediaList) {
            // Xóa file vật lý
            String filePath = media.getMediaUrl().replace("/images/", "");
            fileStorageService.deleteFile(filePath);
        }
        
        // Xóa tất cả record trong DB
        productMediaRepository.deleteByProductId(productId);
    }

    /**
     * Đặt một media làm ảnh chính (isPrimary = true).
     * Tự động bỏ chọn primary của các media khác cùng sản phẩm.
     */
    @Transactional
    public ProductMedia setPrimaryMedia(Long productId, Long mediaId) {
        ProductMedia target = productMediaRepository.findById(mediaId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy media với ID: " + mediaId));
        if (!target.getProduct().getId().equals(productId)) {
            throw new RuntimeException("Media không thuộc sản phẩm này");
        }
        Long variantId = target.getVariant() != null ? target.getVariant().getId() : null;
        // Bỏ primary tất cả media trong cùng scope (product-level hoặc variant-level)
        List<ProductMedia> all = variantId == null
            ? productMediaRepository.findByProductIdAndVariantIsNull(productId)
            : productMediaRepository.findByProductIdAndVariantId(productId, variantId);
        for (ProductMedia m : all) {
            m.setIsPrimary(false);
        }
        productMediaRepository.saveAll(all);
        // Đặt target làm primary
        target.setIsPrimary(true);
        return productMediaRepository.save(target);
    }

    /**
     * Xóa tất cả media của một biến thể
     */
    @Transactional
    public void deleteAllVariantMedia(Long productId, Long variantId) {
        List<ProductMedia> mediaList = getVariantMedia(productId, variantId);

        for (ProductMedia media : mediaList) {
            String filePath = media.getMediaUrl().replace("/images/", "");
            fileStorageService.deleteFile(filePath);
        }

        productMediaRepository.deleteByProductIdAndVariantId(productId, variantId);
    }
}
