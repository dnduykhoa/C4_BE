package j2ee_backend.nhom05.repository;

import j2ee_backend.nhom05.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface IRoleRepository extends JpaRepository<Role, Long> {
    
    // Tìm role theo tên
    Optional<Role> findByName(String name);
    
    // Kiểm tra role có tồn tại theo tên
    boolean existsByName(String name);
}
