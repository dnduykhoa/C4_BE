package j2ee_backend.nhom05.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import j2ee_backend.nhom05.dto.ProductVariantRequest;
import j2ee_backend.nhom05.dto.ProductVariantValueRequest;
import j2ee_backend.nhom05.model.AttributeDefinition;
import j2ee_backend.nhom05.model.AttributeDefinition.DataType;
import j2ee_backend.nhom05.model.Product;
import j2ee_backend.nhom05.model.ProductVariant;
import j2ee_backend.nhom05.model.ProductVariantValue;
import j2ee_backend.nhom05.repository.IAttributeDefinitionRepository;
import j2ee_backend.nhom05.repository.IOrderRepository;
import j2ee_backend.nhom05.repository.IProductRepository;
import j2ee_backend.nhom05.repository.IProductVariantRepository;

@Service
public class ProductVariantService {

    @Autowired
    private IProductVariantRepository productVariantRepository;

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IOrderRepository orderRepository;

    @Autowired
    private IAttributeDefinitionRepository attributeDefinitionRepository;

    @Autowired
    private ProductMediaService productMediaService;

    @Autowired
    private SseService sseService;

    @Autowired
    private PreorderRequestService preorderRequestService;

    @Transactional(readOnly = true)
    public List<ProductVariant> getVariantsByProduct(Long productId, boolean onlyActive) {
        productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + productId));
        List<ProductVariant> variants;
        if (onlyActive) {
            variants = productVariantRepository.findByProductIdAndIsActiveTrueOrderByDisplayOrderAsc(productId);
        } else {
            variants = productVariantRepository.findByProductIdOrderByDisplayOrderAsc(productId);
        }
        variants.forEach(v -> v.setSoldCount(orderRepository.countSoldQuantityByVariantId(v.getId())));
        return variants;
    }

    @Transactional(readOnly = true)
    public Optional<ProductVariant> getVariantById(Long productId, Long variantId) {
        return productVariantRepository.findByIdAndProductId(variantId, productId);
    }

    @Transactional
    public ProductVariant createVariant(Long productId, ProductVariantRequest request) {
        validateRequest(request);

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + productId));

        ProductVariant variant = new ProductVariant();
        applyVariantFields(variant, request);
        variant.setProduct(product);

        List<ProductVariantValue> values = buildVariantValues(variant, request.getValues());
        variant.setValues(values);

        ProductVariant saved = productVariantRepository.save(variant);
        preorderRequestService.notifyVariantAvailability(product, saved);
        return saved;
    }

    @Transactional
    public ProductVariant updateVariant(Long productId, Long variantId, ProductVariantRequest request) {
        validateRequest(request);

        ProductVariant existing = productVariantRepository.findByIdAndProductId(variantId, productId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể với ID: " + variantId));

        applyVariantFields(existing, request);

        existing.getValues().clear();
        productVariantRepository.flush();
        List<ProductVariantValue> values = buildVariantValues(existing, request.getValues());
        existing.getValues().addAll(values);

        ProductVariant saved = productVariantRepository.save(existing);
        sseService.broadcastVariantUpdate(productId, saved.getId(), saved.getIsActive(), saved.getStockQuantity());
        preorderRequestService.notifyVariantAvailability(saved.getProduct(), saved);
        return saved;
    }

    @Transactional
    public void deleteVariant(Long productId, Long variantId) {
        ProductVariant existing = productVariantRepository.findByIdAndProductId(variantId, productId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể với ID: " + variantId));
        productMediaService.deleteAllVariantMedia(productId, variantId);
        productVariantRepository.delete(existing);
        sseService.broadcastVariantUpdate(productId, variantId, false, 0);
    }

    @Transactional(readOnly = true)
    public Map<String, List<String>> getVariantOptions(Long productId) {
        List<ProductVariant> variants = productVariantRepository.findByProductIdAndIsActiveTrueOrderByDisplayOrderAsc(productId);

        Map<String, LinkedHashSet<String>> optionSet = new LinkedHashMap<>();
        for (ProductVariant variant : variants) {
            if (variant.getValues() == null) continue;
            for (ProductVariantValue value : variant.getValues()) {
                String key = normalize(value.getAttrKey());
                String displayValue = getComparableValue(value);
                if (key == null || displayValue == null) continue;
                optionSet.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(displayValue);
            }
        }

        Map<String, List<String>> result = new LinkedHashMap<>();
        optionSet.forEach((k, v) -> result.put(k, new ArrayList<>(v)));
        return result;
    }

    @Transactional(readOnly = true)
    public Optional<ProductVariant> resolveVariant(Long productId, Map<String, String> selections) {
        if (selections == null || selections.isEmpty()) return Optional.empty();

        Map<String, String> normalizedSelections = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : selections.entrySet()) {
            String key = normalize(entry.getKey());
            String value = normalize(entry.getValue());
            if (key != null && value != null) {
                normalizedSelections.put(key, value);
            }
        }

        if (normalizedSelections.isEmpty()) return Optional.empty();

        List<ProductVariant> candidates = productVariantRepository.findByProductIdAndIsActiveTrueOrderByDisplayOrderAsc(productId);
        for (ProductVariant variant : candidates) {
            if (matchesSelections(variant, normalizedSelections)) {
                return Optional.of(variant);
            }
        }
        return Optional.empty();
    }

    private boolean matchesSelections(ProductVariant variant, Map<String, String> selections) {
        if (variant.getValues() == null || variant.getValues().isEmpty()) return false;

        Map<String, String> variantMap = new LinkedHashMap<>();
        for (ProductVariantValue value : variant.getValues()) {
            String key = normalize(value.getAttrKey());
            String attrValue = getComparableValue(value);
            if (key != null && attrValue != null) {
                variantMap.put(key, attrValue);
            }
        }

        for (Map.Entry<String, String> selection : selections.entrySet()) {
            if (!Objects.equals(variantMap.get(selection.getKey()), selection.getValue())) {
                return false;
            }
        }
        return true;
    }

    private List<ProductVariantValue> buildVariantValues(ProductVariant variant, List<ProductVariantValueRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new RuntimeException("Biến thể phải có ít nhất 1 giá trị lựa chọn");
        }

        List<ProductVariantValue> values = new ArrayList<>();
        Set<String> usedKeys = new HashSet<>();
        for (ProductVariantValueRequest request : requests) {
            String key = normalize(request.getAttrKey());
            if (request.getAttrDefId() == null && key == null) {
                throw new RuntimeException("Mỗi lựa chọn phải có attrDefId hoặc attrKey");
            }

            ProductVariantValue value = new ProductVariantValue();
            value.setVariant(variant);
            Integer valueDisplayOrder = request.getDisplayOrder();
            value.setDisplayOrder(valueDisplayOrder == null ? 0 : valueDisplayOrder);
            value.setAttrValue(normalize(request.getAttrValue()));
            value.setValueNumber(request.getValueNumber());

            if (request.getAttrDefId() != null) {
                AttributeDefinition def = attributeDefinitionRepository.findById(request.getAttrDefId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thuộc tính với ID: " + request.getAttrDefId()));
                value.setAttributeDefinition(def);
                value.setAttrKey(def.getAttrKey());
                enforceValueTypeByDefinition(value, def);
            } else {
                value.setAttrKey(request.getAttrKey());
            }

            String normalizedKey = normalize(value.getAttrKey());
            if (normalizedKey == null) {
                throw new RuntimeException("Khóa thuộc tính biến thể không hợp lệ");
            }
            String dedupeKey = normalizedKey.toLowerCase();
            if (!usedKeys.add(dedupeKey)) {
                throw new RuntimeException("Mỗi thuộc tính chỉ được xuất hiện 1 lần trong biến thể: " + normalizedKey);
            }
            value.setAttrKey(normalizedKey);

            if (normalize(value.getAttrValue()) == null && value.getValueNumber() == null) {
                throw new RuntimeException("Giá trị lựa chọn không được để trống");
            }

            values.add(value);
        }
        return values;
    }

    private void applyVariantFields(ProductVariant variant, ProductVariantRequest request) {
        variant.setSku(normalize(request.getSku()));
        variant.setPrice(request.getPrice());
        Integer stockQuantity = request.getStockQuantity();
        Integer displayOrder = request.getDisplayOrder();
        Boolean requestedActive = request.getIsActive();

        int stock = stockQuantity == null ? 0 : stockQuantity;
        variant.setStockQuantity(stock);
        variant.setDisplayOrder(displayOrder == null ? 0 : displayOrder);

        if (requestedActive != null) {
            variant.setIsActive(requestedActive);
        } else if (variant.getIsActive() == null) {
            variant.setIsActive(stock > 0);
        }
    }

    private void validateRequest(ProductVariantRequest request) {
        if (request == null) {
            throw new RuntimeException("Dữ liệu biến thể không hợp lệ");
        }
        if (normalize(request.getSku()) == null) {
            throw new RuntimeException("Tên biến thể không được để trống");
        }
        if (request.getPrice() == null || request.getPrice().signum() <= 0) {
            throw new RuntimeException("Giá biến thể phải lớn hơn 0");
        }
        if (request.getStockQuantity() != null && request.getStockQuantity() < 0) {
            throw new RuntimeException("Số lượng biến thể không được âm");
        }
    }



    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void enforceValueTypeByDefinition(ProductVariantValue value, AttributeDefinition def) {
        DataType dataType = def.getDataType() == null ? DataType.STRING : def.getDataType();
        String attrValue = normalize(value.getAttrValue());
        BigDecimal numberValue = value.getValueNumber();

        switch (dataType) {
            case NUMBER -> {
                if (numberValue == null && attrValue != null) {
                    try {
                        numberValue = new BigDecimal(attrValue);
                    } catch (NumberFormatException ex) {
                        throw new RuntimeException("Thuộc tính " + def.getName() + " phải là số");
                    }
                }
                if (numberValue == null) {
                    throw new RuntimeException("Thuộc tính " + def.getName() + " phải có giá trị số");
                }
                value.setValueNumber(numberValue);
                value.setAttrValue(null);
            }
            case BOOLEAN -> {
                if (attrValue == null) {
                    throw new RuntimeException("Thuộc tính " + def.getName() + " phải là true/false");
                }
                String normalizedBoolean = attrValue.toLowerCase();
                if (!"true".equals(normalizedBoolean) && !"false".equals(normalizedBoolean)) {
                    throw new RuntimeException("Thuộc tính " + def.getName() + " chỉ chấp nhận true/false");
                }
                value.setAttrValue(normalizedBoolean);
                value.setValueNumber(null);
            }
            case STRING, LIST -> {
                if (attrValue == null) {
                    throw new RuntimeException("Thuộc tính " + def.getName() + " không được để trống");
                }
                value.setAttrValue(attrValue);
                value.setValueNumber(null);
            }
        }
    }

    private String getComparableValue(ProductVariantValue value) {
        String attrValue = normalize(value.getAttrValue());
        if (attrValue != null) return attrValue;
        if (value.getValueNumber() != null) {
            return value.getValueNumber().stripTrailingZeros().toPlainString();
        }
        return null;
    }
}
