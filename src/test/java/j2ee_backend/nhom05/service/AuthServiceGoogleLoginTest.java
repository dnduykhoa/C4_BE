package j2ee_backend.nhom05.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import j2ee_backend.nhom05.dto.auth.GoogleProfileResponse;
import j2ee_backend.nhom05.model.Role;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.repository.IRoleRepository;
import j2ee_backend.nhom05.repository.IUserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceGoogleLoginTest {

    @Mock
    private IRoleRepository roleRepository;

    @Mock
    private IUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = org.mockito.Mockito.spy(new AuthService());
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "roleRepository", roleRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "userRepository", userRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "passwordEncoder", passwordEncoder);
    }

    @Test
    void loginWithGoogle_shouldReturnExistingUserByProviderId() {
        GoogleProfileResponse profile = new GoogleProfileResponse("google-sub-1", "user@gmail.com", true, "User One", null);
        doReturn(profile).when(authService).verifyGoogleIdToken("id-token");

        User existing = new User();
        existing.setId(1L);
        existing.setEmail("user@gmail.com");
        existing.setProvider("google");
        existing.setProviderId("google-sub-1");
        existing.setActive(true);

        when(userRepository.findByProviderAndProviderId("google", "google-sub-1")).thenReturn(Optional.of(existing));

        User result = authService.loginWithGoogle("id-token");

        assertSame(existing, result);
        verify(userRepository, times(1)).findByProviderAndProviderId("google", "google-sub-1");
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginWithGoogle_shouldLinkExistingUserByEmailWithoutCreatingDuplicate() {
        GoogleProfileResponse profile = new GoogleProfileResponse("google-sub-2", "legacy@gmail.com", true, "Legacy User", null);
        doReturn(profile).when(authService).verifyGoogleIdToken("id-token");

        User legacyUser = new User();
        legacyUser.setId(2L);
        legacyUser.setEmail("legacy@gmail.com");
        legacyUser.setFullName(null);
        legacyUser.setActive(true);

        when(userRepository.findByProviderAndProviderId("google", "google-sub-2")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("legacy@gmail.com")).thenReturn(Optional.of(legacyUser));
        when(userRepository.save(legacyUser)).thenReturn(legacyUser);

        User result = authService.loginWithGoogle("id-token");

        assertSame(legacyUser, result);
        assertEquals("google", legacyUser.getProvider());
        assertEquals("google-sub-2", legacyUser.getProviderId());
        assertEquals("Legacy User", legacyUser.getFullName());
        verify(userRepository, times(1)).save(legacyUser);
    }

    @Test
    void loginWithGoogle_shouldCreateNewUserWhenNoProviderAndNoEmail() {
        GoogleProfileResponse profile = new GoogleProfileResponse("google-sub-3", "newuser@gmail.com", true, "New User", null);
        doReturn(profile).when(authService).verifyGoogleIdToken("id-token");

        when(userRepository.findByProviderAndProviderId("google", "google-sub-3")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newuser@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-google-password");

        Role userRole = new Role();
        userRole.setName("USER");
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        User result = authService.loginWithGoogle("id-token");

        User savedUser = userCaptor.getValue();
        assertNotNull(savedUser);
        assertEquals("newuser@gmail.com", savedUser.getEmail());
        assertEquals("google", savedUser.getProvider());
        assertEquals("google-sub-3", savedUser.getProviderId());
        assertEquals("New User", savedUser.getFullName());
        assertEquals("encoded-google-password", savedUser.getPassword());
        assertNotNull(savedUser.getRoles());
        assertEquals(1, savedUser.getRoles().size());

        assertSame(savedUser, result);
        verify(userRepository, times(1)).findByProviderAndProviderId("google", "google-sub-3");
        verify(userRepository, times(1)).findByEmail("newuser@gmail.com");
        verify(roleRepository, times(1)).findByName(eq("USER"));
    }
}
