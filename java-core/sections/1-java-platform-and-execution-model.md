# 1. Java platform and execution model

### JDK, JVM, and bytecode

- The **JDK** contains development tools and a Java runtime: `javac`, `java`, `jar`, `javadoc`, `jdb`, `jshell`, `jcmd`, and others.
- The **JVM** executes class files. It loads, verifies, links, initializes, and runs bytecode.
- `javac` normally translates `.java` source into platform-independent `.class` bytecode.
- A JVM may interpret bytecode and compile hot methods to native code with a just-in-time compiler.
- Java SE is the specification/platform; OpenJDK and Oracle JDK are implementations/distributions.

```text
Source (.java) -> javac -> bytecode (.class) -> class loader/JVM -> machine execution
```

"Write once, run anywhere" depends on a compatible runtime and portable code; native libraries, filesystem assumptions, encodings, and OS behavior can still reduce portability.

### Object identity and memory addresses

Java deliberately hides raw native memory pointers. The GC can move objects in memory, so exposing raw addresses would break memory safety. Three useful approaches:

1. **`System.identityHashCode(obj)`** — returns the default hashcode, often derived from the object's initial memory address. Format with `Integer.toHexString(...)` to match `Object.toString()` (e.g., `java.lang.Integer@1b6d3586`).
2. **OpenJDK JOL** (`org.openjdk.jol`) — `VM.current().addressOf(obj)` reports the real heap address for diagnostics.
3. **`sun.misc.Unsafe`** — reads raw memory offsets; misuse can crash the JVM.

```java
Integer x = 1000;
// pointer-like hex identity
System.out.println(Integer.toHexString(System.identityHashCode(x))); // e.g. 4e50df2e
```

### JVM runtime data areas

| Area | Contents | Lifetime |
| --- | --- | --- |
| **Heap** | All object instances and arrays | Until GC collects them |
| **Per-thread stack** | Stack frames, local variables, operand stack | Until the method / thread exits |
| **Metaspace** | Class metadata, `vtable`s, static field values | Until the class is unloaded |
| **Code cache** | JIT-compiled native code | JVM lifetime |

> Do not oversimplify to "primitives live on the stack, objects on the heap." The actual rule: **data lives wherever its containing context lives** (see Section 2 for the full breakdown).

### The complete lifecycle: compile → load → execute

```
┌──────────────────────────────────────────────────────────┐
│ 1. javac (Compile Time)                                  │
│    • Emits symbolic bytecode instructions:               │
│      - invokevirtual  → dynamic vtable dispatch          │
│      - invokespecial  → direct call (super/private/ctor) │
│      - invokestatic   → static method call               │
│    • NO vtable exists yet                                │
└──────────────────────────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────┐
│ 2. Class Loading / Linking (JVM Runtime)                 │
│    • JVM loads .class into Metaspace                     │
│    • Builds the vtable for the class:                    │
│      - Inherits parent vtable slots                      │
│      - Replaces overridden method slots                  │
│      - Appends new method slots                          │
└──────────────────────────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────┐
│ 3. Execution (JVM Runtime)                               │
│    • invokevirtual: reads object Klass Word →            │
│      looks up vtable → calls correct method              │
│    • invokespecial: jumps directly to target method      │
└──────────────────────────────────────────────────────────┘
```

### Compilation and entry point

Traditional entry point:

```java
public final class Hello {
    public static void main(String[] args) {
        System.out.println("Hello, Java");
    }
}
```

The filename is `Hello.java` because the top-level public class is `Hello`. Compile with `javac Hello.java`, then run with `java Hello`.

Java 25 permanently adds compact source files and instance `main` methods for simpler beginner programs. Learn the traditional form first because it remains common in production code and works on Java 21.
