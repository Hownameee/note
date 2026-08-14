# 4. Object contracts, immutability, and strings

### `equals` and `hashCode`

`Object.equals` defaults to identity (`return this == obj;`). A value type normally overrides both methods to compare internal state.

#### 1. The 5 Formal Contracts of `equals(Object obj)`
A correct equality relation must be:
- **Reflexive**: `x.equals(x)` must return `true`.
- **Symmetric**: `x.equals(y)` must return `true` if and only if `y.equals(x)` returns `true`.
- **Transitive**: If `x.equals(y)` is `true` and `y.equals(z)` is `true`, then `x.equals(z)` must be `true`.
- **Consistent**: Multiple invocations must return the same result, provided no fields modified.
- **Non-nullity**: `x.equals(null)` must return `false` (never throw `NullPointerException`).

#### 2. The `equals` & `hashCode` Contract Rule
- **If `a.equals(b) == true`** $\implies$ `a.hashCode() == b.hashCode()` **MUST be `true`**.
- **If `a.hashCode() == b.hashCode()`** $\implies$ `a.equals(b)` may be `true` or `false` (**Hash Collision**).
- **If `a.hashCode() != b.hashCode()`** $\implies$ `a.equals(b)` is guaranteed to be `false`.

#### 3. Why `equals()` does NOT compare `hashCode()`
`hashCode()` produces a 32-bit signed `int` (~4.29 billion values). Because potential object states are infinite, distinct objects can map to the same hash code (pigeonhole principle).
```java
String s1 = "FB";
String s2 = "Ea";

System.out.println(s1.hashCode()); // 2236
System.out.println(s2.hashCode()); // 2236
System.out.println(s1.equals(s2)); // false (different content)
```
If `equals()` compared hash codes, `"FB"` and `"Ea"` would falsely be considered equal.

#### 4. Is default `Object.hashCode()` a pointer/memory address?
**No.** It is often assumed to be a memory address, but it is not:
1. **Garbage Collection moves objects**: During memory compaction or copying GC cycles (G1, ZGC, Parallel), objects are relocated to new memory addresses. If `hashCode()` were the pointer, an object's hash code would mutate over its lifetime, breaking consistency.
2. **32-bit vs 64-bit**: `hashCode()` returns a 32-bit `int`, while modern JVM memory addresses are 64-bit.

**HotSpot JVM implementation**:
- Generated lazily via a **thread-local pseudo-random number generator** (Marsaglia XOR-shift PRNG by default in OpenJDK).
- Once computed, the 31-bit value is **cached directly in the object's Mark Word (object header)** so it stays permanent across GC relocations.
- You can always get the default identity hash code using `System.identityHashCode(obj)`, even if the class overrides `hashCode()`.

#### 5. Why you MUST override `hashCode()` when overriding `equals()`
If you override `equals()` but omit `hashCode()`, logically equal objects inherit distinct identity hash codes from `Object`. This breaks hash-based collections (`HashMap`, `HashSet`):
- **`HashMap.get(key)` returns `null`**: The search key maps to a different bucket based on its distinct identity hash code, so `HashMap` searches the wrong bucket and never even calls `equals()`.
- **`HashSet` allows duplicate elements**: Two equal objects are routed to different buckets and both get inserted into the `Set`.

#### 6. How `HashMap` uses `hashCode()` and `equals()` together
- **`hashCode()`** provides **routing/bucketing ($O(1)$ speed)**.
- **`equals()`** provides **exact equality verification (correctness)**.

```
map.get(key)
    │
    ▼
1. Calculate key.hashCode() -> determine array bucket index (O(1))
    │
    ├─ Bucket is empty ──> Key does not exist (instant return null)
    │
    └─ Bucket has entries ──> Traverse bucket and call equals() only on candidates
```

#### 7. Standard implementation pattern
```java
public class Person {
    private final String name;
    private final int age;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // 1. Fast reference identity check
        if (o == null || getClass() != o.getClass()) return false; // 2. Null & type check
        Person other = (Person) o;
        return age == other.age && Objects.equals(name, other.name); // 3. State comparison
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age); // Must use the exact same fields as equals()
    }
}
```

Never mutate fields that participate in equality or hashing while an object is a key in a hash-based collection (doing so strands the entry in the wrong bucket).

### `==` versus `equals`

- On primitives, `==` compares bitwise values (`5 == 5`).
- On references, `==` compares reference identity (whether both operands point to the exact same Heap memory address).
- `equals` compares according to the class contract (default is identity `this == obj`, overridden to compare state in value objects).
- Stateful/service objects (`Thread`, `InputStream`, `UserService`) intentionally retain default identity comparison.
- Records synthesize component-based `equals`, `hashCode`, and `toString` automatically.

### Designing immutable types

An immutable type typically:

1. establishes all state during construction;
2. exposes no mutators;
3. keeps fields private and final;
4. makes defensive copies of mutable inputs and outputs;
5. prevents subclass-based mutation when necessary.

A final reference to a mutable list is not enough. Prefer `List.copyOf(input)` when its null-rejection and shallow-copy semantics match the contract.

### Strings

`String` is immutable. Concatenation creates a result value; compilers may optimize expressions. Use `StringBuilder` for repeated single-threaded construction. `StringBuffer` synchronizes operations but is rarely the right default.

The string pool can reuse canonical string instances, which is why `==` may appear to work for literals. Always use `equals` for content.

Remember that a `char` is a UTF-16 code unit. Use code-point APIs when processing arbitrary Unicode text.

### Wrappers, boxing, and caching

Generics require reference types, so primitives are boxed (`int` to `Integer`) and unboxed when needed. Unboxing `null` throws `NullPointerException`. Wrapper caching makes some identity comparisons appear to work; never depend on it for value equality.
