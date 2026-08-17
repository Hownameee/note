public class main {
    static public void main(String[] args) {
        String str = "Hello, xin chào Việt Nam! 🇻🇳";

        System.out.println("String: " + str);
        System.out.println("UFT-16 Length: " + str.length());
        // Code-point count (actual number of Unicode characters)
        System.out.println("Code point count: " + str.codePointCount(0, str.length()));

        // Code-point breaks down
        str.codePoints().forEach(cp -> {
            String hex = String.format("U+%04X", cp);
            String character = Character.toString(cp);
            System.out.printf("%-10s (decimal: %-7d) -> '%s'%n", hex, cp, character);
        });
    }
}
