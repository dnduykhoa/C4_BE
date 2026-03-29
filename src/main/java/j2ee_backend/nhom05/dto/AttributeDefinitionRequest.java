package j2ee_backend.nhom05.dto;

import j2ee_backend.nhom05.model.AttributeDefinition;
import j2ee_backend.nhom05.model.AttributeDefinition.DataType;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * DTO nhận request tạo / cập nhật AttributeDefinition.
 * Dùng riêng vì AttributeDefinition có quan hệ lazy với AttributeGroup,
 * nên không thể nhận trực tiếp từ JSON.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttributeDefinitionRequest {

    private String name;
    private String attrKey;
    private DataType dataType = DataType.STRING;
    private String unit;
    private Boolean isFilterable = false;
    private Boolean isRequired = false;
    private Integer displayOrder = 0;
    private Boolean isActive = true;

    /** ID của nhóm thuộc tính (AttributeGroup). Null nếu không thuộc nhóm nào. */
    private Long groupId;

    /** Chuyển đổi DTO sang entity (không set attributeGroup — service sẽ xử lý). */
    public AttributeDefinition toEntity() {
        AttributeDefinition def = new AttributeDefinition();
        def.setName(name);
        def.setAttrKey(attrKey);
        def.setDataType(dataType != null ? dataType : DataType.STRING);
        def.setUnit(unit);
        def.setIsFilterable(isFilterable != null ? isFilterable : false);
        def.setIsRequired(isRequired != null ? isRequired : false);
        def.setDisplayOrder(displayOrder != null ? displayOrder : 0);
        def.setIsActive(isActive != null ? isActive : true);
        return def;
    }
}
