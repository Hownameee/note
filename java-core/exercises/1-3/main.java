public class main {
    static public void main(String[] args) {
        // fizzbuzz problem: n % 3 == 0 -> fizz, n % 5 == 0 -> buzz, both -> fizzbuzz

        int n = 100;
        for (int i = 0; i < n; i++) {

            String str = switch (i % 15) {
                case 0 -> "FizzBuzz";
                case 3, 6, 9, 12 -> "Fizz";
                case 5, 10 -> "Buzz";
                default -> "";
            };

            System.out.println(i + " -> " + str);
        }
    }
}
