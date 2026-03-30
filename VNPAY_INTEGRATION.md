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

---

## Task 1: ✅ Config VnpayConfig + Implement createPaymentUrl()

### Cac file da tao/cap nhat:

#### 1. `src/main/java/j2ee_backend/nhom05/config/VnpayConfig.java`
- Component theo pattern ConfigurationProperties voi prefix "vnpay"
- Cac properties: tmnCode, hashSecret, baseUrl, returnUrl, frontendUrl
- Dung @ConfigurationProperties de binding voi application.properties tu dong

#### 2. `src/main/resources/application.properties`
- Config sandbox VNPay:
  - vnpay.tmn-code=TMNCODE (lay tu VNPay sandbox)
  - vnpay.hash-secret=HASKSECRET (lay tu VNPay sandbox)
  - vnpay.base-url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
  - vnpay.return-url=http://localhost:8080/api/vnpay/callback
  - vnpay.frontend-url=http://localhost:3000

#### 3. `src/main/java/j2ee_backend/nhom05/service/VnpayService.java`
- **createPaymentUrl(orderCode, amount, ipAddr)**: Tao URL thanh toan VNPay
  - Build day du tham so theo chuan VNPay v2.1.0:
    - vnp_Version, vnp_Command, vnp_TmnCode, vnp_Amount (× 100)
    - vnp_CreateDate, vnp_ExpireDate (15 phut sau)
    - vnp_IpAddr, vnp_Locale, vnp_OrderInfo, vnp_ReturnUrl, vnp_TxnRef
  - Sap xep tham so theo alphabet
  - Ky HMAC-SHA512 tao vnp_SecureHash
  - Return: Payment URL ==> http://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Version=...&vnp_SecureHash=...
  
- **verifyCallback(params)**: Xac minh chu ky tu VNPay callback
  - Loai bo vnp_SecureHash khoi params
  - Sap xep theo alphabet, tong hash va so sanh

### Cach test:
1. Thay TMNCODE va HASKSECRET bang gia tri tu VNPay sandbox
2. Tao don with paymentMethod=VNPAY
3. OrderController goi VnpayService.createPaymentUrl()
4. Client redirect den URL -> trang thanh toan sandbox VNPay se hien thi

---

## Task 2: ✅ Implement VNPay callback handler + Order confirmation logic

### Mo ta:
VNPay redirect ve GET /api/vnpay/callback sau khi nguoi dung thanh toan. Backend verify lai chu ky (HMAC-SHA512), kiem tra vnp_ResponseCode de xac dinh thanh cong/that bai, cap nhat don hang tuong ung, va redirect browser ve frontend.

### Cac file da tao/cap nhat:

#### 1. `src/main/java/j2ee_backend/nhom05/controller/PaymentController.java`
- Them @Autowired VnpayService de su dung verifyCallback
- **Endpoint /api/vnpay/callback**:
  - Nhan tat ca params tu VNPay (vnp_ResponseCode, vnp_TxnRef, vnp_SecureHash, etc.)
  - Buoc 1: Verify chu ky qua vnpayService.verifyCallback(params)
    - Neu chu ky khong hop le -> redirect failed, khong xu ly gi them
  - Buoc 2: Kiem tra vnp_ResponseCode:
    - "00" = thanh toan thanh cong -> goi orderService.confirmVnpayPayment(orderCode)
    - Khac "00" = that bai/huy -> goi orderService.cancelVnpayPayment(orderCode)
  - Buoc 3: Redirect browser ve frontend:
    - Thanh cong: /orders?vnpay=success&orderCode={orderCode}
    - That bai: /orders?vnpay=failed&code={responseCode}&orderCode={orderCode}
  - Su dung try-catch de tranh loi khi don da duoc xu ly truoc do

```java
@GetMapping("/api/vnpay/callback")
public void vnpayCallback(@RequestParam Map<String, String> params,
                          HttpServletResponse response) throws IOException {
    String responseCode = params.get("vnp_ResponseCode");
    String orderCode    = params.get("vnp_TxnRef");
    boolean isValid     = vnpayService.verifyCallback(params);

    if (isValid && "00".equals(responseCode) && orderCode != null) {
        try {
            orderService.confirmVnpayPayment(orderCode);
        } catch (Exception ignored) {}
        response.sendRedirect(frontendUrl + "/orders?vnpay=success&orderCode=" + orderCode);
    } else {
        if (orderCode != null) {
            try {
                orderService.cancelVnpayPayment(orderCode);
            } catch (Exception ignored) {}
        }
        String code = responseCode != null ? responseCode : "unknown";
        response.sendRedirect(frontendUrl + "/orders?vnpay=failed&code=" + code + "&orderCode=" + orderCode);
    }
}
```

#### 2. `src/main/java/j2ee_backend/nhom05/service/OrderService.java`
- Cap nhat createOrder: set paymentDeadline cho ca VNPAY va MOMO (30 phut)
  
```java
if (paymentMethod == PaymentMethod.MOMO || paymentMethod == PaymentMethod.VNPAY) {
    order.setPaymentDeadline(LocalDateTime.now().plusMinutes(30));
}
```

- **confirmVnpayPayment(orderCode)**: Xu ly khi thanh toan thanh cong
  - Tim don hang theo orderCode
  - Kiem tra status = PENDING
  - Cap nhat: status = CONFIRMED, paymentDeadline = null
  - Xoa gio hang cua user (neu con)
  - Luu vao database

```java
@Transactional
public void confirmVnpayPayment(String orderCode) {
    Order order = orderRepository.findByOrderCode(orderCode)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderCode));
    if (order.getStatus() == OrderStatus.PENDING) {
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentDeadline(null);
        orderRepository.save(order);
        try {
            cartRepository.findByUserId(order.getUser().getId())
                    .ifPresent(cart -> cartRepository.delete(cart));
        } catch (Exception ignored) {}
    }
}
```

- **cancelVnpayPayment(orderCode)**: Xu ly khi thanh toan that bai/huy
  - Tim don hang theo orderCode
  - Kiem tra status = PENDING
  - Gia han paymentDeadline them 30 phut de user co the thanh toan lai
  - Giu nguyen status = PENDING

```java
@Transactional
public void cancelVnpayPayment(String orderCode) {
    orderRepository.findByOrderCode(orderCode).ifPresent(order -> {
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setPaymentDeadline(LocalDateTime.now().plusMinutes(30));
            orderRepository.save(order);
        }
    });
}
```

- Cap nhat **retryPayment**: Ho tro ca VNPAY va MOMO

```java
if (order.getPaymentMethod() != PaymentMethod.MOMO && 
    order.getPaymentMethod() != PaymentMethod.VNPAY) {
    throw new RuntimeException("Đơn hàng này không áp dụng thanh toán lại online");
}
```

- Cap nhat **expireUnpaidOrders**: Tu dong huy don VNPAY/MOMO qua han

```java
List<Long> expiredIds = orderRepository.findExpiredUnpaidOrderIds(
    OrderStatus.PENDING,
    List.of(PaymentMethod.MOMO, PaymentMethod.VNPAY),
    LocalDateTime.now()
);
```

### Luong xu ly callback:
1. User thanh toan tai VNPay sandbox
2. VNPay redirect browser ve /api/vnpay/callback voi cac params (vnp_ResponseCode, vnp_TxnRef, vnp_SecureHash, etc.)
3. Backend verify chu ky bang cach tai tinh HMAC-SHA512 va so voi vnp_SecureHash
4. Neu chu ky hop le va responseCode = "00":
   - Goi confirmVnpayPayment -> cap nhat don PENDING -> CONFIRMED
   - Xoa paymentDeadline
   - Xoa gio hang
   - Redirect: /orders?vnpay=success
5. Neu chu ky hop le nhung responseCode != "00" (that bai/huy):
   - Goi cancelVnpayPayment -> gia han deadline them 30 phut
   - Giu PENDING de user co the retry
   - Redirect: /orders?vnpay=failed&code={code}
6. Neu chu ky khong hop le (gia mao):
   - Khong xu ly gi, chi redirect failed
   - Don hang khong bi thay doi

### Cach test:
1. Tao don voi paymentMethod=VNPAY
2. Nhan vnpayUrl tu response va mo trong browser
3. **Test thanh cong**: Thanh toan thanh cong tren sandbox VNPay
   - Kiem tra don chuyen tu PENDING -> CONFIRMED
   - Kiem tra paymentDeadline = null
   - Kiem tra redirect ve /orders?vnpay=success
4. **Test that bai**: Huy thanh toan tren sandbox VNPay
   - Kiem tra don van la PENDING
   - Kiem tra paymentDeadline duoc gia han
   - Kiem tra redirect ve /orders?vnpay=failed
5. **Test gia mao**: Goi truc tiep /api/vnpay/callback voi params tu y
   - Kiem tra chu ky verify that bai
   - Kiem tra don khong bi thay doi
   - Kiem tra redirect ve failed

### Dieu kien hoan thanh:
- ✅ Thanh toan thanh cong tren sandbox → don chuyen CONFIRMED (PAID)
- ✅ Huy tren sandbox → don xu ly dung (giu PENDING, gia han deadline)
- ✅ Gia mao callback → khong co gi thay doi (verify that bai)
