# `@GeneratedValue` in JPA — Deep Dive

## What it does

`@GeneratedValue` tells Hibernate: **"I don't want to manage the ID myself — you handle it."**
It always pairs with `@Id`. Without it, you'd need to set the ID manually before every `save()`.

---

## The 5 Strategies

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

### 3. `GenerationType.TABLE`

Hibernate uses `org.hibernate.id.enhanced.TableGenerator` under the hood. It maintains a table where each row is a named counter for a given entity type.

**Minimal setup (unnamed):**

```java
@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private Long id;
}
```

When no table name is given, Hibernate defaults to `hibernate_sequences`. When no `pkColumnValue` is specified, it uses the `"default"` segment.

```sql
create table hibernate_sequences (
    sequence_name varchar(255) not null,
    next_val      bigint,
    primary key (sequence_name)
);
```

**Configured setup with `@TableGenerator`:**

```java
@Id
@GeneratedValue(strategy = GenerationType.TABLE, generator = "table-generator")
@TableGenerator(
    name            = "table-generator",
    table           = "table_identifier",
    pkColumnName    = "table_name",
    valueColumnName = "product_id",
    allocationSize  = 5
)
private Long id;
```

**What actually runs in the DB for 3 inserts** (shows the locking overhead):

```sql
-- 1. Lock the counter row and read current value
SELECT tbl.product_id FROM table_identifier tbl
WHERE  tbl.table_name = 'Product' FOR UPDATE;

-- 2. Update counter by allocationSize (5)
UPDATE table_identifier
SET    product_id = 6
WHERE  product_id = 1 AND table_name = 'Product';

-- ... (repeats SELECT FOR UPDATE + UPDATE when block is exhausted)

-- 3. Finally insert the entities
INSERT INTO Product (product_name, id) VALUES ('Product 1', 1);
INSERT INTO Product (product_name, id) VALUES ('Product 2', 2);
INSERT INTO Product (product_name, id) VALUES ('Product 3', 3);
```

**Slowest of all strategies** — the `SELECT FOR UPDATE` on the counter table creates a lock contention bottleneck under concurrent load. Works across all databases, but **avoid in production**.

---

### 4. `GenerationType.AUTO` (Default)

```java
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
private Long id;
```

Hibernate picks a strategy based on the DB dialect. For PostgreSQL with Hibernate 6+, it defaults to **SEQUENCE** (using a shared `hibernate_sequence`).

Behavior can vary across DB and Hibernate versions — avoid in serious projects; be explicit about your strategy.

---

### 5. `GenerationType.UUID`

Hibernate uses `org.hibernate.id.UUIDGenerator` internally. It supports pluggable UUID generation strategies via the `org.hibernate.id.UUIDGenerationStrategy` contract.

**Default: RFC 4122 Version 4 (random)**

```java
@Entity
public class Book {
    @Id
    @GeneratedValue  // UUID type on field implies UUID generation
    private UUID id; // field type is java.util.UUID

    private String title;
}
```

The ID is generated **entirely in Java memory** before the `INSERT` — no DB round-trip needed, enabling batching.

**Alternative: RFC 4122 Version 1 (time-based)** via `@GenericGenerator`:

```java
@Id
@GeneratedValue(generator = "custom-uuid")
@GenericGenerator(
    name     = "custom-uuid",
    strategy = "org.hibernate.id.UUIDGenerator",
    parameters = {
        @Parameter(
            name  = "uuid_gen_strategy_class",
            value = "org.hibernate.id.uuid.CustomVersionOneStrategy"
        )
    }
)
private UUID id;
```

Version 1 UUIDs are time-ordered (using IP address instead of MAC address), which can be more index-friendly in databases.

> Requires: Jakarta Persistence 3.1+ / Hibernate 6+. Not available in JPA 2.x.
> The field type can be `java.util.UUID` (stored as `UUID` natively in PostgreSQL) or `String` (stored as `VARCHAR(36)`).

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
- [Hibernate 6.5 — Using table identifier generator](https://docs.hibernate.org/orm/6.5/userguide/html_single/#identifiers-generators-table)
- [Hibernate 6.5 — Using AUTO](https://docs.hibernate.org/orm/6.5/userguide/html_single/#identifiers-generators-auto)
- [PostgreSQL — CREATE SEQUENCE](https://www.postgresql.org/docs/17/sql-createsequence.html)
