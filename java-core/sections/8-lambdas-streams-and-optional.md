# 8. Lambdas, streams, and Optional

Java 8 introduced functional programming paradigms to the platform, centering on **Functional Interfaces**, **Lambda Expressions**, **Streams API**, and **`Optional<T>`**. These features enable declarative, expressive, and high-performance data processing pipelines.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           JAVA FUNCTIONAL CORE                          │
├─────────────────────────┬─────────────────────────┬─────────────────────┤
│   1. LAMBDAS & SAM      │    2. STREAMS API       │   3. OPTIONAL<T>    │
│  • invokedynamic (indy) │  • Lazy Pipeline        │  • Monadic Return   │
│  • Variable Capture     │  • Spliterator          │  • Eager vs Lazy    │
│  • Method References    │  • ForkJoinPool (Par.)  │  • Anti-patterns    │
└─────────────────────────┴─────────────────────────┴─────────────────────┘
```

---

### Functional Interfaces and Lambdas

#### 1. The Single Abstract Method (SAM) Contract
A **Functional Interface** defines exactly **one abstract method**.
- **`@FunctionalInterface` Annotation**: An informative compiler check. If an annotated interface contains zero or more than one abstract method, `javac` emits a compile-time error.
- **Methods excluded from the SAM count**:
  - `default` methods (concrete implementations).
  - `static` methods (concrete utility methods).
  - Public abstract methods matching signatures from `java.lang.Object` (e.g., `equals(Object)`, `hashCode()`, `toString()`).

```java
@FunctionalInterface
public interface DataValidator<T> {
    // 1. The Single Abstract Method (SAM)
    boolean validate(T data);

    // 2. Default methods do not break SAM
    default DataValidator<T> and(DataValidator<T> other) {
        return data -> this.validate(data) && other.validate(data);
    }

    // 3. Static methods do not break SAM
    static <T> DataValidator<T> alwaysValid() {
        return data -> true;
    }

    // 4. Object methods do not break SAM
    @Override
    boolean equals(Object obj);
}
```

---

#### 2. Core Functional Interface Categories

The `java.util.function` package provides standard interfaces to represent common behavioral contracts:

| Interface | Signature | Semantics | Production Example |
| :--- | :--- | :--- | :--- |
| **`Function<T, R>`** | `T -> R` | Transforms input $T$ to output $R$ | `User::getEmail` |
| **`BiFunction<T, U, R>`** | `(T, U) -> R` | Transforms two inputs into $R$ | `(x, y) -> x + y` |
| **`Predicate<T>`** | `T -> boolean` | Evaluates a boolean condition on $T$ | `user -> user.getAge() >= 18` |
| **`BiPredicate<T, U>`** | `(T, U) -> boolean` | Evaluates a condition on two inputs | `String::equalsIgnoreCase` |
| **`Consumer<T>`** | `T -> void` | Accepts input to perform side-effects | `System.out::println` |
| **`BiConsumer<T, U>`** | `(T, U) -> void` | Accepts two inputs for side-effects | `(k, v) -> map.put(k, v)` |
| **`Supplier<T>`** | `() -> T` | Factory producing values on-demand | `() -> Instant.now()` |
| **`UnaryOperator<T>`** | `T -> T` | Specialization of `Function<T, T>` | `String::trim` |
| **`BinaryOperator<T>`** | `(T, T) -> T` | Specialization of `BiFunction<T, T, T>` | `Math::max` |

#### Primitive Specializations (Zero-Allocation Operations)
Generic interfaces like `Function<Integer, Double>` incur heavy **Autoboxing / Unboxing** penalties (allocating 24-byte `Integer` objects on the heap). Java provides specialized primitive interfaces to maintain zero boxing overhead:
- `IntPredicate`, `LongPredicate`, `DoublePredicate`
- `ToIntFunction<T>`, `ToLongFunction<T>`, `ToDoubleFunction<T>`
- `IntToLongFunction`, `LongToDoubleFunction`
- `IntConsumer`, `LongSupplier`, `IntUnaryOperator`

---

#### 3. Method References (`::`)

Method references provide compact, readable syntax for lambdas that merely delegate to an existing method:

| Reference Type | Syntax | Lambda Equivalent | Example |
| :--- | :--- | :--- | :--- |
| **Static** | `Class::staticMethod` | `(args) -> Class.staticMethod(args)` | `Integer::parseInt` |
| **Bound Instance** | `instance::instanceMethod` | `(args) -> instance.instanceMethod(args)` | `System.out::println` |
| **Unbound Instance** | `Class::instanceMethod` | `(target, args) -> target.instanceMethod(args)` | `String::toLowerCase` |
| **Constructor** | `Class::new` / `Type[]::new` | `(args) -> new Class(args)` | `ArrayList::new`, `String[]::new` |

---

#### 4. Handling Checked Exceptions at Functional Boundaries
Standard functional interfaces in `java.util.function` do not declare `throws Exception`.
- **Bad Practice**: Littering stream pipelines with verbose `try-catch` blocks.
- **Idiomatic Solutions**:
  1. Wrap throwing methods in private helper methods that convert checked exceptions to domain unchecked exceptions (`RuntimeException`).
  2. Implement an explicit `ThrowingFunction<T, R, E extends Exception>` adapter.
  3. Use functional result types (e.g., `Result<T>` or `Either<L, R>`).

---

### Deep Mechanics: How Lambdas Work Under the Hood

#### 1. Anonymous Inner Classes (AIC) vs. Lambda Expressions

```
ANONYMOUS INNER CLASS (Legacy):
Source.java ──javac──> Source.class + Source$1.class (Extra .class on disk/metaspace)
Runtime      ──new────> Allocates a new instance on Heap for EVERY execution

LAMBDA EXPRESSION (Modern Java):
Source.java ──javac──> Emits `invokedynamic` (indy) bytecode instruction in Source.class
Runtime      ──indy───> LambdaMetafactory dynamically spins bytecode in memory + caches CallSite
```

```
┌───────────────────────────────────────────────┬───────────────────────────────────────────────┐
│             ANONYMOUS INNER CLASS             │               LAMBDA EXPRESSION               │
├───────────────────────────────────────────────┼───────────────────────────────────────────────┤
│ • Generates a distinct `.class` file at       │ • No separate `.class` file generated at      │
│   compile time (`Outer$1.class`).             │   compile time.                               │
│ • Allocates a new object instance on the heap │ • Dynamic class spin via `LambdaMetafactory`; │
│   every time the expression executes.         │   reuses singleton instance if non-capturing. │
│ • Defines its own `this` reference scope      │ • Lexical scoping: `this` points to the       │
│   (shadows the enclosing instance).           │   enclosing class instance.                   │
│ • Can declare additional fields and methods.  │ • Pure behavioral implementation of the SAM.  │
└───────────────────────────────────────────────┴───────────────────────────────────────────────┘
```

#### 2. The `invokedynamic` & `LambdaMetafactory` Protocol
1. **Compile Time (`javac`)**: The lambda body is desugared into a private synthetic method (e.g., `private static synthetic void lambda$main$0()`). An `invokedynamic` instruction is emitted at the call site with a bootstrap method pointing to `LambdaMetafactory.metafactory()`.
2. **First Invocation (Linkage)**: The JVM invokes `LambdaMetafactory`, which generates an implementation class in memory via bytecode spinning (`MethodHandles`) and links it to a constant `CallSite`.
3. **Subsequent Invocations**: Direct, highly-optimized method invocation through the cached `CallSite`. Non-capturing lambdas are cached as singletons (zero garbage collection overhead).

---

### Variable Capture: The "Final or Effectively Final" Invariant

A lambda can access local variables from its enclosing scope only if they are **`final`** or **`effectively final`** (never reassigned after initialization).

```java
public void process() {
    int port = 8080; // effectively final
    Runnable r1 = () -> System.out.println("Port: " + port); // OK

    int counter = 0;
    // counter = 1; // If uncommented, line below fails to compile
    // Runnable r2 = () -> System.out.println(counter);
}
```

#### Memory Architecture Rationale:
1. **Stack vs. Heap Lifecycles**:
   - Local variables live in the **Stack Frame** of the method. When the method returns, its stack frame is popped and destroyed.
   - The Lambda instance lives on the **Heap** and can outlive the method execution (e.g., passed to another thread or stored in a field).
2. **Value Copying (Capture Mechanism)**:
   - To make the variable available after the stack frame is destroyed, the JVM copies the primitive value (or object reference) into a synthetic private field of the lambda object.
   - If Java permitted mutating local stack variables, the stack value and the lambda's copied heap value would diverge, creating fatal data inconsistencies.

```
Stack Frame (process())                    Heap Memory
┌─────────────────────────┐                ┌──────────────────────────────┐
│ int port = 8080         │ ───Value Copy─>│ Lambda Instance              │
│ (Popped on method exit) │                │   private final int arg$1    │
└─────────────────────────┘                │   (Permanent value: 8080)    │
                                           └──────────────────────────────┘
```

> **Warning: Object Mutation Risk**:
> If the captured variable is an object reference (`final List<String> list`), the reference itself cannot be changed, but the internal state of the object (`list.add("item")`) *can* be mutated. If the lambda executes concurrently across multiple threads, this causes severe **data races** and **memory visibility bugs**.

---

### Streams Architecture: Lazy Computational Pipelines

#### 1. Stream vs. Collection Contract
- **Collection**: In-memory **Data Structure**. Eagerly computed, stores all elements in RAM, and can be iterated multiple times.
- **Stream**: **Computational Pipeline**. Lazily evaluated, does not store data, operates on a source on-demand, and is **single-use** (cannot be reused once a terminal operation executes).

---

#### 2. Pipeline Execution: Spliterator, Intermediate & Terminal Operations

$$\text{Source} \longrightarrow \text{Intermediate Operations (0..N)} \longrightarrow \text{Terminal Operation (1)}$$

```
Source (List/Set/Array)
     │
     ▼
filter(word -> !word.isBlank())  ───┐
     │                              │ Intermediate Operations (LAZY)
     ▼                              │ No data processed yet!
map(String::toLowerCase)         ───┘
     │
     ▼
collect(Collectors.toList())     ───► Terminal Operation (EAGER)
                                      Triggers traversal & consumes stream
```

#### A. Intermediate Operations (Lazy)
Intermediate operations return a new Stream and configure the pipeline without traversing data:
- **Stateless Operations**: `filter`, `map`, `flatMap`, `peek`. Each element is processed independently without knowledge of preceding elements.
- **Stateful Operations**: `sorted`, `distinct`, `limit`, `skip`. Must retain state or buffer elements before emitting downstream.
- **Loop Fusion**: The JVM fuses all stateless intermediate operations into a **single pass** over the data. Each item flows through the entire pipeline (`filter -> map`) before the next item is fetched, eliminating intermediate collection buffers.
- **Short-Circuiting**: Stops traversal as soon as conditions are met (`limit`, `findFirst`, `findAny`, `anyMatch`, `allMatch`, `noneMatch`).

#### B. Terminal Operations (Eager)
Terminal operations traverse the pipeline, produce a final non-stream result (value, collection, or side-effect), and **close/consume** the stream.
- Examples: `collect`, `reduce`, `forEach`, `count`, `min`, `max`, `anyMatch`, `findFirst`.

---

### Core Stream Operations & Comparisons

#### 1. `map` vs. `flatMap`
- **`map` ($1 \to 1$)**: Maps each element $T$ to exactly one element $R$.
- **`flatMap` ($1 \to 0..N$)**: Transforms each element $T$ into a `Stream<R>`, then **flattens** the nested streams into a single composite stream.

```
map:     [ [1, 2], [3, 4] ] ──map(List::size)──────────────> [ 2, 2 ]
flatMap: [ [1, 2], [3, 4] ] ──flatMap(Collection::stream)──> [ 1, 2, 3, 4 ]
```

```java
// Flattening order items
List<Order> orders = getOrders();
List<String> distinctItemNames = orders.stream()
    .flatMap(order -> order.getItems().stream()) // Flatten nested lists
    .map(Item::getName)
    .distinct()
    .toList();                                   // Java 16+ unmodifiable list
```

---

#### 2. `findFirst` vs. `findAny`
- **`findFirst()`**: Preserves deterministic **encounter order**. In parallel streams, it requires strict thread coordination, reducing parallelism throughput.
- **`findAny()`**: Returns any matching element without respecting encounter order. Significantly faster in parallel stream execution.

---

#### 3. `reduce` vs. `collect`

```
┌────────────────────────────────────────────────────────────────────────┐
│ reduce  = IMMUTABLE FOLD (Produces a new value on every step)          │
│ collect = MUTABLE REDUCTION (Accumulates into a mutable container)     │
└────────────────────────────────────────────────────────────────────────┘
```

- **`reduce`**: Combines elements using an associative accumulator (`(a, b) -> a + b`). Best suited for immutable value types (`int`, `String`, `BigDecimal`).
- **`collect`**: Accumulates elements into a mutable target container (`List`, `Map`, `StringBuilder`) using the `Collector` interface:
  - **`supplier()`**: Creates the empty accumulator container (`() -> new ArrayList<>()`).
  - **`accumulator()`**: Adds an element to the container (`(list, item) -> list.add(item)`).
  - **`combiner()`**: Merges two containers during parallel execution (`(l1, l2) -> { l1.addAll(l2); return l1; }`).
  - **`finisher()`**: Transforms the intermediate container to final output (`Function.identity()`).
  - **`characteristics()`**: Optimizer flags (`CONCURRENT`, `UNORDERED`, `IDENTITY_FINISH`).

---

#### 4. Essential Production Collectors

```java
List<Employee> employees = getEmployees();

// 1. Grouping by Department
Map<Department, List<Employee>> byDept = employees.stream()
    .collect(Collectors.groupingBy(Employee::getDepartment));

// 2. Downstream Aggregations: Count & Sum
Map<Department, Long> headCount = employees.stream()
    .collect(Collectors.groupingBy(Employee::getDepartment, Collectors.counting()));

Map<Department, Double> totalSalary = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.summingDouble(Employee::getSalary)));

// 3. toMap with Merge Function (Prevents IllegalStateException on duplicate keys)
Map<String, Employee> empByEmail = employees.stream()
    .collect(Collectors.toMap(
        Employee::getEmail,
        Function.identity(),
        (existing, replacement) -> existing)); // Duplicate resolution strategy
```

---

### Parallel Streams and Concurrency Realities

#### 1. Under the Hood: `ForkJoinPool.commonPool()`
- Calling `.parallelStream()` partitions the data via **`Spliterator.trySplit()`** and executes tasks across the JVM-wide `ForkJoinPool.commonPool()`.
- Thread pool size defaults to $\text{Runtime.getRuntime().availableProcessors()} - 1$.

```
                            Spliterator.trySplit()
                               ┌─────────────┐
                               │  ForkJoin   │
                ┌──────────────┤ commonPool  ├──────────────┐
                │              └─────────────┘              │
                ▼                                           ▼
          Worker Thread 1                             Worker Thread 2
          (Array Chunk 1)                             (Array Chunk 2)
                │                                           │
                └─────────────────────┬─────────────────────┘
                                      ▼
                             Collector.combiner()
                                (Final Merge)
```

#### 2. The $N \times Q > 10,000$ Rule
Only consider parallel streams when $N$ (element count) multiplied by $Q$ (computational cost per element) exceeds $10,000$, and the task is **pure CPU-bound and stateless**.

#### 3. Critical Parallel Stream Anti-Patterns:
1. **Blocking I/O (Database, Network, File calls)**: `commonPool` is shared across the entire JVM. Blocking a worker thread starves all other parallel streams and `CompletableFuture` tasks in the application.
2. **Poor Spliterator Source ($O(N)$ split cost)**: `ArrayList` and primitive arrays split in $O(1)$. `LinkedList` and `Stream.iterate` require $O(N)$ pointer chasing to split, completely negating parallelism benefits.
3. **Shared Mutable State**:
   ```java
   // CRITICAL BUG: Data race, lost updates, and memory corruption
   List<Integer> list = new ArrayList<>();
   numbers.parallelStream().forEach(list::add); // NOT THREAD-SAFE!
   ```
4. **Non-Associative Reductions**: Reductions must satisfy $(a \circ b) \circ c = a \circ (b \circ c)$. Operations like subtraction (`(a, b) -> a - b`) produce erratic, non-deterministic results in parallel.

---

### `Optional<T>`: Design Philosophy & Type Safety

#### 1. Purpose: Explicit Return-Type Modeling
`Optional<T>` is a container designed specifically as a **method return type** to communicate that a result may be absent, forcing callers to handle absence explicitly at compile time.

```java
// Fluent transformation pipeline
String street = Optional.ofNullable(user)
    .filter(User::isActive)
    .map(User::getAddress)
    .map(Address::getStreet)
    .map(String::trim)
    .orElse("Unknown Street");
```

---

#### 2. Evaluation Semantics: `orElse` vs. `orElseGet` vs. `orElseThrow`

```
┌────────────────────────────────────────────────────────────────────────┐
│ orElse(value)         ──> EAGER evaluation (Always calculates value)   │
│ orElseGet(supplier)   ──> LAZY evaluation  (Executes only when empty)  │
│ orElseThrow(supplier) ──> LAZY evaluation  (Throws only when empty)    │
└────────────────────────────────────────────────────────────────────────┘
```

```java
public User getUser(String id) {
    Optional<User> userOpt = repository.findById(id);

    // ❌ PERFORMANCE / SIDE-EFFECT TRAP:
    // createDefaultUser() IS ALWAYS EXECUTED, even if userOpt is present!
    User u1 = userOpt.orElse(createDefaultUser());

    // ✅ CORRECT:
    // Supplier runs ONLY if userOpt is empty
    User u2 = userOpt.orElseGet(() -> createDefaultUser());

    // ✅ Explicit domain exception
    User u3 = userOpt.orElseThrow(() -> new UserNotFoundException(id));
}
```

---

### Critical Pitfalls & Production Best Practices

- **Never call `.get()` blindly**: Calling `optional.get()` without checking `isPresent()` reproduces the exact same runtime crash as `NullPointerException` (throws `NoSuchElementException`).
- **Do not use `Optional` in fields or parameters**: `Optional` is not `Serializable` (causes `NotSerializableException` in caching/remote frameworks) and adds 16 bytes of heap overhead per reference. Use standard nullable fields and provide `Optional` accessors if needed.
- **Never wrap Collections in `Optional`**: Return an empty collection (`Collections.emptyList()`, `List.of()`) instead of `Optional<List<T>>` or `null`.
- **Never assign `null` to an `Optional` reference**: Assigning `Optional<User> user = null;` defeats the purpose of the type. Always initialize with `Optional.empty()`.
- **Stream Re-use Exception**: Streams cannot be re-used. Once a terminal operation is called, subsequent calls throw `IllegalStateException`.
- **Stateless & Non-interfering Lambdas**: Functions passed to stream operations must not modify the backing data source during stream traversal (prevents `ConcurrentModificationException`).
