package j2ee_backend.nhom05.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductComparisonResponse {
    private Long categoryId;
    private String categoryName;
    private List<ProductCompareItemDto> products = new ArrayList<>();
    private List<ProductCompareAttributeRowDto> attributes = new ArrayList<>();
}
