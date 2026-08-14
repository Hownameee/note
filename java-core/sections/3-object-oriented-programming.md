# 3. Object-oriented programming

### Classes and objects

A class groups state and behavior. An object is an instance. Constructors establish valid initial state; they are not inherited and have no return type.

Use access control intentionally:

- `private`: code enclosed by the top-level class/interface that encloses the declaration
- package-private: same package
- `protected`: same package plus subclasses under Java's protected-access rules
- `public`: accessible wherever the declaring type is accessible

Encapsulation means preserving invariants behind an API, not merely generating getters and setters.

#### java.lang.Object — root of the entire type hierarchy

Every class directly or indirectly extends `java.lang.Object`. If no `extends` clause is written, the compiler inserts `extends Object` implicitly. This guarantees every object has:
- `.toString()`, `.equals(Object)`, `.hashCode()`, `.getClass()`
- Thread coordination: `.wait()`, `.notify()`, `.notifyAll()`

Special types also ultimately extend `Object`:
- **Enums**: `enum Color` → `java.lang.Enum` → `Object`
- **Records** (Java 14+): `record Point(...)` → `java.lang.Record` → `Object`
- **Arrays**: `int[]`, `String[]` → direct subclass of `Object`

The only class not extending `Object` is `Object` itself (`Object.class.getSuperclass()` → `null`).

#### How the JVM builds a single object instance on the Heap

When `new Child()` is called (`Child extends Parent extends Grandparent`):

1. **ONE contiguous memory block** — the JVM allocates one block large enough for all fields from every ancestor. No separate objects per class level.
2. **Memory layout** (ancestor fields come first):
   ```
   ┌─────────────────────────────────────────────────────────────┐
   │              CHILD OBJECT ON THE HEAP                       │
   ├─────────────────────────────────────────────────────────────┤
   │ Mark Word (8 B) — GC metadata, identity hashcode, locks     │
   │ Klass Word (4–8 B) — pointer to Child.class in Metaspace    │
   ├─────────────────────────────────────────────────────────────┤
   │ Grandparent fields (in declaration order)                   │
   │ Parent fields (in declaration order)                        │
   │ Child fields (in declaration order)                         │
   └─────────────────────────────────────────────────────────────┘
   ```
3. **Constructor chain fires top-down**:
   `Object()` → `Grandparent()` → `Parent()` → `Child()`
   Each level initializes its own field offsets before the child continues.

#### Constructors are NOT inherited

A subclass cannot call a parent's constructor directly; it must declare its own and delegate via `super(...)`. If no `super(...)` appears, the compiler inserts `super()` (zero-arg parent ctor) as the first statement.

```java
class Person {
    protected String name;
    Person(String name) { this.name = name; }
}
class User extends Person {
    private String role;
    User(String name, String role) {
        super(name); // must call Person(String) explicitly
        this.role = role;
    }
}
// new User("Alice"); // ❌ COMPILER ERROR if User(String) is not declared
```

> Avoid calling overridable methods from constructors — the subclass field offsets may not yet be initialized.

### Inheritance, composition, and polymorphism

- A class extends at most one class and may implement multiple interfaces.
- Overriding selects behavior **dynamically** from the runtime object's class (resolved via `vtable` at execution time).
- Overloading selects a signature at **compile time** based on the declared parameter types.
- Static methods are **hidden**, not overridden — dispatch is compile-time based on the reference type.
- Fields are resolved by the **reference's declared type**, not polymorphically (compile-time `getfield` opcode).
- Prefer composition when there is no stable "is-a" relationship or when inheritance would expose fragile implementation details.

```java
interface Shape {
    double area();
}

record Circle(double radius) implements Shape {
    Circle {
        if (radius < 0) throw new IllegalArgumentException("negative radius");
    }

    @Override public double area() {
        return Math.PI * radius * radius;
    }
}
```

#### Field shadowing vs. method overriding

When a child declares a field with the same name as a parent field, **both fields exist** in the single heap object at different memory offsets. This is called **field shadowing**, not overriding:

```java
class Parent { protected String name = "Parent"; }
class Child extends Parent {
    private String name = "Child"; // shadows Parent.name — both exist on heap!
    void show() {
        System.out.println(this.name);  // "Child"  — Child's field offset
        System.out.println(super.name); // "Parent" — Parent's field offset
    }
}

Parent ref = new Child();
System.out.println(ref.name);      // 🔴 "Parent" — compile-time getfield Parent.name
System.out.println(ref.getName()); // 🟢 "Child"  — runtime vtable lookup
```

Key asymmetry:
- **Methods** → polymorphic — resolved at runtime via vtable — the actual heap object's class wins.
- **Fields** → NOT polymorphic — resolved at compile time via hardcoded `getfield ClassName.field` based on the declared reference type.

> Best practice: keep all fields `private` and expose via getters. This routes all field access through the polymorphic vtable mechanism.

#### Multi-level hierarchy and `super.super` is illegal

In a 3-level hierarchy (`Grandparent → Parent → Child`), inside `Child`:
- `this.name` → `Child.name`
- `super.name` → `Parent.name`
- `super.super.name` → **ILLEGAL** — Java does not support this syntax

To expose a grandparent's field from a grandchild, the intermediate class must provide a getter method.

#### Static method hiding

Static methods belong to the class, not an instance. They are dispatched at compile time from the declared reference type — no vtable involved:

```java
class Parent { public static void show() { System.out.println("Parent"); } }
class Child  { public static void show() { System.out.println("Child"); } }

Parent ref = new Child();
ref.show(); // 🔴 "Parent" — compile-time binding, declared reference type wins
```

Placing `@Override` on a static method is a **compile error**.

#### `@Override` annotation — a compile-time safety guard only

`@Override` does NOT change runtime behavior. It tells `javac`:
> "I intend to override a parent method. Verify my signature matches; report an error if it doesn't."

Without `@Override`, a typo silently creates a new independent method, and the parent's version keeps running — a very hard bug to notice:

```java
class Child extends Parent {
    // Typo: lowercase 'p' — silently creates a NEW method, does NOT override!
    public void printname() { System.out.println("Child"); }
}
Person p = new Child();
p.printName(); // 🔴 still calls Parent.printName(), not the child version!
```

Always use `@Override` when intending to override.

#### Bytecode instructions for method dispatch

| Instruction | Used for | Resolved | vtable? |
| --- | --- | --- | --- |
| `invokevirtual` | Instance method call (`ref.foo()`) | Runtime | ✅ Yes |
| `invokespecial` | `super.foo()`, `private` methods, constructors (`new`) | Runtime (target fixed at compile time) | ❌ No |
| `invokestatic` | `static` method call | Compile time | ❌ No |
| `invokeinterface` | Interface method call | Runtime | ✅ Yes (itable) |

`super.foo()` compiles to `invokespecial` — it bypasses the vtable and jumps directly to the named parent method in Metaspace. This is the only way from inside a child to call a parent version that has been overridden.

Example generated bytecode:
```bytecode
public void test();
  Code:
     0: aload_0
     1: invokevirtual #2   // this.getName() — vtable lookup at runtime
     4: pop
     5: aload_0
     6: invokespecial #3   // super.getName() — direct call, vtable bypassed
     9: return
```

#### Why overriding is resolved at runtime, not compile time

The compiler emits `invokevirtual` because the specific object type is unknown at compile time:

```java
static void greet(Person p) {
    p.printName(); // p could be Person, User, or Admin — only known at runtime
}
greet(new Person());
greet(new User());
greet(new Admin());
```

Static binding is only possible for fixed targets: `static`, `private`, `final`, or `super` calls.

#### vtable is created at class loading time, NOT compile time

`javac` emits symbolic references (e.g., `invokevirtual #2`). The actual vtable — an array of native function pointers — is built by the JVM in Metaspace during **class linking** (after the `.class` file is loaded):

1. JVM inspects the superclass's vtable.
2. For each virtual method: inherits the parent slot, or replaces it if overridden by this class.
3. New methods get appended as additional slots.

Why not at compile time?
- Classes can be compiled separately and deployed independently (replacing a `.jar` without recompiling everything).
- New classes can be loaded dynamically at runtime via `ClassLoader`.
- Native function pointer addresses are OS/architecture-specific; they cannot be determined from cross-platform bytecode.

An abstract class can hold instance state, protected helpers, and constructors. An interface defines a role/capability and supports multiple implementation inheritance; it can have abstract, default, static, and private methods, plus constants. Choose based on the type relationship and evolution needs, not on a rule that one is always better.

### `this`, `super`, initialization, and dispatch

Instance initialization runs superclass construction before subclass initialization. Avoid calling overridable methods from constructors: the subclass state may not yet be initialized.
