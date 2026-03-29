package j2ee_backend.nhom05.dto;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCompareAttributeRowDto {
    private String attrKey;
    private String attrName;
    private String unit;
    private Integer displayOrder;
    private Map<Long, String> values = new LinkedHashMap<>();
}
