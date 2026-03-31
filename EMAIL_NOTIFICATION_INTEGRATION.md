# Tich hop gui email thong bao (J2EE_backend)

Tai lieu nay liet ke cac doan code tich hop gui email theo thu tu a -> z (theo ten file).

## A. pom.xml
- Vai tro: khai bao dependency email.
- Doan code tich hop:

  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
  </dependency>

## B. src/main/java/j2ee_backend/nhom05/service/AuthService.java
- Vai tro: trigger email trong luong auth.
- Doan code tich hop:

  // Gui ma 2FA khi dang nhap
  emailService.sendTwoFactorCode(user.getEmail(), code);

  // Gui ma reset password
  emailService.sendPasswordResetEmail(request.getEmail(), token);

- Cac luong email tu AuthService:
  - Dang nhap 2 buoc (2FA): gui OTP 6 so.
  - Quen mat khau: gui ma reset 6 so.

## C. src/main/java/j2ee_backend/nhom05/service/EmailService.java
- Vai tro: service trung tam de tao va gui email HTML.
- Doan code tich hop cot loi:

  @Autowired
  private JavaMailSender mailSender;

  @Value("${spring.mail.username}")
  private String fromEmail;

  MimeMessage message = mailSender.createMimeMessage();
  MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
  helper.setFrom(fromEmail);
  helper.setTo(toEmail);
  helper.setSubject("...");
  helper.setText(htmlContent, true);
  mailSender.send(message);

- Cac ham gui email da duoc tich hop:
  - sendPasswordResetEmail
  - sendTwoFactorCode
  - sendPaymentPendingEmail
  - sendOrderConfirmationEmail
  - sendOrderCancelledEmail
  - sendPaymentConfirmedEmail
  - sendPreorderConfirmationEmail
  - sendPreorderAvailableEmail
  - sendPaymentExpiredEmail

- Ham phu tro:
  - EmailOrderItem: DTO noi bo de render san pham trong email.
  - buildItemsTableHtml: render bang HTML danh sach san pham.

## D. src/main/java/j2ee_backend/nhom05/service/OrderService.java
- Vai tro: trigger email theo vong doi don hang.
- Doan code tich hop:

  // Don COD dat thanh cong
  emailService.sendOrderConfirmationEmail(...);

  // Huy don (user/admin/tu dong)
  emailService.sendOrderCancelledEmail(...);

  // Thanh toan online thanh cong (VNPAY/MoMo)
  emailService.sendPaymentConfirmedEmail(...);

  // Thanh toan online that bai/chua xong
  emailService.sendPaymentPendingEmail(...);

  // Qua han thanh toan online
  emailService.sendPaymentExpiredEmail(...);

- Cac diem goi cu the:
  - createOrder: gui email xac nhan dat hang (CASH).
  - updateOrderStatus/cancelOrder: gui email huy don.
  - confirmVnpayPayment, confirmMomoPayment: gui email thanh toan thanh cong.
  - cancelVnpayPayment, cancelMomoPayment: gui email nhac thanh toan lai.
  - expireUnpaidOrders: gui email het han thanh toan.

## E. src/main/java/j2ee_backend/nhom05/service/PreorderRequestService.java
- Vai tro: trigger email cho chuc nang cho hang.
- Doan code tich hop:

  // Tao yeu cau cho hang
  emailService.sendPreorderConfirmationEmail(...);

  // Hang ve -> thong bao cho danh sach cho
  emailService.sendPreorderAvailableEmail(...);

## F. src/main/resources/application.properties
- Vai tro: cau hinh SMTP Gmail cho spring mail.
- Doan code tich hop:

  spring.mail.host=smtp.gmail.com
  spring.mail.port=587
  spring.mail.username=...
  spring.mail.password=...
  spring.mail.properties.mail.smtp.auth=true
  spring.mail.properties.mail.smtp.starttls.enable=true
  spring.mail.properties.mail.smtp.starttls.required=true

## Tong ket luong tich hop email
1. Cac service nghiep vu (AuthService, OrderService, PreorderRequestService) goi EmailService.
2. EmailService tao MimeMessage HTML va gui qua JavaMailSender.
3. Thong tin SMTP duoc nap tu application.properties.

---

## Task 1: ✅ Cau hinh Gmail SMTP (spring.mail.* + App Password)

### Cac file da tao/cap nhat:

#### 1. `pom.xml`
- Da co san dependency spring-boot-starter-mail (ke thua version tu Spring Boot parent):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

#### 2. `src/main/resources/application.properties`
- Cau hinh Gmail SMTP day du voi App Password:
```properties
# Email Configuration (Gmail SMTP - App Password)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=gearlabfounder@gmail.com
spring.mail.password=rmwa onei zzrr wjkz   # App Password 16 ky tu tu Google
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000
```

### Dieu kien hoan thanh:
- ✅ Dependency spring-boot-starter-mail da co trong pom.xml
- ✅ Gmail SMTP port 587 + STARTTLS da cau hinh
- ✅ Dung App Password (16 ky tu) thay vi mat khau Gmail truc tiep
- ✅ Them timeout properties tranh treo ket noi SMTP

### Cach lay App Password tu Google:
1. Vao Google Account -> Security -> 2-Step Verification -> App passwords
2. Chon app "Mail", device "Other (custom name)"
3. Google cap 16 ky tu -> copy vao spring.mail.password (co the de co khoang trang hoac khong)

### Luu y bao mat:
- TUYET DOI KHONG commit App Password len git public
- Nen dung bien moi truong hoac secrets manager tren production

## Task 2: ✅ Tạo service trung tâm email và helper buildItemsTableHtml

### Cac file da tao/cap nhat:

#### 1. `src/main/java/j2ee_backend/nhom05/dto/EmailOrderItem.java`
- Tạo DTO nội bộ chứa thông tin sản phẩm email: ảnh, tên, biến thể, số lượng, đơn giá, thành tiền.

#### 2. `src/main/java/j2ee_backend/nhom05/service/EmailService.java`
- Tạo service trung tâm để xử lý nội dung email đơn hàng.
- Implement helper `buildItemsTableHtml(List<EmailOrderItem> items)` để tạo bảng HTML inline gồm:
  - Ảnh sản phẩm
  - Tên sản phẩm
  - Thông tin biến thể
  - Số lượng
  - Đơn giá
  - Thành tiền
- Bổ sung helper `buildItemsTableHtmlFromOrderItems(List<OrderItem> orderItems)` để chuyển đổi `OrderItem` sang `EmailOrderItem` và render HTML.

### Đã làm:
- ✅ Tạo service trung tâm email (`EmailService`).
- ✅ Tạo DTO `EmailOrderItem` để chuẩn hoá dữ liệu đơn hàng dùng cho email.
- ✅ Implement helper render bảng HTML inline cho danh sách sản phẩm.
- ✅ Kiểm tra build để đảm bảo code mới biên dịch thành công.

### Chưa làm / phần còn lại giành cho task 3:
- ⚠️ Chưa tích hợp `EmailService` vào các luồng gửi email thực tế.
- ⚠️ Chưa thêm các phương thức gửi email cụ thể như `sendOrderConfirmationEmail`, `sendOrderCancelledEmail`, `sendPaymentPendingEmail`, v.v.
- ⚠️ Chưa xây dựng template email đầy đủ cho các loại email đơn hàng.
