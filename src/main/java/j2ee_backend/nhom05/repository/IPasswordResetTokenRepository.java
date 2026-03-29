package j2ee_backend.nhom05.repository;

import j2ee_backend.nhom05.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface IPasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    Optional<PasswordResetToken> findByEmailAndTokenAndUsedFalseAndExpiryDateAfter(
        String email, String token, LocalDateTime currentTime);
    
    void deleteByExpiryDateBefore(LocalDateTime currentTime);
    
    void deleteByEmail(String email);
}
