package j2ee_backend.nhom05.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import j2ee_backend.nhom05.dto.UserNotificationResponse;
import j2ee_backend.nhom05.model.NotificationType;
import j2ee_backend.nhom05.model.ProductQuestion;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.model.UserNotification;
import j2ee_backend.nhom05.repository.IUserNotificationRepository;

@Service
public class NotificationService {

    @Autowired
    private IUserNotificationRepository notificationRepository;

    @Autowired
    private SseService sseService;

    public void notifyQuestionAnswered(ProductQuestion question) {
        User customer = question.getCustomer();
        String productName = question.getProduct().getName();

        UserNotification entity = new UserNotification();
        entity.setUser(customer);
        entity.setType(NotificationType.PRODUCT_QA_REPLY);
        entity.setTitle("Câu hỏi của bạn đã được phản hồi");
        entity.setContent("Sản phẩm \"" + productName + "\" vừa có phản hồi mới.");
        entity.setReferenceUrl("/products/" + question.getProduct().getId() + "#qna-" + question.getId());
        entity.setIsRead(false);

        UserNotification saved = notificationRepository.save(entity);

        Map<String, Object> eventPayload = Map.of(
            "notificationId", saved.getId(),
            "type", saved.getType().name(),
            "title", saved.getTitle(),
            "content", saved.getContent(),
            "referenceUrl", saved.getReferenceUrl(),
            "createdAt", saved.getCreatedAt().toString()
        );

        sseService.sendToUser(customer.getId(), "qna-answer", eventPayload);
    }

    public List<UserNotificationResponse> getMyNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<UserNotificationResponse> getMyUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDescIdDesc(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public UserNotificationResponse markAsRead(Long notificationId, Long userId) {
        UserNotification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền thao tác thông báo này");
        }

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());

        return toResponse(notificationRepository.save(notification));
    }

    public void markAllAsRead(Long userId) {
        List<UserNotification> notifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDescIdDesc(userId);
        LocalDateTime now = LocalDateTime.now();
        notifications.forEach(item -> {
            item.setIsRead(true);
            item.setReadAt(now);
        });
        notificationRepository.saveAll(notifications);
    }

    private UserNotificationResponse toResponse(UserNotification entity) {
        return new UserNotificationResponse(
            entity.getId(),
            entity.getType(),
            entity.getTitle(),
            entity.getContent(),
            entity.getReferenceUrl(),
            entity.getIsRead(),
            entity.getCreatedAt(),
            entity.getReadAt()
        );
    }
}
