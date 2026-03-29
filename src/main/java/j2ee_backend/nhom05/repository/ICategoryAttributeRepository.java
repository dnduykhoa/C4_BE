package j2ee_backend.nhom05.repository;

import j2ee_backend.nhom05.model.CategoryAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ICategoryAttributeRepository extends JpaRepository<CategoryAttribute, Long> {

    List<CategoryAttribute> findByCategoryIdOrderByDisplayOrderAsc(Long categoryId);

    List<CategoryAttribute> findByAttributeDefinitionId(Long attrDefId);

    boolean existsByCategoryIdAndAttributeDefinitionId(Long categoryId, Long attrDefId);

    Optional<CategoryAttribute> findByCategoryIdAndAttributeDefinitionId(Long categoryId, Long attrDefId);
}
