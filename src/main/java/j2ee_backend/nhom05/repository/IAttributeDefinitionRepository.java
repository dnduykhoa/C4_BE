package j2ee_backend.nhom05.repository;

import j2ee_backend.nhom05.model.AttributeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface IAttributeDefinitionRepository extends JpaRepository<AttributeDefinition, Long> {

    Optional<AttributeDefinition> findByAttrKey(String attrKey);

    boolean existsByAttrKey(String attrKey);

    List<AttributeDefinition> findByIsActiveTrueOrderByDisplayOrderAsc();

    List<AttributeDefinition> findByIsFilterableTrue();

    List<AttributeDefinition> findByAttributeGroupId(Long groupId);
}
