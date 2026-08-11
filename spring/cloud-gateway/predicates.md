# Spring Cloud Gateway Server MVC - Request Predicates

> **Overview**: Request Predicates decide **whether an incoming request matches a route**. They inspect attributes of the `ServerRequest` (time, path, method, headers, query parameters, cookies, host, weights, etc.).
> Multiple predicates can be combined using `.and(...)` and `.or(...)`.

---

## Complete Gateway Predicates Summary

| Predicate Factory | Description | YAML Property Example | Java DSL Method |
| :--- | :--- | :--- | :--- |
| **`After`** | Matches requests after a specified datetime. | `After=2026-11-25T00:00:00+07:00` | `GatewayRequestPredicates.after(...)` |
| **`Before`** | Matches requests before a specified datetime. | `Before=2026-12-31T23:59:59+07:00` | `GatewayRequestPredicates.before(...)` |
| **`Between`** | Matches requests between two datetimes. | `Between=datetime1, datetime2` | `GatewayRequestPredicates.between(...)` |
| **`Cookie`** | Matches cookie name and regex value. | `Cookie=chocolate, ch.p` | `GatewayRequestPredicates.cookie(...)` |
| **`Header`** | Matches header name and regex value. | `Header=X-Request-Id, \d+` | `GatewayRequestPredicates.header(...)` |
| **`Host`** | Matches `Host` header against Ant-style patterns. | `Host=**.somehost.org` | `GatewayRequestPredicates.host(...)` |
| **`Method`** | Matches HTTP methods (`GET`, `POST`, etc.). | `Method=GET,POST` | `GatewayRequestPredicates.method(...)` |
| **`Path`** | Matches request URI path against Spring `PathPattern`. | `Path=/red/{segment}` | `GatewayRequestPredicates.path(...)` |
| **`Query`** | Matches query param existence or regex value. | `Query=red, gree.` | `GatewayRequestPredicates.query(...)` |
| **`Weight`** | Route traffic based on calculated group percentage. | `Weight=group1, 8` | `GatewayRequestPredicates.weight(...)` |

---

## 1. Time-Based Predicates (`After`, `Before`, `Between`)

Control route availability based on system clock **without application restarts**. Time can be specified as an ISO-8601 `ZonedDateTime` string or epoch milliseconds.

### Use Cases:
* **`Between`**: Scheduled maintenance windows.
* **`After`**: Scheduled feature/flash-sale release.
* **`Before`**: API deprecation / End-of-Life (EOL).

#### YAML Config Example
```yaml
spring:
  cloud:
    gateway:
      server:
        webmvc:
          routes:
            - id: maintenance_route
              uri: https://maintenance.example.com
              predicates:
                - Path=/api/**
                - Between=2026-08-10T02:00:00+07:00[Asia/Ho_Chi_Minh], 2026-08-10T04:00:00+07:00[Asia/Ho_Chi_Minh]
```

#### Java DSL Config Example
```java
import java.time.ZonedDateTime;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.after;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.before;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.between;

@Bean
public RouterFunction<ServerResponse> timeBasedRoutes() {
    return route("after_route")
        .route(after(ZonedDateTime.parse("2026-11-25T00:00:00+07:00[Asia/Ho_Chi_Minh]")), http())
        .before(uri("https://example.org"))
        .build();
}
```

---

## 2. Cookie Request Predicate

Matches requests containing a specific cookie name whose value satisfies a Java regular expression.

#### YAML Config Example
```yaml
spring:
  cloud:
    gateway:
      server:
        webmvc:
          routes:
            - id: cookie_route
              uri: https://example.org
              predicates:
                - Cookie=chocolate, ch.p
```

#### Java DSL Config Example
```java
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.cookie;

@Bean
public RouterFunction<ServerResponse> cookieRoute() {
    return route("cookie_route")
        .route(cookie("chocolate", "ch.p"), http())
        .before(uri("https://example.org"))
        .build();
}
```
*Matches if request includes a cookie named `chocolate` with values like `chip` or `chop`.*

---

## 3. Header Request Predicate

Matches requests carrying an HTTP Header name whose value matches a regular expression.

#### YAML Config Example
```yaml
spring:
  cloud:
    gateway:
      server:
        webmvc:
          routes:
            - id: header_route
              uri: https://example.org
              predicates:
                - Header=X-Request-Id, \d+
```

#### Java DSL Config Example
```java
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.header;

@Bean
public RouterFunction<ServerResponse> headerRoute() {
    return route("header_route")
        .route(header("X-Request-Id", "\\d+"), http())
        .before(uri("https://example.org"))
        .build();
}
```
*Matches if header `X-Request-Id` consists of one or more digits (e.g., `X-Request-Id: 12345`).*

---

## 4. Host Request Predicate

Matches requests based on the `Host` header using Ant-style patterns (with `.` as separator). Supports URI template variables.

#### YAML Config Example
```yaml
spring:
  cloud:
    gateway:
      server:
        webmvc:
          routes:
            - id: host_route
              uri: https://example.org
              predicates:
                - Host=**.somehost.org,**.anotherhost.org
```

#### Java DSL Config Example
```java
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.host;

@Bean
public RouterFunction<ServerResponse> hostRoute() {
    return route("host_route")
        .route(host("**.somehost.org", "**.anotherhost.org"), http())
        .before(uri("https://example.org"))
        .build();
}
```
*Matches `Host` values like `www.somehost.org`, `beta.somehost.org`, or `api.anotherhost.org`.*

---

## 5. Method Request Predicate

Matches HTTP verbs (`GET`, `POST`, `PUT`, `DELETE`, etc.).

#### YAML Config Example
```yaml
spring:
  cloud:
    gateway:
      server:
        webmvc:
          routes:
            - id: method_route
              uri: https://example.org
              predicates:
                - Method=GET,POST
```

#### Java DSL Config Example
```java
import org.springframework.http.HttpMethod;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.method;

@Bean
public RouterFunction<ServerResponse> methodRoute() {
    return route("method_route")
        .route(method(HttpMethod.GET, HttpMethod.POST), http())
        .before(uri("https://example.org"))
        .build();
}
```

---

## 6. Path Request Predicate

Matches the HTTP request URI path using Spring `PathPattern` matcher. Supports URI variables (`/red/{segment}`).

#### YAML Config Example
```yaml
spring:
  cloud:
    gateway:
      server:
        webmvc:
          routes:
            - id: path_route
              uri: https://example.org
              predicates:
                - Path=/red/{segment},/blue/{segment}
```

#### Java DSL Config Example
```java
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@Bean
public RouterFunction<ServerResponse> pathRoute() {
    return route("path_route")
        .route(path("/red/{segment}", "/blue/{segment}"), http())
        .before(uri("https://example.org"))
        .build();
}
```

> **Accessing Path Template Variables**:
> Variables like `{segment}` are extracted into `ServerRequest` attributes. You can access them via `MvcUtils`:
> ```java
> Map<String, Object> uriVariables = MvcUtils.getUriTemplateVariables(request);
> String segment = (String) uriVariables.get("segment");
> ```

---

## 7. Query Request Predicate

Matches request query parameters (`?param=value`). Supports:
1. Checking param existence (`Query=green`).
2. Checking param existence AND regex value (`Query=red, gree.`).

#### YAML Config Examples
```yaml
# Example 1: Check parameter presence
spring:
  cloud:
    gateway:
      server:
        webmvc:
          routes:
            - id: query_param_exists
              uri: https://example.org
              predicates:
                - Query=green
```

```yaml
# Example 2: Check parameter presence and value regex
spring:
  cloud:
    gateway:
      server:
        webmvc:
          routes:
            - id: query_param_value_regex
              uri: https://example.org
              predicates:
                - Query=red, gree.
```

#### Java DSL Config Example
```java
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.query;

// Parameter presence check
@Bean
public RouterFunction<ServerResponse> queryExistsRoute() {
    return route("query_route")
        .route(query("green"), http())
        .before(uri("https://example.org"))
        .build();
}

// Parameter value regex check (matches ?red=green or ?red=greet)
@Bean
public RouterFunction<ServerResponse> queryRegexRoute() {
    return route("query_regex_route")
        .route(query("red", "gree."), http())
        .before(uri("https://example.org"))
        .build();
}
```

---

## 8. Weight Request Predicate (A/B Testing & Canary Deployments)

Calculates traffic distribution per group using assigned integer weights. Useful for A/B testing or canary releases.

#### YAML Config Example (80% / 20% Split)
```yaml
spring:
  cloud:
    gateway:
      server:
        webmvc:
          routes:
            - id: weight_high
              uri: https://weighthigh.org
              predicates:
                - Weight=group1, 8
            - id: weight_low
              uri: https://weightlow.org
              predicates:
                - Weight=group1, 2
```

#### Java DSL Config Example
```java
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.weight;

@Bean
public RouterFunction<ServerResponse> gatewayRouterFunctionsWeights() {
    return route("weight_high")
        .route(weight("group1", 8).and(path("/**")), http())
        .before(uri("https://weighthigh.org"))
        .build()
        .and(
            route("weight_low")
                .route(weight("group1", 2).and(path("/**")), http())
                .before(uri("https://weightlow.org"))
                .build()
        );
}
```
*Forwards ~80% of traffic to `weighthigh.org` and ~20% of traffic to `weightlow.org`.*

---

## 9. Combining Predicates

Predicates can be chained together using `.and(...)` or `.or(...)`:

```java
// Match GET requests to /v2/api with X-Client-Type: Mobile header and query param ?version=2
RequestPredicate customPredicate = path("/v2/api/**")
    .and(method(HttpMethod.GET))
    .and(header("X-Client-Type", "Mobile"))
    .and(query("version", "2"));
```
