package j2ee_backend.nhom05.service;

import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import j2ee_backend.nhom05.model.Order;

@Service
public class OrderPaymentEmailService {

    private static final Logger log = LoggerFactory.getLogger(OrderPaymentEmailService.class);
    private static final DateTimeFormatter DEADLINE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public OrderPaymentEmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void sendOrderExpiredCancellationEmail(Order order) {
        String recipient = resolveRecipient(order);
        if (recipient == null) {
            log.warn("Skip expired-order email because recipient is missing for order {}", order.getOrderCode());
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipient);
        message.setSubject("Don hang da bi huy do qua han thanh toan");
        message.setText(buildExpiredOrderBody(order));

        try {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender == null) {
                log.warn("Skip sending expired-order email because JavaMailSender is not configured");
                return;
            }
            mailSender.send(message);
        } catch (MailException ex) {
            // Keep scheduler resilient: logging failure is safer than breaking auto-cancel flow.
            log.error("Failed to send expired-order email for order {}", order.getOrderCode(), ex);
        }
    }

    private String resolveRecipient(Order order) {
        if (order.getEmail() != null && !order.getEmail().isBlank()) {
            return order.getEmail().trim();
        }
        if (order.getUser() != null && order.getUser().getEmail() != null && !order.getUser().getEmail().isBlank()) {
            return order.getUser().getEmail().trim();
        }
        return null;
    }

    private String buildExpiredOrderBody(Order order) {
        StringBuilder builder = new StringBuilder();
        builder.append("Xin chao ");
        builder.append(order.getFullName() != null ? order.getFullName() : "quy khach");
        builder.append(",\n\n");
        builder.append("Don hang ").append(order.getOrderCode());
        builder.append(" da bi huy vi qua han thanh toan");

        if (order.getCancelledAt() != null) {
            builder.append(" luc ")
                    .append(order.getCancelledAt().format(DEADLINE_FORMATTER));
        }

        builder.append(".\n");
        builder.append("Quy khach vui long dat don moi neu van co nhu cau mua hang.\n\n");
        builder.append("Tran trong.");
        return builder.toString();
    }
}
