package j2ee_backend.nhom05.service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import j2ee_backend.nhom05.config.RoleAccess;
import j2ee_backend.nhom05.dto.auth.UpdateProfileRequest;
import j2ee_backend.nhom05.model.Role;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.repository.IRoleRepository;
import j2ee_backend.nhom05.repository.IUserRepository;
import j2ee_backend.nhom05.validator.PhoneValidator;

@Service
public class UserService {
    
    @Autowired
    private IUserRepository userRepository;
    
    @Autowired
    private IRoleRepository roleRepository;
    
    // Lấy user theo ID
    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
    }
    
    /** Lấy danh sách tất cả user. */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /** Tìm kiếm user theo username, email hoặc họ tên. */
    public List<User> searchUsers(String keyword) {
        return userRepository
            .findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                keyword, keyword, keyword);
    }

    /** Vô hiệu hóa / kích hoạt tài khoản user (admin/manager). */
    @Transactional
    public User updateUserActivation(Long id, boolean active, User actor) {
        if (actor == null) {
            throw new RuntimeException("Không xác định được người thực hiện");
        }

        User user = findById(id);
        Set<String> actorRoles = RoleAccess.getRoles(actor);
        boolean isAdmin = actorRoles.contains("ADMIN");
        boolean isManager = actorRoles.contains("MANAGER");

        if (!isAdmin && !isManager) {
            throw new RuntimeException("Bạn không có quyền cập nhật trạng thái tài khoản");
        }

        if (actor.getId() != null && actor.getId().equals(id) && !active) {
            throw new RuntimeException("Bạn không thể tự vô hiệu hóa tài khoản của mình");
        }

        if (isManager) {
            Set<String> targetCurrentRoles = RoleAccess.getRoles(user);
            if (targetCurrentRoles.contains("ADMIN") || targetCurrentRoles.contains("MANAGER")) {
                throw new RuntimeException("Manager không được thay đổi trạng thái tài khoản Admin hoặc Manager");
            }
        }

        user.setActive(active);
        return userRepository.save(user);
    }

    /** Cập nhật danh sách role cho user (admin). */
    @Transactional
    public User updateUserRoles(Long id, Set<String> roleNames, User actor) {
        if (actor == null) {
            throw new RuntimeException("Không xác định được người thực hiện");
        }

        User user = findById(id);
        Set<String> actorRoles = RoleAccess.getRoles(actor);
        boolean isAdmin = actorRoles.contains("ADMIN");
        boolean isManager = actorRoles.contains("MANAGER");

        if (!isAdmin && !isManager) {
            throw new RuntimeException("Bạn không có quyền phân quyền");
        }

        Set<String> normalizedRoleNames = new HashSet<>();
        for (String roleName : roleNames) {
            String normalized = RoleAccess.normalizeRole(roleName);
            if (normalized.isBlank()) {
                continue;
            }
            normalizedRoleNames.add(normalized.toUpperCase(Locale.ROOT));
        }

        if (normalizedRoleNames.isEmpty()) {
            throw new RuntimeException("Danh sách role không hợp lệ");
        }

        if (normalizedRoleNames.size() != 1) {
            throw new RuntimeException("Mỗi tài khoản chỉ được gán đúng 1 role");
        }

        String targetRoleName = normalizedRoleNames.iterator().next();

        if (isManager) {
            Set<String> targetCurrentRoles = RoleAccess.getRoles(user);
            if (targetCurrentRoles.contains("ADMIN") || targetCurrentRoles.contains("MANAGER")) {
                throw new RuntimeException("Manager không được thay đổi role của Admin hoặc Manager");
            }
            if ("ADMIN".equals(targetRoleName) || "MANAGER".equals(targetRoleName)) {
                throw new RuntimeException("Manager chỉ được phân quyền giữa USER và STAFF");
            }
        }

        Set<Role> roles = new HashSet<>();
        Role role = roleRepository.findByName(targetRoleName)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy role: " + targetRoleName));
        roles.add(role);
        user.setRoles(roles);
        return userRepository.save(user);
    }

    // Cập nhật profile (không cho update role)
    @Transactional
    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findById(userId);
        
        // Kiểm tra email mới có trùng với user khác không
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email đã được sử dụng");
            }
            user.setEmail(request.getEmail());
        }
        
        // Cập nhật các field được phép
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(PhoneValidator.normalize(request.getPhone()));
        }
        if (request.getBirthDate() != null) {
            user.setBirthDate(request.getBirthDate());
        }
        
        return userRepository.save(user);
    }
}
