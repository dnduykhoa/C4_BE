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
      return ResponseEntity.ok(response);
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

  @Value("${google.client-id:}")
  private String googleClientId;

  @Transactional
  public User loginWithGoogle(String idToken) {
    GoogleProfileResponse profile = verifyGoogleIdToken(idToken);

    // Tim user da lien ket provider+providerId
    // Hoac link theo email neu da ton tai
    // Neu chua co thi tao user moi voi provider=google, providerId=sub
  }

  public GoogleProfileResponse verifyGoogleIdToken(String idToken) {
    // Kiem tra google.client-id da cau hinh
    // Goi Google tokeninfo API va map ve GoogleTokenInfo
    // Validate aud, sub, email, email_verified
    // Tra profile hop le cho cac endpoint su dung
  }

  private GoogleTokenInfo fetchGoogleTokenInfo(String idToken) {
    // GET https://oauth2.googleapis.com/tokeninfo?id_token={idToken}
    // Neu token sai/het han thi throw RuntimeException
  }

- Rang buoc lien quan Google trong login/password:
  - Chan dang nhap password neu provider = google.
  - Chan doi mat khau/reset mat khau/2FA email cho tai khoan Google.

  // Trong login thuong (/api/auth/login)
  if (GOOGLE_PROVIDER.equalsIgnoreCase(user.getProvider())) {
      throw new RuntimeException("Tai khoan nay dang nhap bang Google, vui long dung Google Sign-In");
  }

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

## J. Yeu cau task hien tai da hoan thanh

- Endpoint `POST /api/auth/google`:
  - Nhan body: `{ "idToken": "..." }`
  - Goi `authService.loginWithGoogle(...)`
  - Goi `authSessionService.issueLoginTokens(...)`
  - Tra ve truc tiep `LoginResponse`.

- Luong dang nhap thuong `POST /api/auth/login`:
  - Neu user co `provider = google` thi chan dang nhap password ngay.
  - Message loi tra ve ro rang: `Tai khoan nay dang nhap bang Google, vui long dung Google Sign-In`.

- Cach test nhanh:
  1. Dang nhap bang Google de tao/gan user provider=google.
  2. Goi `POST /api/auth/login` voi email cua tai khoan do + mat khau bat ky.
  3. Ky vong HTTP 401 va thong bao loi nhu tren.

- Mau request/response de nghiem thu:
  1. Google login:
     - Request: `POST /api/auth/google` voi body `{ "idToken": "<google_id_token>" }`
     - Expected: HTTP 200, body la `LoginResponse` (khong boc `ApiResponse`).
  2. Login password voi account Google:
     - Request: `POST /api/auth/login` voi body `{ "emailOrPhone": "google_user@email.com", "password": "any" }`
     - Expected: HTTP 401, body chua message: `Tai khoan nay dang nhap bang Google, vui long dung Google Sign-In`.

- Check project:
  - Da chay `./mvnw.cmd -DskipTests compile` va `BUILD SUCCESS`.
