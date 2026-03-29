package j2ee_backend.nhom05.dto.pcbuilder;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PcBuildSelection {
    // Legacy flat fields (query-param based)
    private Long cpuId;
    private Long mainboardId;
    private Long ramId;
    private Long gpuId;
    private Long storageId;
    private Long psuId;
    private Long caseId;
    private Long coolingId;

    // Legacy optional variant fields
    private Long cpuVariantId;
    private Long mainboardVariantId;
    private Long ramVariantId;
    private Long gpuVariantId;
    private Long storageVariantId;
    private Long psuVariantId;
    private Long caseVariantId;
    private Long coolingVariantId;

    // Legacy optional quantity fields
    private Integer cpuQuantity;
    private Integer mainboardQuantity;
    private Integer ramQuantity;
    private Integer gpuQuantity;
    private Integer storageQuantity;
    private Integer psuQuantity;
    private Integer caseQuantity;
    private Integer coolingQuantity;

    // New nested contract
    private PcBuildSelectionItem cpu;
    private PcBuildSelectionItem mainboard;
    private PcBuildSelectionItem ram;
    private PcBuildSelectionItem gpu;
    private PcBuildSelectionItem storage;
    private PcBuildSelectionItem psu;

    @JsonProperty("case")
    private PcBuildSelectionItem caseSelection;

    private PcBuildSelectionItem cooling;
    private List<PcBuildSelectionItem> ramSelections;

    public PcBuildSelectionItem resolveCpuSelection() {
        return merge(cpu, cpuId, cpuVariantId, cpuQuantity);
    }

    public PcBuildSelectionItem resolveMainboardSelection() {
        return merge(mainboard, mainboardId, mainboardVariantId, mainboardQuantity);
    }

    public PcBuildSelectionItem resolveGpuSelection() {
        return merge(gpu, gpuId, gpuVariantId, gpuQuantity);
    }

    public PcBuildSelectionItem resolveStorageSelection() {
        return merge(storage, storageId, storageVariantId, storageQuantity);
    }

    public PcBuildSelectionItem resolvePsuSelection() {
        return merge(psu, psuId, psuVariantId, psuQuantity);
    }

    public PcBuildSelectionItem resolveCaseSelection() {
        return merge(caseSelection, caseId, caseVariantId, caseQuantity);
    }

    public PcBuildSelectionItem resolveCoolingSelection() {
        return merge(cooling, coolingId, coolingVariantId, coolingQuantity);
    }

    public List<PcBuildSelectionItem> resolveRamSelections() {
        if (ramSelections != null && !ramSelections.isEmpty()) {
            List<PcBuildSelectionItem> result = new ArrayList<>();
            for (PcBuildSelectionItem item : ramSelections) {
                PcBuildSelectionItem normalized = normalizeQuantity(item);
                if (normalized.getProductId() != null) {
                    result.add(normalized);
                }
            }
            if (!result.isEmpty()) {
                return result;
            }
        }

        PcBuildSelectionItem fallback = merge(ram, ramId, ramVariantId, ramQuantity);
        if (fallback.getProductId() == null) {
            return List.of();
        }
        return List.of(fallback);
    }

    private PcBuildSelectionItem merge(PcBuildSelectionItem nested, Long legacyProductId, Long legacyVariantId, Integer legacyQuantity) {
        PcBuildSelectionItem result = new PcBuildSelectionItem();
        if (nested != null) {
            result.setProductId(nested.getProductId());
            result.setVariantId(nested.getVariantId());
            result.setQuantity(nested.getQuantity());
        }
        if (result.getProductId() == null) {
            result.setProductId(legacyProductId);
        }
        if (result.getVariantId() == null) {
            result.setVariantId(legacyVariantId);
        }
        if (result.getQuantity() == null) {
            result.setQuantity(legacyQuantity);
        }
        return normalizeQuantity(result);
    }

    private PcBuildSelectionItem normalizeQuantity(PcBuildSelectionItem item) {
        if (item == null) {
            return new PcBuildSelectionItem(null, null, 1);
        }
        Integer quantity = item.getQuantity();
        if (quantity == null || quantity < 1) {
            quantity = 1;
        }
        item.setQuantity(quantity);
        return item;
    }
}
