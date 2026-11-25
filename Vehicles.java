/** Interface: anything that can raise an alert (Interfaces). */
interface Alertable { String alertText(); }

/** Abstract base class (Abstraction, Encapsulation). */
abstract class Vehicle implements Alertable, Comparable<Vehicle> {
    private static int counter = 100;
    private final int id;
    private final String tag;
    private final char axis;   // 'h' = road (east), 'v' = avenue (south)
    private final int line;    // which road or avenue, 0 to 2
    private double pos = -30;

    Vehicle(char axis, int line) {
        this.id = ++counter;
        this.axis = axis;
        this.line = line;
        this.tag = prefix() + "-" + id;
    }

    abstract int getPriority();   // polymorphism: each type answers differently
    abstract String getType();
    abstract String prefix();

    void advance(double dt) { pos += 186 * dt; }
    int getId() { return id; }
    String getTag() { return tag; }
    char getAxis() { return axis; }
    int getLine() { return line; }
    double getPos() { return pos; }

    /** Higher priority first, so a PriorityQueue serves the most urgent vehicle. */
    public int compareTo(Vehicle o) { return Integer.compare(o.getPriority(), getPriority()); }
    public String alertText() { return getType() + " " + tag; }

    /** Factory method. */
    static Vehicle create(String type, char axis, int line) {
        switch (type) {
            case "Ambulance": return new Ambulance(axis, line);
            case "Fire truck": return new FireTruck(axis, line);
            case "Police car": return new PoliceCar(axis, line);
            default: throw new IllegalArgumentException("Unknown vehicle type: " + type);
        }
    }
}

/** Inheritance: three kinds of emergency vehicle. */
class Ambulance extends Vehicle {
    Ambulance(char a, int l) { super(a, l); }
    int getPriority() { return 3; }
    String getType() { return "Ambulance"; }
    String prefix() { return "A"; }
}
class FireTruck extends Vehicle {
    FireTruck(char a, int l) { super(a, l); }
    int getPriority() { return 2; }
    String getType() { return "Fire truck"; }
    String prefix() { return "F"; }
}
class PoliceCar extends Vehicle {
    PoliceCar(char a, int l) { super(a, l); }
    int getPriority() { return 1; }
    String getType() { return "Police car"; }
    String prefix() { return "P"; }
}
