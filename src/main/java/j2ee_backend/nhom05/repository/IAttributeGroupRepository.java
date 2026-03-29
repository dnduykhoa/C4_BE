package j2ee_backend.nhom05.repository;

import j2ee_backend.nhom05.model.AttributeGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface IAttributeGroupRepository extends JpaRepository<AttributeGroup, Long> {

    Optional<AttributeGroup> findByName(String name);

    List<AttributeGroup> findByIsActiveTrueOrderByDisplayOrderAsc();
}
