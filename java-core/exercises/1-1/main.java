public class main {
    static public void main(String[] args) {
        int max = Integer.MAX_VALUE;
        int min = Integer.MIN_VALUE;

        System.out.println("Max: " + max);
        System.out.println("Min: " + min);

        int overflow = max + 1; // cycle -> this will be back to min
        System.out.println("Overflow: " + overflow);

        long narrowing = (long) Integer.MAX_VALUE + 1;
        System.out.println("long type: " + narrowing);
        System.out.println("int type: " + (int) narrowing); // lost info

        int division = 5;
        System.out.println("Number: " + division);
        System.out.println("Division 2: " + division / 2);

        try {
            int math = Math.addExact(Integer.MAX_VALUE, 1); // throw unchecked exception
        } catch (ArithmeticException e) {
            System.out.println("Exception: " + e);
        }
    }
}
