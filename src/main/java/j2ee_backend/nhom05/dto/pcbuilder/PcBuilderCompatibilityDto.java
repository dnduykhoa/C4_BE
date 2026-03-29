package j2ee_backend.nhom05.dto.pcbuilder;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PcBuilderCompatibilityDto {
    private String status;
    private List<String> reasons;
}
