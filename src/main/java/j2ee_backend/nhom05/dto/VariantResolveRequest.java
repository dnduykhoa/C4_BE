package j2ee_backend.nhom05.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VariantResolveRequest {
    private Map<String, String> selections;
}
