# Spring DAO Support & Exception Translation

Spring's Data Access Object (DAO) support provides a consistent way to work with different persistence technologies (JDBC, Hibernate, JPA) and shields the application from catching vendor-specific exceptions.

---

## 1. Consistent Exception Hierarchy

Different persistence frameworks throw different exceptions (e.g., JDBC throws `SQLException`, JPA throws `PersistenceException`, Hibernate throws `HibernateException`). 

To prevent your business logic from being polluted with technology-specific catch blocks, Spring translates these into a unified hierarchy under **`DataAccessException`**.

```
                           DataAccessException (Unchecked)
                                         │
        ┌────────────────────────────────┼────────────────────────────────┐
        ▼                                ▼                                ▼
BadSqlGrammarException      DataAccessResourceFailureException   OptimisticLockingFailureException
  (Syntax Error)                 (DB Connection Down)              (Concurrent Write Clash)
```

### Key Features of Spring's Exception Translation:
*   **Unchecked Runtime Exceptions:** `DataAccessException` extends `RuntimeException`. You do not have to write boilerplate `try-catch` blocks for errors you cannot recover from (like database connection loss or syntax errors).
*   **No Information Loss:** Spring wraps the original low-level exception, so you can still access the raw driver/vendor codes if you need them.
*   **Consistent Model:** Allows you to swap persistence frameworks under the hood (e.g., migrating from JDBC to JPA) without changing exception handling logic in your service layers.

---

## 2. The Role of the `@Repository` Annotation

To guarantee that your DAO or repository classes perform this automatic exception translation, you must annotate them with **`@Repository`**.

### Functions of `@Repository`:
1.  **Stereotype Identification:** Identifies the class as a data access component during classpath scanning (component scanning).
2.  **Exception Translation Advice:** Spring automatically applies a Post-Processor (`PersistenceExceptionTranslationPostProcessor`) that wraps the `@Repository` class in an AOP proxy to intercept raw database exceptions and translate them into Spring's `DataAccessException` hierarchy.

```java
@Repository // Enables component scanning and exception translation proxies
public class JpaMovieFinder implements MovieFinder {
    // ...
}
```

---

## 3. Injecting Persistence Resources (Examples)

Depending on the technology stack, you inject the appropriate resource into your `@Repository` bean using dependency injection.

### A. JPA Repository (`@PersistenceContext`)
For JPA, you inject the `EntityManager`. The standard annotation for this is **`@PersistenceContext`** (which injects a thread-safe proxy to the actual transaction-bound `EntityManager`).

```java
@Repository
public class JpaMovieFinder implements MovieFinder {

    @PersistenceContext
    private EntityManager entityManager;

    public Movie findById(Long id) {
        return entityManager.find(Movie.class, id);
    }
}
```

### B. Classic Hibernate Repository (`SessionFactory`)
For legacy Hibernate APIs, you inject the `SessionFactory`.

```java
@Repository
public class HibernateMovieFinder implements MovieFinder {

    private SessionFactory sessionFactory;

    @Autowired
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Movie findById(Long id) {
        return sessionFactory.getCurrentSession().get(Movie.class, id);
    }
}
```

### C. Plain JDBC Repository (`DataSource` & `JdbcTemplate`)
For raw JDBC, you inject the `DataSource` and use it to instantiate a `JdbcTemplate` (which handles resource opening and closing).

```java
@Repository
public class JdbcMovieFinder implements MovieFinder {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void init(DataSource dataSource) {
        // Instantiate JdbcTemplate programmatically using the injected DataSource
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public Movie findById(Long id) {
        return jdbcTemplate.queryForObject(
            "SELECT * FROM movies WHERE id = ?", 
            new BeanPropertyRowMapper<>(Movie.class), 
            id
        );
    }
}
```
