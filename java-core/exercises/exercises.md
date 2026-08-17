# 16. Java Core Practice Exercises and Study Plan

The goal is to turn explanations into observable behavior. Use only the JDK for the first pass. Add a test framework later if you want a larger project structure.

## Practice rules

1. Predict output or failure before running code.
2. Compile with warnings enabled when useful:

   ```bash
   javac -Xlint:all Example.java
   ```

3. Test normal, boundary, null/empty, invalid, and concurrent cases as applicable.
4. Explain the language/API contract separately from the current JDK implementation.
5. Keep each first solution small; refactor only after it works and tests expose duplication.

## Level 1: language foundations

1. **Numeric explorer:** Print the minimum/maximum primitive integral values. Demonstrate overflow, narrowing, integer division, and `Math.addExact`.
2. **Unicode inspector:** Given a string containing ASCII, Vietnamese text, and an emoji, print UTF-16 length, code-point count, and each code point.
3. **Control-flow kata:** Implement FizzBuzz with a loop, then use a switch expression to categorize values.
4. **Array statistics:** Calculate min, max, mean, and median without collection utilities. Define behavior for an empty input.
5. **Pass-by-value proof:** Write methods that mutate an object, reassign its parameter, change a primitive parameter, and return replacements. Explain every result.

## Level 2: OOP, contracts, and immutability

6. **Bank account model:** Protect the non-negative balance invariant. Separate a rejected command from an unexpected failure.
7. **Shape hierarchy:** Use an interface, two records, and an exhaustive operation. Decide where validation belongs.
8. **Immutable user:** Store an ID, name, date of birth, and roles. Prevent callers from mutating internal role state.
9. **Equality laboratory:** Create correct and deliberately broken value types. Observe their behavior in `HashSet` and `HashMap`.
10. **Composition refactor:** Start with an inheritance-based notification design, then replace it with composable delivery strategies. Describe the coupling removed.

## Level 3: generics and collections

11. **Generic stack:** Implement `Stack<T>` using `ArrayDeque<T>` and define empty-stack behavior.
12. **PECS copy:** Implement a generic copy/transform API using bounded wildcards. Write calls that should and should not compile.
13. **Word frequency:** Normalize a text and count words with a map. Sort output by descending frequency and then alphabetically.
14. **Deduplicate while preserving order:** Solve with a set, then explain time and memory trade-offs.
15. **Top K values:** Use `PriorityQueue` without fully sorting a large list. Explain complexity.
16. **LRU-like cache:** Extend `LinkedHashMap` or wrap it. Define eviction, capacity, and concurrency behavior.
17. **Collection factory comparison:** Demonstrate `List.of`, `copyOf`, `Arrays.asList`, unmodifiable views, and defensive copies.
18. **Mutable-key bug:** Reproduce a failed `HashMap` lookup after key mutation, then fix the design.

## Level 4: errors, files, and time

19. **CSV-like importer:** Parse a deliberately simple documented format. Report line-numbered validation errors while preserving causes for I/O failures.
20. **Resource failure:** Build two `AutoCloseable` test resources that throw on close. Inspect the primary and suppressed exceptions.
21. **File indexer:** Walk a directory with `Files`, group regular files by extension, and close every resource-owning stream.
22. **Time-zone scheduler:** Convert a local meeting time in `Asia/Ho_Chi_Minh` to `Instant` and two other zones. Test a daylight-saving gap/overlap in a zone that uses DST.

## Level 5: functions and streams

23. **Loop versus stream:** Implement the same transaction summary imperatively and with streams. Compare clarity, not line count.
24. **Nested data:** Use `flatMap` to turn departments and employees into a unique sorted skill list.
25. **Collector practice:** Group orders by customer, then compute count and total. Define a safe money representation.
26. **Optional boundary:** Model repository lookup as `Optional`; compare `orElse`, `orElseGet`, `map`, `flatMap`, and `orElseThrow` with observable suppliers.
27. **Parallel-stream trap:** Add shared mutable state inside a parallel pipeline, reproduce incorrect behavior, then replace it with a valid collector. Do not treat one successful run as proof.

## Level 6: concurrency and JVM observation

28. **Counter race:** Compare plain `int`, `volatile int`, `synchronized`, `AtomicInteger`, and `LongAdder` under contention. Explain correctness separately from timing.
29. **Bounded pipeline:** Build producers and consumers with `ArrayBlockingQueue`. Define backpressure, interruption, poison-pill or other shutdown behavior, and error reporting.
30. **Task comparison:** Run many simulated blocking tasks using a fixed platform-thread pool and one virtual thread per task. Measure elapsed time and resource behavior without claiming a universal winner.
31. **Deadlock lab:** Intentionally create a two-lock deadlock in a disposable program, capture a thread dump with `jcmd`, then fix it using lock ordering.
32. **Safe publication:** Compare unsafe lazy initialization, a holder idiom, and volatile double-checked locking. Explain the happens-before edges.
33. **Retention lab:** Keep objects in an unbounded static collection, inspect a class histogram, then bound/clear the state. Run with a deliberately small heap only in the disposable example.

## Capstone projects

### A. Library lending CLI

Model books, copies, members, loans, and overdue rules. Use immutable identifiers/value types, collections chosen by access pattern, `java.time`, explicit exceptions, file persistence, and tests for equality and invalid transitions.

Minimum interview discussion:

- Why each collection was chosen
- Entity identity versus value equality
- How invariants are enforced
- How file corruption and partial reads are handled
- What must change to support concurrent requests

### B. Concurrent log analyzer

Read multiple text files, parse timestamps and levels, aggregate counts, and report the most frequent messages. First implement sequentially; then use an executor or virtual threads for file-level concurrency.

Minimum interview discussion:

- I/O-bound versus CPU-bound work
- Bounded concurrency and backpressure
- Thread-safe aggregation versus per-task results and merging
- Malformed lines, cancellation, and resource cleanup
- Measurement method and when concurrency hurts

## Eight-week study plan

| Week | Read | Build | Interview review |
| --- | --- | --- | --- |
| 1 | Guide sections 1–2 | Exercises 1–5 | Questions 1–10 |
| 2 | Sections 3–4 | Exercises 6–10 | Questions 11–30 |
| 3 | Sections 5–6 | Exercises 11–18 | Questions 31–50 |
| 4 | Section 7 and date/I/O parts of 9 | Exercises 19–22 | Questions 51–60 |
| 5 | Section 8 | Exercises 23–27 | Questions 61–72 |
| 6 | Section 10 | Exercises 28–30 | Questions 73–88 |
| 7 | Sections 11–13 | Exercises 31–33 | Questions 89–100 |
| 8 | Sections 14–15 | One capstone | Two mock interviews |

## Mock interview scoring rubric

Score each answer from 0 to 3:

| Score | Evidence |
| --- | --- |
| 0 | No usable answer or materially incorrect claim |
| 1 | Definition only; cannot apply it |
| 2 | Correct contract plus a relevant example |
| 3 | Correct contract, example, trade-off, failure mode, and version/implementation nuance when relevant |

Track weak topics, not only total score. Revisit a missed question after one day, three days, one week, and two weeks.

## Suggested source-reading loop

When an answer is uncertain:

1. Read the relevant [dev.java tutorial](https://dev.java/learn/).
2. Check the type or package contract in the [Java SE API](https://docs.oracle.com/en/java/javase/25/docs/api/index.html).
3. Use the [Java Language Specification](https://docs.oracle.com/javase/specs/jls/se25/html/index.html) for language semantics.
4. Use the [JVM Specification](https://docs.oracle.com/javase/specs/jvms/se25/html/index.html) for class-file and JVM semantics.
5. Write the smallest program that distinguishes competing explanations.
