package j2ee_backend.nhom05.service;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import j2ee_backend.nhom05.dto.pcbuilder.PcBuildSelection;
import j2ee_backend.nhom05.dto.pcbuilder.PcBuildSelectionItem;
import j2ee_backend.nhom05.dto.pcbuilder.PcBuilderCheckoutItemDto;
import j2ee_backend.nhom05.dto.pcbuilder.PcBuilderCompatibilityDto;
import j2ee_backend.nhom05.dto.pcbuilder.PcBuilderOptionItemDto;
import j2ee_backend.nhom05.dto.pcbuilder.PcBuilderOptionsResponseDto;
import j2ee_backend.nhom05.dto.pcbuilder.PcBuilderSlotDto;
import j2ee_backend.nhom05.dto.pcbuilder.PcBuilderSummaryResponseDto;
import j2ee_backend.nhom05.dto.pcbuilder.PcBuilderVariantOptionDto;
import j2ee_backend.nhom05.dto.pcbuilder.PcBuilderWarningDto;
import j2ee_backend.nhom05.model.Category;
import j2ee_backend.nhom05.model.Product;
import j2ee_backend.nhom05.model.ProductSpecification;
import j2ee_backend.nhom05.model.ProductStatus;
import j2ee_backend.nhom05.model.ProductVariant;
import j2ee_backend.nhom05.model.ProductVariantValue;
import j2ee_backend.nhom05.repository.ICategoryRepository;
import j2ee_backend.nhom05.repository.IProductRepository;
import j2ee_backend.nhom05.repository.IProductSpecificationRepository;
import j2ee_backend.nhom05.repository.IProductVariantRepository;
import j2ee_backend.nhom05.repository.IProductVariantValueRepository;

@Service
public class PcBuilderService {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(?:[.,]\\d+)?");
    private static final int MAX_ALLOWED_QUANTITY = 8;

    private static final List<String> SLOT_ORDER = List.of("cpu", "mainboard", "ram", "gpu", "storage", "psu", "case", "cooling");

    private static final Map<String, String> SLOT_LABELS = Map.of(
            "cpu", "CPU",
            "mainboard", "Mainboard",
            "ram", "RAM",
            "gpu", "GPU",
            "storage", "Storage",
            "psu", "PSU",
            "case", "Case",
            "cooling", "Cooling");

    private static final Map<String, List<String>> SLOT_CATEGORY_KEYWORDS = Map.of(
            "cpu", List.of("cpu", "processor", "vi xu ly", "bo xu ly"),
            "mainboard", List.of("mainboard", "motherboard", "bo mach chu", "main"),
            "ram", List.of("ram", "memory"),
            "gpu", List.of("gpu", "vga", "card do hoa", "graphics card"),
            "storage", List.of("ssd", "hdd", "storage", "o cung", "nvme"),
            "psu", List.of("psu", "power supply", "nguon"),
            "case", List.of("case", "vo may"),
            "cooling", List.of("cooling", "tan nhiet", "quat", "aio"));

    @Autowired
    private ICategoryRepository categoryRepository;

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IProductSpecificationRepository productSpecificationRepository;

    @Autowired
    private IProductVariantRepository productVariantRepository;

    @Autowired
    private IProductVariantValueRepository productVariantValueRepository;

    public List<PcBuilderSlotDto> getSlots() {
        List<PcBuilderSlotDto> slots = new ArrayList<>();
        for (String slot : SLOT_ORDER) {
            long count = getProductsForSlot(slot).stream().filter(this::isSellable).count();
            slots.add(new PcBuilderSlotDto(slot, SLOT_LABELS.getOrDefault(slot, slot), count));
        }
        return slots;
    }

    public PcBuilderOptionsResponseDto getOptions(String slot, PcBuildSelection selection) {
        String normalizedSlot = normalize(slot);
        validateSlot(normalizedSlot);

        NormalizedSelection normalizedSelection = normalizeSelection(selection);
        validateSelection(normalizedSelection);

        List<Product> candidates = getProductsForSlot(normalizedSlot)
                .stream()
                .filter(this::isSellable)
                .collect(Collectors.toList());

        BuildContext context = buildContext(normalizedSelection, candidates.stream().map(Product::getId).collect(Collectors.toList()));
        Map<String, String> filters = new LinkedHashMap<>();

        if ("mainboard".equals(normalizedSlot)) {
            String cpuSocket = context.getSelectionSpecValue(normalizedSelection.cpu, "socket", "cpu_socket", "socket_cpu", "cpu-socket");
            if (!cpuSocket.isBlank()) {
                filters.put("cpu_socket", cpuSocket);
                candidates = candidates.stream()
                        .filter(product -> normalizedEquals(context.getProductSpecValue(product.getId(), "socket", "cpu_socket", "socket_cpu", "cpu-socket"), cpuSocket))
                        .collect(Collectors.toList());
            }
        }

        if ("ram".equals(normalizedSlot)) {
            String boardRamType = context.getSelectionSpecValue(normalizedSelection.mainboard, "ram_type", "memory_type", "ddr", "memory", "ram");
            if (!boardRamType.isBlank()) {
                filters.put("ram_type", boardRamType);
                candidates = candidates.stream()
                        .filter(product -> containsNormalized(context.getProductSpecValue(product.getId(), "ram_type", "memory_type", "ddr", "memory", "ram"), boardRamType))
                        .collect(Collectors.toList());
            }
        }

        if ("psu".equals(normalizedSlot)) {
            int recommended = recommendedPsuWatt(context);
            filters.put("min_watt", String.valueOf(recommended));
            candidates = candidates.stream()
                    .filter(product -> context.getProductSpecNumber(product.getId(), "watt", "psu_watt", "max_watt") >= recommended)
                    .collect(Collectors.toList());
        }

        if ("case".equals(normalizedSlot)) {
            String boardFormFactor = context.getSelectionSpecValue(normalizedSelection.mainboard, "form_factor", "mb_form_factor", "motherboard_form_factor");
            if (!boardFormFactor.isBlank()) {
                filters.put("mainboard_form_factor", boardFormFactor);
                candidates = candidates.stream()
                        .filter(product -> containsNormalized(
                                context.getProductSpecValue(product.getId(), "supported_form_factor", "form_factor", "mb_form_factor"),
                                boardFormFactor))
                        .collect(Collectors.toList());
            }
        }

        if ("gpu".equals(normalizedSlot)) {
            double caseMaxGpuLength = context.getSelectionSpecNumber(
                    normalizedSelection.pcCase,
                    "max_gpu_length_mm", "gpu_max_length_mm", "max_gpu_length",
                    "max_vga_length_mm", "vga_max_length_mm", "vga_max_length",
                    "chieu_dai_vga_toi_da", "chieu_dai_gpu_toi_da", "chieu_dai_toi_da_vga");
            if (caseMaxGpuLength > 0) {
                filters.put("max_gpu_length_mm", String.valueOf((int) caseMaxGpuLength));
                candidates = candidates.stream()
                        .filter(product -> {
                            double gpuLength = context.getProductSpecNumber(
                                    product.getId(),
                                    "gpu_length_mm", "length_mm", "gpu_length", "card_length_mm",
                                    "kich_thuoc", "kich_thuoc_gpu", "dimensions", "size", "chieu_dai");
                            return gpuLength <= 0 || gpuLength <= caseMaxGpuLength;
                        })
                        .collect(Collectors.toList());
            }
        }

        List<PcBuilderOptionItemDto> options = candidates.stream()
                .map(product -> {
                    PcBuilderCompatibilityDto compatibility = evaluateOptionCompatibility(normalizedSlot, product, normalizedSelection, context);
                    return toOptionItem(normalizedSlot, product, null, context, compatibility, true);
                })
                .collect(Collectors.toList());

        return new PcBuilderOptionsResponseDto(
                normalizedSlot,
                estimatePower(context),
                recommendedPsuWatt(context),
                filters,
                options);
    }

    public PcBuilderSummaryResponseDto getSummary(PcBuildSelection selection) {
        NormalizedSelection normalizedSelection = normalizeSelection(selection);
        validateSelection(normalizedSelection);

        BuildContext context = buildContext(normalizedSelection, Collections.emptyList());
        List<SelectionLine> selectedLines = normalizedSelection.toOrderedLines();

        List<PcBuilderOptionItemDto> selectedParts = selectedLines.stream()
                .map(line -> {
                    Product product = context.productsById.get(line.productId);
                    if (product == null) {
                        return null;
                    }
                    PcBuildSelectionItem selectionItem = new PcBuildSelectionItem(line.productId, line.variantId, line.quantity);
                    return toOptionItem(line.slotKey, product, selectionItem, context,
                            new PcBuilderCompatibilityDto("COMPATIBLE", List.of()),
                            false);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        BigDecimal totalPrice = selectedParts.stream()
                .map(PcBuilderOptionItemDto::getLineTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int estimatedPower = estimatePower(context);
        int recommendedPsu = recommendedPsuWatt(context);

        List<PcBuilderWarningDto> warnings = evaluateWarnings(normalizedSelection, context, recommendedPsu);
        boolean compatible = warnings.stream().noneMatch(w -> "ERROR".equalsIgnoreCase(w.getSeverity()));

        return new PcBuilderSummaryResponseDto(
                selectedParts,
                totalPrice,
                estimatedPower,
                recommendedPsu,
                compatible,
                warnings);
    }

    public List<PcBuilderCheckoutItemDto> getCheckoutPreview(PcBuildSelection selection) {
        NormalizedSelection normalizedSelection = normalizeSelection(selection);
        validateSelection(normalizedSelection);

        return normalizedSelection.toOrderedLines().stream()
                .map(line -> new PcBuilderCheckoutItemDto(line.productId, line.variantId, line.quantity))
                .collect(Collectors.toList());
    }

    private List<PcBuilderWarningDto> evaluateWarnings(NormalizedSelection selection, BuildContext context, int recommendedPsu) {
        List<PcBuilderWarningDto> warnings = new ArrayList<>();

        String cpuSocket = context.getSelectionSpecValue(selection.cpu, "socket", "cpu_socket");
        String boardSocket = context.getSelectionSpecValue(selection.mainboard, "socket", "cpu_socket");
        if (!cpuSocket.isBlank() && !boardSocket.isBlank() && !normalizedEquals(cpuSocket, boardSocket)) {
            warnings.add(new PcBuilderWarningDto(
                    "ERROR",
                    "CPU_MAINBOARD_SOCKET_MISMATCH",
                    "CPU socket và mainboard socket không tương thích."));
        }

        String boardRamType = context.getSelectionSpecValue(selection.mainboard, "ram_type", "memory_type", "ddr");
        for (PcBuildSelectionItem ramSelection : selection.ramSelections) {
            String ramType = context.getSelectionSpecValue(ramSelection, "ram_type", "memory_type", "ddr");
            if (!boardRamType.isBlank() && !ramType.isBlank() && !containsNormalized(ramType, boardRamType)) {
                warnings.add(new PcBuilderWarningDto(
                        "ERROR",
                        "MAINBOARD_RAM_TYPE_MISMATCH",
                        "RAM type không khớp với mainboard."));
                break;
            }
        }

        int totalRamQuantity = selection.ramSelections.stream().map(PcBuildSelectionItem::getQuantity).reduce(0, Integer::sum);
        int mainboardRamSlots = (int) context.getSelectionSpecNumber(selection.mainboard,
                "max_ram_slots", "ram_slots", "memory_slots", "max_memory_slots");
        if (mainboardRamSlots > 0 && totalRamQuantity > mainboardRamSlots) {
            warnings.add(new PcBuilderWarningDto(
                    "WARNING",
                    "RAM_QUANTITY_EXCEEDS_MAINBOARD_SLOTS",
                    "Tổng số thanh RAM vượt quá số khe RAM tối đa của mainboard."));
        }

        if (hasMixedRamSpec(selection.ramSelections, context)) {
            warnings.add(new PcBuilderWarningDto(
                    "WARNING",
                    "RAM_MIXED_SPEC_WARNING",
                    "Đang trộn nhiều cấu hình RAM khác nhau, có thể giảm độ ổn định/hiệu năng."));
        }

        validateVariantWarnings(selection.toOrderedLines(), context, warnings);

        double psuWatt = context.getSelectionSpecNumber(selection.psu, "watt", "psu_watt", "max_watt");
        if (selection.psu != null && selection.psu.getProductId() != null && psuWatt > 0 && psuWatt < recommendedPsu) {
            warnings.add(new PcBuilderWarningDto(
                    "WARNING",
                    "PSU_INSUFFICIENT_POWER",
                    "PSU có thể không đủ công suất. Nên dùng từ " + recommendedPsu + "W trở lên."));
        }

        String boardFormFactor = context.getSelectionSpecValue(selection.mainboard, "form_factor", "mb_form_factor", "motherboard_form_factor");
        String caseSupportedFormFactor = context.getSelectionSpecValue(selection.pcCase, "supported_form_factor", "form_factor", "mb_form_factor");
        if (!boardFormFactor.isBlank() && !caseSupportedFormFactor.isBlank()
                && !containsNormalized(caseSupportedFormFactor, boardFormFactor)) {
            warnings.add(new PcBuilderWarningDto(
                    "WARNING",
                    "CASE_MAINBOARD_FORM_FACTOR_MISMATCH",
                    "Case có thể không hỗ trợ form factor của mainboard."));
        }

        double caseMaxGpuLength = context.getSelectionSpecNumber(
                selection.pcCase,
                "max_gpu_length_mm", "gpu_max_length_mm", "max_gpu_length",
                "max_vga_length_mm", "vga_max_length_mm", "vga_max_length",
                "chieu_dai_vga_toi_da", "chieu_dai_gpu_toi_da", "chieu_dai_toi_da_vga");
        double gpuLength = context.getSelectionSpecNumber(
                selection.gpu,
                "gpu_length_mm", "length_mm", "gpu_length", "card_length_mm",
                "kich_thuoc", "kich_thuoc_gpu", "dimensions", "size", "chieu_dai");
        if (caseMaxGpuLength > 0 && gpuLength > 0 && gpuLength > caseMaxGpuLength) {
            warnings.add(new PcBuilderWarningDto(
                    "WARNING",
                    "CASE_GPU_LENGTH_MISMATCH",
                    "Case không đủ không gian cho chiều dài GPU đã chọn."));
        }

        if (selection.cpu == null || selection.cpu.getProductId() == null
                || selection.mainboard == null || selection.mainboard.getProductId() == null
                || selection.ramSelections.isEmpty()
                || selection.psu == null || selection.psu.getProductId() == null) {
            warnings.add(new PcBuilderWarningDto(
                    "INFO",
                    "INCOMPLETE_BUILD",
                    "Build chưa đầy đủ, nên chọn đủ CPU/Mainboard/RAM/PSU để đánh giá chính xác."));
        }

        return warnings;
    }

    private void validateVariantWarnings(List<SelectionLine> lines, BuildContext context, List<PcBuilderWarningDto> warnings) {
        for (SelectionLine line : lines) {
            if (line.variantId == null) {
                int stock = context.resolveStock(new PcBuildSelectionItem(line.productId, null, line.quantity));
                if (stock > 0 && line.quantity > stock) {
                    warnings.add(new PcBuilderWarningDto(
                            "WARNING",
                            "VARIANT_OUT_OF_STOCK",
                            "Sản phẩm " + line.slotKey + " không đủ tồn kho cho số lượng đã chọn."));
                }
                continue;
            }

            ProductVariant variant = context.variantsById.get(line.variantId);
            if (variant == null || !context.isVariantBelongsToProduct(line.variantId, line.productId)) {
                warnings.add(new PcBuilderWarningDto(
                        "ERROR",
                        "VARIANT_NOT_FOUND",
                        "Variant đã chọn không tồn tại hoặc không thuộc sản phẩm."));
                continue;
            }

            if (!Boolean.TRUE.equals(variant.getIsActive())) {
                warnings.add(new PcBuilderWarningDto(
                        "ERROR",
                        "VARIANT_INACTIVE",
                        "Variant đã chọn hiện không hoạt động."));
            }

            int stock = variant.getStockQuantity() != null ? variant.getStockQuantity() : 0;
            if (stock <= 0 || line.quantity > stock) {
                warnings.add(new PcBuilderWarningDto(
                        "WARNING",
                        "ram".equals(line.slotKey) ? "RAM_VARIANT_OUT_OF_STOCK" : "VARIANT_OUT_OF_STOCK",
                        "Variant đã chọn không đủ tồn kho cho số lượng yêu cầu."));
            }
        }
    }

    private boolean hasMixedRamSpec(List<PcBuildSelectionItem> ramSelections, BuildContext context) {
        if (ramSelections.size() <= 1) {
            return false;
        }

        Set<String> ramTypes = ramSelections.stream()
                .map(item -> normalizeToken(context.getSelectionSpecValue(item, "ram_type", "memory_type", "ddr")))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());

        Set<Integer> buses = ramSelections.stream()
                .map(item -> (int) Math.round(context.getSelectionSpecNumber(item, "bus", "ram_bus", "memory_bus", "speed_mhz")))
                .filter(value -> value > 0)
                .collect(Collectors.toSet());

        Set<Integer> capacities = ramSelections.stream()
                .map(item -> (int) Math.round(context.getSelectionSpecNumber(item, "capacity_gb", "ram_capacity", "memory_capacity", "capacity")))
                .filter(value -> value > 0)
                .collect(Collectors.toSet());

        return ramTypes.size() > 1 || buses.size() > 1 || capacities.size() > 1;
    }

    private PcBuilderOptionItemDto toOptionItem(
            String slotKey,
            Product product,
            PcBuildSelectionItem selectionItem,
            BuildContext context,
            PcBuilderCompatibilityDto compatibility,
            boolean includeVariants) {

        Long selectedVariantId = selectionItem != null ? selectionItem.getVariantId() : null;
        ProductVariant selectedVariant = selectedVariantId != null ? context.variantsById.get(selectedVariantId) : null;
        if (selectedVariant != null && !context.isVariantBelongsToProduct(selectedVariantId, product.getId())) {
            selectedVariant = null;
            selectedVariantId = null;
        }

        int quantity = selectionItem != null && selectionItem.getQuantity() != null ? selectionItem.getQuantity() : 1;
        if (quantity < 1) {
            quantity = 1;
        }

        BigDecimal unitPrice = context.resolveUnitPrice(new PcBuildSelectionItem(product.getId(), selectedVariantId, quantity));
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        Integer stockQuantity = context.resolveStock(new PcBuildSelectionItem(product.getId(), selectedVariantId, quantity));

        List<PcBuilderVariantOptionDto> variantOptions = includeVariants ? buildVariantOptions(product.getId(), context) : null;
        Long defaultVariantId = resolveDefaultVariantId(variantOptions);

        Map<String, String> keySpecs = buildProductKeySpecs(product, context);
        if (selectedVariant != null) {
            Map<String, String> selectedVariantSpecs = buildVariantKeySpecs(selectedVariant.getId(), context);
            if (!selectedVariantSpecs.isEmpty()) {
                keySpecs.putAll(selectedVariantSpecs);
            }
        }

        PcBuilderOptionItemDto dto = new PcBuilderOptionItemDto();
        dto.setSlotKey(slotKey);
        dto.setProductId(product.getId());
        dto.setVariantId(selectedVariantId);
        dto.setQuantity(quantity);
        dto.setName(product.getName());
        dto.setVariantLabel(selectedVariant != null ? resolveVariantLabel(selectedVariant, context) : null);
        dto.setUnitPrice(unitPrice);
        dto.setLineTotal(lineTotal);
        dto.setPrice(unitPrice);
        dto.setStockQuantity(stockQuantity);
        dto.setBrandName(product.getBrand() != null ? product.getBrand().getName() : "");
        dto.setCategoryName(product.getCategory() != null ? product.getCategory().getName() : "");
        dto.setHasVariants(variantOptions != null && !variantOptions.isEmpty());
        dto.setDefaultVariantId(defaultVariantId);
        dto.setAvailableVariants(variantOptions);
        dto.setCompatibility(compatibility);
        dto.setKeySpecs(keySpecs);
        return dto;
    }

    private Map<String, String> buildProductKeySpecs(Product product, BuildContext context) {
        Map<String, String> keySpecs = new LinkedHashMap<>();
        putSpecIfPresent(keySpecs, "socket", context.getProductSpecValue(product.getId(), "socket", "cpu_socket"));
        putSpecIfPresent(keySpecs, "ram_type", context.getProductSpecValue(product.getId(), "ram_type", "memory_type", "ddr"));
        putSpecIfPresent(keySpecs, "form_factor", context.getProductSpecValue(product.getId(), "form_factor", "mb_form_factor", "motherboard_form_factor"));

        double watt = context.getProductSpecNumber(product.getId(), "watt", "psu_watt", "max_watt", "tdp", "power_draw");
        if (watt > 0) {
            keySpecs.put("watt", String.valueOf((int) watt));
        }
        return keySpecs;
    }

    private Map<String, String> buildVariantKeySpecs(Long variantId, BuildContext context) {
        Map<String, String> result = new LinkedHashMap<>();
        List<ProductVariantValue> values = context.valueListByVariant.getOrDefault(variantId, List.of());
        for (ProductVariantValue value : values) {
            String key = resolveVariantValueKey(value);
            if (key.isBlank()) {
                continue;
            }
            String display = resolveVariantValueDisplay(value);
            if (!display.isBlank()) {
                result.put(key, display);
            }
        }
        return result;
    }

    private String resolveVariantValueDisplay(ProductVariantValue value) {
        if (value.getAttrValue() != null && !value.getAttrValue().isBlank()) {
            return value.getAttrValue();
        }
        if (value.getValueNumber() != null) {
            return value.getValueNumber().stripTrailingZeros().toPlainString();
        }
        return "";
    }

    private List<PcBuilderVariantOptionDto> buildVariantOptions(Long productId, BuildContext context) {
        List<ProductVariant> variants = context.variantsByProductId.getOrDefault(productId, List.of());
        List<PcBuilderVariantOptionDto> result = new ArrayList<>();

        Product parent = context.productsById.get(productId);
        if (parent != null) {
            result.add(new PcBuilderVariantOptionDto(
                    null,
                    "San pham mac dinh",
                    parent.getPrice(),
                    parent.getStockQuantity(),
                    Map.of()));
        }

        if (variants.isEmpty()) {
            return result;
        }

        for (ProductVariant variant : variants) {
            if (!Boolean.TRUE.equals(variant.getIsActive())) {
                continue;
            }
            result.add(new PcBuilderVariantOptionDto(
                    variant.getId(),
                    resolveVariantLabel(variant, context),
                    variant.getPrice(),
                    variant.getStockQuantity(),
                    buildVariantKeySpecs(variant.getId(), context)));
        }
        return result;
    }

    private Long resolveDefaultVariantId(List<PcBuilderVariantOptionDto> variantOptions) {
        if (variantOptions == null || variantOptions.isEmpty()) {
            return null;
        }
        for (PcBuilderVariantOptionDto option : variantOptions) {
            if (option.getStockQuantity() != null && option.getStockQuantity() > 0) {
                return option.getVariantId();
            }
        }
        return variantOptions.get(0).getVariantId();
    }

    private String resolveVariantLabel(ProductVariant variant, BuildContext context) {
        if (variant.getSku() != null && !variant.getSku().isBlank()) {
            return variant.getSku().trim();
        }
        List<ProductVariantValue> values = context.valueListByVariant.getOrDefault(variant.getId(), List.of());
        if (values.isEmpty()) {
            return "Variant #" + variant.getId();
        }
        return values.stream()
                .map(value -> {
                    String key = resolveVariantValueKey(value);
                    String display = resolveVariantValueDisplay(value);
                    return key.isBlank() ? display : key + ": " + display;
                })
                .collect(Collectors.joining(" / "));
    }

    private String resolveVariantValueKey(ProductVariantValue value) {
        if (value.getAttributeDefinition() != null && value.getAttributeDefinition().getAttrKey() != null) {
            return value.getAttributeDefinition().getAttrKey();
        }
        return value.getAttrKey() != null ? value.getAttrKey() : "";
    }

    private void putSpecIfPresent(Map<String, String> target, String key, String value) {
        if (!value.isBlank()) {
            target.put(key, value);
        }
    }

    private int estimatePower(BuildContext context) {
        double total = 0;
        for (SelectionLine line : context.selection.toOrderedLines()) {
            if (!isPowerConsumerSlot(line.slotKey)) {
                continue;
            }
            PcBuildSelectionItem item = new PcBuildSelectionItem(line.productId, line.variantId, line.quantity);
            double componentPower = context.getSelectionSpecNumber(item, "tdp", "power_draw", "watt", "psu_watt", "max_watt");
            if (componentPower > 0) {
                total += (componentPower * line.quantity);
            }
        }
        return (int) Math.ceil(total);
    }

    private boolean isPowerConsumerSlot(String slotKey) {
        return Set.of("cpu", "mainboard", "ram", "gpu", "storage", "cooling").contains(slotKey);
    }

    private int recommendedPsuWatt(BuildContext context) {
        int estimated = estimatePower(context);
        if (estimated <= 0) {
            return 0;
        }
        int withHeadroom = (int) Math.ceil(estimated * 1.25);
        return (int) (Math.ceil(withHeadroom / 50.0) * 50);
    }

    private PcBuilderCompatibilityDto evaluateOptionCompatibility(
            String slot,
            Product candidate,
            NormalizedSelection selection,
            BuildContext context) {

        List<String> reasons = new ArrayList<>();
        String status = "COMPATIBLE";

        if ("mainboard".equals(slot)) {
            String cpuSocket = context.getSelectionSpecValue(selection.cpu, "socket", "cpu_socket", "socket_cpu", "cpu-socket");
            String boardSocket = context.getProductSpecValue(candidate.getId(), "socket", "cpu_socket", "socket_cpu", "cpu-socket");
            if (!cpuSocket.isBlank() && !boardSocket.isBlank() && !normalizedEquals(cpuSocket, boardSocket)) {
                status = "INCOMPATIBLE";
                reasons.add("Socket không khớp với CPU đang chọn");
            }
        }

        if ("ram".equals(slot)) {
            String boardRamType = context.getSelectionSpecValue(selection.mainboard, "ram_type", "memory_type", "ddr", "memory", "ram");
            String ramType = context.getProductSpecValue(candidate.getId(), "ram_type", "memory_type", "ddr", "memory", "ram");
            if (!boardRamType.isBlank() && !ramType.isBlank() && !containsNormalized(ramType, boardRamType)) {
                status = "INCOMPATIBLE";
                reasons.add("RAM type không khớp với mainboard đang chọn");
            }
        }

        if ("psu".equals(slot)) {
            int recommended = recommendedPsuWatt(context);
            double psuWatt = context.getProductSpecNumber(candidate.getId(), "watt", "psu_watt", "max_watt");
            if (recommended > 0 && psuWatt > 0 && psuWatt < recommended) {
                status = "WARNING";
                reasons.add("Công suất PSU thấp hơn mức khuyến nghị");
            }
        }

        if ("case".equals(slot)) {
            String boardFormFactor = context.getSelectionSpecValue(selection.mainboard, "form_factor", "mb_form_factor", "motherboard_form_factor");
            String caseSupported = context.getProductSpecValue(candidate.getId(), "supported_form_factor", "form_factor", "mb_form_factor");
            if (!boardFormFactor.isBlank() && !caseSupported.isBlank() && !containsNormalized(caseSupported, boardFormFactor)) {
                if (!"INCOMPATIBLE".equals(status)) {
                    status = "WARNING";
                }
                reasons.add("Case có thể không hỗ trợ form factor mainboard");
            }
        }

        if ("gpu".equals(slot) && selection.pcCase != null && selection.pcCase.getProductId() != null) {
            double caseMaxGpuLength = context.getSelectionSpecNumber(
                    selection.pcCase,
                    "max_gpu_length_mm", "gpu_max_length_mm", "max_gpu_length",
                    "max_vga_length_mm", "vga_max_length_mm", "vga_max_length",
                    "chieu_dai_vga_toi_da", "chieu_dai_gpu_toi_da", "chieu_dai_toi_da_vga");
            double gpuLength = context.getProductSpecNumber(
                    candidate.getId(),
                    "gpu_length_mm", "length_mm", "gpu_length", "card_length_mm",
                    "kich_thuoc", "kich_thuoc_gpu", "dimensions", "size", "chieu_dai");
            if (caseMaxGpuLength > 0 && gpuLength > 0 && gpuLength > caseMaxGpuLength) {
                if (!"INCOMPATIBLE".equals(status)) {
                    status = "WARNING";
                }
                reasons.add("GPU có thể quá dài so với case hiện tại");
            }
        }

        return new PcBuilderCompatibilityDto(status, reasons);
    }

    private void validateSlot(String slot) {
        if (!SLOT_ORDER.contains(slot)) {
            throw new RuntimeException("Slot không hợp lệ: " + slot);
        }
    }

    private void validateSelection(NormalizedSelection selection) {
        for (SelectionLine line : selection.toOrderedLines()) {
            if (line.quantity < 1) {
                throw new RuntimeException("Quantity phải >= 1");
            }
            if (line.quantity > MAX_ALLOWED_QUANTITY) {
                throw new RuntimeException("Quantity vượt giới hạn tối đa " + MAX_ALLOWED_QUANTITY);
            }
        }
    }

    private List<Product> getProductsForSlot(String slot) {
        Set<Long> categoryIds = resolveSlotCategoryIds(slot);
        if (categoryIds.isEmpty()) {
            return Collections.emptyList();
        }
        return productRepository.findByCategoryIdIn(new ArrayList<>(categoryIds));
    }

    private Set<Long> resolveSlotCategoryIds(String slot) {
        List<String> keywords = SLOT_CATEGORY_KEYWORDS.getOrDefault(slot, List.of());
        if (keywords.isEmpty()) {
            return Collections.emptySet();
        }

        List<Category> allCategories = categoryRepository.findAll();
        Map<Long, List<Category>> childrenByParent = allCategories.stream()
                .filter(category -> category.getParent() != null)
                .collect(Collectors.groupingBy(category -> category.getParent().getId()));

        Set<Long> roots = allCategories.stream()
                .filter(category -> matchesAnyKeyword(category.getName(), keywords))
                .map(Category::getId)
                .collect(Collectors.toSet());

        if (roots.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Long> result = new java.util.HashSet<>();
        Deque<Long> stack = new ArrayDeque<>(roots);
        while (!stack.isEmpty()) {
            Long current = stack.pop();
            if (!result.add(current)) {
                continue;
            }
            for (Category child : childrenByParent.getOrDefault(current, List.of())) {
                stack.push(child.getId());
            }
        }
        return result;
    }

    private boolean matchesAnyKeyword(String text, List<String> keywords) {
        String normalizedText = normalize(text);
        return keywords.stream().anyMatch(keyword -> normalizedText.contains(normalize(keyword)));
    }

    private boolean isSellable(Product product) {
        if (product == null) {
            return false;
        }
        ProductStatus status = product.getStatus();
        if (status == ProductStatus.INACTIVE || status == ProductStatus.OUT_OF_STOCK) {
            return false;
        }
        return product.getStockQuantity() != null && product.getStockQuantity() > 0;
    }

    private boolean normalizedEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return normalizeToken(left).equals(normalizeToken(right));
    }

    private boolean containsNormalized(String text, String probe) {
        if (text == null || probe == null) {
            return false;
        }
        String normalizedText = normalizeToken(text);
        String normalizedProbe = normalizeToken(probe);
        return normalizedText.contains(normalizedProbe) || normalizedProbe.contains(normalizedText);
    }

    private String normalizeToken(String value) {
        return normalize(value).replace("-", "").replace("_", "").replace(" ", "");
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private NormalizedSelection normalizeSelection(PcBuildSelection selection) {
        PcBuildSelection safeSelection = selection != null ? selection : new PcBuildSelection();

        return new NormalizedSelection(
                sanitizeItem(safeSelection.resolveCpuSelection()),
                sanitizeItem(safeSelection.resolveMainboardSelection()),
                sanitizeRamItems(safeSelection.resolveRamSelections()),
                sanitizeItem(safeSelection.resolveGpuSelection()),
                sanitizeItem(safeSelection.resolveStorageSelection()),
                sanitizeItem(safeSelection.resolvePsuSelection()),
                sanitizeItem(safeSelection.resolveCaseSelection()),
                sanitizeItem(safeSelection.resolveCoolingSelection()));
    }

    private PcBuildSelectionItem sanitizeItem(PcBuildSelectionItem item) {
        if (item == null) {
            return null;
        }

        Long productId = item.getProductId();
        Long variantId = item.getVariantId();
        if (variantId != null && variantId <= 0) {
            variantId = null;
        }

        if (productId == null && variantId != null) {
            ProductVariant variant = productVariantRepository.findById(variantId).orElse(null);
            if (variant != null && variant.getProduct() != null) {
                productId = variant.getProduct().getId();
            }
        }

        if (productId == null) {
            return null;
        }

        int quantity = item.getQuantity() != null ? item.getQuantity() : 1;
        if (quantity < 1) {
            quantity = 1;
        }
        return new PcBuildSelectionItem(productId, variantId, quantity);
    }

    private List<PcBuildSelectionItem> sanitizeRamItems(List<PcBuildSelectionItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<PcBuildSelectionItem> result = new ArrayList<>();
        for (PcBuildSelectionItem item : items) {
            PcBuildSelectionItem sanitized = sanitizeItem(item);
            if (sanitized != null) {
                result.add(sanitized);
            }
        }
        return result;
    }

    private BuildContext buildContext(NormalizedSelection selection, List<Long> extraProductIds) {
        List<Long> selectedProductIds = selection.selectedProductIds();
        List<Long> allProductIds = new ArrayList<>(selectedProductIds);
        if (extraProductIds != null && !extraProductIds.isEmpty()) {
            allProductIds.addAll(extraProductIds);
        }
        allProductIds = allProductIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());

        Map<Long, Product> productsById = allProductIds.isEmpty()
                ? Collections.emptyMap()
                : productRepository.findAllById(allProductIds).stream()
                        .collect(Collectors.toMap(Product::getId, product -> product));

        List<ProductSpecification> specs = allProductIds.isEmpty()
                ? Collections.emptyList()
                : productSpecificationRepository.findByProductIdInOrderByDisplayOrderAsc(allProductIds);

        Map<Long, Map<String, ProductSpecification>> specsByProduct = new HashMap<>();
        Map<Long, List<ProductSpecification>> specListByProduct = new HashMap<>();
        for (ProductSpecification spec : specs) {
            if (spec.getProduct() == null || spec.getProduct().getId() == null) {
                continue;
            }
            Long productId = spec.getProduct().getId();
            specListByProduct.computeIfAbsent(productId, ignored -> new ArrayList<>()).add(spec);

            String key = normalize(resolveSpecKey(spec));
            if (key.isBlank()) {
                continue;
            }
            specsByProduct
                    .computeIfAbsent(productId, ignored -> new HashMap<>())
                    .putIfAbsent(key, spec);
        }

        List<ProductVariant> variants = allProductIds.isEmpty()
                ? Collections.emptyList()
                : productVariantRepository.findByProductIdIn(allProductIds);

        Map<Long, ProductVariant> variantsById = variants.stream().collect(Collectors.toMap(ProductVariant::getId, variant -> variant));
        Map<Long, List<ProductVariant>> variantsByProductId = variants.stream()
                .collect(Collectors.groupingBy(variant -> variant.getProduct().getId()));
        variantsByProductId.values().forEach(list -> list.sort((left, right) -> {
            int leftOrder = left.getDisplayOrder() != null ? left.getDisplayOrder() : 0;
            int rightOrder = right.getDisplayOrder() != null ? right.getDisplayOrder() : 0;
            return Integer.compare(leftOrder, rightOrder);
        }));

        List<Long> variantIds = variants.stream().map(ProductVariant::getId).collect(Collectors.toList());
        List<ProductVariantValue> variantValues = variantIds.isEmpty()
                ? Collections.emptyList()
                : productVariantValueRepository.findByVariantIdInOrderByDisplayOrderAsc(variantIds);

        Map<Long, Map<String, ProductVariantValue>> valuesByVariant = new HashMap<>();
        Map<Long, List<ProductVariantValue>> valueListByVariant = new HashMap<>();
        for (ProductVariantValue value : variantValues) {
            if (value.getVariant() == null || value.getVariant().getId() == null) {
                continue;
            }
            Long variantId = value.getVariant().getId();
            valueListByVariant.computeIfAbsent(variantId, ignored -> new ArrayList<>()).add(value);

            String key = normalize(resolveVariantValueKey(value));
            if (key.isBlank()) {
                continue;
            }
            valuesByVariant
                    .computeIfAbsent(variantId, ignored -> new HashMap<>())
                    .putIfAbsent(key, value);
        }

        return new BuildContext(selection, productsById, variantsById, variantsByProductId,
                specsByProduct, specListByProduct, valuesByVariant, valueListByVariant);
    }

    private String resolveSpecKey(ProductSpecification spec) {
        if (spec.getAttributeDefinition() != null && spec.getAttributeDefinition().getAttrKey() != null) {
            return spec.getAttributeDefinition().getAttrKey();
        }
        return spec.getSpecKey() != null ? spec.getSpecKey() : "";
    }

    private double parseNumber(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(raw.replace(',', '.'));
        if (!matcher.find()) {
            return 0;
        }
        try {
            return Double.parseDouble(matcher.group());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private class BuildContext {
        private final NormalizedSelection selection;
        private final Map<Long, Product> productsById;
        private final Map<Long, ProductVariant> variantsById;
        private final Map<Long, List<ProductVariant>> variantsByProductId;
        private final Map<Long, Map<String, ProductSpecification>> specsByProduct;
        private final Map<Long, List<ProductSpecification>> specListByProduct;
        private final Map<Long, Map<String, ProductVariantValue>> valuesByVariant;
        private final Map<Long, List<ProductVariantValue>> valueListByVariant;

        private BuildContext(
                NormalizedSelection selection,
                Map<Long, Product> productsById,
                Map<Long, ProductVariant> variantsById,
                Map<Long, List<ProductVariant>> variantsByProductId,
                Map<Long, Map<String, ProductSpecification>> specsByProduct,
                Map<Long, List<ProductSpecification>> specListByProduct,
                Map<Long, Map<String, ProductVariantValue>> valuesByVariant,
                Map<Long, List<ProductVariantValue>> valueListByVariant) {
            this.selection = selection;
            this.productsById = productsById;
            this.variantsById = variantsById;
            this.variantsByProductId = variantsByProductId;
            this.specsByProduct = specsByProduct;
            this.specListByProduct = specListByProduct;
            this.valuesByVariant = valuesByVariant;
            this.valueListByVariant = valueListByVariant;
        }

        private boolean isVariantBelongsToProduct(Long variantId, Long productId) {
            if (variantId == null || productId == null) {
                return false;
            }
            ProductVariant variant = variantsById.get(variantId);
            return variant != null && variant.getProduct() != null && Objects.equals(variant.getProduct().getId(), productId);
        }

        private String getSelectionSpecValue(PcBuildSelectionItem item, String... keys) {
            if (item == null || item.getProductId() == null) {
                return "";
            }

            if (item.getVariantId() != null && isVariantBelongsToProduct(item.getVariantId(), item.getProductId())) {
                String variantValue = getVariantValue(item.getVariantId(), keys);
                if (!variantValue.isBlank()) {
                    return variantValue;
                }
            }

            return getProductSpecValue(item.getProductId(), keys);
        }

        private double getSelectionSpecNumber(PcBuildSelectionItem item, String... keys) {
            if (item == null || item.getProductId() == null) {
                return 0;
            }

            if (item.getVariantId() != null && isVariantBelongsToProduct(item.getVariantId(), item.getProductId())) {
                double variantNumber = getVariantNumber(item.getVariantId(), keys);
                if (variantNumber > 0) {
                    return variantNumber;
                }
            }

            return getProductSpecNumber(item.getProductId(), keys);
        }

        private BigDecimal resolveUnitPrice(PcBuildSelectionItem item) {
            if (item == null || item.getProductId() == null) {
                return BigDecimal.ZERO;
            }

            if (item.getVariantId() != null && isVariantBelongsToProduct(item.getVariantId(), item.getProductId())) {
                ProductVariant variant = variantsById.get(item.getVariantId());
                if (variant != null && variant.getPrice() != null) {
                    return variant.getPrice();
                }
            }

            Product product = productsById.get(item.getProductId());
            if (product == null || product.getPrice() == null) {
                return BigDecimal.ZERO;
            }
            return product.getPrice();
        }

        private int resolveStock(PcBuildSelectionItem item) {
            if (item == null || item.getProductId() == null) {
                return 0;
            }

            if (item.getVariantId() != null && isVariantBelongsToProduct(item.getVariantId(), item.getProductId())) {
                ProductVariant variant = variantsById.get(item.getVariantId());
                Integer variantStock = variant != null ? variant.getStockQuantity() : 0;
                return variantStock != null ? variantStock : 0;
            }

            Product product = productsById.get(item.getProductId());
            Integer stock = product != null ? product.getStockQuantity() : 0;
            return stock != null ? stock : 0;
        }

        private String getProductSpecValue(Long productId, String... keys) {
            ProductSpecification spec = findProductSpec(productId, keys);
            if (spec == null) {
                return "";
            }
            if (spec.getSpecValue() != null && !spec.getSpecValue().isBlank()) {
                return spec.getSpecValue();
            }
            if (spec.getValueNumber() != null) {
                return spec.getValueNumber().stripTrailingZeros().toPlainString();
            }
            return "";
        }

        private double getProductSpecNumber(Long productId, String... keys) {
            ProductSpecification spec = findProductSpec(productId, keys);
            if (spec == null) {
                return 0;
            }
            if (spec.getValueNumber() != null) {
                return spec.getValueNumber().doubleValue();
            }
            return parseNumber(spec.getSpecValue());
        }

        private String getVariantValue(Long variantId, String... keys) {
            ProductVariantValue value = findVariantValue(variantId, keys);
            if (value == null) {
                return "";
            }
            String display = resolveVariantValueDisplay(value);
            return display != null ? display : "";
        }

        private double getVariantNumber(Long variantId, String... keys) {
            ProductVariantValue value = findVariantValue(variantId, keys);
            if (value == null) {
                return 0;
            }
            if (value.getValueNumber() != null) {
                return value.getValueNumber().doubleValue();
            }
            return parseNumber(value.getAttrValue());
        }

        private ProductSpecification findProductSpec(Long productId, String... keys) {
            if (productId == null || keys == null || keys.length == 0) {
                return null;
            }
            Map<String, ProductSpecification> specs = specsByProduct.get(productId);
            if (specs == null || specs.isEmpty()) {
                return null;
            }
            for (String key : keys) {
                ProductSpecification matched = specs.get(normalize(key));
                if (matched != null) {
                    return matched;
                }
            }

            List<ProductSpecification> specList = specListByProduct.getOrDefault(productId, List.of());
            for (String key : keys) {
                String probe = normalizeToken(key);
                if (probe.isBlank()) {
                    continue;
                }
                for (ProductSpecification spec : specList) {
                    String specKey = normalizeToken(resolveSpecKey(spec));
                    if (specKey.isBlank()) {
                        continue;
                    }
                    if (specKey.equals(probe) || specKey.contains(probe)) {
                        return spec;
                    }
                }
            }
            return null;
        }

        private ProductVariantValue findVariantValue(Long variantId, String... keys) {
            if (variantId == null || keys == null || keys.length == 0) {
                return null;
            }
            Map<String, ProductVariantValue> values = valuesByVariant.get(variantId);
            if (values == null || values.isEmpty()) {
                return null;
            }

            for (String key : keys) {
                ProductVariantValue matched = values.get(normalize(key));
                if (matched != null) {
                    return matched;
                }
            }

            List<ProductVariantValue> valueList = valueListByVariant.getOrDefault(variantId, List.of());
            for (String key : keys) {
                String probe = normalizeToken(key);
                if (probe.isBlank()) {
                    continue;
                }
                for (ProductVariantValue value : valueList) {
                    String valueKey = normalizeToken(resolveVariantValueKey(value));
                    if (valueKey.isBlank()) {
                        continue;
                    }
                    if (valueKey.equals(probe) || valueKey.contains(probe)) {
                        return value;
                    }
                }
            }
            return null;
        }
    }

    private static class NormalizedSelection {
        private final PcBuildSelectionItem cpu;
        private final PcBuildSelectionItem mainboard;
        private final List<PcBuildSelectionItem> ramSelections;
        private final PcBuildSelectionItem gpu;
        private final PcBuildSelectionItem storage;
        private final PcBuildSelectionItem psu;
        private final PcBuildSelectionItem pcCase;
        private final PcBuildSelectionItem cooling;

        private NormalizedSelection(
                PcBuildSelectionItem cpu,
                PcBuildSelectionItem mainboard,
                List<PcBuildSelectionItem> ramSelections,
                PcBuildSelectionItem gpu,
                PcBuildSelectionItem storage,
                PcBuildSelectionItem psu,
                PcBuildSelectionItem pcCase,
                PcBuildSelectionItem cooling) {
            this.cpu = cpu;
            this.mainboard = mainboard;
            this.ramSelections = ramSelections != null ? ramSelections : List.of();
            this.gpu = gpu;
            this.storage = storage;
            this.psu = psu;
            this.pcCase = pcCase;
            this.cooling = cooling;
        }

        private List<Long> selectedProductIds() {
            return toOrderedLines().stream()
                    .map(line -> line.productId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        }

        private List<SelectionLine> toOrderedLines() {
            List<SelectionLine> lines = new ArrayList<>();
            addIfPresent(lines, "cpu", cpu);
            addIfPresent(lines, "mainboard", mainboard);
            for (PcBuildSelectionItem ramItem : ramSelections) {
                addIfPresent(lines, "ram", ramItem);
            }
            addIfPresent(lines, "gpu", gpu);
            addIfPresent(lines, "storage", storage);
            addIfPresent(lines, "psu", psu);
            addIfPresent(lines, "case", pcCase);
            addIfPresent(lines, "cooling", cooling);
            return lines;
        }

        private void addIfPresent(List<SelectionLine> lines, String slot, PcBuildSelectionItem item) {
            if (item == null || item.getProductId() == null) {
                return;
            }
            int quantity = item.getQuantity() != null ? item.getQuantity() : 1;
            if (quantity < 1) {
                quantity = 1;
            }
            lines.add(new SelectionLine(slot, item.getProductId(), item.getVariantId(), quantity));
        }
    }

    private static class SelectionLine {
        private final String slotKey;
        private final Long productId;
        private final Long variantId;
        private final int quantity;

        private SelectionLine(String slotKey, Long productId, Long variantId, int quantity) {
            this.slotKey = slotKey;
            this.productId = productId;
            this.variantId = variantId;
            this.quantity = quantity;
        }
    }
}
