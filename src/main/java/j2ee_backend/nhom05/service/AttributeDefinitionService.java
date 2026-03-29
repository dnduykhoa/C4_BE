package j2ee_backend.nhom05.service;

import j2ee_backend.nhom05.model.AttributeDefinition;
import j2ee_backend.nhom05.model.AttributeGroup;
import j2ee_backend.nhom05.repository.IAttributeDefinitionRepository;
import j2ee_backend.nhom05.repository.IAttributeGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class AttributeDefinitionService {

    @Autowired
    private IAttributeDefinitionRepository attributeDefinitionRepository;

    @Autowired
    private IAttributeGroupRepository attributeGroupRepository;

    public List<AttributeDefinition> getAllDefinitions() {
        return attributeDefinitionRepository.findAll();
    }

    public List<AttributeDefinition> getActiveDefinitions() {
        return attributeDefinitionRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    public List<AttributeDefinition> getFilterableDefinitions() {
        return attributeDefinitionRepository.findByIsFilterableTrue();
    }

    public List<AttributeDefinition> getDefinitionsByGroup(Long groupId) {
        return attributeDefinitionRepository.findByAttributeGroupId(groupId);
    }

    public Optional<AttributeDefinition> getDefinitionById(Long id) {
        return attributeDefinitionRepository.findById(id);
    }

    public Optional<AttributeDefinition> getDefinitionByKey(String attrKey) {
        return attributeDefinitionRepository.findByAttrKey(attrKey);
    }

    public AttributeDefinition createDefinition(AttributeDefinition def, Long groupId) {
        if (attributeDefinitionRepository.existsByAttrKey(def.getAttrKey())) {
            throw new RuntimeException("Đã tồn tại thuộc tính với key: " + def.getAttrKey());
        }
        if (groupId != null) {
            AttributeGroup group = attributeGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm thuộc tính với ID: " + groupId));
            def.setAttributeGroup(group);
        }
        return attributeDefinitionRepository.save(def);
    }

    public AttributeDefinition updateDefinition(Long id, AttributeDefinition details, Long groupId) {
        AttributeDefinition def = attributeDefinitionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy thuộc tính với ID: " + id));

        // Kiểm tra trùng attrKey nếu thay đổi
        if (!def.getAttrKey().equals(details.getAttrKey()) &&
                attributeDefinitionRepository.existsByAttrKey(details.getAttrKey())) {
            throw new RuntimeException("Đã tồn tại thuộc tính với key: " + details.getAttrKey());
        }

        def.setName(details.getName());
        def.setAttrKey(details.getAttrKey());
        def.setDataType(details.getDataType());
        def.setUnit(details.getUnit());
        def.setIsFilterable(details.getIsFilterable());
        def.setIsRequired(details.getIsRequired());
        def.setDisplayOrder(details.getDisplayOrder());
        def.setIsActive(details.getIsActive());

        if (groupId != null) {
            AttributeGroup group = attributeGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm thuộc tính với ID: " + groupId));
            def.setAttributeGroup(group);
        } else {
            def.setAttributeGroup(null);
        }
        return attributeDefinitionRepository.save(def);
    }

    public void deleteDefinition(Long id) {
        AttributeDefinition def = attributeDefinitionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy thuộc tính với ID: " + id));
        attributeDefinitionRepository.delete(def);
    }
}
