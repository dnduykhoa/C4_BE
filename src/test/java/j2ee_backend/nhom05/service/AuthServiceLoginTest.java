package j2ee_backend.nhom05.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import j2ee_backend.nhom05.dto.auth.LoginRequest;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.repository.IRoleRepository;
import j2ee_backend.nhom05.repository.IUserRepository;
import j2ee_backend.nhom05.repository.IPasswordResetTokenRepository;
import j2ee_backend.nhom05.repository.ITwoFactorCodeRepository;
import j2ee_backend.nhom05.validator.PasswordValidator;

/**
 * Test Case – Chức năng Đăng nhập (White-box)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("4.2.3 - Test Case: Chức năng Đăng nhập")
class AuthServiceLoginTest {

    @Mock private IUserRepository userRepository;
    @Mock private IRoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PasswordValidator passwordValidator;
    @Mock private IPasswordResetTokenRepository resetTokenRepository;
    @Mock private ITwoFactorCodeRepository twoFactorCodeRepository;
    @Mock private AuthSessionService authSessionService;
    @Mock private HttpServletRequest httpRequest;

    private AuthService authService;

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
    }

    private LoginRequest loginRequest(String emailOrPhone, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmailOrPhone(emailOrPhone);
        req.setPassword(password);
        return req;
    }

    // -----------------------------------------------------------------------
    // DN-WB-01
    // Sai cả định dạng Email lẫn SĐT – ngắt ngay tại bước kiểm tra định dạng
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("DN-WB-01: Sai định dạng email và SĐT")
    void DNWB01_login_invalidFormat_throwsException() {
        LoginRequest req = loginRequest("abc", "123");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login(req, httpRequest));
        assertEquals("Vui lòng nhập đúng định dạng email hoặc số điện thoại", ex.getMessage());

        verifyNoInteractions(userRepository);
    }

    // -----------------------------------------------------------------------
    // DN-WB-02
    // SĐT đúng định dạng nhưng tài khoản hoàn toàn không tồn tại trong DB
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("DN-WB-02: SĐT hợp lệ nhưng không tồn tại trong DB")
    void DNWB02_login_validPhoneNotFound_throwsException() {
        LoginRequest req = loginRequest("0999999999", "anyPassword");

        when(userRepository.findByEmail("0999999999")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("0999999999")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login(req, httpRequest));
        assertEquals("Email hoặc số điện thoại không tồn tại", ex.getMessage());
    }

    // -----------------------------------------------------------------------
    // DN-WB-03
    // Email tồn tại trong DB nhưng tài khoản đang bị khóa
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("DN-WB-03: Email tồn tại, tài khoản bị vô hiệu hóa")
    void DNWB03_login_emailFoundButDisabled_throwsException() {
        LoginRequest req = loginRequest("test@gmail.com", "anyPassword");

        User disabledUser = new User();
        disabledUser.setActive(false);
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(disabledUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login(req, httpRequest));
        assertEquals("Tài khoản đã bị vô hiệu hóa", ex.getMessage());
    }

    // -----------------------------------------------------------------------
    // DN-WB-04
    // Email hợp lệ, tài khoản hoạt động, nhưng mật khẩu không khớp
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("DN-WB-04: Email đúng, tài khoản active, mật khẩu sai")
    void DNWB04_login_wrongPassword_throwsException() {
        LoginRequest req = loginRequest("test@gmail.com", "SaiMatKhau");

        User activeUser = new User();
        activeUser.setActive(true);
        activeUser.setPassword("encoded_password");
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("SaiMatKhau", "encoded_password")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login(req, httpRequest));
        assertEquals("Mật khẩu không chính xác", ex.getMessage());
    }

    // -----------------------------------------------------------------------
    // DN-WB-05
    // Email đúng định dạng, không tìm thấy theo cả email lẫn SĐT
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("DN-WB-05: Email không tồn tại trong DB")
    void DNWB05_login_emailNotFoundAnywhere_throwsException() {
        LoginRequest req = loginRequest("notfound@gmail.com", "anyPassword");

        when(userRepository.findByEmail("notfound@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("notfound@gmail.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login(req, httpRequest));
        assertEquals("Email hoặc số điện thoại không tồn tại", ex.getMessage());
    }

    // -----------------------------------------------------------------------
    // DN-WB-06
    // Đầu vào là Email, tìm bằng Email không thấy, tìm bằng SĐT lại thấy
    // nhưng tài khoản đó đang bị khóa
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("DN-WB-06: Email không thấy, tra SĐT có tài khoản nhưng bị vô hiệu hóa")
    void DNWB06_login_emailMissingPhoneFoundButDisabled_throwsException() {
        LoginRequest req = loginRequest("weird@gmail.com", "anyPassword");

        User disabledUser = new User();
        disabledUser.setActive(false);
        when(userRepository.findByEmail("weird@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("weird@gmail.com")).thenReturn(Optional.of(disabledUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login(req, httpRequest));
        assertEquals("Tài khoản đã bị vô hiệu hóa", ex.getMessage());
    }
}
