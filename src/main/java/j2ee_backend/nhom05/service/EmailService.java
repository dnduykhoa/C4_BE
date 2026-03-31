package j2ee_backend.nhom05.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import j2ee_backend.nhom05.dto.EmailOrderItem;
import j2ee_backend.nhom05.model.OrderItem;
import j2ee_backend.nhom05.model.Product;
import j2ee_backend.nhom05.model.ProductMedia;
import j2ee_backend.nhom05.model.ProductVariant;
import j2ee_backend.nhom05.model.ProductVariantValue;

@Service
public class EmailService {

    public String buildItemsTableHtml(List<EmailOrderItem> items) {
        if (items == null || items.isEmpty()) {
            return "<p>Không có sản phẩm trong đơn hàng.</p>";
        }

        StringBuilder html = new StringBuilder();
        html.append("<table style=\"width:100%;border-collapse:collapse;font-family:Arial,sans-serif;\">")
                .append("<thead>")
                .append("<tr style=\"background:#f5f5f5;text-align:left;\">")
                .append(columnHeader("Hình ảnh"))
                .append(columnHeader("Sản phẩm"))
                .append(columnHeader("Biến thể"))
                .append(columnHeader("Số lượng"))
                .append(columnHeader("Đơn giá"))
                .append(columnHeader("Thành tiền"))
                .append("</tr>")
                .append("</thead>")
                .append("<tbody>");

        for (EmailOrderItem item : items) {
            html.append(buildTableRow(item));
        }

        html.append("</tbody>")
                .append("</table>");
        return html.toString();
    }

    public String buildItemsTableHtmlFromOrderItems(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return buildItemsTableHtml(new ArrayList<>());
        }
        List<EmailOrderItem> emailItems = orderItems.stream()
                .map(this::mapOrderItem)
                .collect(Collectors.toList());
        return buildItemsTableHtml(emailItems);
    }

    public EmailOrderItem mapOrderItem(OrderItem orderItem) {
        if (orderItem == null) {
            return new EmailOrderItem();
        }

        String imageUrl = resolveImageUrl(orderItem.getProduct(), orderItem.getVariant());
        String productName = orderItem.getProduct() != null ? orderItem.getProduct().getName() : "Sản phẩm";
        String variantName = buildVariantName(orderItem.getVariant());
        Integer quantity = orderItem.getQuantity() != null ? orderItem.getQuantity() : 0;
        BigDecimal unitPrice = orderItem.getUnitPrice() != null ? orderItem.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal lineTotal = orderItem.getSubtotal() != null ? orderItem.getSubtotal() : BigDecimal.ZERO;

        return new EmailOrderItem(imageUrl, productName, variantName, quantity, unitPrice, lineTotal);
    }

    private String columnHeader(String text) {
        return "<th style=\"padding:12px 8px;border:1px solid #dddddd;font-size:13px;color:#333333;\">"
                + escapeHtml(text) + "</th>";
    }

    private String buildTableRow(EmailOrderItem item) {
        StringBuilder row = new StringBuilder();
        row.append("<tr style=\"border-bottom:1px solid #dddddd;\">")
                .append(buildCell(resolveImageCell(item.getImageUrl())))
                .append(buildCell("<strong>" + escapeHtml(item.getProductName()) + "</strong>"))
                .append(buildCell(escapeHtml(item.getVariantName())))
                .append(buildCell("<div style=\"text-align:center;\">" + item.getQuantity() + "</div>"))
                .append(buildCell("<div style=\"text-align:right;\">" + formatMoney(item.getUnitPrice()) + "</div>"))
                .append(buildCell("<div style=\"text-align:right;\">" + formatMoney(item.getLineTotal()) + "</div>"))
                .append("</tr>");
        return row.toString();
    }

    private String buildCell(String content) {
        return "<td style=\"padding:10px 8px;border:1px solid #dddddd;vertical-align:top;\">" + content + "</td>";
    }

    private String resolveImageCell(String imageUrl) {
        if (imageUrl != null && !imageUrl.isBlank()) {
            return "<img src=\"" + escapeHtml(imageUrl)
                    + "\" alt=\"Product Image\" style=\"display:block;width:80px;height:auto;border:0;object-fit:contain;\" />";
        }
        return "<div style=\"width:80px;height:80px;background:#f3f3f3;color:#999999;font-size:12px;display:flex;align-items:center;justify-content:center;border:1px solid #e1e1e1;\">Không có ảnh</div>";
    }

    private String resolveImageUrl(Product product, ProductVariant variant) {
        if (variant != null && variant.getMedia() != null && !variant.getMedia().isEmpty()) {
            return variant.getMedia().stream()
                    .filter(media -> "IMAGE".equalsIgnoreCase(media.getMediaType()))
                    .findFirst()
                    .map(ProductMedia::getMediaUrl)
                    .orElse(null);
        }
        if (product != null && product.getPrimaryImage() != null) {
            return product.getPrimaryImage().getMediaUrl();
        }
        return null;
    }

    private String buildVariantName(ProductVariant variant) {
        if (variant == null) {
            return "Mặc định";
        }
        if (variant.getValues() == null || variant.getValues().isEmpty()) {
            return variant.getSku() != null ? variant.getSku() : "Biến thể";
        }
        return variant.getValues().stream()
                .map(this::formatVariantValue)
                .collect(Collectors.joining(", "));
    }

    private String formatVariantValue(ProductVariantValue value) {
        if (value == null) {
            return "";
        }
        String label = value.getAttributeDefinition() != null
                ? value.getAttributeDefinition().getAttrKey()
                : value.getAttrKey();
        return escapeHtml(label != null ? label : "") + ": " + escapeHtml(value.getDisplayValue());
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "0 ₫";
        }
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        formatter.setMaximumFractionDigits(0);
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        return formatter.format(amount);
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
}
