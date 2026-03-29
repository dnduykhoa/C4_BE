package j2ee_backend.nhom05.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import j2ee_backend.nhom05.dto.ProductCompareAttributeRowDto;
import j2ee_backend.nhom05.dto.ProductCompareItemDto;
import j2ee_backend.nhom05.dto.ProductComparisonResponse;
import j2ee_backend.nhom05.model.Category;
import j2ee_backend.nhom05.model.Product;
import j2ee_backend.nhom05.model.ProductMedia;
import j2ee_backend.nhom05.model.ProductSpecification;
import j2ee_backend.nhom05.model.ProductStatus;
import j2ee_backend.nhom05.repository.ICategoryRepository;
import j2ee_backend.nhom05.repository.IOrderRepository;
import j2ee_backend.nhom05.repository.IProductRepository;
import j2ee_backend.nhom05.repository.IProductSpecificationRepository;

@Service
public class ProductService {

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IOrderRepository orderRepository;

    @Autowired
    private ICategoryRepository categoryRepository;

    @Autowired
    private IProductSpecificationRepository specificationRepository;

    @Autowired
    private ProductMediaService productMediaService;

    @Autowired
    private SseService sseService;

    @Autowired
    private PreorderRequestService preorderRequestService;

    @Autowired
    private ProductReviewService reviewService;

    // Lấy tất cả sản phẩm
    public List<Product> getAllProducts() {
        return enrichProducts(deduplicateById(productRepository.findAll()));
    }

    // Lấy sản phẩm theo ID
    public Optional<Product> getProductById(Long id) {
        Optional<Product> opt = productRepository.findById(id);
        opt.ifPresent(this::enrichProduct);
        return opt;
    }

    // Tạo sản phẩm mới
    public Product createProduct(Product product) {
        // Thiết lập quan hệ hai chiều cho media
        if (product.getMedia() != null) {
            for (ProductMedia media : product.getMedia()) {
                media.setProduct(product);
            }
        }

        // Thiết lập quan hệ hai chiều cho specifications
        if (product.getSpecifications() != null) {
            for (ProductSpecification spec : product.getSpecifications()) {
                spec.setProduct(product);
            }
        }

        normalizeStatusByStock(product, null, true);
        Product saved = productRepository.save(product);
        preorderRequestService.notifyProductAvailability(saved);
        return saved;
    }

    // Cập nhật sản phẩm (chỉ thông tin cơ bản)
    public Product updateProduct(Long id, Product productDetails) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));

        ProductStatus previousStatus = product.getStatus();

        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setStockQuantity(productDetails.getStockQuantity());
        product.setCategory(productDetails.getCategory());
        product.setBrand(productDetails.getBrand());
        product.setStatus(productDetails.getStatus());

        normalizeStatusByStock(product, previousStatus, false);

        Product saved = productRepository.save(product);
        sseService.broadcastProductUpdate(saved.getId(), saved.getStatus().name(), saved.getStockQuantity());
        preorderRequestService.notifyProductAvailability(saved);
        return saved;
    }

    // Cập nhật sản phẩm với media (thông tin + hình ảnh/video)
    public Product updateProductWithMedia(Long id, Product productDetails, MultipartFile[] files,
            boolean replaceMedia) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));

        ProductStatus previousStatus = product.getStatus();

        // Cập nhật thông tin cơ bản
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setStockQuantity(productDetails.getStockQuantity());
        product.setCategory(productDetails.getCategory());
        product.setBrand(productDetails.getBrand());
        product.setStatus(productDetails.getStatus());

        normalizeStatusByStock(product, previousStatus, false);

        // Xử lý media nếu có files mới
        if (files != null && files.length > 0) {
            // Xóa media cũ nếu replaceMedia = true
            if (replaceMedia) {
                productMediaService.deleteAllProductMedia(id);
            }

            // Upload media mới
            productMediaService.uploadProductMedia(id, files, false);
        }

        Product saved = productRepository.save(product);
        sseService.broadcastProductUpdate(saved.getId(), saved.getStatus().name(), saved.getStockQuantity());
        preorderRequestService.notifyProductAvailability(saved);
        return saved;
    }

    // Ngừng kinh doanh sản phẩm (xóa mềm - chuyển status = INACTIVE)
    public Product deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
        product.setStatus(ProductStatus.INACTIVE);
        product.setNewArrivalAt(null);
        Product saved = productRepository.save(product);
        sseService.broadcastProductUpdate(saved.getId(), "INACTIVE", saved.getStockQuantity());
        return saved;
    }

    // Đánh dấu hàng sắp về (chuyển status = OUT_OF_STOCK)
    // Đánh dấu hàng mới về (chuyển status = NEW_ARRIVAL)
    public Product markNewArrival(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
        if (product.getStatus() != ProductStatus.OUT_OF_STOCK) {
            throw new RuntimeException("Chỉ có thể đánh dấu hàng mới về từ trạng thái Hàng sắp về");
        }
        if (product.getStockQuantity() == null || product.getStockQuantity() <= 0) {
            throw new RuntimeException("Cần nhập tồn kho lớn hơn 0 trước khi chuyển sang Hàng mới về");
        }
        product.setStatus(ProductStatus.NEW_ARRIVAL);
        product.setNewArrivalAt(LocalDateTime.now());
        Product saved = productRepository.save(product);
        sseService.broadcastProductUpdate(saved.getId(), "NEW_ARRIVAL", saved.getStockQuantity());
        preorderRequestService.notifyProductAvailability(saved);
        return saved;
    }

    // Đánh dấu hết hàng (chuyển status = OUT_OF_STOCK)
    public Product markOutOfStock(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
        if (product.getStockQuantity() != null && product.getStockQuantity() > 0) {
            throw new RuntimeException("Không thể gán Hàng sắp về khi tồn kho vẫn lớn hơn 0");
        }
        product.setStatus(ProductStatus.OUT_OF_STOCK);
        product.setNewArrivalAt(null);
        Product saved = productRepository.save(product);
        sseService.broadcastProductUpdate(saved.getId(), "OUT_OF_STOCK", saved.getStockQuantity());
        return saved;
    }

    // Kích hoạt lại sản phẩm (chuyển status = ACTIVE)
    public Product restoreProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
        product.setStatus(ProductStatus.ACTIVE);
        product.setNewArrivalAt(null);
        Product saved = productRepository.save(product);
        sseService.broadcastProductUpdate(saved.getId(), "ACTIVE", saved.getStockQuantity());
        return saved;
    }

    // Tìm kiếm sản phẩm theo tên
    public List<Product> searchProductsByName(String name) {
        return enrichProducts(deduplicateById(productRepository.findByNameContainingIgnoreCase(name)));
    }

    // Lấy sản phẩm theo danh mục
    public List<Product> getProductsByCategory(Long categoryId) {
        return enrichProducts(deduplicateById(productRepository.findByCategoryId(categoryId)));
    }

    // Lấy sản phẩm cùng loại (cùng category), loại trừ chính sản phẩm đang xem
    public List<Product> getSameTypeProducts(Long productId, Integer limit) {
        Product baseProduct = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + productId));

        if (baseProduct.getCategory() == null) {
            throw new RuntimeException("Sản phẩm chưa được gán danh mục để so sánh cùng loại");
        }

        List<Product> sameTypeProducts = deduplicateById(
                productRepository.findByCategoryIdAndIdNot(baseProduct.getCategory().getId(), productId));

        if (sameTypeProducts == null || sameTypeProducts.isEmpty()) {
            return sameTypeProducts;
        }

        int effectiveLimit = (limit == null || limit <= 0) ? 10 : limit;
        if (sameTypeProducts.size() > effectiveLimit) {
            sameTypeProducts = new ArrayList<>(sameTypeProducts.subList(0, effectiveLimit));
        }

        return enrichProducts(sameTypeProducts);
    }

    // So sánh nhiều sản phẩm cùng loại theo thông số EAV
    public ProductComparisonResponse compareProducts(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            throw new RuntimeException("Danh sách sản phẩm so sánh không được để trống");
        }

        Set<Long> normalizedIds = new LinkedHashSet<>();
        for (Long id : productIds) {
            if (id != null) {
                normalizedIds.add(id);
            }
        }

        if (normalizedIds.size() < 2) {
            throw new RuntimeException("Cần chọn ít nhất 2 sản phẩm để so sánh");
        }

        List<Long> orderedIds = new ArrayList<>(normalizedIds);
        List<Product> fetchedProducts = productRepository.findAllById(orderedIds);
        if (fetchedProducts.size() != orderedIds.size()) {
            throw new RuntimeException("Có sản phẩm không tồn tại trong danh sách so sánh");
        }

        Map<Long, Product> productsById = new LinkedHashMap<>();
        for (Product product : fetchedProducts) {
            productsById.put(product.getId(), product);
        }

        List<Product> orderedProducts = new ArrayList<>();
        for (Long id : orderedIds) {
            Product product = productsById.get(id);
            if (product == null) {
                throw new RuntimeException("Có sản phẩm không tồn tại trong danh sách so sánh");
            }
            orderedProducts.add(product);
        }

        Product baseProduct = orderedProducts.get(0);
        if (baseProduct.getCategory() == null) {
            throw new RuntimeException("Sản phẩm chưa được gán danh mục để so sánh");
        }
        Long categoryId = baseProduct.getCategory().getId();
        String categoryName = baseProduct.getCategory().getName();

        for (Product product : orderedProducts) {
            if (product.getCategory() == null || !categoryId.equals(product.getCategory().getId())) {
                throw new RuntimeException("Chỉ có thể so sánh các sản phẩm cùng loại (cùng danh mục)");
            }
        }

        enrichProducts(orderedProducts);

        List<ProductSpecification> specifications = specificationRepository
                .findByProductIdInOrderByDisplayOrderAsc(orderedIds);

        Map<String, ProductCompareAttributeRowDto> attributeRowsByKey = new LinkedHashMap<>();
        for (ProductSpecification specification : specifications) {
            String attrKey = resolveAttrKey(specification);
            if (attrKey == null || attrKey.isBlank()) {
                continue;
            }

            ProductCompareAttributeRowDto row = attributeRowsByKey.computeIfAbsent(attrKey, k -> {
                ProductCompareAttributeRowDto created = new ProductCompareAttributeRowDto();
                created.setAttrKey(k);
                created.setAttrName(resolveAttrName(specification, k));
                created.setUnit(resolveUnit(specification));
                created.setDisplayOrder(resolveDisplayOrder(specification));
                created.setValues(new LinkedHashMap<>());
                return created;
            });

            if ((row.getAttrName() == null || row.getAttrName().isBlank())
                    && specification.getAttributeDefinition() != null
                    && specification.getAttributeDefinition().getName() != null) {
                row.setAttrName(specification.getAttributeDefinition().getName());
            }

            if ((row.getUnit() == null || row.getUnit().isBlank())
                    && specification.getAttributeDefinition() != null
                    && specification.getAttributeDefinition().getUnit() != null) {
                row.setUnit(specification.getAttributeDefinition().getUnit());
            }

            if (row.getDisplayOrder() == null) {
                row.setDisplayOrder(resolveDisplayOrder(specification));
            }

            Long productId = specification.getProduct() != null ? specification.getProduct().getId() : null;
            if (productId != null) {
                row.getValues().put(productId, normalizeSpecValue(specification.getDisplayValue()));
            }
        }

        for (ProductCompareAttributeRowDto row : attributeRowsByKey.values()) {
            for (Long productId : orderedIds) {
                row.getValues().putIfAbsent(productId, "-");
            }
        }

        List<ProductCompareAttributeRowDto> attributeRows = new ArrayList<>(attributeRowsByKey.values());
        attributeRows.sort(Comparator
                .comparing((ProductCompareAttributeRowDto row) -> row.getDisplayOrder() == null ? Integer.MAX_VALUE
                        : row.getDisplayOrder())
                .thenComparing(row -> row.getAttrName() == null ? "" : row.getAttrName(),
                        String.CASE_INSENSITIVE_ORDER));

        List<ProductCompareItemDto> compareItems = new ArrayList<>();
        for (Product product : orderedProducts) {
            compareItems.add(buildCompareItem(product));
        }

        ProductComparisonResponse response = new ProductComparisonResponse();
        response.setCategoryId(categoryId);
        response.setCategoryName(categoryName);
        response.setProducts(compareItems);
        response.setAttributes(attributeRows);
        return response;
    }

    // Lấy sản phẩm theo thương hiệu
    public List<Product> getProductsByBrand(Long brandId) {
        return enrichProducts(deduplicateById(productRepository.findByBrandId(brandId)));
    }

    // Lấy sản phẩm đang hoạt động (status = ACTIVE)
    public List<Product> getActiveProducts() {
        return enrichProducts(deduplicateById(productRepository.findByStatus(ProductStatus.ACTIVE)));
    }

    // Lấy sản phẩm hàng sắp về (status = OUT_OF_STOCK)
    // Lấy sản phẩm hàng mới về (status = NEW_ARRIVAL)
    public List<Product> getNewArrivalProducts() {
        return enrichProducts(deduplicateById(productRepository.findByStatus(ProductStatus.NEW_ARRIVAL)));
    }

    // Lấy sản phẩm hết hàng (status = OUT_OF_STOCK)
    public List<Product> getOutOfStockProducts() {
        return enrichProducts(deduplicateById(productRepository.findByStatus(ProductStatus.OUT_OF_STOCK)));
    }

    // Lấy sản phẩm ngừng kinh doanh (status = INACTIVE)
    public List<Product> getInactiveProducts() {
        return enrichProducts(deduplicateById(productRepository.findByStatus(ProductStatus.INACTIVE)));
    }

    // Lấy sản phẩm theo khoảng giá
    public List<Product> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return enrichProducts(deduplicateById(productRepository.findByPriceBetween(minPrice, maxPrice)));
    }

    // Lấy sản phẩm theo danh sách nhiều categoryId
    public List<Product> getProductsByCategories(List<Long> categoryIds) {
        return enrichProducts(deduplicateById(productRepository.findByCategoryIdIn(categoryIds)));
    }

    // Lấy sản phẩm theo danh mục và tất cả danh mục con của nó
    public List<Product> getProductsByCategoryTree(Long categoryId) {
        List<Long> ids = new ArrayList<>();
        ids.add(categoryId);
        // Lấy tất cả danh mục con trực tiếp
        List<Category> children = categoryRepository.findByParentId(categoryId);
        for (Category child : children) {
            ids.add(child.getId());
        }
        return enrichProducts(deduplicateById(productRepository.findByCategoryIdIn(ids)));
    }

    // Lọc sản phẩm theo nhiều tiêu chí kết hợp (danh mục, thương hiệu, giá, tên)
    public List<Product> filterProducts(List<Long> categoryIds, List<Long> brandIds,
            BigDecimal minPrice, BigDecimal maxPrice, String name) {
        return enrichProducts(deduplicateById(productRepository.findAll(
                ProductFilterSpec.build(categoryIds, brandIds, minPrice, maxPrice, name))));
    }

    private List<Product> deduplicateById(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return products;
        }
        return new ArrayList<>(new LinkedHashMap<>(
                products.stream().collect(java.util.stream.Collectors.toMap(
                        Product::getId,
                        product -> product,
                        (first, second) -> first,
                        LinkedHashMap::new)))
                .values());
    }

    private void normalizeStatusByStock(Product product, ProductStatus previousStatus, boolean isCreate) {
        if (product.getStatus() == ProductStatus.INACTIVE) {
            product.setNewArrivalAt(null);
            return;
        }

        Integer stockValue = product.getStockQuantity();
        int stock = stockValue != null ? stockValue.intValue() : 0;
        if (stock <= 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
            product.setNewArrivalAt(null);
            return;
        }

        if (isCreate) {
            product.setStatus(ProductStatus.NEW_ARRIVAL);
            product.setNewArrivalAt(LocalDateTime.now());
            return;
        }

        if (previousStatus == ProductStatus.OUT_OF_STOCK) {
            product.setStatus(ProductStatus.NEW_ARRIVAL);
            product.setNewArrivalAt(LocalDateTime.now());
            return;
        }

        if (product.getStatus() == null) {
            product.setStatus(ProductStatus.ACTIVE);
        }

        if (product.getStatus() == ProductStatus.NEW_ARRIVAL) {
            if (product.getNewArrivalAt() == null) {
                product.setNewArrivalAt(LocalDateTime.now());
            }
        } else {
            product.setNewArrivalAt(null);
        }
    }

    private ProductCompareItemDto buildCompareItem(Product product) {
        ProductCompareItemDto item = new ProductCompareItemDto();
        item.setId(product.getId());
        item.setName(product.getName());
        item.setPrice(product.getPrice());
        item.setStockQuantity(product.getStockQuantity());
        item.setStatus(product.getStatus() != null ? product.getStatus().name() : null);
        item.setBrandName(product.getBrand() != null ? product.getBrand().getName() : null);

        String imageUrl = null;
        ProductMedia primaryImage = product.getPrimaryImage();
        if (primaryImage != null) {
            imageUrl = primaryImage.getMediaUrl();
        } else if (product.getMedia() != null && !product.getMedia().isEmpty()) {
            imageUrl = product.getMedia().get(0).getMediaUrl();
        }
        item.setImageUrl(imageUrl);
        return item;
    }

    private String resolveAttrKey(ProductSpecification specification) {
        if (specification.getAttributeDefinition() != null
                && specification.getAttributeDefinition().getAttrKey() != null
                && !specification.getAttributeDefinition().getAttrKey().isBlank()) {
            return specification.getAttributeDefinition().getAttrKey();
        }
        return specification.getSpecKey();
    }

    private String resolveAttrName(ProductSpecification specification, String fallbackKey) {
        if (specification.getAttributeDefinition() != null
                && specification.getAttributeDefinition().getName() != null
                && !specification.getAttributeDefinition().getName().isBlank()) {
            return specification.getAttributeDefinition().getName();
        }
        return fallbackKey;
    }

    private String resolveUnit(ProductSpecification specification) {
        if (specification.getAttributeDefinition() != null) {
            return specification.getAttributeDefinition().getUnit();
        }
        return null;
    }

    private Integer resolveDisplayOrder(ProductSpecification specification) {
        if (specification.getAttributeDefinition() != null
                && specification.getAttributeDefinition().getDisplayOrder() != null) {
            return specification.getAttributeDefinition().getDisplayOrder();
        }
        return specification.getDisplayOrder();
    }

    private String normalizeSpecValue(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }

    private void enrichProduct(Product product) {
        if (product == null) return;
        
        // Set soldCount for product
        product.setSoldCount(orderRepository.countSoldQuantityByProductId(product.getId()));
        
        // Set soldCount and reviewSummary for variants
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            for (var variant : product.getVariants()) {
                variant.setSoldCount(orderRepository.countSoldQuantityByVariantId(variant.getId()));
                
                // Set reviewSummary cho từng variant
                try {
                    var variantSummary = reviewService.getReviewSummaryByVariant(product.getId(), variant.getId());
                    variant.setReviewSummary(new Product.ReviewSummary(
                        variantSummary.averageRating,
                        variantSummary.totalReviews
                    ));
                } catch (Exception e) {
                    variant.setReviewSummary(new Product.ReviewSummary(0.0, 0));
                }
            }
        }
        
        // Set reviewSummary cho product (chỉ đánh giá không có variant)
        try {
            var summary = reviewService.getReviewSummary(product.getId());
            product.setReviewSummary(new Product.ReviewSummary(
                summary.averageRating,
                summary.totalReviews
            ));
        } catch (Exception e) {
            product.setReviewSummary(new Product.ReviewSummary(0.0, 0));
        }
    }

    private List<Product> enrichProducts(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return products;
        }
        for (Product product : products) {
            enrichProduct(product);
        }
        return products;
    }

    public void expireNewArrivalProducts() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(5);
        List<Product> expiredProducts = productRepository.findByStatusAndNewArrivalAtBefore(
            ProductStatus.NEW_ARRIVAL,
            threshold
        );
        for (Product product : expiredProducts) {
            product.setStatus(ProductStatus.ACTIVE);
            product.setNewArrivalAt(null);
            Product saved = productRepository.save(product);
            sseService.broadcastProductUpdate(saved.getId(), "ACTIVE", saved.getStockQuantity());
        }
    }
}
