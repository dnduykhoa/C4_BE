package j2ee_backend.nhom05.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import j2ee_backend.nhom05.model.AttributeDefinition;
import j2ee_backend.nhom05.model.Category;
import j2ee_backend.nhom05.model.CategoryAttribute;
import j2ee_backend.nhom05.repository.IAttributeDefinitionRepository;
import j2ee_backend.nhom05.repository.ICategoryAttributeRepository;
import j2ee_backend.nhom05.repository.ICategoryRepository;

@Service
public class CategoryAttributeService {

    @Autowired
    private ICategoryAttributeRepository categoryAttributeRepository;

    @Autowired
    private ICategoryRepository categoryRepository;

    @Autowired
    private IAttributeDefinitionRepository attributeDefinitionRepository;

    /** Lấy tất cả thuộc tính của một danh mục (đã sắp xếp theo display_order). */
    public List<CategoryAttribute> getAttributesByCategory(Long categoryId) {
        categoryRepository.findById(categoryId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID: " + categoryId));
        return categoryAttributeRepository.findByCategoryIdOrderByDisplayOrderAsc(categoryId);
    }

    /** Gán một thuộc tính vào danh mục. */
    public CategoryAttribute assignAttribute(Long categoryId, Long attrDefId,
                                              Boolean isRequired, Integer displayOrder) {
        if (categoryAttributeRepository.existsByCategoryIdAndAttributeDefinitionId(categoryId, attrDefId)) {
            throw new RuntimeException("Thuộc tính này đã được gán cho danh mục.");
        }
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID: " + categoryId));
        AttributeDefinition def = attributeDefinitionRepository.findById(attrDefId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy thuộc tính với ID: " + attrDefId));

        CategoryAttribute ca = new CategoryAttribute();
        ca.setCategory(category);
        ca.setAttributeDefinition(def);
        ca.setIsRequired(isRequired != null ? isRequired : false);
        ca.setDisplayOrder(displayOrder != null ? displayOrder : 0);
        return categoryAttributeRepository.save(ca);
    }

    /** Cập nhật is_required / display_order của một liên kết Category-Attribute. */
    public CategoryAttribute updateAssignment(Long id, Boolean isRequired, Integer displayOrder) {
        CategoryAttribute ca = categoryAttributeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy liên kết với ID: " + id));
        if (isRequired != null) ca.setIsRequired(isRequired);
        if (displayOrder != null) ca.setDisplayOrder(displayOrder);
        return categoryAttributeRepository.save(ca);
    }

    /** Gỡ bỏ thuộc tính khỏi danh mục. */
    public void removeAttribute(Long id) {
        CategoryAttribute ca = categoryAttributeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy liên kết với ID: " + id));
        categoryAttributeRepository.delete(ca);
    }

    /** Gỡ bỏ thuộc tính khỏi danh mục theo categoryId + attrDefId. */
    public void removeAttributeByCategoryAndDef(Long categoryId, Long attrDefId) {
        CategoryAttribute ca = categoryAttributeRepository
            .findByCategoryIdAndAttributeDefinitionId(categoryId, attrDefId)
            .orElseThrow(() -> new RuntimeException("Thuộc tính này chưa được gán cho danh mục."));
        categoryAttributeRepository.delete(ca);
    }
}
