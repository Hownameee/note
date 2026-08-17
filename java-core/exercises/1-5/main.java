class Person {
    String name;

    Person(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Person{name='" + name + "'}";
    }
}

public class main {

    // 1. Change a primitive parameter
    static void changePrimitive(int x) {
        x = 99; // Modifies local copy on stack
    }

    // 2. Mutate an object's internal state via the reference
    static void mutateObject(Person p) {
        p.name = "Bob"; // Modifies the object on the heap via copied reference
    }

    // 3. Reassign the object parameter reference
    static void reassignParameter(Person p) {
        p = new Person("Charlie"); // Only rebinds local parameter p to a new object
    }

    // 4. Return replacement (idiomatic way to "replace" an object or primitive)
    static Person returnReplacement(Person p) {
        return new Person("David");
    }

    public static void main(String[] args) {
        System.out.println("=== 1. Primitive Parameter ===");
        int num = 10;
        System.out.println("Before changePrimitive: " + num);
        changePrimitive(num);
        System.out.println("After changePrimitive:  " + num); // Still 10

        System.out.println("\n=== 2. Mutate Object ===");
        Person person1 = new Person("Alice");
        System.out.println("Before mutateObject: " + person1);
        mutateObject(person1);
        System.out.println("After mutateObject:  " + person1); // Name changed to Bob

        System.out.println("\n=== 3. Reassign Parameter ===");
        Person person2 = new Person("Alice");
        System.out.println("Before reassignParameter: " + person2);
        reassignParameter(person2);
        System.out.println("After reassignParameter:  " + person2); // Still Alice!

        System.out.println("\n=== 4. Return Replacement ===");
        Person person3 = new Person("Alice");
        System.out.println("Before returnReplacement: " + person3);
        person3 = returnReplacement(person3);
        System.out.println("After returnReplacement:  " + person3); // Replaced with David
    }
}
