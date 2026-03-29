package j2ee_backend.nhom05.repository;

import j2ee_backend.nhom05.model.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IVoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {

    boolean existsByVoucherIdAndUserId(Long voucherId, Long userId);

    Optional<VoucherUsage> findByOrderId(Long orderId);

    int countByVoucherId(Long voucherId);
}
