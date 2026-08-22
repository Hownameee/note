import java.util.*;

/**
 * Exercise 26: Optional boundary
 *
 * Task:
 * Model repository lookup returning Optional<T>.
 * Compare orElse, orElseGet, map, flatMap, and orElseThrow with observable suppliers.
 *
 * Requirements:
 * 1. Part 1: Observe side-effects of orElse (EAGER) vs orElseGet (LAZY) when entity EXISTS.
 * 2. Part 2: Observe side-effects when entity is MISSING.
 * 3. Part 3: Use monadic map & flatMap to navigate User -> Optional<Address> -> city safely.
 * 4. Part 4: Use orElseThrow to handle missing entity at system boundary.
 */
public class main {

    public record Address(String street, String city) {}

    public record User(String id, String name, Optional<Address> address) {}

    // Simulated Repository
    public static class UserRepository {
        private final Map<String, User> db = new HashMap<>();

        public void save(User user) {
            db.put(user.id(), user);
        }

        public Optional<User> findById(String id) {
            return Optional.ofNullable(db.get(id));
        }
    }

    // Observable supplier to demonstrate eager vs lazy evaluation
    public static User createDefaultUserObservable(String callerTag) {
        System.out.println("  -> [SIDE-EFFECT] createDefaultUserObservable() EXECUTED by: " + callerTag);
        return new User("GUEST-0", "Guest", Optional.empty());
    }

    public static void main(String[] args) {
        UserRepository repo = new UserRepository();
        repo.save(new User("USR-1", "Alice", Optional.of(new Address("123 Main St", "Hanoi"))));
        repo.save(new User("USR-2", "Bob", Optional.empty())); // User without address

        System.out.println("==================================================");
        System.out.println("1. orElse vs orElseGet when USER EXISTS (USR-1)");
        System.out.println("==================================================");
        User u1 = repo.findById("USR-1").orElse(createDefaultUserObservable("1"));
        User u2 = repo.findById("USR-1").orElseGet(() -> createDefaultUserObservable("2"));

        System.out.println("\n==================================================");
        System.out.println("2. orElse vs orElseGet when USER IS MISSING (USR-99)");
        System.out.println("==================================================");
        User u3 = repo.findById("USR-99").orElse(createDefaultUserObservable("1"));
        User u4 = repo.findById("USR-99").orElseGet(() -> createDefaultUserObservable("2"));

        System.out.println("\n==================================================");
        System.out.println("3. Monadic Transformations: map & flatMap");
        System.out.println("==================================================");
        List.of("USR-1", "USR-2", "USR-99").forEach(id -> {
            String city = repo.findById(id)
                    .flatMap(User::address)
                    .map(Address::city)
                    .orElse("NO_CITY_AVAILABLE");
            System.out.printf("User %-6s City: %s%n", id, city);
        });

        System.out.println("\n==================================================");
        System.out.println("4. orElseThrow for strict boundaries");
        System.out.println("==================================================");
        try {
            User u99 = repo.findById("USR-99")
                    .orElseThrow(() -> new NoSuchElementException("Not Found"));
        } catch (NoSuchElementException e) {
            System.out.println(e);
        }
    }
}
