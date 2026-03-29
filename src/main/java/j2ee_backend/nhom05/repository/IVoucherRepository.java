package j2ee_backend.nhom05.repository;

import j2ee_backend.nhom05.model.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IVoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByCodeIgnoreCase(String code);

    List<Voucher> findAllByOrderByCreatedAtDesc();

    List<Voucher> findByIsActiveTrueOrderByCreatedAtDesc();

    boolean existsByCodeIgnoreCase(String code);
}
