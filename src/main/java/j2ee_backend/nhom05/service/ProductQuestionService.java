package j2ee_backend.nhom05.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import j2ee_backend.nhom05.config.RoleAccess;
import j2ee_backend.nhom05.dto.ProductQuestionResponse;
import j2ee_backend.nhom05.model.Product;
import j2ee_backend.nhom05.model.ProductQuestion;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.repository.IProductQuestionRepository;
import j2ee_backend.nhom05.repository.IProductRepository;
import j2ee_backend.nhom05.repository.IUserRepository;

@Service
public class ProductQuestionService {

    @Autowired
    private IProductQuestionRepository productQuestionRepository;

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    public ProductQuestionResponse askQuestion(Long productId, String content, UserDetails principal) {
        if (principal == null) {
            throw new RuntimeException("Vui lòng đăng nhập để đặt câu hỏi");
        }

        if (RoleAccess.isBackoffice(principal)) {
            throw new RuntimeException("Tài khoản backoffice không thể tạo câu hỏi sản phẩm");
        }

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        User customer = (User) principal;

        ProductQuestion entity = new ProductQuestion();
        entity.setProduct(product);
        entity.setCustomer(customer);
        entity.setQuestion(content.trim());
        entity.setIsAnswered(false);

        return toResponse(productQuestionRepository.save(entity));
    }

    public List<ProductQuestionResponse> getQuestionsByProduct(Long productId, UserDetails principal) {
        if (!productRepository.existsById(productId)) {
            throw new RuntimeException("Không tìm thấy sản phẩm");
        }

        if (principal != null && RoleAccess.isBackoffice(principal)) {
            return productQuestionRepository.findByProductIdOrderByAskedAtDescIdDesc(productId).stream()
                .map(this::toResponse)
                .toList();
        }

        List<ProductQuestion> publicAnswered = productQuestionRepository
            .findByProductIdAndIsAnsweredTrueOrderByAskedAtDescIdDesc(productId);

        if (principal == null) {
            return publicAnswered.stream().map(this::toResponse).toList();
        }

        User customer = (User) principal;
        List<ProductQuestion> ownQuestions = productQuestionRepository
            .findByProductIdAndCustomerIdOrderByAskedAtDescIdDesc(productId, customer.getId());

        List<ProductQuestion> merged = new ArrayList<>(publicAnswered);
        for (ProductQuestion own : ownQuestions) {
            if (merged.stream().noneMatch(item -> item.getId().equals(own.getId()))) {
                merged.add(own);
            }
        }

        merged.sort((a, b) -> {
            int compareAskedAt = b.getAskedAt().compareTo(a.getAskedAt());
            if (compareAskedAt != 0) {
                return compareAskedAt;
            }
            return b.getId().compareTo(a.getId());
        });

        return merged.stream().map(this::toResponse).toList();
    }

    public List<ProductQuestionResponse> getAdminQuestions(Boolean answered) {
        List<ProductQuestion> questions;

        if (answered == null) {
            questions = productQuestionRepository.findAllByOrderByAskedAtDescIdDesc();
        } else {
            questions = productQuestionRepository.findByIsAnsweredOrderByAskedAtDescIdDesc(answered);
        }

        return questions.stream().map(this::toResponse).toList();
    }

    public ProductQuestionResponse answerQuestion(Long questionId, String answer, UserDetails principal) {
        if (principal == null || !RoleAccess.hasAnyRole(principal, "STAFF")) {
            throw new RuntimeException("Bạn không có quyền phản hồi câu hỏi");
        }

        ProductQuestion entity = productQuestionRepository.findById(questionId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy câu hỏi"));

        User answeredBy = userRepository.findById(((User) principal).getId())
            .orElseThrow(() -> new RuntimeException("Không tìm thấy người phản hồi"));

        entity.setAnswer(answer.trim());
        entity.setIsAnswered(true);
        entity.setAnsweredBy(answeredBy);
        entity.setAnsweredAt(LocalDateTime.now());

        ProductQuestion saved = productQuestionRepository.save(entity);
        notificationService.notifyQuestionAnswered(saved);

        return toResponse(saved);
    }

    private ProductQuestionResponse toResponse(ProductQuestion entity) {
        String customerName = entity.getCustomer().getFullName() != null && !entity.getCustomer().getFullName().isBlank()
            ? entity.getCustomer().getFullName()
            : entity.getCustomer().getUsername();

        String answeredByName = null;
        if (entity.getAnsweredBy() != null) {
            answeredByName = entity.getAnsweredBy().getFullName() != null && !entity.getAnsweredBy().getFullName().isBlank()
                ? entity.getAnsweredBy().getFullName()
                : entity.getAnsweredBy().getUsername();
        }

        return new ProductQuestionResponse(
            entity.getId(),
            entity.getProduct().getId(),
            entity.getProduct().getName(),
            entity.getCustomer().getId(),
            customerName,
            entity.getQuestion(),
            entity.getAnswer(),
            entity.getIsAnswered(),
            entity.getAskedAt(),
            entity.getAnsweredAt(),
            entity.getAnsweredBy() != null ? entity.getAnsweredBy().getId() : null,
            answeredByName
        );
    }
}
