package j2ee_backend.nhom05.repository;

import j2ee_backend.nhom05.model.TwoFactorCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ITwoFactorCodeRepository extends JpaRepository<TwoFactorCode, Long> {

    @Query("SELECT t FROM TwoFactorCode t WHERE t.emailOrPhone = :emailOrPhone AND t.code = :code AND t.used = false AND t.expiryDate > :now")
    Optional<TwoFactorCode> findValidCode(
        @Param("emailOrPhone") String emailOrPhone,
        @Param("code") String code,
        @Param("now") LocalDateTime now
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM TwoFactorCode t WHERE t.emailOrPhone = :emailOrPhone")
    void deleteByEmailOrPhone(@Param("emailOrPhone") String emailOrPhone);
}
