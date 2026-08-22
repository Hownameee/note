import java.util.*;
import java.util.stream.Collectors;

/**
 * Exercise 24: Nested data
 *
 * Task:
 * Use flatMap to turn nested departments and employees into a unique sorted skill list.
 *
 * Requirements:
 * 1. Flatten departments -> employees -> skills.
 * 2. Deduplicate skills (distinct).
 * 3. Sort skills in alphabetical order.
 * 4. Return as an unmodifiable or standard List<String>.
 */
public class main {

    public record Employee(String name, List<String> skills) {}

    public record Department(String name, List<Employee> employees) {}

    // Extract unique, alphabetically sorted skills from all employees across all departments
    public static List<String> extractUniqueSortedSkills(List<Department> departments) {
        return departments.stream()
                .flatMap(dept -> dept.employees().stream())
                .flatMap(emp -> emp.skills().stream())
                .distinct()
                .sorted()
                .toList();
    }

    public static void main(String[] args) {
        List<Department> departments = List.of(
                new Department("Engineering", List.of(
                        new Employee("Alice", List.of("Java", "Docker", "Kubernetes")),
                        new Employee("Bob", List.of("Python", "Java", "AWS"))
                )),
                new Department("Data Science", List.of(
                        new Employee("Charlie", List.of("Python", "SQL", "TensorFlow")),
                        new Employee("David", List.of("R", "SQL", "Tableau"))
                )),
                new Department("Platform", List.of(
                        new Employee("Eve", List.of("Go", "Kubernetes", "Docker", "Terraform"))
                ))
        );

        System.out.println("=== Unique Sorted Skills ===");
        List<String> skills = extractUniqueSortedSkills(departments);
        skills.forEach(skill -> System.out.println(" - " + skill));

        System.out.println("\nTotal unique skills count: " + skills.size());
    }
}
