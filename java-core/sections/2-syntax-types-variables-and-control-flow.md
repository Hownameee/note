# 2. Syntax, types, variables, and control flow

### Primitive and reference types

Java has eight primitive types:

| Group | Types | Notes |
| --- | --- | --- |
| Integral | `byte`, `short`, `int`, `long` | Signed two's-complement values |
| Floating point | `float`, `double` | IEEE 754 behavior; not exact decimal arithmetic |
| Character | `char` | One UTF-16 code unit, not necessarily one Unicode character |
| Logical | `boolean` | `true` or `false` |

A reference variable stores a reference to an object or `null`. Java is always pass-by-value: a method receives a copy of a primitive value or a copy of an object reference.

```java
static void rename(StringBuilder value) {
    value.append("!");       // mutates the shared object
    value = new StringBuilder("new"); // only reassigns the local copy
}
```

#### Reference types are NOT just wrappers

The 8 wrapper classes (`Integer`, `Long`, `Double`, etc.) are only a tiny subset of reference types. Most reference types are general objects:

| Category | Examples | Notes |
| --- | --- | --- |
| **Wrapper classes** | `Integer`, `Double`, `Boolean` | Box a single primitive value |
| **Custom classes** | `Person`, `Thread`, `HttpServer` | Domain / system logic |
| **Collections** | `ArrayList`, `HashMap` | Manage references internally |
| **Arrays** | `int[]`, `String[]` | Fixed-size objects on the Heap |
| **System / native refs** | `FileDescriptor`, `Socket` | Hold OS-level handles (e.g., `int fd`) |

#### Physical bottom layer: everything is primitives + pointers

No matter how complex, drilling an object to RAM level yields only:
1. **Primitive values** — raw bits (`byte`, `int`, `long`, etc.)
2. **Reference/pointer values** — 32- or 64-bit memory addresses pointing to other heap objects

Example — `String` internals (Java 9+):
```java
public final class String {
    private final byte[] value; // pointer to byte-array on heap
    private final byte coder;   // LATIN1(0) or UTF16(1) — raw primitive
    private int hash;           // cached hashCode — raw primitive
}
// byte[] itself contains raw bytes packed contiguously on the heap.
```

### Variables and initialization

- Fields and array elements receive default values (see table below).
- Local variables must be definitely assigned before use — the compiler enforces this.
- `final` prevents reassignment after initialization; it does not make a referenced object immutable.
- `var` asks the compiler to infer a local variable's static type. It is not dynamic typing and cannot be used for fields or method parameters.

#### Where data actually lives (the golden rule)

> **Data lives wherever its containing context lives.**

| Declaration | Memory location |
| --- | --- |
| Local variable in a method (`int x = 5;`) | **Stack** (method's stack frame) |
| Instance field (`class Foo { int x; }`) | **Heap** (inside the object) |
| Element in an array (`int[] arr = {5};`) | **Heap** (inside the array object) |
| `static` field (`static int x;`) | **Heap / Metaspace** (class metadata area) |

> The JIT compiler's **Escape Analysis** + **Scalar Replacement** optimization can eliminate heap allocation entirely for objects that never leave their creating method, placing their primitive fields in CPU registers or on the stack instead.

#### Default values for fields and array elements

| Type | Default value |
| --- | --- |
| `byte`, `short`, `int`, `long` | `0` |
| `float`, `double` | `0.0` |
| `boolean` | `false` |
| `char` | `'\u0000'` (null character) |
| Any reference type | `null` |

Local variables get **no** default value:
```java
int x;
System.out.println(x); // ❌ COMPILER ERROR: variable x might not have been initialized
```

#### Autoboxing and the Integer cache

When a primitive is passed where an `Object` is expected, the compiler inserts `Integer.valueOf(...)` (autoboxing). `Integer.valueOf` caches instances for **−128 to +127**, so variables in that range point to the **same cached heap object**:

```java
Integer a = 1;   // cached
Integer b = 1;   // same cached object as a
Integer c = 200; // new heap object
Integer d = 200; // different new heap object

System.out.println(a == b); // true  (same instance)
System.out.println(c == d); // false (different instances)

// Autoboxing pitfall: identityHashCode(Object) boxes the int first
int aa = 1;
System.identityHashCode(aa); // → Integer.valueOf(1) → same cached object as a and b!
```

> Never rely on `==` for wrapper value equality. Always use `.equals()`.

### Numeric conversions

- Widening conversions, such as `int` to `long`, are generally implicit.
- Narrowing conversions, such as `long` to `int`, require a cast and may lose information.
- Integer arithmetic commonly promotes operands smaller than `int` to `int`.
- Integer overflow wraps according to the type; use `Math.addExact` and related methods when overflow must be detected.
- Use `BigDecimal` for exact decimal-domain calculations such as money, with an explicit scale and rounding policy.

### Control flow

Know `if`, `switch`, `for`, enhanced `for`, `while`, `do-while`, `break`, `continue`, and `return`. Modern `switch` can be an expression:

```java
String label = switch (status) {
    case 200, 201 -> "success";
    case 404 -> "missing";
    default -> "other";
};
```

Prefer exhaustive expressions when modeling a closed set of cases. Do not rely on fall-through unless it is deliberate and obvious.

### Arrays and varargs

Arrays are fixed-size, indexed, mutable objects with a runtime component type. Varargs compile to arrays:

```java
static int sum(int... values) { /* values is int[] */ }
```

Generic arrays are restricted because arrays are covariant and reified while generic types are invariant and mostly erased. For example, `String[]` is an `Object[]`, but `List<String>` is not a `List<Object>`.

#### Arrays are Objects

Every array — even `int[]` — is an instance of `java.lang.Object` allocated on the Heap:

```java
int[] arr = new int[5];
System.out.println(arr instanceof Object);               // true
System.out.println(arr.getClass().getSuperclass());      // class java.lang.Object
Object o = arr;                                          // valid
// Object[] bad = new int[3];                            // ❌ COMPILE ERROR
```

Arrays also implement `Cloneable` and `Serializable`.

#### Heap memory layout of an array object

```
┌─────────────────────────────────────────────────────────────┐
│                 ARRAY OBJECT ON THE HEAP                    │
├──────────────────┬──────────────────┬───────────────────────┤
│  Object Header   │   length (4 B)   │  Contiguous payload   │
│  (12–16 bytes)   │  arr.length O(1) │  elements packed here │
└──────────────────┴──────────────────┴───────────────────────┘
```

- **Primitive arrays** (`int[]`): raw values packed back-to-back.
- **Reference arrays** (`String[]`): 4–8 byte reference pointers to other heap objects.
- `arr.length` is an O(1) memory read, not a method call.

#### JVM internal type descriptors for arrays

```java
new int[0].getClass().getName()    // → [I
new byte[0].getClass().getName()   // → [B
new String[0].getClass().getName() // → [Ljava.lang.String;
new int[0][0].getClass().getName() // → [[I  (array of int[])
```

`[` = array dimension; `I`/`B`/`D` = primitive type code; `L...;` = reference class.

#### Multidimensional arrays are arrays of arrays

`int[][]` is NOT a contiguous 2D memory block. It is an array of references to independent `int[]` rows (rows can have different lengths — jagged arrays).

#### Array type covariance (object arrays only)

```java
String[] strArr = new String[]{"hello"};
Object[] objArr = strArr;   // ✅ Covariant assignment
objArr[0] = 42;             // ❌ ArrayStoreException at RUNTIME

Object ok   = new int[3];   // ✅ any array IS an Object
Object[] bad = new int[3];  // ❌ COMPILE ERROR (primitive arrays are not Object[])
```
