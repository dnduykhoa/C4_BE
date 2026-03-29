package j2ee_backend.nhom05.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import j2ee_backend.nhom05.config.JwtUtil;
import j2ee_backend.nhom05.dto.auth.LoginResponse;
import j2ee_backend.nhom05.dto.auth.TrustedDeviceResponse;
import j2ee_backend.nhom05.model.AuthSession;
import j2ee_backend.nhom05.model.Role;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.repository.IAuthSessionRepository;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthSessionService {

    private final IAuthSessionRepository authSessionRepository;
    private final JwtUtil jwtUtil;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    @Value("${jwt.refresh-remember-me-expiration:2592000000}")
    private long refreshRememberMeExpiration;

    @Value("${auth.admin.ip-lock-enabled:true}")
    private boolean adminIpLockEnabled;

    public AuthSessionService(IAuthSessionRepository authSessionRepository, JwtUtil jwtUtil) {
        this.authSessionRepository = authSessionRepository;
        this.jwtUtil = jwtUtil;
    }

    public String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return normalizeIp(ip != null ? ip : "127.0.0.1");
    }

    public String resolveDeviceId(HttpServletRequest request) {
        String deviceId = firstNonBlank(
                request.getHeader("X-Device-Id"),
                request.getHeader("X-Device-ID"),
                request.getHeader("X-Device-Fingerprint"));

        if (deviceId != null) {
            return normalizeDeviceId(deviceId);
        }

        // Fallback để tránh fail login/refresh khi frontend chưa gửi custom header.
        String fallback = String.join("|",
                normalizeDevicePart(request.getHeader("User-Agent")),
                normalizeDevicePart(request.getHeader("Sec-CH-UA-Platform")),
                normalizeDevicePart(request.getHeader("Accept-Language")),
                resolveClientIp(request));
        return normalizeDeviceId(fallback);
    }

    public String resolveDeviceName(HttpServletRequest request) {
        String deviceName = request.getHeader("X-Device-Name");
        if (deviceName == null || deviceName.isBlank()) {
            return "Unknown device";
        }
        return deviceName.length() > 255 ? deviceName.substring(0, 255) : deviceName;
    }

    public boolean shouldRequireTwoFactor(User user, HttpServletRequest request) {
        if (user.isTwoFactorEnabled()) {
            return true;
        }
        if (!isAdmin(user)) {
            return false;
        }
        String deviceHash = sha256(resolveDeviceId(request));
        return authSessionRepository
                .findTopByUserIdAndDeviceIdHashAndTrustedDeviceTrueAndRevokedFalseOrderByLastSeenAtDesc(
                        user.getId(), deviceHash)
                .isEmpty();
    }

    public void validateAdminIpPolicyBeforeLogin(User user, HttpServletRequest request) {
        if (!adminIpLockEnabled || !isAdmin(user)) {
            return;
        }
        String deviceHash = sha256(resolveDeviceId(request));
        String currentIp = resolveClientIp(request);
        authSessionRepository
                .findTopByUserIdAndDeviceIdHashAndTrustedDeviceTrueAndRevokedFalseOrderByLastSeenAtDesc(
                        user.getId(), deviceHash)
                .ifPresent(session -> {
                    if (session.getIpAddress() != null
                            && shouldEnforceAdminIpLock(session.getIpAddress(), currentIp)
                            && !isEquivalentIp(session.getIpAddress(), currentIp)) {
                        throw new RuntimeException("Admin chỉ được đăng nhập từ IP đã xác thực trước đó");
                    }
                });
    }

    @Transactional
    public LoginResponse issueLoginTokens(User user, boolean rememberMe, HttpServletRequest request, String message) {
        String rawRefreshToken = generateRefreshToken();
        String refreshTokenHash = sha256(rawRefreshToken);
        String deviceIdHash = sha256(resolveDeviceId(request));
        String ip = resolveClientIp(request);
        LocalDateTime now = LocalDateTime.now();

        if (isAdmin(user)) {
            revokeAllActiveSessions(user.getId());
        } else {
            revokeByDevice(user.getId(), deviceIdHash);
        }

        AuthSession session = new AuthSession();
        session.setUser(user);
        session.setDeviceIdHash(deviceIdHash);
        session.setDeviceName(resolveDeviceName(request));
        session.setIpAddress(ip);
        session.setRefreshTokenHash(refreshTokenHash);
        session.setRefreshTokenExpiresAt(now.plusNanos(resolveRefreshExpiration(rememberMe) * 1_000_000L));
        session.setTrustedDevice(true);
        session.setRevoked(false);
        session.setCreatedAt(now);
        session.setLastSeenAt(now);
        AuthSession saved = authSessionRepository.save(session);

        String accessToken = jwtUtil.generateToken(user.getUsername(), rememberMe, saved.getId());
        Set<String> roles = user.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet());

        return new LoginResponse(
                message,
                accessToken,
                rawRefreshToken,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getBirthDate(),
                roles);
    }

    @Transactional
    public LoginResponse refresh(String refreshToken, HttpServletRequest request) {
        String refreshHash = sha256(refreshToken);
        AuthSession session = authSessionRepository
                .findByRefreshTokenHashAndRevokedFalseAndRefreshTokenExpiresAtAfter(refreshHash, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Refresh token không hợp lệ hoặc đã hết hạn"));

        String deviceHash = sha256(resolveDeviceId(request));
        if (!session.getDeviceIdHash().equals(deviceHash)) {
            throw new RuntimeException("Thiết bị không khớp với phiên đăng nhập");
        }

        String currentIp = resolveClientIp(request);
        if (adminIpLockEnabled && isAdmin(session.getUser())
            && session.getIpAddress() != null
            && shouldEnforceAdminIpLock(session.getIpAddress(), currentIp)
            && !isEquivalentIp(session.getIpAddress(), currentIp)) {
            session.setRevoked(true);
            authSessionRepository.save(session);
            throw new RuntimeException("Phiên admin bị chặn do thay đổi IP");
        }

        String newRefreshToken = generateRefreshToken();
        session.setRefreshTokenHash(sha256(newRefreshToken));
        session.setRefreshTokenExpiresAt(
                LocalDateTime.now().plusNanos(refreshRememberMeExpiration * 1_000_000L));
        session.setLastSeenAt(LocalDateTime.now());
        if (!isAdmin(session.getUser())
            || !shouldEnforceAdminIpLock(session.getIpAddress(), currentIp)) {
            session.setIpAddress(currentIp);
        }
        authSessionRepository.save(session);

        String newAccessToken = jwtUtil.generateToken(session.getUser().getUsername(), true, session.getId());
        Set<String> roles = session.getUser().getRoles().stream().map(Role::getName)
                .collect(java.util.stream.Collectors.toSet());

        return new LoginResponse(
                "Làm mới phiên đăng nhập thành công",
                newAccessToken,
                newRefreshToken,
                session.getUser().getId(),
                session.getUser().getUsername(),
                session.getUser().getEmail(),
                session.getUser().getFullName(),
                session.getUser().getPhone(),
                session.getUser().getBirthDate(),
                roles);
    }

    @Transactional
    public void revokeByRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        authSessionRepository
                .findByRefreshTokenHashAndRevokedFalseAndRefreshTokenExpiresAtAfter(sha256(refreshToken), LocalDateTime.now())
                .ifPresent(session -> {
                    session.setRevoked(true);
                    authSessionRepository.save(session);
                });
    }

    @Transactional(readOnly = true)
    public boolean isSessionValid(Long sessionId, String username, HttpServletRequest request) {
        if (sessionId == null) {
            return false;
        }

        AuthSession session = authSessionRepository
                .findByIdAndRevokedFalseAndRefreshTokenExpiresAtAfter(sessionId, LocalDateTime.now())
                .orElse(null);
        if (session == null || session.getUser() == null || !session.getUser().getUsername().equals(username)) {
            return false;
        }

        if (!adminIpLockEnabled || !isAdmin(session.getUser())) {
            return true;
        }

        String currentIp = resolveClientIp(request);
        if (session.getIpAddress() != null
                && shouldEnforceAdminIpLock(session.getIpAddress(), currentIp)
                && !isEquivalentIp(session.getIpAddress(), currentIp)) {
            session.setRevoked(true);
            authSessionRepository.save(session);
            return false;
        }
        return true;
    }

    public List<TrustedDeviceResponse> listTrustedDevices(Long userId) {
        return authSessionRepository.findByUserIdAndTrustedDeviceTrueOrderByLastSeenAtDesc(userId)
                .stream()
                .map(this::toTrustedDeviceResponse)
                .toList();
    }

    public List<TrustedDeviceResponse> listTrustedDevicesForActor(User actor) {
        List<AuthSession> sessions;
        if (isAdmin(actor)) {
            sessions = authSessionRepository.findByTrustedDeviceTrueOrderByLastSeenAtDesc();
        } else {
            sessions = authSessionRepository.findByUserIdAndTrustedDeviceTrueOrderByLastSeenAtDesc(actor.getId());
        }

        return sessions.stream()
                .map(this::toTrustedDeviceResponse)
                .toList();
    }

    @Transactional
    public void revokeTrustedDevice(Long userId, Long sessionId) {
        AuthSession session = authSessionRepository.findByIdAndUserIdAndTrustedDeviceTrue(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thiết bị cần thu hồi"));
        session.setRevoked(true);
        authSessionRepository.save(session);
    }

    @Transactional
    public void revokeTrustedDeviceForActor(User actor, Long sessionId) {
        AuthSession session;
        if (isAdmin(actor)) {
            session = authSessionRepository.findByIdAndTrustedDeviceTrue(sessionId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thiết bị cần thu hồi"));
        } else {
            session = authSessionRepository.findByIdAndUserIdAndTrustedDeviceTrue(sessionId, actor.getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thiết bị cần thu hồi"));
        }

        session.setRevoked(true);
        authSessionRepository.save(session);
    }

    private void revokeAllActiveSessions(Long userId) {
        List<AuthSession> sessions = authSessionRepository.findByUserIdAndRevokedFalse(userId);
        for (AuthSession session : sessions) {
            session.setRevoked(true);
        }
        if (!sessions.isEmpty()) {
            authSessionRepository.saveAll(sessions);
        }
    }

    private void revokeByDevice(Long userId, String deviceIdHash) {
        List<AuthSession> sessions = authSessionRepository.findByUserIdAndDeviceIdHashAndRevokedFalse(userId, deviceIdHash);
        for (AuthSession session : sessions) {
            session.setRevoked(true);
        }
        if (!sessions.isEmpty()) {
            authSessionRepository.saveAll(sessions);
        }
    }

    private long resolveRefreshExpiration(boolean rememberMe) {
        return rememberMe ? refreshRememberMeExpiration : refreshExpiration;
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .map(role -> role.replaceFirst("^ROLE_", "").toUpperCase())
                .anyMatch(role -> role.equals("ADMIN"));
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String normalizeDeviceId(String deviceId) {
        String normalized = deviceId.trim();
        if (normalized.length() > 512) {
            return normalized.substring(0, 512);
        }
        return normalized;
    }

    private String normalizeDevicePart(String value) {
        if (value == null || value.isBlank()) {
            return "na";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() > 128) {
            return normalized.substring(0, 128);
        }
        return normalized;
    }

        private TrustedDeviceResponse toTrustedDeviceResponse(AuthSession session) {
        Set<String> ownerRoles = session.getUser().getRoles().stream()
            .map(Role::getName)
            .map(role -> role.replaceFirst("^ROLE_", "").toUpperCase())
            .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));

        return new TrustedDeviceResponse(
            session.getId(),
            session.getUser().getUsername(),
            session.getUser().getEmail(),
            ownerRoles,
            session.getDeviceName(),
            session.getIpAddress(),
            !session.isRevoked() && session.getRefreshTokenExpiresAt().isAfter(LocalDateTime.now()),
            session.getCreatedAt(),
            session.getLastSeenAt());
        }

    private String generateRefreshToken() {
        byte[] randomBytes = UUID.randomUUID().toString().concat(UUID.randomUUID().toString())
                .getBytes(StandardCharsets.UTF_8);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Không thể mã hóa dữ liệu bảo mật", e);
        }
    }

    private boolean shouldEnforceAdminIpLock(String storedIp, String currentIp) {
        if (!adminIpLockEnabled) {
            return false;
        }
        return !isLocalOrPrivateIp(storedIp) && !isLocalOrPrivateIp(currentIp);
    }

    private boolean isEquivalentIp(String firstIp, String secondIp) {
        return normalizeIp(firstIp).equals(normalizeIp(secondIp));
    }

    private String normalizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "";
        }
        String normalized = ip.trim();
        if (normalized.contains(",")) {
            normalized = normalized.split(",")[0].trim();
        }
        if (normalized.startsWith("::ffff:")) {
            normalized = normalized.substring(7);
        }
        if ("::1".equals(normalized) || "0:0:0:0:0:0:0:1".equals(normalized)) {
            return "127.0.0.1";
        }
        int zoneIndex = normalized.indexOf('%');
        if (zoneIndex > 0) {
            normalized = normalized.substring(0, zoneIndex);
        }
        return normalized;
    }

    private boolean isLocalOrPrivateIp(String ip) {
        String normalized = normalizeIp(ip);
        if (normalized.isBlank()) {
            return true;
        }
        if (normalized.equals("127.0.0.1") || normalized.equals("localhost")) {
            return true;
        }
        if (normalized.startsWith("10.") || normalized.startsWith("192.168.")) {
            return true;
        }
        if (normalized.startsWith("172.")) {
            String[] parts = normalized.split("\\.");
            if (parts.length >= 2) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return false;
    }
}