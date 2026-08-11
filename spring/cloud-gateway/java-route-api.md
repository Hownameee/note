# Spring Cloud Gateway Server MVC - Java Routes API

> **Overview**: Spring Cloud Gateway Server MVC leverages Spring WebMvc's functional endpoints (`WebMvc.fn RouterFunctions.Builder`) to construct routes programmatically in Java.

---

## 1. Creating Routes with `RouterFunctions.Builder`

Routes are created as Spring `@Bean` definitions of type `RouterFunction<ServerResponse>`.

### 1.1 Basic Usage (`RouterFunctions.route()`)
You obtain a `RouterFunctions.Builder` instance by calling `org.springframework.web.servlet.function.RouterFunctions.route()`.

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

@Configuration
class SimpleGateway {

    @Bean
    public RouterFunction<ServerResponse> getRoute() {
        return route()
            .GET("/get", http())
            .before(uri("https://example.org"))
            .build();
    }
}
```

* Method shortcuts exist for all standard HTTP verbs (`GET`, `POST`, `PUT`, `DELETE`, `PATCH`, etc.) combined with path predicates.
* Overloaded methods exist for additional `RequestPredicate` parameters as well as generic `.route(RequestPredicate, HandlerFunction)` definitions.

---

## 2. Named Routes with `GatewayRouterFunctions.route(routeId)`

Standard `RouterFunctions.route()` creates an anonymous route. However, advanced gateway filters (like metrics, tracing, rate limiting, and circuit breaking) require route metadata—specifically a **Route ID**.

To create a named route with metadata, use `GatewayRouterFunctions.route(String routeId)`:

```java
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

@Configuration
class SimpleGateway {

    @Bean
    public RouterFunction<ServerResponse> getRoute() {
        return route("simple_route") // Attaches "simple_route" as route metadata
            .GET("/get", http())
            .before(uri("https://example.org"))
            .build();
    }
}
```

> **How it works**: `GatewayRouterFunctions.route("simple_route")` creates a standard `RouterFunctions.Builder` and automatically injects a `before` filter that sets the `routeId` into the request attributes (`GATEWAY_ROUTE_ID_ATTR`).

---

## 3. Gateway Handler Functions

Gateway routes require a `HandlerFunction<ServerResponse>` to process requests. SCG MVC provides implementations in `org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions`.

### 3.1 HTTP Handler Function (`http()`)

The core handler for proxying HTTP traffic is `HandlerFunctions.http()`.

* **No-arg `http()` Behavior**: The function looks for the target downstream URI in the request attribute `MvcUtils.GATEWAY_REQUEST_URL_ATTR` (populated by filters like `BeforeFilterFunctions.uri(...)` or dynamic load balancers).

> [!CAUTION]
> **Deprecation Notice (v4.1.7+)**:
> `HandlerFunctions.http(String)` and `HandlerFunctions.http(URI)` are **deprecated**.
>
> **Recommended Pattern**:
> ```java
> // Preferred modern approach:
> route("my_route").GET("/api", http()).before(uri("https://backend-service.com")).build();
> ```

---

## 4. Spring Cloud Function Integration

By adding `spring-cloud-function-context` to your project dependencies, Spring Cloud Gateway automatically turns Java functional beans (`java.util.function.Function`, `Supplier`, `Consumer`) into HTTP endpoints.

### 4.1 Dependency Setup
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-function-context</artifactId>
</dependency>
```

### 4.2 Defining Function Beans
When the dependency is present, the **bean name** of any `Function` automatically becomes the route path:

```java
@SpringBootApplication
public class DemoFunctionGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoFunctionGatewayApplication.class, args);
    }

    @Bean
    public Function<String, String> uppercase() {
        return input -> input.toUpperCase();
    }

    @Bean
    public Function<String, String> concat() {
        return input -> input + input;
    }
}
```

### 4.3 Invoking Functions via HTTP

| Invocation Style | HTTP Method & URL | Input | Result |
| :--- | :--- | :--- | :--- |
| **Path Variable (GET)** | `GET http://localhost:8080/uppercase/hello` | `"hello"` | `HELLO` |
| **Request Body (POST)** | `POST http://localhost:8080/concat`<br>`Body: "hello"` | `"hello"` | `hellohello` |
| **Composed Functions (POST)** | `POST http://localhost:8080/concat,uppercase`<br>`Body: "hello"` | `"hello"` | `HELLOHELLO` |

#### cURL Examples:

**Single Function POST**:
```bash
curl -d '"hello"' -H "Content-Type: application/json" -X POST http://localhost:8080/concat
# Output: hellohello
```

**Composed Functions POST** (evaluated left-to-right: `concat` then `uppercase`):
```bash
curl -d '"hello"' -H "Content-Type: application/json" -X POST http://localhost:8080/concat,uppercase
# Output: HELLOHELLO
```

---

## Summary Comparison Table

| Router Provider | Primary Method | Feature / Best Use Case |
| :--- | :--- | :--- |
| `RouterFunctions` | `route()` | Standard Spring WebMvc functional router (anonymous route) |
| `GatewayRouterFunctions` | `route(String routeId)` | SCG MVC router bean (attaches Route ID metadata for filters/metrics) |
| `HandlerFunctions` | `http()` | Proxies request downstream to URI set in `BeforeFilterFunctions.uri()` |
| Spring Cloud Function | Functional Beans | Maps `@Bean Function<T, R>` automatically to HTTP endpoints with function composition support |
