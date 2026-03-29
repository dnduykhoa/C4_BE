package j2ee_backend.nhom05.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import j2ee_backend.nhom05.model.PreorderRequest;
import j2ee_backend.nhom05.model.PreorderRequestStatus;

@Repository
public interface IPreorderRequestRepository extends JpaRepository<PreorderRequest, Long> {

    List<PreorderRequest> findAllByOrderByCreatedAtDescIdDesc();

    List<PreorderRequest> findByProductIdAndVariantIsNullAndStatusOrderByCreatedAtAscIdAsc(
        Long productId,
        PreorderRequestStatus status
    );

    List<PreorderRequest> findByProductIdAndVariantIdAndStatusOrderByCreatedAtAscIdAsc(
        Long productId,
        Long variantId,
        PreorderRequestStatus status
    );

    List<PreorderRequest> findByStatusInOrderByCreatedAtAscIdAsc(Collection<PreorderRequestStatus> statuses);

    boolean existsByEmailIgnoreCaseAndProductIdAndVariantIsNullAndStatusIn(
        String email,
        Long productId,
        Collection<PreorderRequestStatus> statuses
    );

    boolean existsByEmailIgnoreCaseAndProductIdAndVariantIdAndStatusIn(
        String email,
        Long productId,
        Long variantId,
        Collection<PreorderRequestStatus> statuses
    );
}