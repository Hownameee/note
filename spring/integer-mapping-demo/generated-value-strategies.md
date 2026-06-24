# `@GeneratedValue` in JPA — Deep Dive

## What it does

`@GeneratedValue` tells Hibernate: **"I don't want to manage the ID myself — you handle it."**
It always pairs with `@Id`. Without it, you'd need to set the ID manually before every `save()`.

---

## The 2 Primary Strategies

### 1. `GenerationType.IDENTITY`

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

**How it works internally:**

Hibernate uses `org.hibernate.id.IdentityGenerator` under the hood. It tries 3 approaches to retrieve the DB-generated value, in order:

1. **`Statement#getGeneratedKeys()`** — if the JDBC driver supports it (most modern drivers do).
2. **`INSERT + SELECT` syntax** — if `Dialect#supportsInsertSelectIdentity()` is true, Hibernate uses a dialect-specific combined statement.
3. **Separate identity query** — as a last resort, Hibernate issues a separate SQL command defined by `Dialect#getIdentitySelectString`.

```sql
-- Hibernate sends:
INSERT INTO employee (name, qualification) VALUES ('Alice', 'Engineer');
-- DB assigns id = 42 automatically
-- Hibernate retrieves id = 42 via getGeneratedKeys()
```

Supported by: MySQL, PostgreSQL, SQL Server.

> ⚠️ **Extended Persistence Context Warning** (from Hibernate docs):
> Because the entity row must be **physically inserted before the ID is known**, IDENTITY generation is problematic with **extended persistence contexts** (long-lived transactions / conversations). Hibernate officially recommends using **SEQUENCE** instead in those scenarios.

> ⚠️ **Batch INSERT is disabled**: Hibernate cannot batch `INSERT` statements for entities using `IDENTITY` generation, because it needs each individual ID immediately after insertion. If the application frequently creates many entities of the same type at once, this can be a significant performance bottleneck.

**Delayed insert behavior (Hibernate 5.3+):**

Hibernate 5.3 introduced delayed entity inserts when `flush-mode != AUTO`. This caused issues for entities with `IDENTITY` or `SEQUENCE` IDs involved in associations. Hibernate 5.4 added a smarter algorithm to decide if the insert should be delayed or not.

If you hit edge cases, you can revert to the legacy behavior:

```properties
hibernate.id.disable_delayed_identity_inserts=true
```

---

### 2. `GenerationType.SEQUENCE`

```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "emp_seq")
@SequenceGenerator(name = "emp_seq", sequenceName = "employee_seq", allocationSize = 50)
private Long id;
```

**How it works internally:**

1. Before inserting, Hibernate calls `SELECT nextval('employee_seq')` to get the next ID *from the DB sequence object*.
2. By default, it pre-fetches a **block of IDs** (controlled by `allocationSize`, default = 50) in one DB round-trip.
3. It assigns IDs from memory for the next 50 inserts, only calling the sequence again when the block is exhausted.
4. Because Hibernate already knows the ID before inserting, it **can batch** multiple `INSERT` statements together.

```sql
-- Hibernate pre-fetches a block:
SELECT nextval('employee_seq');  -- returns 1 (representing IDs 1-50)

-- Then batches all inserts together:
INSERT INTO employee (id, name) VALUES (1, 'Alice');
INSERT INTO employee (id, name) VALUES (2, 'Bob');
INSERT INTO employee (id, name) VALUES (3, 'Carol');
-- ... no extra DB round-trips needed until id 51
```

Supported by: PostgreSQL, Oracle, H2.

---


## Sequence Design Strategy: Global vs Per-Table

When using `GenerationType.SEQUENCE`, you face an important architectural decision: should all entities share **one global sequence**, or should each table have **its own dedicated sequence**? The answer also differs subtly depending on whether you target **Oracle** or **PostgreSQL**.

### Under-the-Hood Differences: Oracle vs PostgreSQL

Both databases treat a `SEQUENCE` as a first-class database object. However, their syntax and caching model differ:

| Aspect | PostgreSQL | Oracle |
| :--- | :--- | :--- |
| **Get next value** | `SELECT nextval('seq_name')` | `SELECT seq_name.NEXTVAL FROM DUAL` |
| **Session cache** | Each session has its own cache — values can appear out-of-order across sessions | Shared pool cache — consistent across all sessions |
| **Auto-increment alternative** | `SERIAL` / `BIGSERIAL` (backed by sequence) | `GENERATED AS IDENTITY` (Oracle 12c+, also backed by sequence internally) |
| **Hibernate Dialect** | `PostgreSQLDialect` auto-generates `nextval(...)` | `OracleDialect` auto-generates `.NEXTVAL` |

Hibernate's `Dialect` layer abstracts both syntaxes — you write `@SequenceGenerator` once and Hibernate generates the correct SQL for each database.

---

### Strategy 1: Global Sequence (One sequence shared across the entire DB)

When you use `GenerationType.AUTO` without specifying a custom `@SequenceGenerator`, Hibernate defaults to a single shared sequence — `hibernate_sequence` (Hibernate 5) or `seq_gen_sequence` (Hibernate 6) — used by **every entity** in the application.

```java
// No explicit generator — Hibernate picks a shared sequence
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
private Long id;
```

**What actually happens in the DB:**

```sql
-- Hibernate calls one shared sequence for ALL tables:
SELECT nextval('hibernate_sequence');  -- User gets id=1
SELECT nextval('hibernate_sequence');  -- Order gets id=2
SELECT nextval('hibernate_sequence');  -- User gets id=3
SELECT nextval('hibernate_sequence');  -- Product gets id=4
```

**Trade-offs:**

| | |
| :--- | :--- |
| ✅ **Zero setup** | No `@SequenceGenerator` annotation needed on any entity |
| ✅ **Globally unique IDs** | No two rows across *any* table share the same ID — useful in some audit/event systems |
| ❌ **Confusing ID gaps per table** | User IDs: 1, 3, 7, ... Order IDs: 2, 4, 6, ... IDs look non-sequential within a table, making debugging and statistics harder |
| ❌ **Bottleneck at high concurrency** | All insert threads from all entity types contend for the same single sequence object |
| ❌ **Hard to reset** | If you truncate a table, you can't reset just that table's counter without affecting every other entity |

---

### Strategy 2: Per-Table Sequence (One dedicated sequence per table) — Recommended

Each entity declares its own `@SequenceGenerator` pointing to a **dedicated sequence object** in the DB. This is the standard pattern in enterprise applications.

```java
// User entity — uses its own sequence
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(
        name           = "user_seq",
        sequenceName   = "users_id_seq",   // dedicated sequence for this table
        allocationSize = 50
    )
    private Long id;
}

// Order entity — uses its own separate sequence
@Entity
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_seq")
    @SequenceGenerator(
        name           = "order_seq",
        sequenceName   = "orders_id_seq",  // dedicated sequence for this table
        allocationSize = 50
    )
    private Long id;
}
```

**What actually happens in the DB:**

```sql
-- Each table has its own independent counter:
SELECT nextval('users_id_seq');    -- User gets id=1
SELECT nextval('orders_id_seq');   -- Order gets id=1 (independent!)
SELECT nextval('users_id_seq');    -- User gets id=2
SELECT nextval('orders_id_seq');   -- Order gets id=2
```

**Trade-offs:**

| | |
| :--- | :--- |
| ✅ **Clean, sequential IDs per table** | Users: 1, 2, 3, 4 ... Orders: 1, 2, 3, 4 ... Easy to reason about |
| ✅ **Independent reset** | `TRUNCATE users; ALTER SEQUENCE users_id_seq RESTART;` — doesn't affect any other table |
| ✅ **Better concurrency isolation** | High-volume insert on `orders` doesn't contend with inserts on `users` |
| ❌ **More boilerplate** | Every entity needs its own `@SequenceGenerator` annotation |

---

### Aligning `allocationSize` with the DB Sequence Cache

This is a critical subtlety that many developers miss. Hibernate's `allocationSize` (app-level cache) and the DB sequence's `INCREMENT BY` / `CACHE` (DB-level cache) must be **synchronized**, or you will get ID conflicts or large gaps.

```sql
-- PostgreSQL: create the sequence to match Hibernate's allocationSize=50
CREATE SEQUENCE users_id_seq
    START WITH 1
    INCREMENT BY 50     -- must match allocationSize in @SequenceGenerator
    CACHE 50;           -- optional DB-level cache for extra performance

-- Oracle equivalent:
CREATE SEQUENCE users_id_seq
    START WITH 1
    INCREMENT BY 50
    CACHE 50
    NOCYCLE;
```

> **Why this matters:** If Hibernate's `allocationSize=50` but the DB sequence `INCREMENT BY 1`, Hibernate thinks IDs 1–50 are reserved after fetching `nextval()=1`, but the DB only increments by 1. The next `nextval()` call returns 2, not 51 — causing **duplicate ID conflicts** and `DataIntegrityViolationException` errors under concurrent load.

---

## IDENTITY vs SEQUENCE — Head-to-Head

| Factor | `IDENTITY` | `SEQUENCE` |
| :--- | :--- | :--- |
| **ID generated by** | Database (after INSERT) | Database sequence (before INSERT) |
| **Hibernate knows ID before INSERT?** | ❌ No | ✅ Yes |
| **JDBC Batch Insert support** | ❌ Disabled | ✅ Enabled |
| **DB round-trips per insert** | 1 INSERT + 1 `getGeneratedKeys()` | ~1 per 50 inserts (with `allocationSize=50`) |
| **ID gaps possible?** | No (sequential per table) | Yes (gaps at app restart or rollback) |
| **DB support** | MySQL, PostgreSQL, SQL Server | PostgreSQL, Oracle, H2 |
| **Config complexity** | ✅ Zero (simplest) | Slightly more (`@SequenceGenerator` needed) |
| **Performance at high write volume** | ❌ Slower | ✅ Significantly faster |

---

## Which is Best for Sequential `1 → N` Primary Keys?

### ✅ Use `IDENTITY` when

- Write volume is **low to moderate** (typical CRUD web app).
- You want **zero configuration** and **strictly sequential IDs with no gaps**.
- You're using **MySQL** (which has no native sequence object).
- Simplicity is a priority.

### ✅ Use `SEQUENCE` when

- You have **high write throughput** (bulk inserts, batch jobs, event logs).
- You want **JDBC batch insert optimization** — can give **5-10x throughput improvement** for bulk writes.
- You're on **PostgreSQL or Oracle** (sequence objects are first-class citizens).
- You can tolerate **ID gaps** (e.g., IDs jump from 50 → 101 after restart — the gap comes from the pre-allocated block being discarded).

---

## Practical Recommendation

```java
// For most Spring Boot + PostgreSQL apps:
// Simple entities with low-moderate write → IDENTITY is fine
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

// For high-throughput entities (orders, events, audit logs):
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_seq")
@SequenceGenerator(
    name         = "order_seq",
    sequenceName = "order_id_seq",
    allocationSize = 50   // pre-fetch 50 IDs at once
)
private Long id;
```

> **Rule of thumb:** Start with `IDENTITY` for simplicity. Switch to `SEQUENCE` only when you observe write bottlenecks or need batch insert performance — it's an easy migration since it only changes the ID generation mechanism, not the column type (`BIGINT` stays `BIGINT`).

---

## References

- [Jakarta Persistence — @GeneratedValue](https://jakarta.ee/specifications/persistence/3.1/apidocs/jakarta.persistence/jakarta/persistence/generatedvalue)
- [Hibernate 6.5 — Using IDENTITY columns](https://docs.hibernate.org/orm/6.5/userguide/html_single/#identifiers-generators-identity)
- [Hibernate 6.5 — Using sequences](https://docs.hibernate.org/orm/6.5/userguide/html_single/#identifiers-generators-sequence)
- [PostgreSQL — CREATE SEQUENCE](https://www.postgresql.org/docs/17/sql-createsequence.html)
- [Oracle — CREATE SEQUENCE](https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-SEQUENCE.html)
- [Vlad Mihalcea — Hibernate IDENTITY and SEQUENCE generators](https://vladmihalcea.com/hibernate-identity-sequence-and-table-sequence-generator/)
