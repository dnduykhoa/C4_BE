package j2ee_backend.nhom05.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import j2ee_backend.nhom05.config.MomoConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class MomoService {

    @Autowired
    private MomoConfig config;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Tạo URL thanh toán MoMo (captureWallet).
     * Gọi MoMo API v2, trả về payUrl để redirect trình duyệt.
     *
     * @param orderCode mã đơn hàng
     * @param amount    tổng tiền (VND)
     */
    public String createPaymentUrl(String orderCode, BigDecimal amount) {
        try {
            String requestId = UUID.randomUUID().toString();
            String orderInfo = "Thanh toan don hang " + orderCode;
            String extraData = "";
            long amountLong = amount.longValue();

            // rawSignature theo chuẩn MoMo v2
            String rawSignature = "accessKey=" + config.getAccessKey()
                + "&amount=" + amountLong
                + "&extraData=" + extraData
                + "&ipnUrl=" + config.getIpnUrl()
                + "&orderId=" + orderCode
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + config.getPartnerCode()
                + "&redirectUrl=" + config.getReturnUrl()
                + "&requestId=" + requestId
                + "&requestType=" + config.getRequestType();

            String signature = hmacSHA256(config.getSecretKey(), rawSignature);

            // Build request body
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("partnerCode", config.getPartnerCode());
            requestBody.put("requestType", config.getRequestType());
            requestBody.put("ipnUrl", config.getIpnUrl());
            requestBody.put("redirectUrl", config.getReturnUrl());
            requestBody.put("orderId", orderCode);
            requestBody.put("amount", amountLong);
            requestBody.put("orderInfo", orderInfo);
            requestBody.put("requestId", requestId);
            requestBody.put("extraData", extraData);
            requestBody.put("lang", "vi");
            requestBody.put("signature", signature);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(config.getApiUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> httpResponse = httpClient.send(
                httpRequest, HttpResponse.BodyHandlers.ofString());

            JsonNode responseJson = objectMapper.readTree(httpResponse.body());
            int resultCode = responseJson.path("resultCode").asInt(-1);

            if (resultCode == 0) {
                return responseJson.path("payUrl").asText();
            } else {
                String message = responseJson.path("message").asText("Lỗi tạo thanh toán MoMo");
                throw new RuntimeException("MoMo API lỗi [" + resultCode + "]: " + message);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi kết nối MoMo API: " + e.getMessage(), e);
        }
    }

    /**
     * Xác minh chữ ký từ MoMo callback/IPN.
     * rawSignature theo thứ tự key alphabet chuẩn MoMo v2.
     */
    public boolean verifyCallback(Map<String, String> params) {
        String receivedSignature = params.getOrDefault("signature", "");
        if (receivedSignature.isBlank()) return false;

        String rawSignature = "accessKey=" + config.getAccessKey()
            + "&amount=" + params.getOrDefault("amount", "")
            + "&extraData=" + params.getOrDefault("extraData", "")
            + "&message=" + params.getOrDefault("message", "")
            + "&orderId=" + params.getOrDefault("orderId", "")
            + "&orderInfo=" + params.getOrDefault("orderInfo", "")
            + "&orderType=" + params.getOrDefault("orderType", "")
            + "&partnerCode=" + params.getOrDefault("partnerCode", "")
            + "&payType=" + params.getOrDefault("payType", "")
            + "&requestId=" + params.getOrDefault("requestId", "")
            + "&responseTime=" + params.getOrDefault("responseTime", "")
            + "&resultCode=" + params.getOrDefault("resultCode", "")
            + "&transId=" + params.getOrDefault("transId", "");

        String computedSignature = hmacSHA256(config.getSecretKey(), rawSignature);
        return computedSignature.equalsIgnoreCase(receivedSignature);
    }

    // ── Helper HMAC-SHA256 ────────────────────────────────────────────────────
    private String hmacSHA256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tính HMAC-SHA256", e);
        }
    }
}
