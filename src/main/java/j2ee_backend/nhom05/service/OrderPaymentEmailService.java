package j2ee_backend.nhom05.service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import j2ee_backend.nhom05.model.Order;
import j2ee_backend.nhom05.model.PaymentMethod;
import jakarta.mail.internet.MimeMessage;

@Service
public class OrderPaymentEmailService {

    private static final Logger log = LoggerFactory.getLogger(OrderPaymentEmailService.class);
    private static final DateTimeFormatter DEADLINE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final EmailService emailService;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public OrderPaymentEmailService(ObjectProvider<JavaMailSender> mailSenderProvider, EmailService emailService) {
        this.mailSenderProvider = mailSenderProvider;
        this.emailService = emailService;
    }

    public void sendOrderCreatedEmail(Order order) {
        if (order == null) {
            return;
        }

        String recipient = resolveRecipient(order);
        if (recipient == null) {
            log.warn("Skip order-created email because recipient is missing for order {}", order.getOrderCode());
            return;
        }

        try {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender == null) {
                log.warn("Skip sending order-created email because JavaMailSender is not configured");
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            if (fromEmail != null && !fromEmail.isBlank()) {
                helper.setFrom(fromEmail.trim());
            }
            helper.setTo(recipient);
            helper.setSubject("TechStore - Đặt hàng thành công #" + safeText(order.getOrderCode()));

            String customerName = (order.getFullName() != null && !order.getFullName().isBlank())
                    ? escapeHtml(order.getFullName())
                    : "bạn";
            String orderCode = escapeHtml(order.getOrderCode());
            String paymentLabel = escapeHtml(resolvePaymentLabel(order.getPaymentMethod()));
            String shippingAddress = escapeHtml(order.getShippingAddress());
            String phone = escapeHtml(order.getPhone());
            String amountText = formatAmount(order.getTotalAmount());
            String itemsTableHtml = emailService.buildItemsTableHtmlFromOrderItems(order.getItems());

            String htmlContent =
                    "<div style='font-family: Arial, sans-serif; background-color:#f4f6f8; padding:30px;'>"
                    + "<div style='max-width:620px; margin:auto; background:white; border-radius:8px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.1);'>"
                    + "<div style='background:#0f766e; color:white; padding:20px; text-align:center;'>"
                    + "<h2 style='margin:0;'>TechStore</h2>"
                    + "<p style='margin:4px 0 0 0; font-size:14px;'>Xác nhận đặt hàng thành công</p>"
                    + "</div>"
                    + "<div style='padding:28px;'>"
                    + "<p style='margin:0 0 8px 0;'>Xin chào <b>" + customerName + "</b>,</p>"
                    + "<p style='margin:0 0 16px 0;'>Đơn hàng của bạn đã được tạo thành công trên hệ thống.</p>"
                    + "<div style='background:#f0fdfa; border:1px solid #99f6e4; border-radius:8px; padding:14px 16px; margin-bottom:16px;'>"
                    + "<p style='margin:0 0 6px 0;'><b>Mã đơn hàng:</b> " + orderCode + "</p>"
                    + "<p style='margin:0 0 6px 0;'><b>Phương thức thanh toán:</b> " + paymentLabel + "</p>"
                    + "<p style='margin:0 0 6px 0;'><b>Địa chỉ giao hàng:</b> " + shippingAddress + "</p>"
                    + "<p style='margin:0;'><b>Số điện thoại:</b> " + phone + "</p>"
                    + "</div>"
                    + "<p style='margin:0 0 4px 0; font-weight:600; color:#1e293b;'>Chi tiết đơn hàng</p>"
                    + itemsTableHtml
                    + "<div style='border-top:2px solid #e2e8f0; padding-top:12px; text-align:right; margin-top:12px;'>"
                    + "<p style='margin:0; font-size:15px;'>Tổng tiền sau giảm giá: <b style='color:#e60012; font-size:17px;'>" + amountText + " ₫</b></p>"
                    + "</div>"
                    + "<p style='margin-top:20px;'>Chúng tôi đang xử lý đơn hàng và sẽ giao đến bạn trong thời gian sớm nhất.</p>"
                    + "<p style='margin-top:24px;'>Trân trọng,<br><b>TechStore Team</b></p>"
                    + "</div>"
                    + "<div style='background:#f8fafc; padding:16px; text-align:center; font-size:12px; color:#64748b;'>"
                    + "© 2026 TechStore. All rights reserved."
                    + "</div>"
                    + "</div>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send order-created email for order {}", order.getOrderCode(), ex);
        }
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

    private String resolvePaymentLabel(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            return "Không xác định";
        }
        return switch (paymentMethod) {
            case CASH -> "Tiền mặt (COD)";
            case MOMO -> "MoMo";
            case VNPAY -> "VNPay";
        };
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        formatter.setMaximumFractionDigits(0);
        return formatter.format(amount);
    }

    private String safeText(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }
        return value.trim();
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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
