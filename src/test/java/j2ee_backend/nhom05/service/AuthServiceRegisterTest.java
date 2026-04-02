package j2ee_backend.nhom05.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import j2ee_backend.nhom05.dto.auth.RegisterRequest;
import j2ee_backend.nhom05.model.Role;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.repository.IRoleRepository;
import j2ee_backend.nhom05.repository.IUserRepository;
import j2ee_backend.nhom05.repository.IPasswordResetTokenRepository;
import j2ee_backend.nhom05.repository.ITwoFactorCodeRepository;
import j2ee_backend.nhom05.validator.PasswordValidator;

/**
 * 4.1.3. Test case – Chức năng Đăng ký (White-box)
 * Tuyến đường: BD → B → C → D → E → F → G → H → I → J → KT
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("4.1.3 - Test Case: Chức năng Đăng ký")
class AuthServiceRegisterTest {

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IRoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordValidator passwordValidator;

    @Mock
    private IPasswordResetTokenRepository resetTokenRepository;

    @Mock
    private ITwoFactorCodeRepository twoFactorCodeRepository;

    @Mock
    private AuthSessionService authSessionService;

    private AuthService authService;

    private Role userRole;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
        ReflectionTestUtils.setField(authService, "userRepository", userRepository);
        ReflectionTestUtils.setField(authService, "roleRepository", roleRepository);
        ReflectionTestUtils.setField(authService, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(authService, "passwordValidator", passwordValidator);
        ReflectionTestUtils.setField(authService, "resetTokenRepository", resetTokenRepository);
        ReflectionTestUtils.setField(authService, "twoFactorCodeRepository", twoFactorCodeRepository);
        ReflectionTestUtils.setField(authService, "authSessionService", authSessionService);
        ReflectionTestUtils.setField(authService, "googleClientId", "");

        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");
    }

    /** Dữ liệu đầu vào hợp lệ dùng làm baseline cho mọi test case */
    private RegisterRequest validRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("newuser");
        req.setPassword("Password1@");
        req.setConfirmPassword("Password1@");
        req.setEmail("newuser@example.com");
        req.setFullName("New User");
        req.setPhone("0912345678");
        req.setBirthDate(LocalDate.of(2000, 1, 1));
        return req;
    }

    // -----------------------------------------------------------------------
    // DK-WB-01: Tuyến BD-B(T)-C(T)-D(T)-E(T)-F(T)-G(T)-H(T)-I-J-KT
    // Đăng ký thành công – toàn bộ nhánh True
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("DK-WB-01: Đăng ký thành công")
    void DKWB01_register_success() {
        RegisterRequest req = validRequest();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(userRepository.findByPhone("0912345678")).thenReturn(Optional.empty());
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("Password1@")).thenReturn("encoded_password");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("newuser");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = authService.register(req);

        assertNotNull(result);
        assertEquals(1L, result.getId());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture()); // Kiểm tra user được tạo ra để lưu vào DB có đúng dữ liệu đã xử lý hay không
        User captured = captor.getValue();

        assertEquals("newuser", captured.getUsername());
        assertEquals("encoded_password", captured.getPassword());
        assertEquals("newuser@example.com", captured.getEmail());
        assertTrue(captured.getRoles().contains(userRole));
        verify(passwordValidator).validate("Password1@");
    }

    // -----------------------------------------------------------------------
    // DK-WB-02: Tuyến BD-B(F)-Z-KT
    // Sai xác nhận mật khẩu – ngắt tại B, trả lỗi 400
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("DK-WB-02: Mật khẩu xác nhận không khớp")
    void DKWB02_register_passwordMismatch_throwsException() {
        RegisterRequest req = validRequest();
        req.setConfirmPassword("DifferentPass1@"); // confirmPassword ≠ password

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(req));
        assertEquals("Mật khẩu xác nhận không khớp", ex.getMessage());

        verify(userRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // DK-WB-03: Tuyến BD-B(T)-C(F)-Z-KT
    // Mật khẩu không đạt chuẩn bảo mật – ngắt tại C, trả lỗi 400
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("DK-WB-03: Mật khẩu không đạt chính sách bảo mật")
    void DKWB03_register_weakPassword_throwsException() {
        RegisterRequest req = validRequest();
        req.setPassword("weakpass");       // thiếu ký tự hoa/số/đặc biệt
        req.setConfirmPassword("weakpass"); // password = confirmPassword (B = True)

        doThrow(new RuntimeException("Mật khẩu không đạt chính sách bảo mật"))
                .when(passwordValidator).validate("weakpass");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(req));
        assertEquals("Mật khẩu không đạt chính sách bảo mật", ex.getMessage());

        verify(userRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // DK-WB-04: Tuyến BD-B(T)-C(T)-D(F)-Z-KT
    // Username đã tồn tại – ngắt tại D, trả lỗi 400
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("DK-WB-04: Username đã được sử dụng")
    void DKWB04_register_duplicateUsername_throwsException() {
        RegisterRequest req = validRequest();

        when(userRepository.existsByUsername("newuser")).thenReturn(true); // username đã có trong DB

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(req));
        assertEquals("Username đã được sử dụng", ex.getMessage());

        verify(userRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // DK-WB-05: Tuyến BD-B(T)-C(T)-D(T)-E(F)-Z-KT
    // Email đã tồn tại – ngắt tại E, trả lỗi 400
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("DK-WB-05: Email đã được sử dụng")
    void DKWB05_register_duplicateEmail_throwsException() {
        RegisterRequest req = validRequest();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(true); // email đã có trong DB

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(req));
        assertEquals("Email đã được sử dụng", ex.getMessage());

        verify(userRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // DK-WB-06: Tuyến BD-B(T)-C(T)-D(T)-E(T)-F(F)-Z-KT
    // SĐT không hợp lệ – ngắt tại F, trả lỗi 400
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("DK-WB-06: Số điện thoại không hợp lệ")
    void DKWB06_register_invalidPhone_throwsException() {
        RegisterRequest req = validRequest();
        req.setPhone("12345"); // sai định dạng Việt Nam

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(req));
        assertEquals("Số điện thoại không hợp lệ", ex.getMessage());

        verify(userRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // DK-WB-07: Tuyến BD-B(T)-C(T)-D(T)-E(T)-F(T)-G(F)-Z-KT
    // SĐT đã tồn tại – ngắt tại G, trả lỗi 400
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("DK-WB-07: Số điện thoại đã được sử dụng")
    void DKWB07_register_duplicatePhone_throwsException() {
        RegisterRequest req = validRequest();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(userRepository.findByPhone("0912345678")).thenReturn(Optional.of(new User())); // phone đã có trong DB

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(req));
        assertEquals("Số điện thoại đã được sử dụng", ex.getMessage());

        verify(userRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // DK-WB-08: Tuyến BD-B(T)-C(T)-D(T)-E(T)-F(T)-G(T)-H(F)-Z-KT
    // Thiếu role USER – ngắt tại H, trả lỗi 400
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("DK-WB-08: Role USER không tồn tại")
    void DKWB08_register_roleMissing_throwsException() {
        RegisterRequest req = validRequest();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(userRepository.findByPhone("0912345678")).thenReturn(Optional.empty());
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty()); // role USER không có trong DB

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(req));
        assertEquals("Role USER không tồn tại", ex.getMessage());

        verify(userRepository, never()).save(any());
    }
}
