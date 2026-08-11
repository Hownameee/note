# Spring Cloud Gateway Server MVC Glossary & Core Concepts

> **Context**: Spring Cloud Gateway Server MVC is built on top of Spring WebMvc's functional endpoints (`WebMvc.fn`), introduced as a synchronous, blocking alternative to the reactive WebFlux gateway.

---

## 1. Route

A **Route** is the basic building block of the gateway. It defines how incoming requests are matched and forwarded to a downstream service.

### Key Attributes:
* **ID**: A unique identifier for the route.
* **Destination URI**: The target endpoint to proxy/forward matching requests (e.g., `http://user-service:8081` or `lb://user-service`).
* **Predicates**: A collection of matching conditions. A route is matched if the **aggregate predicate** evaluates to `true` (logical AND of all predicates).
* **Filters**: Interceptors executed before sending the request downstream or after receiving the response.

### Request Flow:
```
Incoming Request -> Aggregate Predicate Match?
                       ├── Yes ──> Before Filters ──> Forward to Target URI ──> After Filters ──> Response
                       └── No  ──> Evaluate Next Route / 404
```

---

## 2. Predicate

A **Predicate** is a matching rule based on Spring `WebMvc.fn`'s `RequestPredicate`.

* **Type**: `org.springframework.web.servlet.function.RequestPredicate`
* **Input**: `Spring WebMvc.fn ServerRequest`
* **Output**: `boolean` (`true` if matching, `false` otherwise)

### Evaluated Request Attributes:
* HTTP Methods (`GET`, `POST`, `PUT`, etc.)
* Path patterns (`/api/v1/users/**`)
* HTTP Headers (`X-Tenant-ID`, `Authorization`)
* Query Parameters (`?version=2`)
* Cookies & Request attributes

### Combining Predicates:
Predicates can be composed using logical operators:
* `.and(...)` (Logical AND)
* `.or(...)` (Logical OR)
* `.negate()` (Logical NOT)

---

## 3. Filter

Filters are instances of `HandlerFilterFunction`. They intercept HTTP requests and responses to apply cross-cutting concerns like request transformation, logging, rate limiting, authentication, or header manipulation.

### Types & Adaptations:

1. **Before Filter (Request Processor)**
   * **Functional Interface**: `Function<ServerRequest, ServerRequest>`
   * **Adapter**: `HandlerFilterFunction.ofRequestProcessor()`
   * **Role**: Transforms the request *before* sending it downstream.

2. **After Filter (Response Processor)**
   * **Functional Interface**: `BiFunction<ServerRequest, T extends ServerResponse, R extends ServerResponse>`
   * **Adapter**: `HandlerFilterFunction.ofResponseProcessor()`
   * **Role**: Transforms the response *after* receiving it from the downstream service.

3. **Around Filter (Full HandlerFilterFunction)**
   * **Signature**: `(ServerRequest request, HandlerFunction<ServerResponse> next) -> ServerResponse`
   * **Role**: Wraps the request handling lifecycle (pre-processing, forwarding to `next.handle(request)`, post-processing, exception handling, and metrics).

---

## 4. Java Configuration Example

```java
@Configuration
public class GatewayConfig {

    @Bean
    public RouterFunction<ServerResponse> gatewayRoutes() {
        return GatewayRouterFunctions.route("user_service_route")
            // Predicate: Match GET requests to /users/**
            .route(RequestPredicates.path("/users/**")
                    .and(RequestPredicates.method(HttpMethod.GET)),
                   HandlerFunctions.http("http://user-service:8081"))
            // Before Filter: Inject custom request header
            .before(request -> ServerRequest.from(request)
                    .header("X-Gateway-Processed", "true")
                    .build())
            // After Filter: Add response header
            .after((request, response) -> {
                response.headers().add("X-Powered-By", "Spring-Cloud-Gateway-MVC");
                return response;
            })
            .build();
    }
}
```

---

## Summary Comparison Table

| Concept | Description | Type / Interface | Purpose |
| :--- | :--- | :--- | :--- |
| **Route** | Routing rule configuration | Gateway Route Definition | Defines target destination, predicates, and filters |
| **Predicate** | Matching condition | `RequestPredicate` (`ServerRequest` $\rightarrow$ `boolean`) | Evaluates if request matches route rules |
| **Filter** | Interceptor / Transformer | `HandlerFilterFunction` | Modifies request before downstream & response after downstream |
