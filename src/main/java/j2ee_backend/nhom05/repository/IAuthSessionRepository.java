package j2ee_backend.nhom05.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import j2ee_backend.nhom05.model.AuthSession;

@Repository
public interface IAuthSessionRepository extends JpaRepository<AuthSession, Long> {
    Optional<AuthSession> findByRefreshTokenHashAndRevokedFalseAndRefreshTokenExpiresAtAfter(
            String refreshTokenHash,
            LocalDateTime now);

    Optional<AuthSession> findByIdAndRevokedFalseAndRefreshTokenExpiresAtAfter(Long id, LocalDateTime now);

    Optional<AuthSession> findTopByUserIdAndDeviceIdHashAndTrustedDeviceTrueAndRevokedFalseOrderByLastSeenAtDesc(
            Long userId,
            String deviceIdHash);

    List<AuthSession> findByUserIdAndRevokedFalse(Long userId);

    List<AuthSession> findByUserIdAndDeviceIdHashAndRevokedFalse(Long userId, String deviceIdHash);

        List<AuthSession> findByTrustedDeviceTrueOrderByLastSeenAtDesc();

        List<AuthSession> findByUserIdAndTrustedDeviceTrueOrderByLastSeenAtDesc(Long userId);

        Optional<AuthSession> findByIdAndTrustedDeviceTrue(Long id);

        Optional<AuthSession> findByIdAndUserIdAndTrustedDeviceTrue(Long id, Long userId);
}