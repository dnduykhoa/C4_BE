package j2ee_backend.nhom05.service;

import j2ee_backend.nhom05.dto.VoucherRequest;
import j2ee_backend.nhom05.dto.VoucherResponse;
import j2ee_backend.nhom05.dto.VoucherValidateResponse;
import j2ee_backend.nhom05.model.*;
import j2ee_backend.nhom05.repository.IOrderRepository;
import j2ee_backend.nhom05.repository.IVoucherRepository;
import j2ee_backend.nhom05.repository.IVoucherUsageRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VoucherService {

    @Autowired
    private IVoucherRepository voucherRepository;

    @Autowired
    private IVoucherUsageRepository voucherUsageRepository;

    @Autowired
    private IOrderRepository orderRepository;

    // ── CRUD ───────────────────────────────────────────────────────────────────

    @Transactional
    public VoucherResponse create(VoucherRequest request) {
        validateRequest(request);

        if (voucherRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new RuntimeException("Mã voucher '" + request.getCode() + "' đã tồn tại");
        }

        Voucher voucher = new Voucher();
        mapRequestToEntity(request, voucher);
        return toResponse(voucherRepository.save(voucher));
    }

    @Transactional
    public VoucherResponse update(Long id, VoucherRequest request) {
        validateRequest(request);

        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher với ID: " + id));

        // Kiểm tra mã trùng (trừ chính nó)
        voucherRepository.findByCodeIgnoreCase(request.getCode()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new RuntimeException("Mã voucher '" + request.getCode() + "' đã được sử dụng");
            }
        });

        mapRequestToEntity(request, voucher);
        return toResponse(voucherRepository.save(voucher));
    }

    @Transactional(readOnly = true)
    public VoucherResponse getById(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher với ID: " + id));
        return toResponse(voucher);
    }

    @Transactional(readOnly = true)
    public List<VoucherResponse> getAll() {
        return voucherRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VoucherResponse> getActive() {
        return voucherRepository.findByIsActiveTrueOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher với ID: " + id));

        // Giữ lại lịch sử mã giảm giá trong đơn hàng qua appliedVoucherCode,
        // nhưng bỏ liên kết FK tới vouchers để có thể xoá voucher an toàn.
        orderRepository.clearAppliedVoucherReferences(id);

        try {
            voucherRepository.delete(voucher);
            voucherRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new RuntimeException("Không thể xoá voucher này do đang được tham chiếu dữ liệu khác");
        }
    }

    @Transactional
    public VoucherResponse toggleActive(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher với ID: " + id));
        voucher.setIsActive(!Boolean.TRUE.equals(voucher.getIsActive()));
        return toResponse(voucherRepository.save(voucher));
    }

    // ── Validate & Apply ───────────────────────────────────────────────────────

    /**
     * Kiểm tra voucher và tính số tiền giảm (không thực sự dùng).
     * Dùng cho endpoint kiểm tra trước khi đặt hàng.
     *
     * @param code        mã voucher
     * @param orderAmount tổng tiền đơn hàng trước giảm
     * @param userId      ID người dùng kiểm tra
     */
    @Transactional(readOnly = true)
    public VoucherValidateResponse validate(String code, BigDecimal orderAmount, Long userId) {
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code).orElse(null);
        if (voucher == null) {
            return new VoucherValidateResponse(false, "Mã voucher không tồn tại", BigDecimal.ZERO, orderAmount, code);
        }

        String error = checkVoucherEligibility(voucher, orderAmount, userId);
        if (error != null) {
            return new VoucherValidateResponse(false, error, BigDecimal.ZERO, orderAmount, code);
        }

        BigDecimal discount = calcVoucherDiscount(voucher, orderAmount);
        BigDecimal finalAmount = orderAmount.subtract(discount).max(BigDecimal.ZERO);
        return new VoucherValidateResponse(true, "Voucher hợp lệ", discount, finalAmount, code.toUpperCase());
    }

    /**
     * Tính số tiền giảm từ voucher cho một đơn hàng (dùng trong createOrder).
     * Throw RuntimeException nếu voucher không hợp lệ.
     *
     * @param code        mã voucher
     * @param orderAmount tổng tiền đơn hàng (sau khi đã trừ sale discount)
     * @param userId      ID người dùng
     * @return số tiền giảm
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(String code, BigDecimal orderAmount, Long userId) {
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new RuntimeException("Mã voucher '" + code + "' không tồn tại"));

        String error = checkVoucherEligibility(voucher, orderAmount, userId);
        if (error != null) {
            throw new RuntimeException(error);
        }

        return calcVoucherDiscount(voucher, orderAmount);
    }

    /**
     * Đánh dấu voucher đã được sử dụng bởi 1 đơn hàng.
     * Gọi sau khi Order đã được lưu thành công.
     */
    @Transactional
    public void markUsed(String code, User user, Order order) {
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher: " + code));

        VoucherUsage usage = new VoucherUsage();
        usage.setVoucher(voucher);
        usage.setUser(user);
        usage.setOrder(order);
        usage.setUsedAt(LocalDateTime.now());
        voucherUsageRepository.save(usage);

        // Tăng số lần đã dùng
        voucher.setUsedCount(voucher.getUsedCount() + 1);
        voucherRepository.save(voucher);
    }

    /**
     * Hoàn lại voucher khi đơn hàng bị huỷ.
     * MULTI_USE: giảm usedCount, xoá usage record.
     * SINGLE_USE: giảm usedCount về 0 nếu đây là lần dùng đó, xoá usage record.
     */
    @Transactional
    public void releaseVoucher(Long orderId) {
        voucherUsageRepository.findByOrderId(orderId).ifPresent(usage -> {
            Voucher voucher = usage.getVoucher();
            if (voucher.getUsedCount() != null && voucher.getUsedCount() > 0) {
                voucher.setUsedCount(voucher.getUsedCount() - 1);
                voucherRepository.save(voucher);
            }
            voucherUsageRepository.delete(usage);
        });
    }

    // ── Helper riêng ───────────────────────────────────────────────────────────

    /**
     * Kiểm tra điều kiện dùng voucher.
     * Trả về chuỗi lỗi nếu không hợp lệ, null nếu OK.
     */
    private String checkVoucherEligibility(Voucher voucher, BigDecimal orderAmount, Long userId) {
        LocalDateTime now = LocalDateTime.now();

        if (!Boolean.TRUE.equals(voucher.getIsActive())) {
            return "Voucher không còn hoạt động";
        }
        if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) {
            return "Voucher chưa đến thời gian sử dụng";
        }
        if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) {
            return "Voucher đã hết hạn sử dụng";
        }
        if (voucher.getMinOrderAmount() != null && orderAmount.compareTo(voucher.getMinOrderAmount()) < 0) {
            return "Đơn hàng tối thiểu " + voucher.getMinOrderAmount().toPlainString() + "đ để dùng voucher này";
        }

        // Kiểm tra lượt dùng
        if (voucher.getVoucherType() == VoucherType.SINGLE_USE) {
            // Chỉ dùng được 1 lần duy nhất (toàn hệ thống)
            if (voucher.getUsedCount() != null && voucher.getUsedCount() >= 1) {
                return "Voucher này đã được sử dụng";
            }
        } else {
            // MULTI_USE: kiểm tra maxUsageCount và mỗi user chỉ dùng 1 lần
            if (voucher.getMaxUsageCount() != null
                    && voucher.getUsedCount() != null
                    && voucher.getUsedCount() >= voucher.getMaxUsageCount()) {
                return "Voucher đã đạt giới hạn số lượt sử dụng";
            }
            if (userId != null && voucherUsageRepository.existsByVoucherIdAndUserId(voucher.getId(), userId)) {
                return "Bạn đã sử dụng voucher này rồi";
            }
        }

        return null;
    }

    /**
     * Tính số tiền giảm thực tế từ voucher.
     */
    private BigDecimal calcVoucherDiscount(Voucher voucher, BigDecimal orderAmount) {
        BigDecimal discount;

        if (voucher.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = orderAmount
                    .multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (voucher.getMaxDiscountAmount() != null) {
                discount = discount.min(voucher.getMaxDiscountAmount());
            }
        } else {
            // FIXED_AMOUNT
            discount = voucher.getDiscountValue().min(orderAmount);
        }

        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    private void validateRequest(VoucherRequest request) {
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new RuntimeException("Mã voucher không được để trống");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("Tên voucher không được để trống");
        }
        if (request.getDiscountType() == null) {
            request.setDiscountType(DiscountType.PERCENTAGE);
        }
        if (request.getDiscountValue() == null || request.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Giá trị giảm giá phải lớn hơn 0");
        }
        if (request.getDiscountType() == DiscountType.PERCENTAGE
                && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new RuntimeException("Phần trăm giảm giá không được vượt quá 100%");
        }
        if (request.getVoucherType() == null) {
            throw new RuntimeException("Loại voucher không được để trống");
        }
    }

    private void mapRequestToEntity(VoucherRequest request, Voucher voucher) {
        if (request.getDiscountType() == null) {
            request.setDiscountType(DiscountType.PERCENTAGE);
        }
        voucher.setCode(request.getCode().toUpperCase().trim());
        voucher.setName(request.getName());
        voucher.setDescription(request.getDescription());
        voucher.setDiscountType(request.getDiscountType());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
        voucher.setMinOrderAmount(request.getMinOrderAmount());
        voucher.setVoucherType(request.getVoucherType());
        voucher.setMaxUsageCount(request.getMaxUsageCount());
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        voucher.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        if (voucher.getUsedCount() == null) voucher.setUsedCount(0);
    }

    public VoucherResponse toResponse(Voucher voucher) {
        VoucherResponse res = new VoucherResponse();
        res.setId(voucher.getId());
        res.setCode(voucher.getCode());
        res.setName(voucher.getName());
        res.setDescription(voucher.getDescription());
        res.setDiscountType(voucher.getDiscountType());
        res.setDiscountValue(voucher.getDiscountValue());
        res.setMaxDiscountAmount(voucher.getMaxDiscountAmount());
        res.setMinOrderAmount(voucher.getMinOrderAmount());
        res.setVoucherType(voucher.getVoucherType());
        res.setMaxUsageCount(voucher.getMaxUsageCount());
        res.setUsedCount(voucher.getUsedCount());
        res.setStartDate(voucher.getStartDate());
        res.setEndDate(voucher.getEndDate());
        res.setIsActive(voucher.getIsActive());
        res.setCreatedAt(voucher.getCreatedAt());
        res.setUpdatedAt(voucher.getUpdatedAt());
        return res;
    }
}
