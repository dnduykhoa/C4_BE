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
