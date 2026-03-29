# Tich hop dang nhap Google (J2EE_backend)

Tai lieu nay liet ke cac doan code tich hop dang nhap Google theo thu tu a -> z (theo ten file).

## A. src/main/java/j2ee_backend/nhom05/controller/AuthController.java
- Vai tro: expose API backend cho Google Sign-In.
- Doan code chinh:
  - Endpoint: POST /api/auth/google
  - Nhan idToken tu client, goi service xac thuc token va issue JWT session.

  public ResponseEntity<?> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request, HttpServletRequest httpRequest) {
      User user = authService.loginWithGoogle(request.getIdToken());
      authSessionService.validateAdminIpPolicyBeforeLogin(user, httpRequest);
      LoginResponse response = authSessionService.issueLoginTokens(user, false, httpRequest, "Dang nhap Google thanh cong");
      return ResponseEntity.ok(new ApiResponse("Dang nhap Google thanh cong", response));
  }

## B. src/main/java/j2ee_backend/nhom05/dto/auth/GoogleLoginRequest.java
- Vai tro: DTO nhan du lieu login Google tu frontend.
- Doan code tich hop:
  - Truong idToken (bat buoc):

  @NotBlank(message = "ID Token khong duoc de trong")
  private String idToken;

## C. src/main/java/j2ee_backend/nhom05/dto/auth/GoogleTokenInfo.java
- Vai tro: map JSON tra ve tu Google tokeninfo API.
- Doan code tich hop:
  - Cac field can thiet: sub, email, email_verified, aud, name...

  @JsonProperty("email_verified")
  private String emailVerified;

  private String sub;
  private String email;
  private String aud;

## D. src/main/java/j2ee_backend/nhom05/model/User.java
- Vai tro: luu thong tin lien ket tai khoan Google.
- Doan code tich hop:

  @Column(name = "provider", length = 50)
  private String provider;

  @Column(name = "provider_id", length = 255)
  private String providerId;

## E. src/main/java/j2ee_backend/nhom05/repository/IUserRepository.java
- Vai tro: truy van user theo dinh danh nha cung cap OAuth.
- Doan code tich hop:

  Optional<User> findByProviderAndProviderId(String provider, String providerId);

## F. src/main/java/j2ee_backend/nhom05/service/AuthService.java
- Vai tro: xu ly toan bo logic xac thuc Google o backend.
- Doan code tich hop chinh:

  @Value("${google.client-id}")
  private String googleClientId;

  @Transactional
  public User loginWithGoogle(String idToken) {
      GoogleTokenInfo tokenInfo = verifyGoogleToken(idToken);

      if (!googleClientId.equals(tokenInfo.getAud())) {
          throw new RuntimeException("Token Google khong hop le: audience khong khop");
      }

      if (!"true".equals(tokenInfo.getEmailVerified())) {
          throw new RuntimeException("Email Google chua duoc xac thuc");
      }

      String googleId = tokenInfo.getSub();
      String email = tokenInfo.getEmail();

      Optional<User> existingByProvider = userRepository.findByProviderAndProviderId("google", googleId);
      if (existingByProvider.isPresent()) return existingByProvider.get();

      Optional<User> existingByEmail = userRepository.findByEmail(email);
      if (existingByEmail.isPresent()) {
          User user = existingByEmail.get();
          user.setProvider("google");
          user.setProviderId(googleId);
          return userRepository.save(user);
      }

      User newUser = new User();
      newUser.setEmail(email);
      newUser.setProvider("google");
      newUser.setProviderId(googleId);
      return userRepository.save(newUser);
  }

  private GoogleTokenInfo verifyGoogleToken(String idToken) {
      RestTemplate restTemplate = new RestTemplate();
      String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
      GoogleTokenInfo info = restTemplate.getForObject(url, GoogleTokenInfo.class);
      return info;
  }

- Rang buoc lien quan Google trong login/password:
  - Chan dang nhap password neu provider = google.
  - Chan doi mat khau/reset mat khau/2FA email cho tai khoan Google.

## G. src/main/resources/application.properties
- Vai tro: cau hinh Google OAuth2.
- Doan code tich hop:

  google.client-id=...
  google.client-secret=...

## H. pom.xml
- Vai tro: dependency OAuth2 client.
- Doan code tich hop:

  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
  </dependency>

## Tong ket luong tich hop Google
1. Frontend gui idToken -> POST /api/auth/google.
2. AuthController goi AuthService.loginWithGoogle.
3. AuthService goi Google tokeninfo API, validate aud + email_verified.
4. Tim/tao user theo provider+providerId hoac email.
5. Tra ve user de AuthSessionService phat hanh token dang nhap.

## I. Bo sung xac thuc idToken phia backend (GEAR5-outh2)

Muc tieu: backend khong tin du lieu token tu client, phai tu goi Google de kiem tra token that.

- Endpoint da bo sung:
  - POST /api/auth/google/verify
  - Request body: { "idToken": "..." }

- Luong xu ly da bo sung trong backend:
  1. Nhan idToken tu client.
  2. Goi Google tokeninfo: https://oauth2.googleapis.com/tokeninfo?id_token=...
  3. So sanh aud voi cau hinh google.client-id.
  4. Kiem tra email_verified = true.
  5. Neu hop le: tra profile (sub, email, emailVerified, name, picture).
  6. Neu token gia/het han/sai aud/email chua verify: tra 401 ngay, khong xu ly tiep.

- File code da duoc cap nhat:
  - src/main/java/j2ee_backend/nhom05/controller/AuthController.java
  - src/main/java/j2ee_backend/nhom05/service/AuthService.java
  - src/main/java/j2ee_backend/nhom05/dto/auth/GoogleLoginRequest.java
  - src/main/java/j2ee_backend/nhom05/dto/auth/GoogleTokenInfo.java
  - src/main/java/j2ee_backend/nhom05/dto/auth/GoogleProfileResponse.java

- Cau hinh can co:
  - google.client-id=<google_oauth_client_id>
