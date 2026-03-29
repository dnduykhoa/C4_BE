package j2ee_backend.nhom05.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import j2ee_backend.nhom05.dto.PreorderRegistrationRequest;
import j2ee_backend.nhom05.dto.PreorderRequestResponse;
import j2ee_backend.nhom05.model.PreorderRequest;
import j2ee_backend.nhom05.model.PreorderRequestStatus;
import j2ee_backend.nhom05.model.Product;
import j2ee_backend.nhom05.model.ProductStatus;
import j2ee_backend.nhom05.model.ProductVariant;
import j2ee_backend.nhom05.model.ProductVariantValue;
import j2ee_backend.nhom05.repository.IPreorderRequestRepository;
import j2ee_backend.nhom05.repository.IProductRepository;
import j2ee_backend.nhom05.repository.IProductVariantRepository;

@Service
public class PreorderRequestService {

    private static final EnumSet<PreorderRequestStatus> OPEN_STATUSES = EnumSet.of(
        PreorderRequestStatus.WAITING,
        PreorderRequestStatus.NOTIFIED
    );

    @Autowired
    private IPreorderRequestRepository preorderRequestRepository;

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IProductVariantRepository productVariantRepository;

    @Transactional
    public PreorderRequestResponse createRequest(PreorderRegistrationRequest request) {
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        if (product.getStatus() == ProductStatus.INACTIVE) {
            throw new RuntimeException("Sản phẩm này đã ngừng kinh doanh");
        }

        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = productVariantRepository.findByIdAndProductId(request.getVariantId(), product.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm đã chọn"));
            if (!Boolean.TRUE.equals(variant.getIsActive())) {
                throw new RuntimeException("Sản phẩm đã chọn hiện không khả dụng");
            }
            Integer variantStock = variant.getStockQuantity();
            if ((variantStock != null ? variantStock : 0) > 0) {
                throw new RuntimeException("Sản phẩm này hiện đã có hàng, không cần đăng ký chờ");
            }
        } else {
            if (product.getStatus() == ProductStatus.OUT_OF_STOCK) {
                // Ưu tiên cho phép chờ theo sản phẩm cha khi cha đang ở trạng thái Hàng sắp về.
                // Trường hợp này không bắt buộc người dùng chọn một biến thể cụ thể.
            } else if (productVariantRepository.existsByProductIdAndIsActiveTrueAndStockQuantityGreaterThan(product.getId(), 0)) {
                throw new RuntimeException("Vui lòng chọn đúng phân loại sản phẩm để đăng ký chờ");
            }
            Integer productStock = product.getStockQuantity();
            if ((productStock != null ? productStock : 0) > 0
                && product.getStatus() != ProductStatus.OUT_OF_STOCK) {
                throw new RuntimeException("Sản phẩm này hiện đã có hàng, không cần đăng ký chờ");
            }
        }

        String normalizedEmail = normalizeEmail(request.getEmail());
        String normalizedPhone = normalizePhone(request.getPhone());
        String normalizedName = normalizeText(request.getCustomerName());

        if (normalizedName == null) {
            throw new RuntimeException("Họ tên không được để trống");
        }
        if (normalizedPhone == null) {
            throw new RuntimeException("Số điện thoại không được để trống");
        }
        if (normalizedEmail == null) {
            throw new RuntimeException("Email không được để trống");
        }

        boolean duplicateExists = variant == null
            ? preorderRequestRepository.existsByEmailIgnoreCaseAndProductIdAndVariantIsNullAndStatusIn(
                normalizedEmail,
                product.getId(),
                OPEN_STATUSES
            )
            : preorderRequestRepository.existsByEmailIgnoreCaseAndProductIdAndVariantIdAndStatusIn(
                normalizedEmail,
                product.getId(),
                variant.getId(),
                OPEN_STATUSES
            );

        if (duplicateExists) {
            throw new RuntimeException("Email này đã có yêu cầu chờ hàng cho sản phẩm đã chọn");
        }

        PreorderRequest entity = new PreorderRequest();
        entity.setProduct(product);
        entity.setVariant(variant);
        entity.setCustomerName(normalizedName);
        entity.setPhone(normalizedPhone);
        entity.setEmail(normalizedEmail);
        entity.setDesiredQuantity(Math.max(1, request.getDesiredQuantity()));
        entity.setStatus(PreorderRequestStatus.WAITING);

        PreorderRequest saved = preorderRequestRepository.save(entity);
        return toResponse(saved, resolveQueuePosition(saved));
    }

    @Transactional(readOnly = true)
    public List<PreorderRequestResponse> getAllRequests() {
        List<PreorderRequest> requests = preorderRequestRepository.findAllByOrderByCreatedAtDescIdDesc();
        List<PreorderRequestResponse> result = new ArrayList<>();
        for (PreorderRequest request : requests) {
            Integer queuePosition = request.getStatus() == PreorderRequestStatus.WAITING
                ? resolveQueuePosition(request)
                : null;
            result.add(toResponse(request, queuePosition));
        }
        return result;
    }

    @Transactional
    public void notifyProductAvailability(Product product) {
        if (product == null || product.getId() == null) return;
        Integer productStock = product.getStockQuantity();
        int availableSlots = productStock != null ? productStock : 0;
        if (availableSlots <= 0 || product.getStatus() != ProductStatus.NEW_ARRIVAL) {
            return;
        }

        List<PreorderRequest> queue = preorderRequestRepository
            .findByProductIdAndVariantIsNullAndStatusOrderByCreatedAtAscIdAsc(product.getId(), PreorderRequestStatus.WAITING);
        notifyQueue(queue, availableSlots, product.getName());
    }

    @Transactional
    public void notifyVariantAvailability(Product product, ProductVariant variant) {
        if (product == null || variant == null || variant.getId() == null) return;
        if (!Boolean.TRUE.equals(variant.getIsActive())) return;

        Integer variantStock = variant.getStockQuantity();
        int availableSlots = variantStock != null ? variantStock : 0;
        if (availableSlots <= 0) return;

        List<PreorderRequest> queue = preorderRequestRepository
            .findByProductIdAndVariantIdAndStatusOrderByCreatedAtAscIdAsc(product.getId(), variant.getId(), PreorderRequestStatus.WAITING);
        notifyQueue(queue, availableSlots, product.getName());
    }

    @Transactional
    public void processPendingNotifications() {
        List<Product> newArrivals = productRepository.findByStatus(ProductStatus.NEW_ARRIVAL);
        for (Product product : newArrivals) {
            notifyProductAvailability(product);
        }

        List<ProductVariant> availableVariants = productVariantRepository.findByIsActiveTrueAndStockQuantityGreaterThan(0);
        for (ProductVariant variant : availableVariants) {
            notifyVariantAvailability(variant.getProduct(), variant);
        }
    }

    private void notifyQueue(List<PreorderRequest> queue, int availableSlots, String productName) {
        if (queue == null || queue.isEmpty()) return;

        int limit = Math.min(availableSlots, queue.size());
        for (int index = 0; index < limit; index++) {
            PreorderRequest request = queue.get(index);
            request.setStatus(PreorderRequestStatus.NOTIFIED);
            request.setNotifiedAt(LocalDateTime.now());
        }
        preorderRequestRepository.saveAll(queue.subList(0, limit));
    }

    private Integer resolveQueuePosition(PreorderRequest request) {
        if (request == null || request.getId() == null || request.getProduct() == null) {
            return null;
        }

        List<PreorderRequest> queue = request.getVariant() == null
            ? preorderRequestRepository.findByProductIdAndVariantIsNullAndStatusOrderByCreatedAtAscIdAsc(
                request.getProduct().getId(),
                PreorderRequestStatus.WAITING
            )
            : preorderRequestRepository.findByProductIdAndVariantIdAndStatusOrderByCreatedAtAscIdAsc(
                request.getProduct().getId(),
                request.getVariant().getId(),
                PreorderRequestStatus.WAITING
            );

        for (int index = 0; index < queue.size(); index++) {
            if (queue.get(index).getId().equals(request.getId())) {
                return index + 1;
            }
        }
        return null;
    }

    private PreorderRequestResponse toResponse(PreorderRequest request, Integer queuePosition) {
        return new PreorderRequestResponse(
            request.getId(),
            request.getProduct().getId(),
            request.getProduct().getName(),
            request.getVariant() != null ? request.getVariant().getId() : null,
            buildVariantLabel(request.getVariant()),
            request.getCustomerName(),
            request.getPhone(),
            request.getEmail(),
            request.getDesiredQuantity(),
            request.getStatus(),
            queuePosition,
            request.getCreatedAt(),
            request.getNotifiedAt()
        );
    }

    private String buildVariantLabel(ProductVariant variant) {
        if (variant == null) return null;
        if (variant.getSku() != null && !variant.getSku().isBlank()) {
            return variant.getSku().trim();
        }

        List<String> parts = new ArrayList<>();
        for (ProductVariantValue value : variant.getValues()) {
            String attrName = value.getAttributeDefinition() != null && value.getAttributeDefinition().getName() != null
                ? value.getAttributeDefinition().getName().trim()
                : value.getAttrKey();
            String attrValue = value.getAttrValue();
            if ((attrValue == null || attrValue.isBlank()) && value.getValueNumber() != null) {
                attrValue = value.getValueNumber().stripTrailingZeros().toPlainString();
            }
            if (attrName != null && !attrName.isBlank() && attrValue != null && !attrValue.isBlank()) {
                parts.add(attrName + ": " + attrValue);
            }
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private String normalizeText(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeEmail(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? null : normalized.toLowerCase();
    }

    private String normalizePhone(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? null : normalized.replaceAll("[\\s\\-]", "");
    }
}