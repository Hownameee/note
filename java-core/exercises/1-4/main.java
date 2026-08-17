import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class main {
    public static void main(String[] args) {
        ArrayList<Integer> arr = new ArrayList<>(List.of(1, 3, 7, 10));
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        int sum = 0;

        for (int ele : arr) {
            if (ele > max) max = ele;
            if (ele < min) min = ele;
            sum += ele;
        }

        Collections.sort(arr);
        int n = arr.size();
        double median;
        if (n % 2 != 0) {
            median = arr.get(n / 2);
        } else {
            median = (arr.get((n / 2) - 1) + arr.get(n / 2)) / 2.0;
        }
        System.out.println("MAX: " + max);
        System.out.println("MIN: " + min);
        System.out.println("MEAN: " + (float) sum / n);
        System.out.println("MEDIAN: " + median);
    }
}