package j2ee_backend.nhom05.dto.pcbuilder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PcBuilderWarningDto {
    private String severity;
    private String code;
    private String message;
}
