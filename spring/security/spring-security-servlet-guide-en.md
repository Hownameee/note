# Spring Security Servlet Guide

Source scope: Spring Security Reference, Servlet Applications, version 7.1.0.

This guide follows the documentation order from the official servlet reference:
Getting Started, Architecture, Authentication, Authorization, OAuth2, SAML2,
Protection Against Exploits, Integrations, Configuration, Testing, and Appendix.
It is written for a Spring MVC / Spring Boot developer who wants to understand
the moving parts deeply enough to customize them.

Official sources:

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

Spring Security integrates with servlet applications through the standard
Servlet `Filter` API. In Spring Boot, adding Spring Security to the classpath is
enough to make every endpoint require authentication by default.

Minimal dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

With this dependency only, Boot creates a default user named `user`, logs a
generated password at startup, enables form login, enables HTTP Basic, protects
unsafe requests with CSRF, adds important response headers, and requires
authentication for every endpoint.

Try the default behavior:

```bash
curl -i http://localhost:8080/api/me
# HTTP/1.1 401

curl -i -u user:<generated-password> http://localhost:8080/api/me
# The request reaches your controller. If the route does not exist, you get 404,
# which proves authentication passed.
```

Think about a servlet security design in this order:

1. Protocol: normal HTTP, WebSocket, browser form app, REST API, or gateway.
2. Authentication: form login, Basic, JWT resource server, OAuth2 login, SAML2,
   LDAP, x509, pre-authentication, or a custom mechanism.
3. State: session-based web login or stateless bearer-token API.
4. Authorization: URL rules, method rules, domain object rules, or custom
   `AuthorizationManager`.
5. Defense: CSRF, headers, CORS, firewall rules, session fixation, logout, and
   observability.

Demo baseline:

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

- Browser admin console: keep `formLogin`, keep CSRF, use sessions.
- REST API used by mobile clients: prefer OAuth2 Resource Server with JWT or
  opaque tokens; disable sessions if the API is truly stateless.
- Gateway or BFF: often uses OAuth2 Login for the browser and OAuth2 Client to
  call downstream services.

## 2. Architecture

### 2.1 Servlet filters

The servlet container builds a `FilterChain` for each request. A filter can run
logic before the servlet, stop the request by writing a response, wrap or change
the request/response, or run logic after the downstream chain returns.

Spring Security is filter-driven. This matters because filter order determines
behavior. Authentication must happen before authorization. CSRF and CORS must be
placed where they can affect the request before application code runs.

### 2.2 DelegatingFilterProxy

The servlet container knows servlet filters, not Spring beans.
`DelegatingFilterProxy` bridges the two worlds. The container invokes
`DelegatingFilterProxy`; the proxy looks up a Spring bean and delegates filter
work to it.

In a Spring Boot app, you usually do not register this manually. Boot discovers
the Spring Security filter bean and registers it in the servlet filter chain.

### 2.3 FilterChainProxy

`FilterChainProxy` is Spring Security's main servlet filter. It delegates to one
or more `SecurityFilterChain` instances. It is also the central place where
Spring Security applies the HTTP firewall and clears security context state after
the request to prevent leaks between reused servlet threads.

Debugging tip: when you do not understand why a request is secured, inspect
which `SecurityFilterChain` matched and which filters are inside it.

### 2.4 SecurityFilterChain

A `SecurityFilterChain` has two responsibilities:

- decide whether it applies to the current request;
- hold the Spring Security filters for matching requests.

Only the first matching chain is used. This is important when you split API and
web security:

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

Request examples:

- `/api/orders` matches the first chain and uses JWT authentication.
- `/admin` does not match `/api/**`, so it falls through to the web chain.

### 2.5 Security filters

Filters are added by the DSL. For example:

- `csrf()` adds CSRF protection.
- `formLogin()` adds filters and handlers for username/password form login.
- `httpBasic()` adds Basic authentication support.
- `oauth2ResourceServer().jwt()` adds bearer token authentication with a JWT
  decoder.
- `authorizeHttpRequests()` adds request authorization near the end of the
  security chain.

When adding a custom filter, place it relative to a known Spring Security filter:

```java
http.addFilterBefore(new TenantHeaderFilter(), UsernamePasswordAuthenticationFilter.class);
```

Do this only when the custom logic is truly cross-cutting. Most authorization
customization should use `AuthorizationManager`, method security, or application
services instead of a raw filter.

## 3. Authentication

Authentication answers: "Who is this user or client?"

The servlet authentication model has these core objects:

- `SecurityContextHolder`: stores the current `SecurityContext`.
- `SecurityContext`: stores the current `Authentication`.
- `Authentication`: represents either an unauthenticated credential request or
  the authenticated principal.
- `GrantedAuthority`: roles, scopes, or permissions assigned to the principal.
- `AuthenticationManager`: API used by filters to authenticate.
- `ProviderManager`: common `AuthenticationManager` implementation.
- `AuthenticationProvider`: validates one authentication type.
- `AuthenticationEntryPoint`: starts authentication when credentials are needed.

### 3.1 SecurityContextHolder

By default, Spring Security stores the context in a `ThreadLocal`. That lets code
in the same request thread access the current user without explicitly passing it
through every method.

Read the current user:

```java
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
String username = authentication.getName();
Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
```

In Spring MVC, prefer controller method injection:

```java
@GetMapping("/api/me")
Map<String, Object> me(@AuthenticationPrincipal UserDetails user) {
    return Map.of("username", user.getUsername(), "authorities", user.getAuthorities());
}
```

If you create a context manually, create a new empty context instead of mutating
a shared one:

```java
SecurityContext context = SecurityContextHolder.createEmptyContext();
context.setAuthentication(authentication);
SecurityContextHolder.setContext(context);
```

### 3.2 Authentication and GrantedAuthority

`Authentication` usually contains:

- `principal`: the user identity, often `UserDetails`, `Jwt`, or `OAuth2User`.
- `credentials`: secret proof such as a password; usually erased after success.
- `authorities`: application-wide permissions like `ROLE_ADMIN` or
  `SCOPE_orders.read`.

Rules of thumb:

- Use roles for coarse user categories: `ROLE_ADMIN`, `ROLE_SUPPORT`.
- Use permissions/scopes for capabilities: `invoice:approve`,
  `SCOPE_orders.read`.
- Do not create one authority per domain object such as `ORDER_123_READ`; that
  does not scale. Use method security or domain object authorization for that.

### 3.3 AuthenticationManager, ProviderManager, and AuthenticationProvider

`ProviderManager` delegates authentication to a list of providers. Each provider
either authenticates, rejects, or says "I do not support this token type."

Common examples:

- `DaoAuthenticationProvider`: username/password with `UserDetailsService` and
  `PasswordEncoder`.
- `JwtAuthenticationProvider`: bearer JWT.
- SAML provider: validates SAML assertions.
- Custom provider: verifies an OTP, API key, signed request, or legacy token.

Custom provider demo:

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

In real code, never hard-code the key. Validate a hashed key from storage or use
a trusted token protocol.

### 3.4 Username and password authentication

Username/password authentication normally uses:

- a credential-reading filter such as `UsernamePasswordAuthenticationFilter`;
- `AuthenticationManager`;
- `DaoAuthenticationProvider`;
- `UserDetailsService`;
- `PasswordEncoder`.

Production password setup:

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

`User.withDefaultPasswordEncoder()` is useful for samples only. For production,
use a real `PasswordEncoder`, normally the delegating encoder, which prefixes
hashes with an id such as `{bcrypt}` so old and new algorithms can coexist.

### 3.5 Form login

Form login flow:

1. A browser requests a protected URL.
2. Authorization fails because the user is anonymous.
3. `ExceptionTranslationFilter` calls an `AuthenticationEntryPoint`.
4. The entry point redirects to the login page.
5. User submits username and password.
6. `UsernamePasswordAuthenticationFilter` creates a token and calls
   `AuthenticationManager`.
7. On success, Spring Security stores the `Authentication`, applies session
   strategy, publishes an event, and redirects to the saved request.
8. On failure, it clears the context and invokes the failure handler.

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

Your HTML form must submit fields named `username` and `password` by default.
If CSRF is enabled, include the CSRF token.

### 3.6 HTTP Basic and Digest

HTTP Basic sends credentials with each request using the `Authorization` header.
Use it only over HTTPS. It is useful for simple service-to-service demos,
internal tools, and tests, but bearer tokens are usually better for modern APIs.

```java
http
    .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
    .httpBasic(Customizer.withDefaults());
```

Digest authentication exists for compatibility but is rarely chosen for new
applications. Prefer HTTPS plus stronger mechanisms.

### 3.7 Password storage, JDBC, LDAP, and UserDetails

`UserDetailsService` loads user records. `DaoAuthenticationProvider` compares the
presented password with the stored hash through `PasswordEncoder`.

Use cases:

- In-memory users: tests, demos, very small internal tools.
- JDBC users: simple applications with relational user tables.
- Custom `UserDetailsService`: common when users already live in your domain
  model.
- LDAP or Active Directory: enterprise identity stores.

Custom `UserDetailsService` sketch:

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

### 3.8 Authentication persistence and sessions

For stateful web applications, successful authentication is saved so later
requests in the same session know the user. For stateless APIs, you usually set
`SessionCreationPolicy.STATELESS` and authenticate each request from the bearer
token.

```java
http.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.STATELESS));
```

Session topics to understand:

- session fixation protection changes the session id after login;
- concurrent session control can limit how many sessions a user has;
- invalid session handling controls the response for expired sessions;
- remember-me creates a persistent login signal, not a replacement for strong
  authentication.

### 3.9 Other authentication mechanisms

Spring Security's servlet section also covers:

- MFA and one-time tokens for step-up authentication;
- passkeys/WebAuthn for phishing-resistant login;
- anonymous authentication so unauthenticated users still have an
  `Authentication` object;
- pre-authentication when an external system already authenticated the user;
- JAAS, CAS, X.509, Kerberos, Run-As, logout, and authentication events.

Use these when the deployment environment or organization needs that protocol.
For example, use X.509 when client certificates identify users; use
pre-authentication behind a trusted reverse proxy only if the proxy strips and
sets identity headers safely.

### 3.10 Kerberos

Kerberos is a network authentication protocol commonly used in Windows/Active
Directory environments. In a servlet application, it is usually chosen for
enterprise single sign-on where browsers or desktop clients can obtain a service
ticket from a Key Distribution Center.

Typical moving parts:

- KDC: trusted Kerberos authority, often Active Directory.
- Principal: identity name for a user or service.
- Keytab: file containing long-lived service keys. Treat it like a secret.
- SPNEGO: browser/server negotiation mechanism for Kerberos over HTTP.

Use Kerberos when your company already standardizes on Kerberos/AD SSO. Do not
choose it for a new public web or mobile API unless the deployment environment
requires it. OAuth2/OIDC is usually easier for internet-facing and API-first
systems.

## 4. Authorization

Authorization answers: "Is this authenticated principal allowed to do this?"

Spring Security 7 emphasizes the `AuthorizationManager` API. Older
`AccessDecisionManager` and voter APIs are legacy and should be avoided in new
applications.

### 4.1 Authorities and roles

An `AuthorizationManager` reads authorities from the current `Authentication`.
Role rules such as `hasRole("ADMIN")` look for `ROLE_ADMIN` by default.

Prefer this style:

```java
.requestMatchers("/admin/**").hasRole("ADMIN")
.requestMatchers("/orders/**").hasAuthority("order:read")
```

If your organization does not use the `ROLE_` prefix, expose
`GrantedAuthorityDefaults`, but do so deliberately because it affects role-based
checks:

```java
@Bean
static GrantedAuthorityDefaults grantedAuthorityDefaults() {
    return new GrantedAuthorityDefaults("");
}
```

### 4.2 Authorize HTTP requests

`authorizeHttpRequests` configures URL authorization. Rules are evaluated in
order. The first matching rule wins.

```java
http.authorizeHttpRequests(authorize -> authorize
    .requestMatchers("/", "/health", "/login").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
    .requestMatchers(HttpMethod.POST, "/api/products/**").hasAuthority("product:write")
    .requestMatchers("/admin/**").hasRole("ADMIN")
    .anyRequest().authenticated());
```

Important details:

- Put specific rules before broad rules.
- Include `anyRequest()` as the final fallback.
- Static resources can be permitted instead of ignored, unless you intentionally
  want them outside the security filter chain.
- `AuthorizationFilter` runs late in the security chain, after authentication
  and built-in protections.
- Servlet dispatches such as `FORWARD` and `ERROR` can also be authorized. If
  your views or error pages break, explicitly permit those dispatcher types.

Dispatcher example:

```java
http.authorizeHttpRequests(authorize -> authorize
    .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
    .anyRequest().authenticated());
```

Custom `AuthorizationManager` use case: allow access only when the path tenant
matches the user's tenant.

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

Enable method security:

```java
@Configuration
@EnableMethodSecurity
class MethodSecurityConfig {
}
```

Then secure services:

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

Use method security for business rules because controllers are not the only
entry point into services. Avoid putting complex SpEL everywhere; move complex
checks into a bean, as shown with `@orderSecurity`.

Method security checks are applied by Spring AOP. Self-invocation inside the
same class can bypass proxies. If method `a()` calls secured method `b()` in the
same object, the call may not pass through the security interceptor. Split the
secured method into another bean or call through the proxy when needed.

### 4.4 Domain object ACLs and authorization events

ACL support is for per-object permissions such as "user A can read document 42."
It is powerful but heavier than normal role/authority checks. Use it when you
need dynamic object permissions and a database-backed permission model.

Authorization events let the app observe granted and denied decisions. They are
useful for audit logging and security monitoring, but avoid logging sensitive
payloads or credentials.

## 5. OAuth2

Spring Security servlet OAuth2 support covers three major roles:

- Resource Server: protects APIs by validating bearer tokens.
- Client: obtains and stores authorized client tokens for calling other systems.
- Authorization Server: issues tokens. This is provided by Spring Authorization
  Server and has its own model.

OAuth2 Login is a client feature for logging users in with OAuth2 or OpenID
Connect providers.

### 5.1 OAuth2 Resource Server

Use this when your API receives `Authorization: Bearer <token>`.

JWT configuration with issuer discovery:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://issuer.example.com
```

Equivalent explicit security chain:

```java
http
    .authorizeHttpRequests(authorize -> authorize
        .requestMatchers("/actuator/health").permitAll()
        .requestMatchers("/api/admin/**").hasAuthority("SCOPE_admin")
        .anyRequest().authenticated())
    .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
```

JWT flow:

1. `BearerTokenAuthenticationFilter` extracts the bearer token.
2. `JwtDecoder` validates signature, issuer, expiry, and other configured
   claims.
3. Spring converts claims to authorities.
4. Authorization rules use those authorities.

Customize JWT authorities:

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

Use opaque tokens when the authorization server wants APIs to introspect tokens
instead of validating self-contained JWTs.

### 5.2 OAuth2 Login

Use OAuth2 Login when your application signs users in through Google, GitHub,
Keycloak, Okta, Azure AD, or another OIDC/OAuth2 provider.

Basic Boot configuration:

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

- If the app has a browser UI and you want centralized identity, use OAuth2
  Login/OIDC.
- If the app only exposes an API and receives bearer tokens, use Resource Server
  instead.

Customize logged-in user mapping:

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

Use OAuth2 Client when your application needs to call another OAuth2-protected
service. The important concepts are:

- `ClientRegistration`: client id, secret, scopes, provider endpoints.
- `OAuth2AuthorizedClient`: client registration plus access/refresh tokens.
- `OAuth2AuthorizedClientManager`: obtains, refreshes, and returns authorized
  clients.

Example with `WebClient`:

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

Common grant use cases:

- Authorization Code: a user is present; the app calls another service on that
  user's behalf.
- Client Credentials: no user is present; a backend service calls another
  backend as itself.
- Refresh Token: the client renews access without asking the user to log in
  again.
- JWT Bearer or Token Exchange: advanced delegation scenarios where one token is
  exchanged for another audience or subject.

For service-to-service calls, prefer client credentials with narrow scopes and
short-lived access tokens:

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

Spring Security references the authorization server role, but implementation is
in Spring Authorization Server. Use it when your organization needs to issue
OAuth2/OIDC tokens. Do not add an authorization server to every application.
Centralize it unless there is a strong product reason.

## 6. SAML2

SAML2 is common in enterprise single sign-on. In SAML terms:

- Identity Provider (IdP): authenticates the user, such as Okta, Azure AD, ADFS.
- Service Provider (SP): your application.
- Assertion: signed identity statement sent from IdP to SP.
- Relying Party Registration: Spring Security configuration for an SP/IdP pair.

Minimal Boot-style configuration:

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

Use SAML2 when an enterprise IdP mandates SAML. Use OIDC when you can choose a
newer protocol and want simpler API/mobile integration.

SAML login flow:

1. User requests a protected URL in the service provider application.
2. Spring Security sends an authentication request to the identity provider.
3. The user authenticates at the identity provider.
4. The identity provider posts a signed SAML response to the application's ACS
   endpoint.
5. Spring Security validates the signature, recipient, audience, time window,
   and configured relying party details.
6. A `Saml2Authentication` is stored in the security context.
7. URL or method authorization uses authorities mapped from the assertion.

Customization points:

- Use metadata to avoid manually copying IdP certificates and endpoints.
- Map SAML attributes to application authorities in a converter or user service.
- Configure logout only after confirming the IdP supports the flow you want.
- Rotate certificates deliberately; broken SAML certificates usually break login
  for everyone at once.

## 7. Protection Against Exploits

Spring Security enables several defenses by default. Understand them before
turning them off.

### 7.1 CSRF

CSRF protection prevents a malicious site from causing a user's browser to submit
state-changing requests with the user's cookies.

Keep CSRF enabled for browser apps that use cookies or sessions:

```java
http.csrf(Customizer.withDefaults());
```

For a stateless bearer-token API that does not use cookies for authentication,
CSRF is usually disabled:

```java
http
    .csrf(csrf -> csrf.disable())
    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
```

SPA pattern with a readable CSRF cookie:

```java
http.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
```

The frontend reads the token cookie and sends it in the expected header for
unsafe methods such as POST, PUT, PATCH, and DELETE.

### 7.2 Security headers

Spring Security writes headers that mitigate common browser attacks, including
cache control, content sniffing, clickjacking, and HTTPS downgrade risk.

Customize only when you know the browser behavior you need:

```java
http.headers(headers -> headers
    .frameOptions(frame -> frame.sameOrigin())
    .httpStrictTransportSecurity(hsts -> hsts
        .includeSubDomains(true)
        .preload(true)));
```

Use `frameOptions().sameOrigin()` for embedded H2 console or same-origin admin
frames in development. Do not disable frame protection globally in production
unless the page is intentionally embeddable and protected another way.

### 7.3 HTTP firewall

`HttpFirewall` normalizes and rejects suspicious requests before they reach your
application. It helps protect against path traversal, encoded path tricks, and
ambiguous URL handling.

If a legitimate client is rejected, prefer fixing the client URL. Relax firewall
rules only after verifying the risk.

### 7.4 HTTPS, sessions, and logout

Use HTTPS everywhere. Behind a reverse proxy, configure forwarded headers so the
app understands the original scheme:

```yaml
server:
  forward-headers-strategy: framework
```

Logout usually invalidates the session, clears security context, and removes
remember-me cookies:

```java
http.logout(logout -> logout
    .logoutUrl("/logout")
    .logoutSuccessUrl("/")
    .deleteCookies("JSESSIONID"));
```

## 8. Integrations

Spring Security integrates with servlet APIs, Spring MVC, CORS, WebSocket,
Spring Data, concurrency utilities, JSP taglibs, localization, and observability.

### 8.1 Servlet API

Spring Security integrates with servlet methods such as:

- `HttpServletRequest#getRemoteUser()`
- `HttpServletRequest#getUserPrincipal()`
- `HttpServletRequest#isUserInRole(String role)`
- `HttpServletRequest#login(username, password)`
- `HttpServletRequest#logout()`

Prefer Spring Security abstractions in application code, but this integration is
useful for libraries and legacy servlet code.

### 8.2 Spring MVC

Useful MVC integration points:

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

CORS must run before Spring Security because preflight requests do not include
credentials. Provide a `CorsConfigurationSource` and enable `cors()`.

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

Do not use `*` with credentials in production. List exact origins.

### 8.4 Concurrency

The security context is thread-local by default. When you run work in another
thread, wrap tasks so the context is propagated:

```java
Executor delegate = Executors.newFixedThreadPool(4);
Executor secured = new DelegatingSecurityContextExecutor(delegate);
secured.execute(() -> auditService.recordCurrentUserAction());
```

### 8.5 Spring Data

Spring Data integration can use the current principal in repository queries, but
keep complex authorization in services. Database filtering is useful for
multi-tenant reads, while method security still protects service operations.

### 8.6 WebSocket

For WebSocket/STOMP, authenticate the HTTP handshake and authorize messages.
Do not assume that securing the handshake is enough; message destinations need
rules too.

### 8.7 Observability

Security events and observations help with audit and monitoring. Record usernames
or stable subject ids, decision outcomes, and endpoint names. Avoid recording
tokens, passwords, cookies, authorization headers, or full sensitive payloads.

## 9. Configuration

### 9.1 Java configuration

Modern servlet configuration uses beans, not `WebSecurityConfigurerAdapter`.
Create one or more `SecurityFilterChain` beans.

Recommended full example:

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

Use multiple chains when different request groups need different protocols. Use
`securityMatcher` to scope each chain and `@Order` to make precedence explicit.

Do not put API and browser behavior in one chain if it causes confusing CSRF,
session, or entry-point behavior.

### 9.3 Ignoring versus permitting

`permitAll()` keeps the request inside the security filter chain and simply
grants access. `web.ignoring()` bypasses Spring Security entirely.

Prefer `permitAll()` for login pages, static resources, health endpoints, and
public APIs unless you intentionally need zero Spring Security behavior.

### 9.4 Custom DSLs and shared objects

Advanced applications can create custom configurers or retrieve shared objects
from `HttpSecurity`. Use this for reusable infrastructure, not for business
rules that belong in services.

### 9.5 Kotlin and namespace configuration

The servlet reference also documents Kotlin DSL and XML namespace configuration.
For new Spring Boot Java/Kotlin applications, prefer bean-based Java/Kotlin
configuration with `SecurityFilterChain`.

Use XML namespace configuration mainly when maintaining an older application
that already uses XML. Avoid mixing XML and Java configuration for the same
security concern unless there is a migration plan, because it becomes hard to
predict which chain or bean owns a rule.

Kotlin example:

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

Add test support:

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

### 10.1 MockMvc setup

With Spring Boot tests, use MockMvc and Spring Security test support:

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

### 10.2 Testing form login, Basic, logout, and OAuth2

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

Request post-processors let you attach security state to a request without
running a full external identity provider:

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

These tests do not prove the external provider is configured correctly. They
prove that your application reacts correctly once Spring Security has produced
the expected `Authentication`.

### 10.3 Testing method security

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

Test the rule you depend on, not just the happy path. For every sensitive
endpoint, include unauthenticated, authenticated-but-forbidden, and allowed
cases.

## 11. Appendix and Practical Customization Checklist

### 11.1 Database schemas

Spring Security documents schemas for JDBC users, authorities, remember-me
tokens, OAuth2 authorized clients, and ACLs. Use the official schema as a
starting point, then adapt naming and migrations to your project.

### 11.2 Proxy server configuration

When running behind Nginx, a load balancer, Kubernetes ingress, or a platform
proxy, configure forwarded headers so redirects, secure cookies, and HSTS match
the public URL.

Common Boot setting:

```yaml
server:
  forward-headers-strategy: framework
```

Also configure the proxy to set `X-Forwarded-Proto`, `X-Forwarded-Host`, and
related headers correctly, and do not trust forwarded headers from the public
internet unless the proxy strips untrusted values.

### 11.3 Decision checklist

Use this checklist when customizing:

1. What kind of client calls this endpoint: browser, SPA, mobile app, server, or
   webhook?
2. What credential does it send: cookie session, username/password, Basic, JWT,
   opaque token, SAML assertion, certificate, API key?
3. Is the request stateful or stateless?
4. Which `SecurityFilterChain` should match it first?
5. Which authentication filter creates the `Authentication`?
6. Which provider validates it?
7. Which authorities should be produced?
8. Which URL and method rules apply?
9. Which service-level method rules apply?
10. Should CSRF be enabled?
11. Which headers, CORS rules, and firewall behavior are required?
12. Which tests prove unauthenticated, forbidden, and allowed behavior?

### 11.4 Common mistakes

- Disabling CSRF for a session-based browser app.
- Using `hasRole("ROLE_ADMIN")` instead of `hasRole("ADMIN")`.
- Placing broad request matchers before specific ones.
- Mixing browser form login and stateless API behavior in one chain without a
  clear reason.
- Forgetting that only the first matching `SecurityFilterChain` runs.
- Expecting method security to intercept self-invocation.
- Logging bearer tokens or passwords during authentication failures.
- Trusting identity headers from a proxy without ensuring the proxy overwrites
  untrusted incoming headers.
