import java.util.*;
import java.util.stream.Collectors;

/**
 * Exercise 23: Loop versus stream
 *
 * Goal:
 * Implement the same transaction summary imperatively and with streams.
 * Compare clarity, reasoning overhead, and maintainability.
 */
public class main {

    public record Transaction(String id, String category, double amount, boolean completed) {}

    public record Summary(int count, double totalAmount, List<String> transactionIds) {}

    // 1. Imperative Loop Approach
    public static Summary summarizeImperative(List<Transaction> transactions, String targetCategory) {
        int count = 0;
        double total = 0.0;
        List<String> ids = new ArrayList<>();

        for (Transaction t : transactions) {
            if (t.completed() && t.category().equalsIgnoreCase(targetCategory)) {
                count++;
                total += t.amount();
                ids.add(t.id());
            }
        }

        return new Summary(count, total, ids);
    }

    // 2. Stream Approach
    public static Summary summarizeStream(List<Transaction> transactions, String targetCategory) {
        List<Transaction> filtered = transactions.stream()
                .filter(Transaction::completed)
                .filter(t -> t.category().equalsIgnoreCase(targetCategory))
                .toList();

        int count = filtered.size();
        double total = filtered.stream()
                .mapToDouble(Transaction::amount)
                .sum();
        List<String> ids = filtered.stream()
                .map(Transaction::id)
                .toList();

        return new Summary(count, total, ids);
    }

    public static void main(String[] args) {
        List<Transaction> transactions = List.of(
                new Transaction("TX-101", "ELECTRONICS", 299.99, true),
                new Transaction("TX-102", "GROCERY", 45.50, true),
                new Transaction("TX-103", "ELECTRONICS", 120.00, false), // cancelled
                new Transaction("TX-104", "ELECTRONICS", 850.00, true),
                new Transaction("TX-105", "BOOKS", 30.00, true)
        );

        String category = "ELECTRONICS";

        System.out.println("=== 1. Imperative Summary ===");
        Summary imperativeSummary = summarizeImperative(transactions, category);
        System.out.println(imperativeSummary);

        System.out.println("\n=== 2. Stream Summary ===");
        Summary streamSummary = summarizeStream(transactions, category);
        System.out.println(streamSummary);

        // Verification
        boolean match = imperativeSummary.equals(streamSummary);
        System.out.println("\nResults match: " + match);
    }
}
