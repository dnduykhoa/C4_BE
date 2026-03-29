# Tich hop thanh toan VNPAY (J2EE_backend)

Tai lieu nay liet ke cac doan code tich hop VNPAY theo thu tu a -> z (theo ten file).

## A. src/main/java/j2ee_backend/nhom05/config/SecurityConfig.java
- Vai tro: mo public callback endpoint tu VNPAY.
- Doan code tich hop:

  .requestMatchers(
      "/api/auth/**",
      "/api/vnpay/**",
      "/api/momo/**",
      "/api/sse/**",
      "/images/**"
  ).permitAll()

## B. src/main/java/j2ee_backend/nhom05/config/VnpayConfig.java
- Vai tro: bind cau hinh vnpay.* tu application.properties.
- Doan code tich hop:

  @ConfigurationProperties(prefix = "vnpay")
  public class VnpayConfig {
      private String tmnCode;
      private String hashSecret;
      private String baseUrl;
      private String returnUrl;
      private String frontendUrl;
  }

## C. src/main/java/j2ee_backend/nhom05/controller/OrderController.java
- Vai tro: tao payment URL VNPAY khi dat don va khi retry payment.
- Doan code tich hop:

  if ("VNPAY".equalsIgnoreCase(request.getPaymentMethod())) {
      String ip = resolveClientIp(httpRequest);
      String vnpayUrl = vnpayService.createPaymentUrl(order.getOrderCode(), order.getTotalAmount(), ip);
      order.setVnpayUrl(vnpayUrl);
  }

  if ("VNPAY".equalsIgnoreCase(order.getPaymentMethod())) {
      String ip = resolveClientIp(httpRequest);
      String vnpayUrl = vnpayService.createPaymentUrl(order.getOrderCode(), order.getTotalAmount(), ip);
      order.setVnpayUrl(vnpayUrl);
  }

## D. src/main/java/j2ee_backend/nhom05/controller/PaymentController.java
- Vai tro: nhan callback redirect tu VNPAY, verify chu ky, cap nhat don, redirect ve frontend.
- Doan code tich hop:

  @GetMapping("/api/vnpay/callback")
  public void vnpayCallback(@RequestParam Map<String, String> params, HttpServletResponse response) throws IOException {
      String responseCode = params.get("vnp_ResponseCode");
      String orderCode = params.get("vnp_TxnRef");
      boolean isValid = vnpayService.verifyCallback(params);

      if (isValid && "00".equals(responseCode) && orderCode != null) {
          orderService.confirmVnpayPayment(orderCode);
          response.sendRedirect(frontendUrl + "/orders?vnpay=success&orderCode=" + orderCode);
      } else {
          orderService.cancelVnpayPayment(orderCode);
          response.sendRedirect(frontendUrl + "/orders?vnpay=failed&code=" + code + "&orderCode=" + orderCode);
      }
  }

## E. src/main/java/j2ee_backend/nhom05/dto/OrderResponse.java
- Vai tro: tra ve link thanh toan cho frontend.
- Doan code tich hop:

  private String vnpayUrl;  // Chi co gia tri khi paymentMethod = VNPAY
  private LocalDateTime paymentDeadline;

## F. src/main/java/j2ee_backend/nhom05/model/PaymentMethod.java
- Vai tro: enum phuong thuc thanh toan co VNPAY.
- Doan code tich hop:

  public enum PaymentMethod {
      CASH,
      VNPAY,
      MOMO
  }

## G. src/main/java/j2ee_backend/nhom05/service/OrderService.java
- Vai tro: xu ly trang thai don hang cho luong VNPAY.
- Doan code tich hop:
  - Tao don: set paymentDeadline cho VNPAY/MOMO.
  - confirmVnpayPayment(orderCode): PENDING -> CONFIRMED, xoa deadline, gui email thanh toan thanh cong.
  - cancelVnpayPayment(orderCode): giu PENDING, gia han deadline + gui email nhac thanh toan lai.
  - retryPayment: cho phep thanh toan lai VNPAY trong han, gioi han so lan retry.
  - expireUnpaidOrders: tu dong huy don VNPAY/MOMO qua han.

  if (paymentMethod == PaymentMethod.VNPAY || paymentMethod == PaymentMethod.MOMO) {
      order.setPaymentDeadline(LocalDateTime.now().plusMinutes(30));
  }

  public void confirmVnpayPayment(String orderCode) { ... }
  public void cancelVnpayPayment(String orderCode) { ... }

## H. src/main/java/j2ee_backend/nhom05/service/VnpayService.java
- Vai tro: dong goi toan bo ky thuat VNPAY (tao URL + verify callback).
- Doan code tich hop:

  public String createPaymentUrl(String orderCode, BigDecimal amount, String ipAddr) {
      Map<String, String> vnpParams = new TreeMap<>();
      vnpParams.put("vnp_Version", "2.1.0");
      vnpParams.put("vnp_Command", "pay");
      vnpParams.put("vnp_TmnCode", config.getTmnCode());
      vnpParams.put("vnp_Amount", vnpAmount);
      vnpParams.put("vnp_TxnRef", orderCode);
      vnpParams.put("vnp_ReturnUrl", config.getReturnUrl());
      ...
      String secureHash = hmacSHA512(config.getHashSecret(), hashData.toString());
      queryString.append("&vnp_SecureHash=").append(secureHash);
      return config.getBaseUrl() + "?" + queryString;
  }

  public boolean verifyCallback(Map<String, String> params) {
      String receivedHash = params.get("vnp_SecureHash");
      ...
      String computedHash = hmacSHA512(config.getHashSecret(), hashData.toString());
      return computedHash.equalsIgnoreCase(receivedHash);
  }

## I. src/main/resources/application.properties
- Vai tro: cau hinh thong so VNPAY sandbox.
- Doan code tich hop:

  vnpay.tmn-code=...
  vnpay.hash-secret=...
  vnpay.base-url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
  vnpay.return-url=${app.backend-url}/api/vnpay/callback
  vnpay.frontend-url=${app.frontend-url}

## Tong ket luong tich hop VNPAY
1. User tao don voi paymentMethod=VNPAY.
2. OrderController goi VnpayService.createPaymentUrl va tra ve vnpayUrl.
3. User thanh toan tai VNPAY, VNPAY redirect callback ve /api/vnpay/callback.
4. PaymentController verify callback qua VnpayService.verifyCallback.
5. Thanh cong -> confirmVnpayPayment; that bai/huy -> cancelVnpayPayment.
6. Backend redirect frontend voi ket qua success/failed.
