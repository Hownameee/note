import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Exercise 27: Parallel-stream trap
 *
 * Task:
 * 1. Part 1: Add shared mutable state inside a parallel pipeline to reproduce race conditions & data corruption.
 * 2. Part 2: Fix the trap by using pure functional reduction / collectors.
 *
 * Requirements:
 * - In testBrokenParallelStream(): Feed 10,000 numbers into a shared, non-thread-safe ArrayList inside parallelStream()
 *   and observe lost updates / crashes over 10 iterations.
 * - In testCorrectParallelStream(): Replace the shared mutable state with a thread-safe collector (e.g. Collectors.toList()).
 */
public class main {

    private static final int ITERATIONS = 10;
    private static final int ELEMENT_COUNT = 10_000;

    public static void testBrokenParallelStream() {
        System.out.println("--- 1. Testing Broken Parallel Stream (Mutating shared ArrayList) ---");
        int failures = 0;

        for (int i = 1; i <= ITERATIONS; i++) {
            List<Integer> unsafeList = new ArrayList<>();

            try {
                IntStream.range(0, ELEMENT_COUNT)
                        .parallel()
                        .forEach(unsafeList::add);

                int actualSize = unsafeList.size();
                if (actualSize != ELEMENT_COUNT) {
                    System.out.printf("Run #%02d: FAILED! Expected %d, but got %d elements (Lost updates)%n",
                            i, ELEMENT_COUNT, actualSize);
                    failures++;
                } else {
                    long nulls = unsafeList.stream().filter(Objects::isNull).count();
                    if (nulls > 0) {
                        System.out.printf("Run #%02d: FAILED! Size is %d but contains %d null elements%n",
                                i, actualSize, nulls);
                        failures++;
                    } else {
                        System.out.printf("Run #%02d: PASSED by chance (Flaky test)%n", i);
                    }
                }
            } catch (Exception e) {
                System.out.printf("Run #%02d: CRASHED with %s%n", i, e.getClass().getSimpleName());
                failures++;
            }
        }

        System.out.printf("Summary: %d / %d runs exhibited data corruption or crashes.%n%n", failures, ITERATIONS);
    }

    public static void testCorrectParallelStream() {
        System.out.println("--- 2. Testing Correct Parallel Stream (Pure Functional Collector) ---");
        int failures = 0;

        for (int i = 1; i <= ITERATIONS; i++) {
            List<Integer> safeList = IntStream.range(0, ELEMENT_COUNT)
                    .parallel()
                    .boxed()
                    .collect(Collectors.toList());

            if (safeList.size() != ELEMENT_COUNT) {
                failures++;
            }
        }

        System.out.printf("Summary: %d / %d runs PASSED flawlessly with 100%% deterministic results.%n",
                ITERATIONS - failures, ITERATIONS);
    }

    public static void main(String[] args) {
        testBrokenParallelStream();
        testCorrectParallelStream();
    }
}
