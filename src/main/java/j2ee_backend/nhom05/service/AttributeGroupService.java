package j2ee_backend.nhom05.service;

import j2ee_backend.nhom05.model.AttributeGroup;
import j2ee_backend.nhom05.repository.IAttributeGroupRepository;
import j2ee_backend.nhom05.repository.IAttributeDefinitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class AttributeGroupService {

    @Autowired
    private IAttributeGroupRepository attributeGroupRepository;

    @Autowired
    private IAttributeDefinitionRepository attributeDefinitionRepository;

    public List<AttributeGroup> getAllGroups() {
        return attributeGroupRepository.findAll();
    }

    public List<AttributeGroup> getActiveGroups() {
        return attributeGroupRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    public Optional<AttributeGroup> getGroupById(Long id) {
        return attributeGroupRepository.findById(id);
    }

    public AttributeGroup createGroup(AttributeGroup group) {
        if (attributeGroupRepository.findByName(group.getName()).isPresent()) {
            throw new RuntimeException("Đã tồn tại nhóm thuộc tính với tên: " + group.getName());
        }
        return attributeGroupRepository.save(group);
    }

    public AttributeGroup updateGroup(Long id, AttributeGroup details) {
        AttributeGroup group = attributeGroupRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm thuộc tính với ID: " + id));
        group.setName(details.getName());
        group.setDescription(details.getDescription());
        group.setDisplayOrder(details.getDisplayOrder());
        group.setIsActive(details.getIsActive());
        return attributeGroupRepository.save(group);
    }

    public void deleteGroup(Long id) {
        AttributeGroup group = attributeGroupRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm thuộc tính với ID: " + id));
        long count = attributeDefinitionRepository.findByAttributeGroupId(id).size();
        if (count > 0) {
            throw new RuntimeException("Không thể xóa nhóm này vì còn " + count + " thuộc tính. Xóa thuộc tính trước.");
        }
        attributeGroupRepository.delete(group);
    }
}
