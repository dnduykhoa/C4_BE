package j2ee_backend.nhom05.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import j2ee_backend.nhom05.model.UserNotification;

@Repository
public interface IUserNotificationRepository extends JpaRepository<UserNotification, Long> {

    List<UserNotification> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    List<UserNotification> findByUserIdAndIsReadFalseOrderByCreatedAtDescIdDesc(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);
}
