package j2ee_backend.nhom05.service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import j2ee_backend.nhom05.config.VnpayConfig;

@Service
public class VnpayService {

    @Autowired
    private VnpayConfig config;

    /**
     * Tạo URL thanh toán VNPay theo chuẩn v2.1.0.
     * Build đầy đủ tham số, sắp xếp theo alphabet, ký HMAC-SHA512.
     *
     * @param orderCode   mã đơn hàng
     * @param amount      tổng tiền (VND)
     * @param ipAddr      IP người dùng
     * @return Payment URL cho client redirect
     */
    public String createPaymentUrl(String orderCode, BigDecimal amount, String ipAddr) {
        try {
            // Tính toán thời gian — VNPay yêu cầu múi giờ Việt Nam (GMT+7)
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            String createDate = sdf.format(new Date());
            
            // Hết hạn sau 15 phút
            long expireTime = System.currentTimeMillis() + 15 * 60 * 1000;
            String expireDate = sdf.format(new Date(expireTime));

            // Build tham số (sử dụng LinkedHashMap để dễ sắp xếp)
            Map<String, String> vnpParams = new LinkedHashMap<>();
            vnpParams.put("vnp_Version", "2.1.0");
            vnpParams.put("vnp_Command", "pay");
            vnpParams.put("vnp_TmnCode", config.getTmnCode());
            vnpParams.put("vnp_Amount", String.valueOf(amount.multiply(BigDecimal.valueOf(100)).longValue()));
            vnpParams.put("vnp_BankCode", "");
            vnpParams.put("vnp_CreateDate", createDate);
            vnpParams.put("vnp_CurrCode", "VND");
            vnpParams.put("vnp_ExpireDate", expireDate);
            vnpParams.put("vnp_IpAddr", ipAddr != null ? ipAddr : "0.0.0.0");
            vnpParams.put("vnp_Locale", "vi");
            vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + orderCode);
            vnpParams.put("vnp_OrderType", "other");
            vnpParams.put("vnp_ReturnUrl", config.getReturnUrl());
            vnpParams.put("vnp_TxnRef", orderCode);

            // Sắp xếp tham số theo alphabet
            List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
            Collections.sort(fieldNames);

            // Build hashData và queryString với URL-encoded values theo chuẩn VNPay
            StringBuilder hashData = new StringBuilder();
            StringBuilder queryString = new StringBuilder();
            boolean isFirst = true;

            for (String fieldName : fieldNames) {
                String fieldValue = vnpParams.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.UTF_8);
                    if (!isFirst) {
                        hashData.append('&');
                        queryString.append('&');
                    }
                    hashData.append(fieldName).append('=').append(encodedValue);
                    queryString.append(fieldName).append('=').append(encodedValue);
                    isFirst = false;
                }
            }

            // Ký HMAC-SHA512
            String secureHash = hmacSHA512(config.getHashSecret(), hashData.toString());
            queryString.append("&vnp_SecureHash=").append(secureHash);

            return config.getBaseUrl() + "?" + queryString.toString();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo URL thanh toán VNPay: " + e.getMessage(), e);
        }
    }

    /**
     * Xác minh chữ ký từ VNPay callback.
     * Tính lại hashData và so sánh với vnp_SecureHash nhận được.
     *
     * @param params tham số từ callback
     * @return true nếu chữ ký hợp lệ
     */
    public boolean verifyCallback(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) {
            return false;
        }

        // Loại bỏ vnp_SecureHash và vnp_SecureHashType khỏi params
        Map<String, String> verifyParams = new LinkedHashMap<>(params);
        verifyParams.remove("vnp_SecureHash");
        verifyParams.remove("vnp_SecureHashType");

        // Sắp xếp theo alphabet
        List<String> fieldNames = new ArrayList<>(verifyParams.keySet());
        Collections.sort(fieldNames);

        // Build hashData với URL-encoded values (đồng nhất với cách VNPay tính)
        StringBuilder hashData = new StringBuilder();
        boolean isFirst = true;
        for (String fieldName : fieldNames) {
            String fieldValue = verifyParams.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                try {
                    String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.UTF_8);
                    if (!isFirst) hashData.append('&');
                    hashData.append(fieldName).append('=').append(encodedValue);
                    isFirst = false;
                } catch (Exception ignored) {}
            }
        }

        // Tính hash và so sánh
        String computedHash = hmacSHA512(config.getHashSecret(), hashData.toString());
        return computedHash.equalsIgnoreCase(receivedHash);
    }

    // ── Helper HMAC-SHA512 ────────────────────────────────────────────────────
    private String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tính HMAC-SHA512", e);
        }
    }
}
