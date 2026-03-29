package j2ee_backend.nhom05.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import j2ee_backend.nhom05.model.ProductQuestion;

@Repository
public interface IProductQuestionRepository extends JpaRepository<ProductQuestion, Long> {

    List<ProductQuestion> findByProductIdOrderByAskedAtDescIdDesc(Long productId);

    List<ProductQuestion> findByProductIdAndIsAnsweredTrueOrderByAskedAtDescIdDesc(Long productId);

    List<ProductQuestion> findByProductIdAndCustomerIdOrderByAskedAtDescIdDesc(Long productId, Long customerId);

    List<ProductQuestion> findByIsAnsweredOrderByAskedAtDescIdDesc(Boolean isAnswered);

    List<ProductQuestion> findAllByOrderByAskedAtDescIdDesc();
}
