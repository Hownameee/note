import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Exercise 25: Collector practice
 *
 * Task:
 * Group orders by customer, then compute order count and total money spent.
 * Define a safe money representation using BigDecimal.
 *
 * Requirements:
 * 1. Use BigDecimal to prevent floating-point rounding issues.
 * 2. Group orders by customer name.
 * 3. Accumulate total spent (Money) and order count per customer.
 * 4. Return Map<String, CustomerSummary>.
 */
public class main {

    // Safe money representation to avoid IEEE-754 floating-point inaccuracies
    public record Money(BigDecimal amount) {
        public static Money of(String val) {
            return new Money(new BigDecimal(val));
        }

        public Money add(Money other) {
            return new Money(this.amount.add(other.amount));
        }

        public static Money ZERO = new Money(BigDecimal.ZERO);

        @Override
        public String toString() {
            return "$" + amount.toPlainString();
        }
    }

    public record Order(String orderId, String customer, Money amount) {}

    public record CustomerSummary(long orderCount, Money totalSpent) {}

    public static Map<String, CustomerSummary> summarizeOrdersByCustomer(List<Order> orders) {
        return orders.stream()
                .collect(Collectors.toMap(
                        Order::customer,
                        order -> new CustomerSummary(1, order.amount()),
                        (s1, s2) -> new CustomerSummary(
                                s1.orderCount() + s2.orderCount(),
                                s1.totalSpent().add(s2.totalSpent())
                        )
                ));
    }

    public static void main(String[] args) {
        List<Order> orders = List.of(
                new Order("ORD-001", "Alice", Money.of("150.50")),
                new Order("ORD-002", "Bob", Money.of("99.99")),
                new Order("ORD-003", "Alice", Money.of("49.50")),
                new Order("ORD-004", "Charlie", Money.of("500.00")),
                new Order("ORD-005", "Bob", Money.of("200.01")),
                new Order("ORD-006", "Alice", Money.of("100.00"))
        );

        System.out.println("=== Customer Order Summaries ===");
        Map<String, CustomerSummary> summaries = summarizeOrdersByCustomer(orders);

        summaries.forEach((customer, summary) -> {
            System.out.printf("Customer: %-10s | Orders: %d | Total: %s%n",
                    customer, summary.orderCount(), summary.totalSpent());
        });
    }
}
