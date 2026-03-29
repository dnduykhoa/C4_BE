package j2ee_backend.nhom05.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import j2ee_backend.nhom05.dto.SaleProgramRequest;
import j2ee_backend.nhom05.dto.SaleProgramResponse;
import j2ee_backend.nhom05.model.DiscountType;
import j2ee_backend.nhom05.model.Product;
import j2ee_backend.nhom05.model.SaleProgram;
import j2ee_backend.nhom05.model.SaleProgramCondition;
import j2ee_backend.nhom05.repository.IProductRepository;
import j2ee_backend.nhom05.repository.ISaleProgramRepository;

@Service
public class SaleProgramService {

    @Autowired
    private ISaleProgramRepository saleProgramRepository;

    @Autowired
    private IProductRepository productRepository;

    // DTO để truyền thông tin dòng sản phẩm khi tính giảm giá
    public static class OrderLineInfo {
        public final Long productId;
        public final BigDecimal unitPrice;
        public final int quantity;

        public OrderLineInfo(Long productId, BigDecimal unitPrice, int quantity) {
            this.productId = productId;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
        }
    }

    // ── CRUD ───────────────────────────────────────────────────────────────────

    @Transactional
    public SaleProgramResponse create(SaleProgramRequest request) {
        validateRequest(request);

        SaleProgram sp = new SaleProgram();
        mapRequestToEntity(request, sp);
        SaleProgram saved = saleProgramRepository.save(sp);
        return toResponse(saved);
    }

    @Transactional
    public SaleProgramResponse update(Long id, SaleProgramRequest request) {
        validateRequest(request);

        SaleProgram sp = saleProgramRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chương trình sale với ID: " + id));

        // Cập nhật các trường cơ bản
        sp.setName(request.getName());
        sp.setDescription(request.getDescription());
        sp.setDiscountType(request.getDiscountType());
        sp.setDiscountValue(request.getDiscountValue());
        sp.setMaxDiscountAmount(request.getMaxDiscountAmount());
        sp.setStartDate(request.getStartDate());
        sp.setEndDate(request.getEndDate());
        sp.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        // Cập nhật sản phẩm: clear rồi add lại (ManyToMany - không orphanRemoval)
        sp.getProducts().clear();
        if (request.getProductIds() != null) {
            for (Long productId : request.getProductIds()) {
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + productId));
                sp.getProducts().add(product);
            }
        }

        // Cập nhật conditions: PHẢI clear trên managed list rồi add mới (orphanRemoval)
        sp.getConditions().clear();
        if (request.getConditions() != null) {
            for (SaleProgramRequest.ConditionRequest cr : request.getConditions()) {
                SaleProgramCondition cond = new SaleProgramCondition();
                cond.setSaleProgram(sp);
                cond.setConditionType(cr.getConditionType());
                cond.setConditionValue(cr.getConditionValue());
                cond.setDescription(cr.getDescription());
                sp.getConditions().add(cond);
            }
        }

        SaleProgram saved = saleProgramRepository.save(sp);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public SaleProgramResponse getById(Long id) {
        SaleProgram sp = saleProgramRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chương trình sale với ID: " + id));
        return toResponse(sp);
    }

    @Transactional(readOnly = true)
    public List<SaleProgramResponse> getAll() {
        return saleProgramRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SaleProgramResponse> getActive() {
        return saleProgramRepository.findByIsActiveTrueOrderByStartDateDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        if (!saleProgramRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy chương trình sale với ID: " + id);
        }
        saleProgramRepository.deleteById(id);
    }

    @Transactional
    public SaleProgramResponse toggleActive(Long id) {
        SaleProgram sp = saleProgramRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chương trình sale với ID: " + id));
        sp.setIsActive(!Boolean.TRUE.equals(sp.getIsActive()));
        return toResponse(saleProgramRepository.save(sp));
    }

    // ── Tính toán giảm giá sale ────────────────────────────────────────────────

    /**
     * Tính tổng số tiền giảm từ các sale program cho một đơn hàng.
     *
     * @param lines         danh sách dòng sản phẩm (productId, unitPrice, quantity)
    * @param paymentMethod phương thức thanh toán ("CASH", "MOMO")
     * @param subtotal      tổng tiền trước giảm (để kiểm tra MIN_ORDER_AMOUNT)
     * @param totalQuantity tổng số lượng sản phẩm (để kiểm tra MIN_QUANTITY)
     * @return tổng số tiền giảm từ sale program
     */
    public BigDecimal calculateSaleDiscount(
            List<OrderLineInfo> lines,
            String paymentMethod,
            BigDecimal subtotal,
            int totalQuantity) {

        LocalDateTime now = LocalDateTime.now();
        BigDecimal totalDiscount = BigDecimal.ZERO;

        for (OrderLineInfo line : lines) {
            // Tìm các sale đang hoạt động áp dụng cho sản phẩm này
            List<SaleProgram> activeSales = saleProgramRepository.findActiveByProductId(line.productId, now);

            BigDecimal bestDiscount = BigDecimal.ZERO;

            for (SaleProgram sp : activeSales) {
                // Kiểm tra TẤT CẢ điều kiện của chương trình sale
                if (!checkAllConditions(sp, paymentMethod, subtotal, totalQuantity)) {
                    continue;
                }
                // Tính giảm giá cho dòng này
                BigDecimal discount = calcDiscountForLine(sp, line.unitPrice, line.quantity);
                if (discount.compareTo(bestDiscount) > 0) {
                    bestDiscount = discount;
                }
            }

            totalDiscount = totalDiscount.add(bestDiscount);
        }

        // Không cho giảm quá tổng tiền đơn hàng
        return totalDiscount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Kiểm tra tất cả điều kiện của một SaleProgram với context đơn hàng.
     * Tất cả điều kiện phải thỏa mãn thì mới áp dụng.
     */
    private boolean checkAllConditions(SaleProgram sp, String paymentMethod,
                                       BigDecimal subtotal, int totalQuantity) {
        for (SaleProgramCondition cond : sp.getConditions()) {
            switch (cond.getConditionType()) {
                case PAYMENT_METHOD:
                    if (!cond.getConditionValue().equalsIgnoreCase(paymentMethod)) {
                        return false;
                    }
                    break;
                case MIN_ORDER_AMOUNT:
                    try {
                        BigDecimal minAmount = new BigDecimal(cond.getConditionValue());
                        if (subtotal.compareTo(minAmount) < 0) return false;
                    } catch (NumberFormatException ignored) {
                        return false;
                    }
                    break;
                case MIN_QUANTITY:
                    try {
                        int minQty = Integer.parseInt(cond.getConditionValue());
                        if (totalQuantity < minQty) return false;
                    } catch (NumberFormatException ignored) {
                        return false;
                    }
                    break;
            }
        }
        return true;
    }

    /**
     * Tính số tiền giảm cho 1 dòng sản phẩm theo 1 sale program.
     */
    private BigDecimal calcDiscountForLine(SaleProgram sp, BigDecimal unitPrice, int quantity) {
        BigDecimal discountPerUnit;

        if (sp.getDiscountType() == DiscountType.PERCENTAGE) {
            // Giảm % trên đơn giá, có giới hạn maxDiscountAmount nếu cài
            discountPerUnit = unitPrice
                    .multiply(sp.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (sp.getMaxDiscountAmount() != null) {
                discountPerUnit = discountPerUnit.min(sp.getMaxDiscountAmount());
            }
        } else {
            // Giảm tiền cố định, không giảm quá đơn giá
            discountPerUnit = sp.getDiscountValue().min(unitPrice);
        }

        return discountPerUnit.multiply(BigDecimal.valueOf(quantity));
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private void validateRequest(SaleProgramRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("Tên chương trình sale không được để trống");
        }
        if (request.getDiscountType() == null) {
            request.setDiscountType(DiscountType.PERCENTAGE);
        }
        if (request.getDiscountValue() == null || request.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Giá trị giảm giá phải lớn hơn 0");
        }
        if (request.getDiscountType() == DiscountType.PERCENTAGE && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new RuntimeException("Phần trăm giảm giá không được vượt quá 100%");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new RuntimeException("Thời gian bắt đầu và kết thúc không được để trống");
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new RuntimeException("Thời gian bắt đầu phải trước thời gian kết thúc");
        }
    }

    private void mapRequestToEntity(SaleProgramRequest request, SaleProgram sp) {
        if (request.getDiscountType() == null) {
            request.setDiscountType(DiscountType.PERCENTAGE);
        }
        sp.setName(request.getName());
        sp.setDescription(request.getDescription());
        sp.setDiscountType(request.getDiscountType());
        sp.setDiscountValue(request.getDiscountValue());
        sp.setMaxDiscountAmount(request.getMaxDiscountAmount());
        sp.setStartDate(request.getStartDate());
        sp.setEndDate(request.getEndDate());
        sp.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        // Map sản phẩm
        Set<Product> products = new HashSet<>();
        if (request.getProductIds() != null && !request.getProductIds().isEmpty()) {
            for (Long productId : request.getProductIds()) {
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + productId));
                products.add(product);
            }
        }
        sp.setProducts(products);

        // Map điều kiện
        List<SaleProgramCondition> conditions = new ArrayList<>();
        if (request.getConditions() != null) {
            for (SaleProgramRequest.ConditionRequest cr : request.getConditions()) {
                SaleProgramCondition cond = new SaleProgramCondition();
                cond.setSaleProgram(sp);
                cond.setConditionType(cr.getConditionType());
                cond.setConditionValue(cr.getConditionValue());
                cond.setDescription(cr.getDescription());
                conditions.add(cond);
            }
        }
        sp.setConditions(conditions);
    }

    public SaleProgramResponse toResponse(SaleProgram sp) {
        SaleProgramResponse res = new SaleProgramResponse();
        res.setId(sp.getId());
        res.setName(sp.getName());
        res.setDescription(sp.getDescription());
        res.setDiscountType(sp.getDiscountType());
        res.setDiscountValue(sp.getDiscountValue());
        res.setMaxDiscountAmount(sp.getMaxDiscountAmount());
        res.setStartDate(sp.getStartDate());
        res.setEndDate(sp.getEndDate());
        res.setIsActive(sp.getIsActive());
        res.setCreatedAt(sp.getCreatedAt());
        res.setUpdatedAt(sp.getUpdatedAt());

        // Map conditions
        if (sp.getConditions() != null) {
            List<SaleProgramResponse.ConditionResponse> condList = sp.getConditions().stream().map(c -> {
                SaleProgramResponse.ConditionResponse cr = new SaleProgramResponse.ConditionResponse();
                cr.setId(c.getId());
                cr.setConditionType(c.getConditionType());
                cr.setConditionValue(c.getConditionValue());
                cr.setDescription(c.getDescription());
                return cr;
            }).collect(Collectors.toList());
            res.setConditions(condList);
        }

        // Map product IDs
        if (sp.getProducts() != null) {
            Set<Long> ids = sp.getProducts().stream().map(Product::getId).collect(Collectors.toSet());
            res.setProductIds(ids);
        }

        return res;
    }
}
