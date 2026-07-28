# 🚀 Advanced Software Engineering Roadmap

Tài liệu này là lộ trình để tiến tới mức độ Senior Backend / Fullstack, tập trung vào bản chất (Under the hood) thay vì chỉ cú pháp.

---

## 🟢 Phase 1: Java Advanced Core

*Bản chất của Java quyết định hiệu năng và sự an toàn của ứng dụng.*

1. **Java Pass-by-Value & Memory Management:** Hiểu về Stack, Heap, và tại sao Java không có Pass-by-Reference.
2. **Mutability vs. Immutability:** Thiết kế Immutable classes, lợi ích cho Thread-safety.
3. **Functional Interfaces & Streams API:** Khai phá sức mạnh của lập trình khai báo trong Java.
4. **Multithreading / Threads:** Thread lifecycle, `ExecutorService`, `CompletableFuture` và **Virtual Threads** (Java 21).
5. **Core Design Patterns:** Singleton, Factory, Builder, Strategy, Observer, Decorator, Proxy.

---

## 🔵 Phase 2: Authentication & Authorization Architecture

*Chuẩn mực bảo mật và kiến trúc hệ thống xác thực.*

1. **Authentication vs Authorization.**
2. **JWT (JSON Web Token):** Cấu trúc, cơ chế mã hóa/ký, vòng đời Token.
3. **OAuth2 & OIDC (OpenID Connect):** Các flow xác thực, Authorization Code.
4. **SSO (Single Sign-On) & MFA:** Cơ chế đăng nhập một lần. Tích hợp Keycloak làm Identity Provider (IdP).

---

## 🟡 Phase 3: Spring Boot Mastery

*Hiểu bản chất thay vì dùng annotation một cách "phép thuật".*

1. **Inversion of Control (IoC) & Dependency Injection (DI):** ApplicationContext và Bean Lifecycle.
2. **AOP (Aspect-Oriented Programming):** Proxy Pattern đằng sau `@Transactional` và bảo mật.
3. **Spring Security Architecture:** `SecurityFilterChain`, bộ lọc request.
4. **JPA & Hibernate Performance (N+1 Query):** Giải quyết tận gốc N+1 query bằng Join Fetch, EntityGraph, BatchSize và DTO Projection.
5. **Spring `@Autowired` Deep Dive:** Cơ chế hoạt động của `AutowiredAnnotationBeanPostProcessor`, thuật toán phân giải Bean và tối ưu hóa Spring Boot 3.* với AOT/Native Image.
6. **Sức mạnh `@Transactional(readOnly = true)`:** Tối ưu hóa Dirty Checking ở mức Hibernate, cấu hình JDBC Connection, và định tuyến sang Read Replicas.
7. **Đồng bộ DTO giữa Backend & Frontend (TypeScript):** Chiến lược tự động hóa bằng Plugin Build-time (Java-to-TS Generator), giải quyết vênh dữ liệu (Date, Long, Enum) và quy trình tối ưu hóa.

---

## 🟣 Phase 4: AI Integration & Model Context Protocol (MCP)

*Đưa AI vào ứng dụng thực tế một cách chuẩn mực.*

1. **AI "Skills" & Tool Calling:** Cách LLM tương tác với external APIs và Database.
2. **Model Context Protocol (MCP):** Kiến trúc MCP, MCP Host, MCP Client, MCP Server. Xây dựng ứng dụng kết nối AI an toàn theo chuẩn Anthropic.
