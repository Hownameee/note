# Spring Security Servlet Class Map

Tài liệu này là companion cho
`spring/security/spring-security-servlet-guide-vi.md`. Mục tiêu là gom các class,
interface, annotation, enum, và test helper quan trọng của Spring Security Servlet
theo nhóm trách nhiệm: chúng dùng để làm gì, nằm ở đâu trong request flow, và
liên hệ với thành phần khác ra sao.

Phạm vi: Spring Security servlet stack, dựa trên nội dung guide servlet 7.1.0
trong repository này. Đây không phải API reference đầy đủ của mọi package Spring
Security, mà là bản đồ các type cần biết khi đọc, cấu hình, hoặc debug security
cho Spring MVC / Spring Boot servlet application.

## 1. Bức tranh tổng thể

Request servlet đi qua các lớp chính theo thứ tự khái niệm sau:

1. Servlet container gọi `FilterChain`.
2. `DelegatingFilterProxy` nối servlet filter với Spring bean.
3. `FilterChainProxy` chọn `SecurityFilterChain` đầu tiên match request.
4. Các security filter trong chain chạy theo thứ tự.
5. Authentication filter tạo `Authentication` chưa xác thực và gọi
   `AuthenticationManager`.
6. `ProviderManager` ủy quyền cho các `AuthenticationProvider`.
7. Provider validate credential, tạo `Authentication` đã xác thực kèm
   `GrantedAuthority`.
8. `SecurityContextHolder` giữ `SecurityContext` cho request hiện tại.
9. `AuthorizationFilter` gọi `AuthorizationManager` để quyết định access.
10. Nếu thiếu credential hoặc bị từ chối, `ExceptionTranslationFilter` chuyển
lỗi thành redirect, 401, hoặc 403 phù hợp.

## 2. Servlet Filter Và Security Chain

| Type | Vai trò | Quan hệ chính |
| --- | --- | --- |
| `Filter` | Chuẩn Servlet API để xử lý request trước/sau servlet. | Spring Security triển khai bảo mật bằng nhiều filter nằm trong servlet `FilterChain`. |
| `FilterChain` | Chuỗi filter của servlet container cho một request. | Container gọi từng `Filter`; Spring Security nằm trong chain này qua `DelegatingFilterProxy`. |
| `DelegatingFilterProxy` | Servlet filter bridge sang Spring bean. | Container gọi proxy; proxy tìm bean Spring Security, thường là `springSecurityFilterChain`, rồi delegate. |
| `FilterChainProxy` | Filter trung tâm của Spring Security. | Được `DelegatingFilterProxy` gọi; chọn một `SecurityFilterChain`, áp dụng `HttpFirewall`, và dọn `SecurityContext` sau request. |
| `SecurityFilterChain` | Một tập filter bảo vệ một nhóm request. | Match bằng request matcher; chain match đầu tiên thắng. Được tạo bằng `HttpSecurity#build()`. |
| `HttpSecurity` | Builder DSL để tạo `SecurityFilterChain`. | Các configurer như `csrf`, `formLogin`, `httpBasic`, `oauth2ResourceServer`, `authorizeHttpRequests` thêm filter và shared object vào chain. |
| `SessionCreationPolicy` | Enum điều khiển cách Spring Security dùng HTTP session. | `STATELESS` thường dùng cho bearer-token API; stateful session dùng cho browser login. |
| `WebSecurityConfigurerAdapter` | API cấu hình cũ. | Legacy; ứng dụng mới nên khai báo bean `SecurityFilterChain`. |
| `@Order` | Annotation Spring để sắp thứ tự nhiều chain. | Chain có order nhỏ hơn được xét trước; quan trọng khi tách `/api/**` khỏi web UI. |

Quan hệ quan trọng: nhiều `SecurityFilterChain` có thể cùng tồn tại. Dùng
`securityMatcher` và `@Order` để tách API stateless khỏi browser UI stateful.
Khi debug, cần xác định chain nào match trước khi xem rule authorization.

## 3. Security Context Và Principal

| Type | Vai trò | Quan hệ chính |
| --- | --- | --- |
| `SecurityContextHolder` | Nơi truy cập `SecurityContext` hiện tại. | Mặc định lưu bằng `ThreadLocal`; filter persistence/context holder set và clear context quanh request. |
| `SecurityContext` | Container chứa `Authentication` hiện tại. | Được lưu trong `SecurityContextHolder`; có thể được lưu tiếp vào session cho web app stateful. |
| `Authentication` | Đại diện request credential hoặc principal đã xác thực. | Authentication filter tạo bản chưa xác thực; provider trả bản đã xác thực; authorization đọc authorities từ đây. |
| `GrantedAuthority` | Một quyền cấp ứng dụng. | Gắn trên `Authentication`; dùng bởi `AuthorizationManager`, `hasRole`, `hasAuthority`, và method security. |
| `SimpleGrantedAuthority` | Implementation đơn giản của `GrantedAuthority`. | Dùng khi tạo authority như `ROLE_ADMIN`, `SCOPE_orders.read`, hoặc permission tùy biến. |
| `UserDetails` | Principal chuẩn cho username/password. | Được `UserDetailsService` load và được `DaoAuthenticationProvider` dùng để so password. |
| `User` | Implementation/builder tiện dụng của `UserDetails`. | Dùng cho in-memory user, sample, hoặc chuyển domain account thành `UserDetails`. |
| `Jwt` | Principal/token model cho JWT resource server. | Được `JwtDecoder` tạo sau khi validate token; converter biến claim thành authorities. |
| `OAuth2User` | Principal cho OAuth2 Login. | Được OAuth2/OIDC user service tạo sau khi provider trả user info hoặc ID token. |
| `OidcUser` | Principal OpenID Connect. | Mở rộng OAuth2 user bằng ID token và OIDC claims. |
| `DefaultOidcUser` | Implementation phổ biến của `OidcUser`. | Dùng khi map thêm authorities sau OIDC login. |
| `Saml2Authentication` | Authentication sau SAML2 login. | Được lưu vào `SecurityContext`; authorization đọc authorities đã map từ SAML assertion. |

Quy tắc thực tế: `Authentication#getName()` là cách đọc subject phổ biến, nhưng
logic nghiệp vụ thường cần principal cụ thể hơn như `UserDetails`, `Jwt`,
`OidcUser`, hoặc custom principal.

## 4. Authentication Core

| Type | Vai trò | Quan hệ chính |
| --- | --- | --- |
| `AuthenticationManager` | API authenticate một `Authentication`. | Authentication filter gọi manager; manager trả `Authentication` đã xác thực hoặc ném exception. |
| `ProviderManager` | Implementation phổ biến của `AuthenticationManager`. | Duyệt danh sách `AuthenticationProvider`; provider đầu tiên support token type sẽ validate. |
| `AuthenticationProvider` | Validate một loại authentication token. | Ví dụ `DaoAuthenticationProvider`, `JwtAuthenticationProvider`, hoặc custom provider API key/OTP. |
| `AbstractAuthenticationToken` | Base class tiện dụng cho custom authentication token. | Dùng khi tự định nghĩa token như `ApiKeyAuthenticationToken`. |
| `DaoAuthenticationProvider` | Provider cho username/password. | Dùng `UserDetailsService` để load user và `PasswordEncoder` để so password hash. |
| `JwtAuthenticationProvider` | Provider cho JWT bearer token. | Dùng `JwtDecoder`; thường được cấu hình qua `oauth2ResourceServer().jwt()`. |
| `UserDetailsService` | Load user theo username. | Dependency của `DaoAuthenticationProvider`; có thể là in-memory, JDBC, LDAP, hoặc custom domain service. |
| `InMemoryUserDetailsManager` | `UserDetailsService` lưu user trong memory. | Phù hợp sample, test, hoặc internal tool rất nhỏ. |
| `PasswordEncoder` | Hash và verify password. | Dùng trong `DaoAuthenticationProvider`; production nên dùng delegating encoder. |
| `PasswordEncoderFactories` | Factory tạo delegating `PasswordEncoder`. | `createDelegatingPasswordEncoder()` tạo encoder hỗ trợ prefix như `{bcrypt}`. |
| `UsernameNotFoundException` | Báo không tìm thấy user trong `UserDetailsService`. | Được provider xử lý như authentication failure. |
| `BadCredentialsException` | Báo credential sai. | Custom provider thường ném exception này khi secret/token không hợp lệ. |

Flow username/password: `UsernamePasswordAuthenticationFilter` đọc form,
tạo token, gọi `AuthenticationManager`; `ProviderManager` gọi
`DaoAuthenticationProvider`; provider dùng `UserDetailsService` và
`PasswordEncoder`; kết quả là `Authentication` có principal và authorities.

## 5. Authentication Entry Points Và Login Filter

| Type | Vai trò | Quan hệ chính |
| --- | --- | --- |
| `UsernamePasswordAuthenticationFilter` | Xử lý form login username/password. | Đọc field `username` và `password`, gọi `AuthenticationManager`, rồi chạy success/failure handling. |
| `AuthenticationEntryPoint` | Bắt đầu authentication khi request cần credential. | Được `ExceptionTranslationFilter` gọi; có thể redirect login page hoặc trả 401. |
| `ExceptionTranslationFilter` | Dịch security exception thành HTTP response. | Bắt unauthenticated/access denied ở gần cuối chain; gọi `AuthenticationEntryPoint` hoặc access denied handler. |
| `BearerTokenAuthenticationFilter` | Đọc bearer token từ `Authorization` header. | Gọi authentication flow cho resource server; thường đi với `JwtAuthenticationProvider` hoặc opaque token introspection. |

Quan hệ quan trọng: authorization failure của anonymous user không nên tự xử lý
trong controller. `AuthorizationFilter` deny, `ExceptionTranslationFilter` bắt
và chọn response đúng theo cơ chế login/API đã cấu hình.

## 6. Authorization

| Type | Vai trò | Quan hệ chính |
| --- | --- | --- |
| `AuthorizationManager<T>` | API quyết định có cho phép truy cập không. | API hiện đại cho request, method, hoặc custom authorization; đọc `Authentication` và context. |
| `AuthorizationFilter` | Filter thực thi URL authorization. | Chạy sau authentication và protection filter; gọi `AuthorizationManager` cho request đã match. |
| `AuthorizationDecision` | Kết quả grant/deny từ custom manager. | Custom `AuthorizationManager` trả object này. |
| `RequestAuthorizationContext` | Context request cho URL authorization. | Cho custom manager đọc path variables, request, hoặc matcher context. |
| `AccessDecisionManager` | API authorization cũ. | Legacy cùng voter model; tránh dùng cho ứng dụng mới nếu có thể dùng `AuthorizationManager`. |
| `GrantedAuthorityDefaults` | Cấu hình prefix role mặc định. | Ảnh hưởng `hasRole`; mặc định `hasRole("ADMIN")` tìm authority `ROLE_ADMIN`. |
| `DispatcherType` | Servlet enum cho dispatch như `FORWARD` và `ERROR`. | Dùng với `dispatcherTypeMatchers` để permit view forwarding hoặc error page khi cần. |
| `@EnableMethodSecurity` | Bật method security. | Kích hoạt interceptor/AOP cho `@PreAuthorize`, `@PostAuthorize`, và các annotation liên quan. |
| `@PreAuthorize` | Check trước khi method chạy. | Dùng SpEL, đọc `authentication`, tham số method, hoặc bean security tùy biến. |
| `@PostAuthorize` | Check sau khi method chạy. | Dùng khi quyết định cần `returnObject`, ví dụ owner của entity. |

Quan hệ quan trọng: URL authorization bảo vệ entry point HTTP; method security
bảo vệ service operation. Với business rule nhạy cảm, dùng cả hai lớp thay vì
chỉ dựa vào controller.

## 7. OAuth2 Resource Server

| Type | Vai trò | Quan hệ chính |
| --- | --- | --- |
| `BearerTokenAuthenticationFilter` | Trích bearer token từ request. | Đưa token vào authentication pipeline của resource server. |
| `JwtDecoder` | Validate và decode JWT. | Kiểm tra signature, issuer, expiry, claim; tạo `Jwt`. |
| `JwtAuthenticationProvider` | Provider authenticate JWT. | Dùng `JwtDecoder`; trả `Authentication` chứa `Jwt` principal và authorities. |
| `JwtAuthenticationConverter` | Convert `Jwt` thành `Authentication`. | Dùng để tùy biến principal name hoặc authorities từ claim. |
| `JwtGrantedAuthoritiesConverter` | Convert scope/claim thành `GrantedAuthority`. | Mặc định thường tạo authority prefix `SCOPE_`; có thể đổi claim name và prefix. |

Flow JWT: filter đọc token, provider dùng decoder validate, converter tạo
authorities, `AuthorizationManager` kiểm tra `SCOPE_*` hoặc permission tương ứng.

## 8. OAuth2 Login Và OAuth2 Client

| Type | Vai trò | Quan hệ chính |
| --- | --- | --- |
| `ClientRegistration` | Cấu hình OAuth2/OIDC client. | Chứa client id, secret, scope, redirect URI, và provider endpoints. |
| `OAuth2AuthorizedClient` | Một client đã có token. | Kết hợp `ClientRegistration` với access token/refresh token. |
| `OAuth2AuthorizedClientManager` | Lấy và refresh authorized client. | Được app hoặc `WebClient` integration dùng khi gọi downstream service. |
| `OAuth2UserService<R, U>` | Load/map user sau OAuth2/OIDC login. | Cho phép map claim thành domain principal hoặc authorities. |
| `OidcUserRequest` | Request context khi load OIDC user. | Đầu vào của `OAuth2UserService<OidcUserRequest, OidcUser>`. |
| `OidcUserService` | Default service load OIDC user. | Có thể wrap để thêm authorities hoặc custom principal. |
| `ServletOAuth2AuthorizedClientExchangeFilterFunction` | OAuth2 filter cho `WebClient` trong servlet app. | Gắn access token từ `OAuth2AuthorizedClientManager` vào outbound request. |
| `WebClient` | HTTP client reactive của Spring. | Không thuộc Spring Security, nhưng thường được cấu hình với OAuth2 client filter để gọi API khác. |

Phân biệt: OAuth2 Login authenticate user vào ứng dụng browser. OAuth2 Client
lấy token để ứng dụng gọi service khác. Resource Server bảo vệ API nhận bearer
token. Ba vai trò này có thể cùng tồn tại nhưng không thay thế nhau.

## 9. SAML2

| Type | Vai trò | Quan hệ chính |
| --- | --- | --- |
| `Saml2Authentication` | Authentication sau khi SAML response hợp lệ. | Chứa principal và authorities map từ assertion; lưu vào `SecurityContext`. |
| Relying party registration | Cấu hình cặp SP/IdP. | Không xuất hiện như class cụ thể trong guide, nhưng là model cấu hình trung tâm của SAML2 servlet support. |

Flow SAML: app là service provider, IdP authenticate user, Spring Security
validate signed SAML response, tạo `Saml2Authentication`, sau đó URL/method
authorization dùng authorities đã map.

## 10. Exploit Protection, CORS, Và Firewall

| Type | Vai trò | Quan hệ chính |
| --- | --- | --- |
| `CookieCsrfTokenRepository` | Lưu CSRF token trong cookie. | Pattern phổ biến cho SPA cần đọc token và gửi header như `X-XSRF-TOKEN`. |
| `HttpFirewall` | Normalize/reject request đáng nghi. | Được `FilterChainProxy` áp dụng trước khi request vào security chain chính. |
| `CorsConfigurationSource` | Cung cấp CORS config cho security. | Khi có bean này và bật `cors()`, CORS chạy trước auth vì preflight thường không có credential. |
| `CorsConfiguration` | Mô tả allowed origins, methods, headers, credentials. | Được `CorsConfigurationSource` trả về theo path. |
| `UrlBasedCorsConfigurationSource` | Map path pattern sang `CorsConfiguration`. | Implementation phổ biến cho servlet CORS configuration. |

Quan hệ quan trọng: CSRF cần giữ cho browser app dùng cookie/session. API
stateless dùng bearer token thường tắt CSRF và dùng `SessionCreationPolicy.STATELESS`.
CORS phải xử lý preflight trước khi authentication filter từ chối request.

## 11. Servlet, MVC, Và Thread Integration

| Type | Vai trò | Quan hệ chính |
| --- | --- | --- |
| `HttpServletRequest` | Servlet request API. | Spring Security tích hợp các method như `getRemoteUser`, `getUserPrincipal`, `isUserInRole`, `login`, `logout`. |
| `@AuthenticationPrincipal` | Inject principal vào Spring MVC controller. | Đọc principal từ `Authentication` hiện tại mà không cần gọi `SecurityContextHolder` trực tiếp. |
| `@CurrentSecurityContext` | Inject `SecurityContext` vào controller. | Hữu ích khi cần context đầy đủ thay vì chỉ principal. |
| `DelegatingSecurityContextExecutor` | Wrap `Executor` để propagate security context. | Dùng khi chạy async task ở thread khác nhưng vẫn cần current user. |

Lưu ý: `SecurityContextHolder` dùng thread-local mặc định, nên thread mới không
tự có context. Với async/concurrency, dùng wrapper của Spring Security thay vì
tự copy context tùy tiện.

## 12. Configuration Annotation Và DSL

| Type | Vai trò | Quan hệ chính |
| --- | --- | --- |
| `@EnableWebSecurity` | Bật web security servlet. | Thường đặt cùng `@Configuration`; trong Spring Boot nhiều mặc định đã được auto-configure. |
| `@EnableMethodSecurity` | Bật method security. | Kích hoạt AOP/interceptor cho annotation security trên service. |
| `Customizer` | Callback cấu hình mặc định hoặc tùy biến DSL. | `Customizer.withDefaults()` bật cấu hình mặc định có chủ đích cho form login, http basic, csrf, oauth2. |

DSL quan trọng trong `HttpSecurity`:

| DSL | Tác dụng | Filter/type liên quan |
| --- | --- | --- |
| `authorizeHttpRequests` | Cấu hình URL authorization. | Thêm `AuthorizationFilter` và `AuthorizationManager`. |
| `formLogin` | Bật form login. | Dùng `UsernamePasswordAuthenticationFilter`, entry point, success/failure handlers. |
| `httpBasic` | Bật HTTP Basic. | Dùng Basic authentication filter và 401 challenge. |
| `oauth2ResourceServer().jwt()` | Bật JWT resource server. | Dùng `BearerTokenAuthenticationFilter`, `JwtDecoder`, `JwtAuthenticationProvider`. |
| `oauth2Login` | Bật OAuth2/OIDC login. | Dùng client registration, authorized client, user service. |
| `saml2Login` | Bật SAML2 login. | Tạo `Saml2Authentication` sau khi validate assertion. |
| `csrf` | Cấu hình CSRF. | Có thể dùng `CookieCsrfTokenRepository`. |
| `cors` | Bật CORS integration. | Dùng `CorsConfigurationSource`. |
| `sessionManagement` | Cấu hình session policy. | Dùng `SessionCreationPolicy`. |
| `logout` | Cấu hình logout. | Invalidate session, clear context, xóa cookie khi cấu hình. |
| `headers` | Cấu hình security response headers. | Bảo vệ clickjacking, sniffing, HSTS, cache control. |
| `securityMatcher` | Giới hạn chain match request nào. | Quyết định `SecurityFilterChain` áp dụng cho nhóm request nào. |

## 13. Testing Support

| Type/helper | Vai trò | Quan hệ chính |
| --- | --- | --- |
| `MockMvc` | Test HTTP layer trong Spring MVC. | Kết hợp `spring-security-test` để assert security behavior. |
| `@WithMockUser` | Tạo user giả trong security context test. | Dùng cho URL hoặc method security tests. |
| `csrf()` | Request post-processor thêm CSRF token. | Dùng để test POST/PUT/PATCH/DELETE khi CSRF bật. |
| `formLogin()` | Request builder/helper test form login. | Kiểm tra login success/failure mà không cần browser thật. |
| `httpBasic()` | Request post-processor thêm Basic auth. | Test endpoint dùng HTTP Basic. |
| `logout()` | Request builder/helper test logout. | Assert context bị clear và response đúng. |
| `oauth2Login()` | Tạo OAuth2 login authentication giả. | Test app behavior sau OAuth2 login mà không gọi provider thật. |
| `jwt()` | Tạo JWT authentication giả. | Test resource server authorization theo scope/authority. |
| `saml2Login()` | Tạo SAML authentication giả. | Test behavior sau SAML login mà không cần IdP thật. |
| `authenticated()` | Assertion authentication thành công. | Dùng với form login hoặc request đã authenticate. |
| `unauthenticated()` | Assertion không còn authentication. | Dùng sau logout hoặc authentication failure. |

Các helper test này chứng minh ứng dụng xử lý đúng khi Spring Security đã tạo
security state mong đợi. Chúng không thay thế integration test với provider thật
khi cần kiểm chứng issuer, metadata, redirect URI, certificate, hoặc client
secret.

## 14. Relationship Cheat Sheet

### Form Login

`SecurityFilterChain` -> `UsernamePasswordAuthenticationFilter` ->
`AuthenticationManager` -> `ProviderManager` -> `DaoAuthenticationProvider` ->
`UserDetailsService` + `PasswordEncoder` -> `Authentication` ->
`SecurityContextHolder` -> `AuthorizationFilter`.

### JWT Resource Server

`SecurityFilterChain` -> `BearerTokenAuthenticationFilter` ->
`AuthenticationManager` -> `JwtAuthenticationProvider` -> `JwtDecoder` ->
`JwtAuthenticationConverter` -> `Authentication` with `Jwt` principal ->
`AuthorizationManager`.

### OAuth2 Login

`SecurityFilterChain` -> OAuth2 login filters -> provider authorization code
flow -> `OAuth2UserService` / `OidcUserService` -> `OAuth2User` / `OidcUser` ->
`Authentication` -> `SecurityContext` -> URL/method authorization.

### OAuth2 Client Outbound Call

Application service -> `WebClient` ->
`ServletOAuth2AuthorizedClientExchangeFilterFunction` ->
`OAuth2AuthorizedClientManager` -> `OAuth2AuthorizedClient` token ->
downstream API.

### SAML2 Login

`SecurityFilterChain` -> SAML2 login flow -> IdP signed response validation ->
`Saml2Authentication` -> `SecurityContext` -> URL/method authorization.

### Method Security

Caller -> Spring AOP proxy -> `@PreAuthorize` / `@PostAuthorize` ->
method security `AuthorizationManager` -> `Authentication` authorities and
method arguments -> allow/deny.

### CORS And CSRF

Browser preflight -> CORS handling via `CorsConfigurationSource` before auth.
Unsafe browser request -> CSRF filter validates token, often from
`CookieCsrfTokenRepository`, before controller changes state.

## 15. Cách Chọn Type Khi Tùy Biến

| Nhu cầu | Type nên bắt đầu | Tránh |
| --- | --- | --- |
| Thêm rule URL theo path/method | `authorizeHttpRequests`, `AuthorizationManager<RequestAuthorizationContext>` | Custom servlet filter nếu chỉ là authorization rule. |
| Thêm business authorization | `@PreAuthorize`, `@PostAuthorize`, bean checker | Nhét business rule phức tạp vào controller hoặc filter. |
| Username/password với DB riêng | `UserDetailsService`, `PasswordEncoder`, `DaoAuthenticationProvider` | So password thủ công trong controller. |
| API key/legacy token | Custom `AuthenticationProvider` + token extends `AbstractAuthenticationToken` | Tin header trực tiếp mà không có provider validate. |
| JWT bearer API | `oauth2ResourceServer().jwt()`, `JwtDecoder`, converter authorities | Parse JWT thủ công trong controller. |
| Browser SSO bằng OIDC | `oauth2Login`, `OidcUserService`, `DefaultOidcUser` | Dùng resource server để login browser. |
| App gọi service khác bằng OAuth2 | `OAuth2AuthorizedClientManager`, `WebClient` OAuth2 filter | Tự cache access token không có refresh/error handling. |
| SPA cần CSRF token | `CookieCsrfTokenRepository` | Tắt CSRF khi vẫn dùng cookie session. |
| Async task cần current user | `DelegatingSecurityContextExecutor` | Dựa vào `ThreadLocal` tự lan sang thread khác. |
| Test rule security | `MockMvc`, `@WithMockUser`, `jwt()`, `csrf()` | Chỉ test happy path controller. |

## 16. Những Type Dễ Nhầm

- `AuthenticationManager` là API tổng; `ProviderManager` là implementation phổ
  biến; `AuthenticationProvider` mới là nơi validate từng kiểu credential.
- `UserDetails` là principal model cho username/password, không phải domain user
  bắt buộc của toàn app.
- `User.withDefaultPasswordEncoder()` chỉ phù hợp sample vì password encoder
  được tạo inline; production nên khai báo `PasswordEncoder` bean rõ ràng.
- `GrantedAuthority` là quyền đã cấp; role chỉ là convention authority có prefix
  `ROLE_`.
- `AuthorizationManager` là API hiện đại; `AccessDecisionManager` là legacy.
- `permitAll()` vẫn giữ request trong Spring Security filter chain; ignoring bỏ
  qua Spring Security hoàn toàn.
- `OAuth2 Login`, `OAuth2 Client`, và `Resource Server` là ba vai trò khác nhau.
- `SecurityContextHolder` tiện cho code hạ tầng, nhưng controller nên ưu tiên
  `@AuthenticationPrincipal` hoặc `@CurrentSecurityContext`.
- `SessionCreationPolicy.STATELESS` phù hợp bearer-token API, nhưng không phù
  hợp form login session-based thông thường.
