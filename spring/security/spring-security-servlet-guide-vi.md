# Hướng Dẫn Spring Security Servlet

Phạm vì nguồn: Spring Security Reference, Servlet Applications, phiên bản 7.1.0.

Tài liệu này đi theo đúng thứ tự của phần servlet trong tài liệu chính thức:
Getting Started, Architecture, Authentication, Authorization, OAuth2, SAML2,
Protection Against Exploits, Integrations, Configuration, Testing, và Appendix.
Nó dành cho lập trình viên Spring MVC / Spring Boot muốn hiểu đủ sâu để có thể
tùy biến Spring Security một cách đúng.

Nguồn chính thức:

- Servlet index: <https://docs.spring.io/spring-security/reference/servlet/index.html>
- Getting started: <https://docs.spring.io/spring-security/reference/servlet/getting-started.html>
- Architecture: <https://docs.spring.io/spring-security/reference/servlet/architecture.html>
- Authentication: <https://docs.spring.io/spring-security/reference/servlet/authentication/index.html>
- Authorization: <https://docs.spring.io/spring-security/reference/servlet/authorization/index.html>
- OAuth2: <https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html>
- SAML2: <https://docs.spring.io/spring-security/reference/servlet/saml2/index.html>
- Exploit protection: <https://docs.spring.io/spring-security/reference/servlet/exploits/index.html>
- Integrations: <https://docs.spring.io/spring-security/reference/servlet/integrations/index.html>
- Java configuration: <https://docs.spring.io/spring-security/reference/servlet/configuration/java.html>
- Testing: <https://docs.spring.io/spring-security/reference/servlet/test/index.html>

## 1. Getting Started

Spring Security tích hợp với ứng dụng servlet thông qua chuẩn Servlet `Filter`.
Trong Spring Boot, chỉ cần thêm Spring Security vào classpath là mặc định mọi
endpoint đều yêu cầu đăng nhập.

Dependency tối thiểu:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

Với dependency này, Boot tạo user mặc định tên `user`, in password được sinh ra
khi khởi động, bật form login, bật HTTP Basic, bảo vệ các request thay đổi dữ
liệu bằng CSRF, thêm các response header quan trọng, và yêu cầu authentication
cho tất cả endpoint.

Thử hành vi mặc định:

```bash
curl -i http://localhost:8080/api/me
# HTTP/1.1 401

curl -i -u user:<generated-password> http://localhost:8080/api/me
# Request đã đi tới controller. Nếu route không tồn tại thì nhận 404,
# điều này chứng minh authentication đã thành công.
```

Khi thiết kế security cho servlet app, nghĩ theo thứ tự này:

1. Protocol: HTTP thông thường, WebSocket, browser form app, REST API, hoặc
   gateway.
2. Authentication: form login, Basic, JWT resource server, OAuth2 login, SAML2,
   LDAP, x509, pre-authentication, hoặc cơ chế tùy biến.
3. State: web login dùng session hay API stateless dùng bearer token.
4. Authorization: rule theo URL, method, domain object, hoặc
   `AuthorizationManager` tùy biến.
5. Defense: CSRF, headers, CORS, firewall rules, session fixation, logout, và
   observability.

Demo nền tảng:

```java
@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    SecurityFilterChain web(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults())
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
```

Use case:

- Browser admin console: giữ `formLogin`, giữ CSRF, dùng session.
- REST API cho mobile client: ưu tiên OAuth2 Resource Server với JWT hoặc opaque
  token; tắt session nếu API thật sự stateless.
- Gateway hoặc BFF: thường dùng OAuth2 Login cho browser và OAuth2 Client để gọi
  downstream services.

## 2. Architecture

### 2.1 Servlet filters

Servlet container tạo một `FilterChain` cho mỗi request. Filter có thể chạy logic
trước servlet, dừng request bằng cách ghi response, bọc hoặc sửa request/response,
hoặc chạy logic sau khi chain phía sau trả về.

Spring Security dựa trên filter. Điều này quan trọng vì thứ tự filter quyết định
hành vi. Authentication phải chạy trước authorization. CSRF và CORS phải nằm ở
vị trí có thể xử lý request trước khi application code chạy.

### 2.2 DelegatingFilterProxy

Servlet container biết servlet filter, nhưng không biết Spring bean.
`DelegatingFilterProxy` nối hai thế giới này. Container gọi
`DelegatingFilterProxy`; proxy tìm Spring bean và ủy quyền công việc filter cho
bean đó.

Trong Spring Boot, bạn thường không cần đăng ký thủ công. Boot phát hiện bean
filter của Spring Security và đăng ký vào servlet filter chain.

### 2.3 FilterChainProxy

`FilterChainProxy` là servlet filter chính của Spring Security. Nó ủy quyền cho
một hoặc nhiều `SecurityFilterChain`. Đây cũng là nơi trung tâm để Spring
Security áp dụng HTTP firewall và xóa security context sau mỗi request, tránh rò
rỉ state giữa các servlet thread được tái sử dụng.

Mẹo debug: khi không hiểu vì sao một request bị secure, hãy xem
`SecurityFilterChain` nào match và các filter nào nằm trong chain đó.

### 2.4 SecurityFilterChain

Một `SecurityFilterChain` có hai nhiệm vụ:

- quyết định nó có áp dụng cho request hiện tại không;
- chứa các Spring Security filter cho request đã match.

Chỉ chain match đầu tiên được dùng. Điều này quan trọng khi tách API và web
security:

```java
@Configuration
@EnableWebSecurity
class MultiChainSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                .anyRequest().hasAuthority("SCOPE_api.read"))
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    SecurityFilterChain web(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/login", "/assets/**").permitAll()
                .anyRequest().authenticated())
            .formLogin(Customizer.withDefaults());

        return http.build();
    }
}
```

Ví dụ request:

- `/api/orders` match chain đầu tiên và dùng JWT authentication.
- `/admin` không match `/api/**`, nên tiếp tục rồi match web chain.

### 2.5 Security filters

Filter được thêm bởi DSL. Ví dụ:

- `csrf()` thêm CSRF protection.
- `formLogin()` thêm filter và handler cho username/password form login.
- `httpBasic()` thêm Basic authentication.
- `oauth2ResourceServer().jwt()` thêm bearer token authentication với JWT
  decoder.
- `authorizeHttpRequests()` thêm request authorization gần cuối security chain.

Khi thêm custom filter, đặt nó tương đối với một Spring Security filter đã biết:

```java
http.addFilterBefore(new TenantHeaderFilter(), UsernamePasswordAuthenticationFilter.class);
```

Chỉ làm vậy khi logic tùy biến thật sự là cross-cutting. Phần lớn tùy biến
authorization nên dùng `AuthorizationManager`, method security, hoặc service của
ứng dụng thay vì raw filter.

## 3. Authentication

Authentication trả lời câu hỏi: "User hoặc client này là ai?"

Mô hình servlet authentication có các object chính:

- `SecurityContextHolder`: lưu `SecurityContext` hiện tại.
- `SecurityContext`: lưu `Authentication` hiện tại.
- `Authentication`: biểu diễn credential request chưa xác thực hoặc principal đã
  xác thực.
- `GrantedAuthority`: role, scope, hoặc permission của principal.
- `AuthenticationManager`: API được filter dùng để authenticate.
- `ProviderManager`: implementation phổ biến của `AuthenticationManager`.
- `AuthenticationProvider`: validate một kiểu authentication.
- `AuthenticationEntryPoint`: bắt đầu authentication khi cần credential.

### 3.1 SecurityContextHolder

Mặc định, Spring Security lưu context trong `ThreadLocal`. Nhờ vậy code trong
cùng request thread có thể đọc current user mà không cần truyền user qua mọi
method.

Đọc current user:

```java
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
String username = authentication.getName();
Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
```

Trong Spring MVC, nên inject vào method controller:

```java
@GetMapping("/api/me")
Map<String, Object> me(@AuthenticationPrincipal UserDetails user) {
    return Map.of("username", user.getUsername(), "authorities", user.getAuthorities());
}
```

Nếu tạo context thủ công, tạo empty context mới thay vì sửa một context có thể
bị chia sẻ:

```java
SecurityContext context = SecurityContextHolder.createEmptyContext();
context.setAuthentication(authentication);
SecurityContextHolder.setContext(context);
```

### 3.2 Authentication và GrantedAuthority

`Authentication` thường có:

- `principal`: danh tính user, thường là `UserDetails`, `Jwt`, hoặc `OAuth2User`.
- `credentials`: bằng chứng bí mật như password; thường bị xóa sau khi thành
  công.
- `authorities`: quyền cấp ứng dụng như `ROLE_ADMIN` hoặc `SCOPE_orders.read`.

Quy tắc thực tế:

- Dùng role cho nhóm user lớn: `ROLE_ADMIN`, `ROLE_SUPPORT`.
- Dùng permission/scope cho khả năng: `invoice:approve`, `SCOPE_orders.read`.
- Không tạo một authority cho mỗi domain object như `ORDER_123_READ`; cách này
  không scale. Hãy dùng method security hoặc domain object authorization.

### 3.3 AuthenticationManager, ProviderManager, và AuthenticationProvider

`ProviderManager` ủy quyền authentication cho danh sách provider. Mỗi provider
có thể authenticate, reject, hoặc nói "tôi không support token type này."

Ví dụ phổ biến:

- `DaoAuthenticationProvider`: username/password với `UserDetailsService` và
  `PasswordEncoder`.
- `JwtAuthenticationProvider`: bearer JWT.
- SAML provider: validate SAML assertion.
- Custom provider: verify OTP, API key, signed request, hoặc legacy token.

Demo custom provider:

```java
final class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {
    private final String apiKey;

    ApiKeyAuthenticationToken(String apiKey) {
        super(null);
        this.apiKey = apiKey;
        setAuthenticated(false);
    }

    ApiKeyAuthenticationToken(String apiKey, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.apiKey = apiKey;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return this.apiKey;
    }

    @Override
    public Object getPrincipal() {
        return "api-key-client";
    }
}

@Component
class ApiKeyAuthenticationProvider implements AuthenticationProvider {
    @Override
    public Authentication authenticate(Authentication authentication) {
        String apiKey = (String) authentication.getCredentials();
        if (!"known-demo-key".equals(apiKey)) {
            throw new BadCredentialsException("Invalid API key");
        }
        return new ApiKeyAuthenticationToken(apiKey, List.of(new SimpleGrantedAuthority("SCOPE_api.read")));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ApiKeyAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
```

Trong code thật, không hard-code key. Hãy validate key đã hash trong storage
hoặc dùng token protocol đáng tin cậy.

### 3.4 Username và password authentication

Username/password authentication thường dùng:

- filter đọc credential như `UsernamePasswordAuthenticationFilter`;
- `AuthenticationManager`;
- `DaoAuthenticationProvider`;
- `UserDetailsService`;
- `PasswordEncoder`.

Setup password cho production:

```java
@Bean
PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
}

@Bean
UserDetailsService users(PasswordEncoder encoder) {
    UserDetails admin = User.builder()
        .username("admin")
        .password(encoder.encode("change-me"))
        .roles("ADMIN")
        .build();

    return new InMemoryUserDetailsManager(admin);
}
```

`User.withDefaultPasswordEncoder()` chỉ nên dùng cho sample. Production nên dùng
`PasswordEncoder` thật, thường là delegating encoder, vì nó prefix hash bằng id
như `{bcrypt}` để thuật toán cũ và mới cùng tồn tại được.

### 3.5 Form login

Flow form login:

1. Browser request URL cần bảo vệ.
2. Authorization fail vì user anonymous.
3. `ExceptionTranslationFilter` gọi `AuthenticationEntryPoint`.
4. Entry point redirect tới login page.
5. User submit username và password.
6. `UsernamePasswordAuthenticationFilter` tạo token và gọi
   `AuthenticationManager`.
7. Thành công: Spring Security lưu `Authentication`, áp dụng session strategy,
   publish event, và redirect tới request đã lưu.
8. Thất bại: clear context và gọi failure handler.

Custom login page:

```java
http
    .authorizeHttpRequests(authorize -> authorize
        .requestMatchers("/login", "/css/**").permitAll()
        .anyRequest().authenticated())
    .formLogin(form -> form
        .loginPage("/login")
        .loginProcessingUrl("/login")
        .defaultSuccessUrl("/dashboard", true)
        .failureUrl("/login?error")
        .permitAll());
```

HTML form mặc định phải submit field tên `username` và `password`. Nếu CSRF đang
bật, form phải gửi kèm CSRF token.

### 3.6 HTTP Basic và Digest

HTTP Basic gửi credential trong mỗi request bằng header `Authorization`. Chỉ dùng
qua HTTPS. Nó hữu ích cho demo service-to-service, internal tool, và test, nhưng
bearer token thường phù hợp hơn cho API hiện đại.

```java
http
    .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
    .httpBasic(Customizer.withDefaults());
```

Digest authentication tồn tại cho compatibility nhưng hiếm khi được chọn cho
ứng dụng mới. Ưu tiên HTTPS cùng cơ chế mạnh hơn.

### 3.7 Password storage, JDBC, LDAP, và UserDetails

`UserDetailsService` load user record. `DaoAuthenticationProvider` so sánh
password user vừa nhập với stored hash thông qua `PasswordEncoder`.

Use case:

- In-memory users: test, demo, internal tool rất nhỏ.
- JDBC users: ứng dụng đơn giản có bảng user trong relational database.
- Custom `UserDetailsService`: phổ biến khi user đã nằm trong domain model.
- LDAP hoặc Active Directory: enterprise identity store.

Phác thảo custom `UserDetailsService`:

```java
@Service
class AccountUserDetailsService implements UserDetailsService {
    private final AccountRepository accounts;

    AccountUserDetailsService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        Account account = accounts.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(username));

        return User.withUsername(account.username())
            .password(account.passwordHash())
            .authorities(account.permissions().toArray(String[]::new))
            .disabled(!account.enabled())
            .build();
    }
}
```

### 3.8 Authentication persistence và sessions

Với web app stateful, authentication thành công được lưu để request sau trong
cùng session biết user. Với API stateless, thường set
`SessionCreationPolicy.STATELESS` và authenticate mỗi request từ bearer token.

```java
http.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.STATELESS));
```

Các chủ đề session cần hiểu:

- session fixation protection đổi session id sau login;
- concurrent session control giới hạn số session của một user;
- invalid session handling quyết định response khi session hết hạn;
- remember-me tạo tín hiệu login dài hạn, không thay thế strong authentication.

### 3.9 Các cơ chế authentication khác

Phần servlet của Spring Security cũng có:

- MFA và one-time token cho step-up authentication;
- passkeys/WebAuthn cho login chống phishing;
- anonymous authentication để user chưa đăng nhập vẫn có `Authentication` object;
- pre-authentication khi hệ thống bên ngoài đã authenticate user;
- JAAS, CAS, X.509, Kerberos, Run-As, logout, và authentication events.

Dùng các cơ chế này khi môi trường deploy hoặc tổ chức cần protocol đó. Ví dụ,
dùng X.509 khi client certificate định danh user; dùng pre-authentication sau
trusted reverse proxy chỉ khi proxy strip và set identity header an toàn.

### 3.10 Kerberos

Kerberos là network authentication protocol phổ biến trong môi trường
Windows/Active Directory. Trong servlet application, nó thường được chọn cho
enterprise single sign-on khi browser hoặc desktop client có thể lấy service
ticket từ Key Distribution Center.

Thành phần thường gặp:

- KDC: Kerberos authority đáng tin cậy, thường là Active Directory.
- Principal: tên định danh của user hoặc service.
- Keytab: file chứa service keys dài hạn. Hãy xem nó như secret.
- SPNEGO: cơ chế negotiate Kerberos qua HTTP giữa browser và server.

Dùng Kerberos khi công ty đã chuẩn hóa Kerberos/AD SSO. Không nên chọn nó cho
public web hoặc mobile API mới nếu môi trường deploy không bắt buộc. OAuth2/OIDC
thường dễ hơn cho internet-facing và API-first systems.

## 4. Authorization

Authorization trả lời câu hỏi: "Principal đã authenticate này có được phép làm
việc này không?"

Spring Security 7 nhấn mạnh `AuthorizationManager` API. Các API cũ như
`AccessDecisionManager` và voter là legacy, nên tránh trong ứng dụng mới.

### 4.1 Authorities và roles

`AuthorizationManager` đọc authorities từ `Authentication` hiện tại. Rule role
như `hasRole("ADMIN")` mặc định tìm `ROLE_ADMIN`.

Nên viết:

```java
.requestMatchers("/admin/**").hasRole("ADMIN")
.requestMatchers("/orders/**").hasAuthority("order:read")
```

Nếu tổ chức không dùng prefix `ROLE_`, expose `GrantedAuthorityDefaults`, nhưng
hãy làm có chủ đích vì nó ảnh hưởng các role check:

```java
@Bean
static GrantedAuthorityDefaults grantedAuthorityDefaults() {
    return new GrantedAuthorityDefaults("");
}
```

### 4.2 Authorize HTTP requests

`authorizeHttpRequests` cấu hình URL authorization. Rule được đánh giá theo thứ
tự. Rule match đầu tiên thắng.

```java
http.authorizeHttpRequests(authorize -> authorize
    .requestMatchers("/", "/health", "/login").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
    .requestMatchers(HttpMethod.POST, "/api/products/**").hasAuthority("product:write")
    .requestMatchers("/admin/**").hasRole("ADMIN")
    .anyRequest().authenticated());
```

Chi tiết quan trọng:

- Đặt rule cụ thể trước rule rộng.
- Đặt `anyRequest()` làm fallback cuối.
- Static resources có thể `permitAll()` thay vì ignore, trừ khi bạn có ý muốn
  chung nằm ngoài security filter chain.
- `AuthorizationFilter` chạy gần cuối security chain, sau authentication và các
  built-in protection.
- Servlet dispatch như `FORWARD` và `ERROR` cũng có thể bị authorize. Nếu view
  hoặc error page bị lỗi, permit rõ ràng các dispatcher type đó.

Ví dụ dispatcher:

```java
http.authorizeHttpRequests(authorize -> authorize
    .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
    .anyRequest().authenticated());
```

Use case cho custom `AuthorizationManager`: chỉ cho truy cập khi tenant trong
path khớp với tenant của user.

```java
AuthorizationManager<RequestAuthorizationContext> sameTenant = (authentication, context) -> {
    String tenantId = context.getVariables().get("tenantId");
    boolean granted = authentication.get().getAuthorities().stream()
        .anyMatch(authority -> authority.getAuthority().equals("TENANT_" + tenantId));
    return new AuthorizationDecision(granted);
};

http.authorizeHttpRequests(authorize -> authorize
    .requestMatchers("/tenants/{tenantId}/**").access(sameTenant)
    .anyRequest().denyAll());
```

### 4.3 Method security

Bật method security:

```java
@Configuration
@EnableMethodSecurity
class MethodSecurityConfig {
}
```

Sau đó secure service:

```java
@Service
class OrderService {

    @PreAuthorize("hasAuthority('order:read')")
    OrderDto findById(String id) {
        return loadOrder(id);
    }

    @PreAuthorize("@orderSecurity.canApprove(authentication, #id)")
    void approve(String id) {
        approveOrder(id);
    }

    @PostAuthorize("returnObject.owner == authentication.name")
    OrderDto findOwnOrder(String id) {
        return loadOrder(id);
    }
}
```

Dùng method security cho business rule vì controller không phải entry point duy
nhất vào service. Tránh viết SpEL phức tạp khắp nơi; đưa check phức tạp vào bean
như `@orderSecurity`.

Method security được áp dụng bằng Spring AOP. Self-invocation trong cùng class
có thể bypass proxy. Nếu method `a()` gọi secured method `b()` trong cùng object,
call đó có thể không đi qua security interceptor. Tách secured method sang bean
khác hoặc gọi qua proxy khi cần.

### 4.4 Domain object ACLs và authorization events

ACL dành cho permission theo từng object, ví dụ "user A được đọc document 42."
Nó mạnh nhưng nặng hơn role/authority check thông thường. Dùng khi cần dynamic
object permissions và database-backed permission model.

Authorization events giúp ứng dụng quan sát granted/denied decisions. Chúng hữu
ích cho audit log và security monitoring, nhưng tránh log payload nhạy cảm hoặc
credential.

## 5. OAuth2

OAuth2 support cho servlet của Spring Security có ba vai trò lớn:

- Resource Server: bảo vệ API bằng cách validate bearer token.
- Client: lấy và lưu authorized client token để gọi hệ thống khác.
- Authorization Server: phát hành token. Phần này do Spring Authorization Server
  cung cấp và có model riêng.

OAuth2 Login là client feature để đăng nhập user bằng OAuth2 hoặc OpenID
Connect provider.

### 5.1 OAuth2 Resource Server

Dùng khi API nhận `Authorization: Bearer <token>`.

JWT configuration với issuer discovery:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://issuer.example.com
```

Security chain tương đương:

```java
http
    .authorizeHttpRequests(authorize -> authorize
        .requestMatchers("/actuator/health").permitAll()
        .requestMatchers("/api/admin/**").hasAuthority("SCOPE_admin")
        .anyRequest().authenticated())
    .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
```

JWT flow:

1. `BearerTokenAuthenticationFilter` lấy bearer token.
2. `JwtDecoder` validate signature, issuer, expiry, và các claim được cấu hình.
3. Spring convert claims thành authorities.
4. Authorization rules dùng các authorities đó.

Tùy biến JWT authorities:

```java
@Bean
JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
    scopes.setAuthorityPrefix("SCOPE_");
    scopes.setAuthoritiesClaimName("scope");

    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(scopes);
    return converter;
}

http.oauth2ResourceServer(oauth2 -> oauth2
    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
```

Opaque token configuration:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        opaquetoken:
          introspection-uri: https://issuer.example.com/oauth2/introspect
          client-id: resource-api
          client-secret: ${INTROSPECTION_SECRET}
```

Dùng opaque token khi authorization server muốn API introspect token thay vì
validate JWT tự chứa dữ liệu.

### 5.2 OAuth2 Login

Dùng OAuth2 Login khi ứng dụng đăng nhập user thông qua Google, GitHub, Keycloak,
Okta, Azure AD, hoặc OIDC/OAuth2 provider khác.

Boot configuration cơ bản:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-id: notes-web
            client-secret: ${KEYCLOAK_CLIENT_SECRET}
            scope: openid,profile,email
        provider:
          keycloak:
            issuer-uri: https://sso.example.com/realms/notes
```

Security chain:

```java
http
    .authorizeHttpRequests(authorize -> authorize
        .requestMatchers("/", "/login").permitAll()
        .anyRequest().authenticated())
    .oauth2Login(oauth2 -> oauth2
        .defaultSuccessUrl("/dashboard", true));
```

Use case:

- Nếu app có browser UI và muốn centralized identity, dùng OAuth2 Login/OIDC.
- Nếu app chỉ expose API và nhận bearer token, dùng Resource Server thay vì
  OAuth2 Login.

Tùy biến mapping user đã đăng nhập:

```java
@Bean
OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
    OidcUserService delegate = new OidcUserService();
    return request -> {
        OidcUser user = delegate.loadUser(request);
        Set<GrantedAuthority> mapped = new HashSet<>(user.getAuthorities());
        mapped.add(new SimpleGrantedAuthority("ROLE_USER"));
        return new DefaultOidcUser(mapped, user.getIdToken(), user.getUserInfo());
    };
}
```

### 5.3 OAuth2 Client

Dùng OAuth2 Client khi ứng dụng cần gọi service khác được bảo vệ bằng OAuth2.
Khái niệm quan trọng:

- `ClientRegistration`: client id, secret, scopes, provider endpoints.
- `OAuth2AuthorizedClient`: client registration cộng access/refresh tokens.
- `OAuth2AuthorizedClientManager`: lấy, refresh, và trả về authorized clients.

Ví dụ với `WebClient`:

```java
@Bean
WebClient crmClient(OAuth2AuthorizedClientManager authorizedClientManager) {
    ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2 =
        new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
    oauth2.setDefaultClientRegistrationId("crm");

    return WebClient.builder()
        .apply(oauth2.oauth2Configuration())
        .baseUrl("https://crm.example.com")
        .build();
}
```

Use case grant phổ biến:

- Authorization Code: có user hiện diện; app gọi service khác thay mặt user.
- Client Credentials: không có user; backend service gọi backend khác bằng danh
  tính của chính nó.
- Refresh Token: client gia hạn access mà không bắt user login lại.
- JWT Bearer hoặc Token Exchange: delegation nâng cao, đổi một token sang
  audience hoặc subject khác.

Với service-to-service calls, ưu tiên client credentials với scope hẹp và
short-lived access token:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          billing:
            authorization-grant-type: client_credentials
            client-id: notes-service
            client-secret: ${BILLING_CLIENT_SECRET}
            scope: invoice:read
        provider:
          billing:
            token-uri: https://issuer.example.com/oauth2/token
```

### 5.4 Authorization Server

Spring Security reference nhắc tới vai trò authorization server, nhưng
implementation nằm trong Spring Authorization Server. Dùng khi tổ chức cần phát
hành OAuth2/OIDC tokens. Không nên thêm authorization server vào mỗi ứng dụng.
Nên centralize trừ khi có lý do sản phẩm rõ ràng.

## 6. SAML2

SAML2 phổ biến trong enterprise single sign-on. Trong SAML:

- Identity Provider (IdP): authenticate user, ví dụ Okta, Azure AD, ADFS.
- Service Provider (SP): ứng dụng của bạn.
- Assertion: identity statement đã ký gửi từ IdP tới SP.
- Relying Party Registration: cấu hình Spring Security cho một cặp SP/IdP.

Boot-style configuration tối thiểu:

```yaml
spring:
  security:
    saml2:
      relyingparty:
        registration:
          okta:
            assertingparty:
              metadata-uri: https://idp.example.com/metadata
```

Security chain:

```java
http
    .authorizeHttpRequests(authorize -> authorize
        .anyRequest().authenticated())
    .saml2Login(Customizer.withDefaults())
    .saml2Logout(Customizer.withDefaults());
```

Dùng SAML2 khi enterprise IdP bắt buộc SAML. Dùng OIDC khi có thể chọn protocol
mới hơn và muốn tích hợp API/mobile đơn giản hơn.

SAML login flow:

1. User request protected URL trong service provider application.
2. Spring Security gửi authentication request tới identity provider.
3. User authenticate tại identity provider.
4. Identity provider POST signed SAML response tới ACS endpoint của ứng dụng.
5. Spring Security validate signature, recipient, audience, time window, và
   relying party details đã cấu hình.
6. `Saml2Authentication` được lưu trong security context.
7. URL hoặc method authorization dùng authorities được map từ assertion.

Customization points:

- Dùng metadata để tránh copy thủ công IdP certificates và endpoints.
- Map SAML attributes thành application authorities trong converter hoặc user
  service.
- Cấu hình logout chỉ sau khi xác nhận IdP support flow bạn cần.
- Rotate certificates có kế hoạch; certificate SAML lỗi thường làm tất cả user
  không login được.

## 7. Protection Against Exploits

Spring Security bật sẵn nhiều lớp phòng vệ. Hãy hiểu chúng trước khi tắt.

### 7.1 CSRF

CSRF protection ngăn một website độc hại lợi dụng browser của user để submit
request thay đổi dữ liệu bằng cookie của user.

Giữ CSRF cho browser app dùng cookie hoặc session:

```java
http.csrf(Customizer.withDefaults());
```

Với stateless bearer-token API không dùng cookie để authenticate, thường tắt
CSRF:

```java
http
    .csrf(csrf -> csrf.disable())
    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
```

Pattern SPA với CSRF cookie đọc được:

```java
http.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
```

Frontend đọc token cookie và gửi token trong header được yêu cầu cho các method
không an toàn như POST, PUT, PATCH, DELETE.

### 7.2 Security headers

Spring Security ghi các header giảm thiểu browser attack phổ biến, gồm cache
control, content sniffing, clickjacking, và rủi ro downgrade HTTPS.

Chỉ tùy biến khi bạn hiểu hành vi browser cần có:

```java
http.headers(headers -> headers
    .frameOptions(frame -> frame.sameOrigin())
    .httpStrictTransportSecurity(hsts -> hsts
        .includeSubDomains(true)
        .preload(true)));
```

Dùng `frameOptions().sameOrigin()` cho H2 console hoặc admin frame cùng origin
trong development. Không tắt frame protection toàn cục trong production trừ khi
page có chủ đích cho embed và được bảo vệ bằng cách khác.

### 7.3 HTTP firewall

`HttpFirewall` normalize và reject request đáng nghi trước khi chúng tới ứng
dụng. Nó giúp chống path traversal, encoded path tricks, và cách xử lý URL mơ
hồ.

Nếu client hợp lệ bị reject, ưu tiên sửa URL của client. Chỉ nới lỏng firewall
sau khi đã xác minh rủi ro.

### 7.4 HTTPS, sessions, và logout

Dùng HTTPS ở mọi nơi. Khi chạy sau reverse proxy, cấu hình forwarded headers để
ứng dụng hiểu scheme gốc:

```yaml
server:
  forward-headers-strategy: framework
```

Logout thường invalidate session, clear security context, và xóa remember-me
cookies:

```java
http.logout(logout -> logout
    .logoutUrl("/logout")
    .logoutSuccessUrl("/")
    .deleteCookies("JSESSIONID"));
```

## 8. Integrations

Spring Security tích hợp với servlet APIs, Spring MVC, CORS, WebSocket, Spring
Data, concurrency utilities, JSP taglibs, localization, và observability.

### 8.1 Servlet API

Spring Security tích hợp với các servlet methods:

- `HttpServletRequest#getRemoteUser()`
- `HttpServletRequest#getUserPrincipal()`
- `HttpServletRequest#isUserInRole(String role)`
- `HttpServletRequest#login(username, password)`
- `HttpServletRequest#logout()`

Nên dùng abstraction của Spring Security trong application code, nhưng tích hợp
này hữu ích cho library và legacy servlet code.

### 8.2 Spring MVC

MVC integration hữu ích:

```java
@GetMapping("/profile")
String profile(@AuthenticationPrincipal CustomUser user, Model model) {
    model.addAttribute("user", user);
    return "profile";
}

@GetMapping("/context")
String context(@CurrentSecurityContext SecurityContext context) {
    return context.getAuthentication().getName();
}
```

### 8.3 CORS

CORS phải chạy trước Spring Security vì preflight request không có credentials.
Cung cấp `CorsConfigurationSource` và bật `cors()`.

```java
@Bean
CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("https://app.example.com"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}

http.cors(Customizer.withDefaults());
```

Không dùng `*` với credentials trong production. Liệt kê origin cụ thể.

### 8.4 Concurrency

Security context mặc định là thread-local. Khi chạy việc ở thread khác, wrap
task để propagate context:

```java
Executor delegate = Executors.newFixedThreadPool(4);
Executor secured = new DelegatingSecurityContextExecutor(delegate);
secured.execute(() -> auditService.recordCurrentUserAction());
```

### 8.5 Spring Data

Spring Data integration có thể dùng current principal trong repository query,
nhưng authorization phức tạp nên ở service. Database filtering hữu ích cho
multi-tenant reads, còn method security vẫn bảo vệ service operations.

### 8.6 WebSocket

Với WebSocket/STOMP, authenticate HTTP handshake và authorize messages. Đừng bao
giờ giả định secure handshake là đủ; message destinations cũng cần rules.

### 8.7 Observability

Security events và observations giúp audit và monitoring. Ghi username hoặc
stable subject id, kết quả decision, và endpoint name. Tránh ghi token, password,
cookie, authorization header, hoặc full sensitive payload.

## 9. Configuration

### 9.1 Java configuration

Cấu hình servlet hiện đại dùng bean, không dùng `WebSecurityConfigurerAdapter`.
Tạo một hoặc nhiều `SecurityFilterChain` bean.

Ví dụ đầy đủ nên dùng:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/login", "/assets/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll())
            .logout(logout -> logout
                .logoutSuccessUrl("/"))
            .csrf(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
```

### 9.2 Multiple chains

Dùng multiple chains khi các nhóm request cần protocol khác nhau. Dùng
`securityMatcher` để giới hạn chain và `@Order` để làm rõ precedence.

Không nhét hành vi API và browser vào cùng một chain nếu nó làm CSRF, session,
hoặc entry-point behavior khó hiểu.

### 9.3 Ignoring và permitting

`permitAll()` giữ request trong security filter chain và chỉ grant access.
`web.ignoring()` bỏ qua Spring Security hoàn toàn.

Ưu tiên `permitAll()` cho login pages, static resources, health endpoints, và
public APIs, trừ khi bạn chủ đích muốn zero Spring Security behavior.

### 9.4 Custom DSLs và shared objects

Ứng dụng nâng cao có thể tạo custom configurer hoặc lấy shared object từ
`HttpSecurity`. Dùng cho infrastructure tái sử dụng, không dùng cho business rule
nên nằm trong service.

### 9.5 Kotlin và namespace configuration

Servlet reference cũng document Kotlin DSL và XML namespace configuration. Với
ứng dụng Spring Boot Java/Kotlin mới, nên dùng bean-based Java/Kotlin
configuration với `SecurityFilterChain`.

Dùng XML namespace configuration chủ yếu khi maintain ứng dụng cũ đã dùng XML.
Tránh trộn XML và Java configuration cho cùng một security concern nếu không có
migration plan, vì sẽ khó đoán chain hoặc bean nào sở hữu rule.

Ví dụ Kotlin:

```kotlin
@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    fun security(http: HttpSecurity): SecurityFilterChain {
        http {
            authorizeHttpRequests {
                authorize("/admin/**", hasRole("ADMIN"))
                authorize(anyRequest, authenticated)
            }
            formLogin { }
        }
        return http.build()
    }
}
```

## 10. Testing

Thêm test support:

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

### 10.1 MockMvc setup

Với Spring Boot tests, dùng MockMvc và Spring Security test support:

```java
@SpringBootTest
@AutoConfigureMockMvc
class OrderSecurityTests {

    @Autowired
    MockMvc mvc;

    @Test
    void apiRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/orders"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "order:read")
    void userWithAuthorityCanReadOrders() throws Exception {
        mvc.perform(get("/api/orders"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void postRequiresCsrf() throws Exception {
        mvc.perform(post("/profile"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void postWithCsrfCanReachController() throws Exception {
        mvc.perform(post("/profile").with(csrf()))
            .andExpect(status().is3xxRedirection());
    }
}
```

### 10.2 Test form login, Basic, logout, và OAuth2

```java
@Test
void formLoginAuthenticates() throws Exception {
    mvc.perform(formLogin().user("admin").password("password"))
        .andExpect(authenticated());
}

@Test
void basicAuthenticationWorks() throws Exception {
    mvc.perform(get("/api/me").with(httpBasic("admin", "password")))
        .andExpect(status().isOk());
}

@Test
void logoutClearsAuthentication() throws Exception {
    mvc.perform(logout())
        .andExpect(unauthenticated());
}

@Test
void oauth2UserCanAccessProfile() throws Exception {
    mvc.perform(get("/profile").with(oauth2Login()
            .attributes(attributes -> attributes.put("sub", "user-123"))))
        .andExpect(status().isOk());
}
```

Request post-processors cho phép gắn security state vào request mà không cần
chạy external identity provider đầy đủ:

```java
@Test
void jwtScopeCanReadApi() throws Exception {
    mvc.perform(get("/api/orders").with(jwt()
            .authorities(new SimpleGrantedAuthority("SCOPE_orders.read"))))
        .andExpect(status().isOk());
}

@Test
void samlUserCanOpenDashboard() throws Exception {
    mvc.perform(get("/dashboard").with(saml2Login()))
        .andExpect(status().isOk());
}
```

Các test này không chứng minh external provider được cấu hình đúng. Chứng minh
rằng ứng dụng của bạn phản hồi đúng khi Spring Security đã tạo `Authentication`
mong đợi.

### 10.3 Test method security

```java
@SpringBootTest
class MethodSecurityTests {

    @Autowired
    OrderService orders;

    @Test
    @WithMockUser(authorities = "order:read")
    void canReadWithAuthority() {
        assertThatNoException().isThrownBy(() -> orders.findById("order-1"));
    }

    @Test
    @WithMockUser
    void cannotReadWithoutAuthority() {
        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(() -> orders.findById("order-1"));
    }
}
```

Hãy test rule bạn phụ thuộc, không chỉ happy path. Với mỗi endpoint nhạy cảm,
nên có case unauthenticated, authenticated-nhưng-forbidden, và allowed.

## 11. Appendix và Checklist Tùy Biến Thực Tế

### 11.1 Database schemas

Spring Security document các schema cho JDBC users, authorities, remember-me
tokens, OAuth2 authorized clients, và ACLs. Dùng official schema làm điểm bắt
đầu, sau đó điều chỉnh naming và migrations theo project.

### 11.2 Proxy server configuration

Khi chạy sau Nginx, load balancer, Kubernetes ingress, hoặc platform proxy, cấu
hình forwarded headers để redirects, secure cookies, và HSTS khớp public URL.

Boot setting phổ biến:

```yaml
server:
  forward-headers-strategy: framework
```

Cũng cần cấu hình proxy set `X-Forwarded-Proto`, `X-Forwarded-Host`, và các
header liên quan đúng cách, đồng thời không trust forwarded headers từ public
internet nếu proxy không strip giá trị không tin cậy.

### 11.3 Decision checklist

Dùng checklist này khi tùy biến:

1. Loại client nào gọi endpoint: browser, SPA, mobile app, server, hay webhook?
2. Credential nào được gửi: cookie session, username/password, Basic, JWT,
   opaque token, SAML assertion, certificate, API key?
3. Request stateful hay stateless?
4. `SecurityFilterChain` nào phải match đầu tiên?
5. Authentication filter nào tạo `Authentication`?
6. Provider nào validate nó?
7. Authorities nào cần được tạo?
8. URL và method rules nào áp dụng?
9. Service-level method rules nào áp dụng?
10. CSRF có nên bật không?
11. Headers, CORS rules, và firewall behavior nào cần có?
12. Tests nào chứng minh unauthenticated, forbidden, và allowed behavior?

### 11.4 Lỗi phổ biến

- Tắt CSRF cho session-based browser app.
- Dùng `hasRole("ROLE_ADMIN")` thay vì `hasRole("ADMIN")`.
- Đặt broad request matcher trước specific matcher.
- Trộn browser form login và stateless API behavior trong một chain mà không có
  lý do rõ ràng.
- Quên rằng chỉ `SecurityFilterChain` match đầu tiên được chạy.
- Kỳ vọng method security intercept self-invocation.
- Log bearer token hoặc password khi authentication failure.
- Trust identity header từ proxy mà không đảm bảo proxy overwrite incoming
  header không tin cậy.
