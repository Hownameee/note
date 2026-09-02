# 9. Date/Time, I/O, NIO.2, and Serialization

---

## Part 1: Modern Date & Time (`java.time` / JSR-310)

### 1. The Architectural Shift (Why Legacy Date APIs Failed)

Prior to Java 8, date handling relied on `java.util.Date`, `java.util.Calendar`, and `java.text.SimpleDateFormat`. These classes suffered from severe design flaws:

- **Mutability & Thread-Unsafety**: `Calendar` and `SimpleDateFormat` are mutable. Sharing a single `SimpleDateFormat` instance across threads leads to state corruption and race conditions.
- **Confusing Indexing & Names**: Months were 0-indexed (`0 = January`), years were offset by 1900, and `java.util.Date` actually represented an instant in time without a timezone, despite its name.
- **No Domain Separation**: A single `Date` class was used for date-only, time-only, and timestamp values.

**Java 8 (JSR-310)** introduced `java.time`: completely **immutable**, **thread-safe**, and designed with clear separation of temporal domains.

```
                            ┌─────────────────────────────────────────┐
                            │               Machine Time              │
                            │  Instant (UTC timestamp, epoch seconds) │
                            └────────────────────┬────────────────────┘
                                                 │
                                     + ZoneOffset / ZoneId
                                                 │
                                                 ▼
┌────────────────────────────────────────┬────────────────────────────────────────┐
│               Civil Time               │           Timezone / Offset            │
│  • LocalDate (2026-09-02)              │  • OffsetDateTime (+07:00)             │
│  • LocalTime (15:00:00)                │  • ZonedDateTime (Asia/Ho_Chi_Minh)    │
│  • LocalDateTime (2026-09-02T15:00:00) │                                        │
└────────────────────────────────────────┴────────────────────────────────────────┘
```

---

### 2. Core Types Taxonomy & Selection Guide

| Type | ISO-8601 Format Example | Offset / Zone? | Database Mapping (PostgreSQL) | Primary Backend Use Case |
| :--- | :--- | :---: | :--- | :--- |
| **`Instant`** | `2026-09-02T08:00:00.123Z` | Always UTC (`+00:00`) | `TIMESTAMP WITH TIME ZONE` / `BIGINT` | **Top #1 Backend Choice**: DB timestamps (`created_at`, `updated_at`), event queues (Kafka), audit logs, cache TTL. |
| **`OffsetDateTime`** | `2026-09-02T15:00:00+07:00` | Fixed Offset (`+07:00`) | `TIMESTAMP WITH TIME ZONE` | **Top #1 REST API Choice**: JSON Request/Response DTO payloads (preserves caller's local offset). |
| **`ZonedDateTime`** | `2026-09-02T15:00:00+07:00[Asia/Ho_Chi_Minh]` | IANA Zone ID + DST Rules | `TIMESTAMP` + `VARCHAR(zone_id)` | Cross-border scheduling, recurring calendar events with Daylight Saving Time rules (e.g., flight departures). |
| **`LocalDate`** | `2026-09-02` | None | `DATE` | Date-only domain data: Date of birth, card expiration date, daily partition keys. |
| **`LocalTime`** | `15:00:00` | None | `TIME` | Time-only domain data: Store opening hours, alarm clock schedules. |
| **`LocalDateTime`** | `2026-09-02T15:00:00` | None | `TIMESTAMP WITHOUT TIME ZONE` | Local wall-clock only (POS receipts, single physical venue). **Do not use for global event tracking.** |
| **`Duration`** | `PT2H30M` | None (Time-based) | `INTERVAL` / `BIGINT` | Physical time elapsed: HTTP timeouts, Redis TTL, latency measurements. |
| **`Period`** | `P1Y2M15D` | None (Date-based) | `INTERVAL` | Calendar-based spans: Account age, subscription durations (e.g., 1 month). |

---

### 3. Deep Mechanics & Production Pitfalls

#### A. The `LocalDateTime` Trap
`LocalDateTime` has **no timezone or offset information**. It represents a generic wall-clock time.
- `2026-09-02T15:00:00` at `Asia/Ho_Chi_Minh` (UTC+7) is physically **12 hours earlier** than `2026-09-02T15:00:00` at `America/New_York` (UTC-4).
- Storing or transmitting `LocalDateTime` across microservices causes silent data corruption whenever systems reside in different timezones or default JVM settings change.

#### B. Daylight Saving Time (DST) Transitions
In regions with DST (e.g., US, Europe), clock transitions introduce two edge cases:
1. **Gap (Spring Forward)**: E.g., at `01:00`, clocks jump directly to `02:00`. The time `01:30` does **not exist**.
2. **Overlap (Fall Back)**: E.g., at `02:00`, clocks roll back to `01:00`. The time `01:30` occurs **twice**.

`ZonedDateTime` automatically resolves these transitions using the embedded IANA Time Zone Database:
```java
ZoneId london = ZoneId.of("Europe/London");

// 1. Gap Handling: Spring forward jumps over the gap automatically
ZonedDateTime beforeGap = ZonedDateTime.of(2026, 3, 29, 0, 30, 0, 0, london);
ZonedDateTime afterJump = beforeGap.plusHours(2); // Automatically resolves to 03:30 (+01:00)

// 2. Duration vs Period under DST:
// Period: Respects the calendar day (preserves 10:00 AM wall-clock)
ZonedDateTime plusPeriod = beforeGap.plus(Period.ofDays(1)); // 10:00 AM next day

// Duration: Adds exact 24 physical hours (wall-clock becomes 11:00 AM due to 1h lost to DST)
ZonedDateTime plusDuration = beforeGap.plus(Duration.ofDays(1)); // 11:00 AM next day
```

#### C. Testing with `java.time.Clock`
Never call `Instant.now()` or `LocalDate.now()` directly in domain logic. Inject a `java.time.Clock` bean instead:

```java
@Service
public class TokenService {
    private final Clock clock; // Inject via Spring DI

    public TokenService(Clock clock) {
        this.clock = clock;
    }

    public boolean isExpired(Instant expiryTime) {
        return expiryTime.isBefore(Instant.now(clock));
    }
}

// In Unit Test: Deterministic test without thread sleep or flaky timing
Clock fixedClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
TokenService service = new TokenService(fixedClock);
```

#### D. Thread-Safe Formatting with `DateTimeFormatter`
`DateTimeFormatter` instances are **immutable and thread-safe**. Always instantiate them as `public static final` constants:

```java
public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
public static final DateTimeFormatter CUSTOM_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
```

---

## Part 2: I/O and NIO.2 (`java.io` vs `java.nio.file`)

### 1. Legacy `java.io.File` vs Modern `java.nio.file.Path` & `Files`

| Feature | Legacy `java.io.File` (Java 1.0) | Modern `java.nio.file.Path` & `Files` (Java 7+) |
| :--- | :--- | :--- |
| **Error Handling** | Boolean return values (`false` on failure, no root cause exception). | Explicit, detailed exceptions (`NoSuchFileException`, `AccessDeniedException`). |
| **Metadata & Attributes** | Slow, repeated OS calls per attribute (`canRead()`, `length()`). | Bulk attribute reading via `BasicFileAttributes` in a single OS system call. |
| **Symbolic Links** | Inconsistent or broken symlink handling. | Full symlink and POSIX permission support (`LinkOption.NOFOLLOW_LINKS`). |
| **File Operations** | No atomic copy/move operations. | Native atomic support: `Files.move(src, dst, StandardCopyOption.ATOMIC_MOVE)`. |

---

### 2. Modern Filesystem Operations

```java
Path path = Path.of("data", "orders.csv");

// 1. Safe directory creation
Files.createDirectories(path.getParent());

// 2. High-performance, atomic write
Files.writeString(path, "id,total\n1,100", StandardCharsets.UTF_8,
        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

// 3. Fast reading (small to medium files)
String content = Files.readString(path, StandardCharsets.UTF_8);
List<String> allLines = Files.readAllLines(path, StandardCharsets.UTF_8);
```

---

### 3. Critical I/O Traps & Production Rules

#### Trap 1: Relying on Platform Default Charset
Never omit the `Charset` parameter. If omitted, Java defaults to the host OS encoding (e.g., `windows-1252` on older Windows vs `UTF-8` on Linux), silently corrupting multi-byte characters.

```java
// ❌ WRONG: Uses host OS default encoding
Reader reader = new FileReader("data.txt");

// ✅ RIGHT: Explicitly specify StandardCharsets.UTF_8
BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
```

#### Trap 2: Resource Leaks with Lazy Stream APIs
Methods like `Files.lines()`, `Files.list()`, `Files.walk()`, and `Files.find()` return a lazy `Stream<T>` backed by an open OS file descriptor. If not closed, they leak file handles until Garbage Collection occurs, easily exhausting the OS `ulimit -n` limit.

```java
// ❌ WRONG: File descriptor remains open until GC runs
long errorCount = Files.lines(path).filter(l -> l.contains("ERROR")).count();

// ✅ RIGHT: Always enclose in try-with-resources
try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
    long errorCount = lines.filter(l -> l.contains("ERROR")).count();
}
```

#### Trap 3: The `InputStream.read(byte[])` Partial Read Trap
`InputStream.read(byte[] b)` is **not guaranteed** to fill the array in a single invocation. It returns the number of bytes actually read (or `-1` on EOF).

```java
byte[] buffer = new byte[1024];

// ❌ WRONG: Assumes entire 1024 bytes were read
int read = in.read(buffer);
processData(buffer); // May process garbage or truncated bytes!

// ✅ RIGHT (Java 9+): Reads exact N bytes or throws EOFException
byte[] exactData = in.readNBytes(1024);

// ✅ RIGHT (Stream transfer, Java 9+):
in.transferTo(out);
```

#### Trap 4: Unbuffered I/O
Reading or writing single bytes directly via `FileInputStream`/`FileOutputStream` generates a native OS system call per byte, reducing throughput by 100x–1000x. Always wrap raw streams in `BufferedInputStream` / `BufferedOutputStream` or `BufferedReader` / `BufferedWriter`.

---

## Part 3: Java Object Serialization (`Serializable`)

### 1. Mechanisms & Components

Java native serialization encodes an in-memory object graph into a binary stream (`ObjectOutputStream`) and reconstructs it (`ObjectInputStream`) without invoking constructors.

```java
public class UserSession implements Serializable {
    // Unique version identifier for binary compatibility
    private static final long serialVersionUID = 1L;

    private String userId;

    // Excluded from serialization (passwords, DB connections, ephemeral caches)
    private transient String rawPassword;

    // Preserve singleton guarantee during deserialization
    private Object readResolve() {
        return INSTANCE;
    }
}
```

- **`Serializable`**: A marker interface (no methods) signaling JVM serialization permission.
- **`serialVersionUID`**: Verified during deserialization. If omitted, the compiler auto-generates a hash from class structure. Any modification (adding a method/field) changes the hash and throws `InvalidClassException`.
- **`transient`**: Keyword to exclude sensitive or non-serializable fields from the payload.
- **`readResolve()` / `writeReplace()`**: Intercepts serialization lifecycle to preserve invariants or singleton patterns.

---

### 2. Why Java Native Serialization is an Architectural Anti-Pattern

> *"Serialization was a mistake."* — Brian Goetz (Java Language Architect)
> *Effective Java* (Items 85–90) explicitly advises against native serialization.

1. **Remote Code Execution (RCE) via Deserialization Gadgets**:
   - `ObjectInputStream.readObject()` executes code and instantiates classes before application validation occurs.
   - Attackers craft malicious payload byte streams using existing classes on the classpath ("gadget chains", e.g., Apache Commons Collections, Spring, Jackson) to execute arbitrary shell commands.
2. **Bypasses Constructor Validation**:
   - Deserialization is an **extralinguistic mechanism** that constructs objects without invoking constructors, bypassing invariant checks and immutability guarantees.
3. **Breaks Encapsulation**:
   - Private fields become part of the exported external binary API contract forever, constraining internal refactoring.

---

### 3. Modern Industry Standards

For IPC, microservices, and persistence, replace Java native serialization with typed, schema-validated formats:

| Format | Library | Pros | Typical Use Case |
| :--- | :--- | :--- | :--- |
| **JSON** | Jackson, Gson | Human-readable, universal tooling, ubiquitous. | Public REST APIs, frontend integration. |
| **Protocol Buffers** | `protobuf-java`, gRPC | Compact binary, schema-enforced, backward/forward compatibility, blazing fast. | Internal microservice-to-microservice IPC. |
| **Avro** | Apache Avro | Dynamic schema evolution, compact binary. | Kafka event streams, Big Data pipelines. |

---

## Summary Cheat Sheet

| Area | ❌ Anti-Pattern | ✅ Modern Best Practice |
| :--- | :--- | :--- |
| **Date/Time** | `java.util.Date`, `Calendar`, `SimpleDateFormat` | `java.time.Instant`, `OffsetDateTime`, `ZonedDateTime` |
| **Database Timestamp** | `LocalDateTime` (no zone/offset) | `Instant` / `OffsetDateTime` mapped to `TIMESTAMPTZ` |
| **Testing Time** | `Instant.now()`, `System.currentTimeMillis()` | Inject `java.time.Clock` (`Clock.systemUTC()` / `Clock.fixed()`) |
| **File I/O** | `java.io.File`, `FileReader` (platform default charset) | `java.nio.file.Path`, `Files`, `StandardCharsets.UTF_8` |
| **Stream Resources** | Calling `Files.lines()` without closing | Always wrap `Files.lines()`/`Files.walk()` in `try-with-resources` |
| **Reading Bytes** | Assuming `in.read(byte[])` reads the full buffer | Use `in.readNBytes(n)` or `in.transferTo(out)` |
| **Object Serialization**| `Serializable` with `ObjectInputStream` | JSON (Jackson) / Protocol Buffers (gRPC) / Avro |
