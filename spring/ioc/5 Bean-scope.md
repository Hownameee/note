# Spring Bean Scopes

A bean definition is like a "recipe" for creating an object. The **Scope** of a bean tells Spring exactly *how many* instances to create from that recipe and *how long* they should live.

Spring supports six built-in scopes (four of which are only available in web applications), and allows you to create custom scopes.

---

## 1. The Core Scopes

### A. Singleton Scope (Default)
*   **What it is:** Spring creates exactly **one shared instance** of the object per IoC container. Every time you ask for that bean, Spring returns the exact same cached instance.
*   **Use Case:** Stateless services, repositories, configuration classes.
*   **GoF Difference:** A standard "Gang of Four" Singleton is one instance per Java *ClassLoader*. A Spring Singleton is one instance per *Spring Container*.

```java
// Explicitly stating singleton (redundant since it's the default)
@Component
@Scope("singleton")
public class DefaultAccountService { }
```

### B. Prototype Scope
*   **What it is:** Spring creates a **brand-new instance** every single time the bean is requested (either via `@Autowired` or `context.getBean()`).
*   **Use Case:** Stateful beans (where each object needs to hold specific user/transaction data).
*   **Important limitation:** Spring does *not* manage the complete lifecycle of prototype beans. It creates them, hands them to you, and forgets about them. **Destruction callbacks (e.g., `@PreDestroy`) are never called on prototypes.** The client code is responsible for garbage collection.

```java
@Component
@Scope("prototype")
public class UserSessionData { }
```

> [!WARNING]
> **The Singleton-Prototype Trap**
> If you inject a Prototype bean into a Singleton bean, the Prototype is instantiated and injected **only once** (when the Singleton is created). 
> The Singleton will **not** get a new Prototype instance every time it calls a method. To fix this, you must use **Method Injection** (e.g., `@Lookup`) or inject an `ObjectProvider<PrototypeBean>`.

---

## 2. Web-Aware Scopes

To use these, your application must be running in a web-aware context (like Spring Web MVC). 

| Scope | Description |
| :--- | :--- |
| **`request`** | A new instance is created for every single HTTP request. Discarded when the request completes. |
| **`session`** | A new instance is created for the lifecycle of an HTTP Session. |
| **`application`** | Scoped to the `ServletContext`. Similar to a Singleton, but shared across the entire Servlet environment, not just the Spring Context. |
| **`websocket`** | Scoped to the lifecycle of a WebSocket session. |

### Real-World Usage of Web Scopes

Web scopes are perfect for holding data that is specific to a user's interaction with your web application, without needing to pass that data manually through every single method call.

#### 1. `@SessionScope` Usage: A Shopping Cart
The most classic example of a session-scoped bean is an e-commerce shopping cart. You want the cart to remember items as the user clicks through different pages, but User A's cart must be completely isolated from User B's cart.

```java
@Component
@SessionScope
public class ShoppingCart {
    
    // This list will persist across multiple HTTP requests 
    // as long as the user's browser session is active!
    private final List<String> items = new ArrayList<>();

    public void addItem(String item) {
        this.items.add(item);
    }

    public List<String> getItems() {
        return this.items;
    }
}
```

#### 2. `@RequestScope` Usage: Request Auditing / Tracing
If you want to track a unique Correlation ID or trace the performance of a single HTTP request as it travels through your Controllers, Services, and Repositories, use a request-scoped bean.

```java
@Component
@RequestScope
public class RequestTraceContext {
    
    private final String traceId;
    private final long startTime;

    public RequestTraceContext() {
        // This constructor runs exactly ONCE per incoming HTTP request.
        // It is destroyed immediately after the HTTP response is sent.
        this.traceId = UUID.randomUUID().toString();
        this.startTime = System.currentTimeMillis();
    }

    public void log(String message) {
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("[Trace: " + traceId + " | " + elapsed + "ms] " + message);
    }
}
```

---

## 3. Deep Dive: Scoped Proxies (Crucial Concept)

### The Problem
Imagine you have a `Singleton` bean (`UserManager`) that needs to use a `Session` scoped bean (`UserPreferences`).
Because `UserManager` is a Singleton, it is created *once* at startup. But at startup, there is no active HTTP Session! Even if there was, `UserManager` would be permanently stuck with the preferences of the very first user who logged in.

### The Solution: Scoped Proxies
When you inject a short-lived bean (Session/Request) into a long-lived bean (Singleton), Spring doesn't inject the actual object. Instead, it injects a **Proxy** (a wrapper object that looks identical to the real object).

When the Singleton calls a method on the Proxy (e.g., `preferences.getTheme()`), the Proxy pauses, reaches into the *current active HTTP Session*, grabs the real `UserPreferences` object for that specific user, and delegates the method call to it.

> [!NOTE]
> Modern Spring Boot annotations like `@RequestScope` and `@SessionScope` automatically enable this proxy behavior via CGLIB (`proxyMode = ScopedProxyMode.TARGET_CLASS`).

```java
// 1. The short-lived bean (Spring automatically creates a proxy for this)
@Component
@SessionScope
public class UserPreferences {
    private String theme = "dark";
    // getters/setters
}

// 2. The long-lived bean
@Service
public class UserManager {
    
    // Spring injects a CGLIB Proxy here, NOT the real object!
    private final UserPreferences userPreferences;

    public UserManager(UserPreferences userPreferences) {
        this.userPreferences = userPreferences;
    }

    public void printTheme() {
        // The proxy intercepts this call, finds the current HTTP session, 
        // retrieves the real UserPreferences, and delegates the call.
        System.out.println(userPreferences.getTheme());
    }
}
```

---

## 4. Custom Scopes

You are not limited to the built-in scopes. You can create your own (e.g., a "Thread" scope or a "Tenant" scope for multi-tenant SaaS applications).

### How to implement:
1.  Implement the `org.springframework.beans.factory.config.Scope` interface (defining how to `get()`, `remove()`, and destroy objects from your custom scope).
2.  Register the scope with the Spring container using a `CustomScopeConfigurer`.

```java
// Example: Registering Spring's built-in (but inactive by default) Thread Scope
@Configuration
public class CustomScopeConfig {

    @Bean
    public static CustomScopeConfigurer defineThreadScope() {
        CustomScopeConfigurer configurer = new CustomScopeConfigurer();
        configurer.addScope("thread", new SimpleThreadScope());
        return configurer;
    }
}

// Usage:
@Component
@Scope("thread")
public class ThreadContextData { }
```
