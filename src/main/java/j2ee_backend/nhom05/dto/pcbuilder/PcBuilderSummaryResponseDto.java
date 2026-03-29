package j2ee_backend.nhom05.dto.pcbuilder;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PcBuilderSummaryResponseDto {
    private List<PcBuilderOptionItemDto> selectedParts;
    private BigDecimal totalPrice;
    private Integer estimatedPower;
    private Integer recommendedPsuWatt;
    private Boolean compatible;
    private List<PcBuilderWarningDto> warnings;
}
