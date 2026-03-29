package j2ee_backend.nhom05.service;

import j2ee_backend.nhom05.model.AttributeDefinition;
import j2ee_backend.nhom05.model.Product;
import j2ee_backend.nhom05.model.ProductSpecification;
import j2ee_backend.nhom05.repository.IAttributeDefinitionRepository;
import j2ee_backend.nhom05.repository.IProductRepository;
import j2ee_backend.nhom05.repository.IProductSpecificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ProductSpecificationService {

    @Autowired
    private IProductSpecificationRepository specRepository;

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IAttributeDefinitionRepository attributeDefinitionRepository;

    /** Lấy tất cả thuộc tính của sản phẩm, sắp xếp theo display_order. */
    public List<ProductSpecification> getSpecsByProduct(Long productId) {
        productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + productId));
        return specRepository.findByProductIdOrderByDisplayOrderAsc(productId);
    }

    /** Lấy thuộc tính theo ID. */
    public Optional<ProductSpecification> getSpecById(Long id) {
        return specRepository.findById(id);
    }

    /**
     * Thêm một thuộc tính cho sản phẩm.
     * Nếu attrDefId được cung cấp → liên kết chuẩn hóa EAV.
     * Nếu không → lưu theo specKey/specValue dạng tự do (legacy).
     */
    public ProductSpecification addSpec(Long productId, Long attrDefId,
                                         String specKey, String specValue,
                                         BigDecimal valueNumber, Integer displayOrder) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + productId));

        ProductSpecification spec = new ProductSpecification();
        spec.setProduct(product);
        spec.setSpecValue(specValue);
        spec.setValueNumber(valueNumber);
        spec.setDisplayOrder(displayOrder != null ? displayOrder : 0);

        if (attrDefId != null) {
            AttributeDefinition def = attributeDefinitionRepository.findById(attrDefId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuộc tính với ID: " + attrDefId));
            spec.setAttributeDefinition(def);
            // Đặt specKey theo attrKey chuẩn hóa
            spec.setSpecKey(def.getAttrKey());
        } else {
            spec.setSpecKey(specKey);
        }

        return specRepository.save(spec);
    }

    /** Cập nhật giá trị của một thuộc tính sản phẩm. */
    public ProductSpecification updateSpec(Long id, String specValue,
                                            BigDecimal valueNumber, Integer displayOrder) {
        ProductSpecification spec = specRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy thuộc tính sản phẩm với ID: " + id));
        if (specValue != null) spec.setSpecValue(specValue);
        if (valueNumber != null) spec.setValueNumber(valueNumber);
        if (displayOrder != null) spec.setDisplayOrder(displayOrder);
        return specRepository.save(spec);
    }

    /** Xóa một thuộc tính sản phẩm. */
    public void deleteSpec(Long id) {
        ProductSpecification spec = specRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy thuộc tính sản phẩm với ID: " + id));
        specRepository.delete(spec);
    }

    /** Xóa toàn bộ thuộc tính của một sản phẩm. */
    @Transactional
    public void deleteAllSpecsByProduct(Long productId) {
        specRepository.deleteByProductId(productId);
    }

    /**
     * Lọc sản phẩm theo giá trị số của một thuộc tính.
     * VD: Tìm sản phẩm có RAM (attrKey="ram") >= 8 GB.
     */
    public List<Long> filterProductIdsByNumericSpec(String attrKey, BigDecimal minValue, BigDecimal maxValue) {
        return specRepository.findProductIdsByNumericSpec(attrKey, minValue, maxValue);
    }
}
