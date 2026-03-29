package j2ee_backend.nhom05.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryRequest {
    private String name;
    private String description;
    private Integer displayOrder;
    private Boolean isActive;
    private Long parentId;
}
