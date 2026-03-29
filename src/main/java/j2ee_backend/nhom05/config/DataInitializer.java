package j2ee_backend.nhom05.config;

import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import j2ee_backend.nhom05.model.Role;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.repository.IRoleRepository;
import j2ee_backend.nhom05.repository.IUserRepository;

@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

	private final IRoleRepository roleRepository;
	private final IUserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public DataInitializer(IRoleRepository roleRepository, IUserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.roleRepository = roleRepository;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(String... args) {
		Role userRole = ensureRole("USER", "Người dùng - Khách hàng thông thường");
		ensureUser(
				"user",
				"user123",
				"user@techstore.com",
				"Người dùng mặc định",
				"0900000000",
				userRole);
	}

	private Role ensureRole(String roleName, String description) {
		return roleRepository.findByName(roleName).orElseGet(() -> {
			Role role = new Role();
			role.setName(roleName);
			role.setDescription(description);
			return roleRepository.save(role);
		});
	}

	private void ensureUser(String username, String rawPassword, String email, String fullName, String phone, Role role) {
		if (userRepository.existsByUsername(username)) {
			return;
		}

		User user = new User();
		user.setUsername(username);
		user.setPassword(passwordEncoder.encode(rawPassword));
		user.setEmail(email);
		user.setFullName(fullName);
		user.setPhone(phone);
		user.setRoles(Set.of(role));
		userRepository.save(user);
	}
}
       