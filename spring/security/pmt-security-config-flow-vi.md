# PMT SecurityConfig Class And Flow

Tài liệu này giải thích class
`mgm.pmt.backend.securities.SecurityConfig` trong đoạn code được cung cấp. Mục
tiêu là đọc nhanh được mỗi class dùng để làm gì, hai `SecurityFilterChain` hoạt
động ra sao, và request đi qua security flow theo thứ tự nào.

## 1. Vai Trò Tổng Thể

`SecurityConfig` là cấu hình Spring Security cho backend PMT. Class này chia
security thành hai chain:

1. `apiSecurityFilterChain`: áp dụng cho `/api/**`, dùng JWT, stateless, trả
   `401 Unauthorized` khi chưa đăng nhập.
2. `oauthSecurityFilterChain`: áp dụng cho các request còn lại, phục vụ OAuth2
   Login với GitHub, tạo JWT sau khi login thành công, ghi JWT vào cookie, rồi
   redirect về frontend.

Điểm quan trọng: `@Order(1)` chạy trước `@Order(2)`. Vì vậy request `/api/**`
luôn được xử lý bởi API chain trước, không rơi vào OAuth2 login chain.

## 2. Các Class Và Dependency Chính

| Class / Type | Nguồn | Vai trò trong cấu hình |
| --- | --- | --- |
| `SecurityConfig` | App code | Class cấu hình chính. Khai báo hai bean `SecurityFilterChain`. |
| `CustomOAuth2UserService` | App code | Load và map thông tin user từ OAuth2 provider. Theo flow này, service phải gắn local user id vào attribute `pmt_user_id`. |
| `JwtAuthenticationFilter` | App code | Custom filter xác thực request API bằng JWT. Filter được chạy trước `UsernamePasswordAuthenticationFilter`. |
| `JwtService` | App code | Tạo JWT cho user sau OAuth2 login và ghi access token vào cookie response. |
| `UserRepository` | App code | Tìm local `User` trong database bằng UUID lấy từ OAuth2 principal. |
| `User` | App entity | Local user dùng để generate JWT. |
| `HttpSecurity` | Spring Security | Builder DSL để cấu hình filter chain. |
| `SecurityFilterChain` | Spring Security | Bean chứa các security filter áp dụng cho một nhóm request. |
| `@Order` | Spring Core | Sắp thứ tự nhiều `SecurityFilterChain`; số nhỏ hơn có ưu tiên cao hơn. |
| `SessionCreationPolicy.STATELESS` | Spring Security | Không tạo HTTP session cho API; mỗi request API phải tự xác thực bằng JWT. |
| `HttpStatusEntryPoint` | Spring Security | Trả HTTP status cụ thể khi request chưa xác thực. Ở đây trả `401`. |
| `UsernamePasswordAuthenticationFilter` | Spring Security | Filter form login chuẩn. Custom JWT filter được đặt trước filter này. |
| `OAuth2User` | Spring Security OAuth2 | Principal sau khi OAuth2 login thành công. Chứa attributes từ provider và từ `CustomOAuth2UserService`. |

## 3. Constant Và Property

```java
private static final String LOCAL_USER_ID_ATTRIBUTE = "pmt_user_id";
```

`LOCAL_USER_ID_ATTRIBUTE` là tên attribute trong `OAuth2User`. Sau OAuth2 login,
success handler đọc attribute này để biết OAuth user tương ứng với local `User`
nào trong database.

```java
@Value("${app.frontend-url}")
private String frontendUrl;
```

`frontendUrl` là URL frontend để redirect sau login thành công hoặc thất bại.
Ví dụ:

- Thành công: `${frontendUrl}/app`
- Thất bại: `${frontendUrl}`

## 4. API Security Chain

```java
@Bean
@Order(1)
SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception
```

Chain này chỉ áp dụng cho request match `/api/**`:

```java
.securityMatcher("/api/**")
```

### Cấu hình chính

| Cấu hình | Ý nghĩa |
| --- | --- |
| `.csrf(csrf -> csrf.disable())` | Tắt CSRF cho API chain. Phù hợp nếu API dùng bearer token không tự động gửi bằng cookie. Nếu JWT được gửi bằng cookie, cần kiểm tra `SameSite`, CORS, và CSRF strategy. |
| `.cors(Customizer.withDefaults())` | Bật CORS bằng cấu hình mặc định hoặc bean `CorsConfigurationSource` nếu app có khai báo. |
| `.sessionManagement(... STATELESS)` | API không lưu authentication trong session. |
| `.requestMatchers(GET, "/api/v1/auth/me").authenticated()` | Endpoint lấy current user yêu cầu đăng nhập. |
| `.requestMatchers(POST, "/api/v1/auth/logout").authenticated()` | Endpoint logout yêu cầu đăng nhập. |
| `.anyRequest().authenticated()` | Mọi endpoint `/api/**` còn lại đều yêu cầu đăng nhập. |
| `.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))` | Request API chưa đăng nhập nhận `401`, không redirect sang login page. |
| `.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)` | Chạy JWT filter trước form login filter để set authentication cho request API. |

Hai rule riêng cho `/api/v1/auth/me` và `/api/v1/auth/logout` hiện có cùng kết
quả với `.anyRequest().authenticated()`. Chúng vẫn hữu ích như documentation
trong code vì đây là hai endpoint auth quan trọng.

## 5. API Request Flow

Ví dụ request:

```http
GET /api/v1/auth/me
```

Flow:

1. Servlet container nhận request.
2. Spring Security xét `SecurityFilterChain` theo `@Order`.
3. Chain `@Order(1)` match vì path là `/api/**`.
4. CSRF bị tắt cho chain này.
5. CORS được xử lý nếu request là cross-origin.
6. Session policy là `STATELESS`, nên Spring Security không dựa vào HTTP
   session để nhớ user.
7. `JwtAuthenticationFilter` chạy trước `UsernamePasswordAuthenticationFilter`.
8. `JwtAuthenticationFilter` đọc JWT từ request. Vị trí token phụ thuộc
   implementation: header `Authorization`, cookie, hoặc nguồn khác.
9. Nếu JWT hợp lệ, filter tạo `Authentication` và đặt vào `SecurityContext`.
10. `authorizeHttpRequests` kiểm tra rule. Vì endpoint yêu cầu
    `.authenticated()`, request được đi tiếp nếu `SecurityContext` đã có
    authenticated principal.
11. Nếu không có authentication hợp lệ, `HttpStatusEntryPoint` trả
    `401 Unauthorized`.

Kết quả mong đợi:

- Có JWT hợp lệ: request đi tới controller.
- Không có JWT hoặc JWT sai/hết hạn: response `401`.

## 6. OAuth2 Login Security Chain

```java
@Bean
@Order(2)
SecurityFilterChain oauthSecurityFilterChain(HttpSecurity http) throws Exception
```

Chain này xử lý request không match `/api/**`. Nó dùng OAuth2 Login.

### Cấu hình chính

| Cấu hình | Ý nghĩa |
| --- | --- |
| `.csrf(csrf -> csrf.disable())` | Tắt CSRF cho OAuth chain. Cần hiểu rõ nếu app có form/session/cookie state khác. |
| `.cors(Customizer.withDefaults())` | Bật CORS. |
| `.requestMatchers("/", "/error", "/oauth2/**", "/login/**").permitAll()` | Cho phép trang root, error, OAuth2 endpoints, và login endpoints không cần login trước. |
| `.anyRequest().authenticated()` | Request còn lại phải authenticated. |
| `.oauth2Login(...)` | Bật OAuth2 Login flow. |
| `.userInfoEndpoint(... customOAuth2UserService)` | Dùng service custom để load/map user info từ provider. |
| `.successHandler(...)` | Tự xử lý sau login thành công: lấy local user, tạo JWT, ghi cookie, redirect frontend. |
| `.failureHandler(...)` | Log lỗi OAuth và redirect về frontend. |

## 7. OAuth2 Login Success Flow

Flow khi user login GitHub thành công:

1. Browser đi tới endpoint OAuth2 authorization, thường bắt đầu từ
   `/oauth2/authorization/{registrationId}`.
2. Spring Security redirect browser sang GitHub.
3. User xác thực và consent ở GitHub.
4. GitHub redirect về callback của backend.
5. Spring Security đổi authorization code lấy token.
6. Spring Security gọi `customOAuth2UserService` để load user info.
7. `customOAuth2UserService` trả về `OAuth2User`.
8. Success handler chạy:

```java
OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
String userId = oauthUser.getAttribute(LOCAL_USER_ID_ATTRIBUTE);
User user = userRepository.findById(UUID.fromString(userId))
        .orElseThrow(() -> new IllegalStateException("Authenticated OAuth user not found"));
String token = jwtService.generateToken(user);
jwtService.addAccessTokenCookie(response, token);
response.sendRedirect(frontendUrl + "/app");
```

### Từng bước trong success handler

| Bước | Ý nghĩa |
| --- | --- |
| Cast principal sang `OAuth2User` | OAuth2 Login tạo principal dạng OAuth2 user. |
| Đọc attribute `pmt_user_id` | Lấy local user id do `CustomOAuth2UserService` gắn vào. |
| `UUID.fromString(userId)` | Chuyển string id sang UUID. Nếu attribute thiếu/sai format, flow sẽ lỗi. |
| `userRepository.findById(...)` | Đảm bảo local user tồn tại trong database. |
| `jwtService.generateToken(user)` | Tạo access token nội bộ cho backend API. |
| `jwtService.addAccessTokenCookie(response, token)` | Ghi JWT vào cookie để frontend dùng cho request sau. |
| `response.sendRedirect(frontendUrl + "/app")` | Đưa user về màn hình app sau khi login thành công. |

## 8. OAuth2 Login Failure Flow

Khi OAuth2 login thất bại:

```java
failureHandler((request, response, exception) -> {
    log.error("GitHub OAuth login failed", exception);
    response.sendRedirect(frontendUrl);
})
```

Flow:

1. Spring Security bắt lỗi trong OAuth2 login.
2. Failure handler log exception.
3. Browser được redirect về `frontendUrl`.

Không nên log token, authorization code, cookie, hoặc raw provider payload. Đoạn
code hiện tại log exception object; cần đảm bảo exception message không chứa dữ
liệu nhạy cảm từ provider hoặc request.

## 9. Vì Sao Có Hai Chain

Tách chain giúp API và browser login có hành vi khác nhau:

| Nhu cầu | API chain | OAuth chain |
| --- | --- | --- |
| Path | `/api/**` | Các path còn lại |
| Auth mechanism | JWT custom filter | OAuth2 Login |
| Session | Stateless | Không set stateless rõ ràng; OAuth2 flow có thể dùng state/session trong quá trình login |
| Unauthenticated response | `401 Unauthorized` | OAuth2 login/redirect behavior |
| Sau login | Không xử lý login | Tạo JWT cookie và redirect frontend |

Nếu không tách chain, request API chưa login có thể bị redirect sang OAuth login
thay vì trả `401`, gây khó xử lý cho frontend/mobile client.

## 10. Quan Hệ Với Frontend

Sau OAuth2 login thành công, backend redirect:

```text
${app.frontend-url}/app
```

Trước khi redirect, backend đã gọi:

```java
jwtService.addAccessTokenCookie(response, token);
```

Điều này ngụ ý frontend có thể gọi API sau login mà không cần tự cầm token trong
JavaScript, nếu cookie được browser gửi tự động. Khi dùng cookie cho JWT, cần
đặt thuộc tính cookie cẩn thận:

- `HttpOnly`: giảm rủi ro token bị đọc bởi JavaScript khi có XSS.
- `Secure`: chỉ gửi cookie qua HTTPS.
- `SameSite=Lax` hoặc `Strict` nếu phù hợp flow; `None` chỉ dùng khi cần
  cross-site cookie và phải đi kèm `Secure`.
- `Path`: giới hạn phạm vi cookie nếu có thể.
- Expiration ngắn cho access token.

## 11. Điểm Cần Kiểm Tra Khi Review Code

- `CustomOAuth2UserService` luôn set attribute `pmt_user_id` hay không.
- `pmt_user_id` có chắc là UUID hợp lệ trước khi gọi `UUID.fromString`.
- `JwtAuthenticationFilter` đọc token từ đâu: cookie hay `Authorization` header.
- Nếu JWT nằm trong cookie và browser tự gửi cookie tới `/api/**`, cần có chiến
  lược chống CSRF rõ ràng vì API chain đang disable CSRF.
- `CorsConfigurationSource` có cho phép đúng `frontendUrl`, đúng methods, đúng
  headers, và credential policy hay không.
- `JwtService.addAccessTokenCookie` có set `HttpOnly`, `Secure`, `SameSite`,
  `Max-Age`, và `Path` phù hợp không.
- Failure handler có vô tình log thông tin nhạy cảm không.
- `frontendUrl` có được validate/cấu hình cố định theo environment không, tránh
  open redirect do cấu hình sai.

## 12. Tóm Tắt Flow Ngắn

### API flow

```text
/api/** request
-> @Order(1) apiSecurityFilterChain
-> JwtAuthenticationFilter
-> SecurityContext
-> authorizeHttpRequests authenticated()
-> controller hoặc 401
```

### OAuth2 login flow

```text
/oauth2/** hoặc /login/**
-> @Order(2) oauthSecurityFilterChain
-> OAuth2 Login with GitHub
-> CustomOAuth2UserService
-> successHandler
-> UserRepository find local User
-> JwtService generate token
-> add access token cookie
-> redirect frontendUrl + "/app"
```
