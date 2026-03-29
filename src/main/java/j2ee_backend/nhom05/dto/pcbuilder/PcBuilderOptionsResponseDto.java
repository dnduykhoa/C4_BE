package j2ee_backend.nhom05.dto.pcbuilder;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PcBuilderOptionsResponseDto {
    private String slot;
    private Integer estimatedPower;
    private Integer recommendedPsuWatt;
    private Map<String, String> appliedFilters;
    private List<PcBuilderOptionItemDto> options;
}
