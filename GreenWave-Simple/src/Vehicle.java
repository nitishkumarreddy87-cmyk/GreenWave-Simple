/** Interface: anything that can describe itself in an alert. */
interface Alertable { String alertText(); }

/** Abstract class: the common part of every emergency vehicle. */
abstract class Vehicle implements Alertable, Comparable<Vehicle> {
    private static int counter = 100;
    private final int id;
    private final char axis;     // 'h' = road (moves east), 'v' = avenue (moves south)
    private final int line;      // which road or avenue: 0, 1 or 2
    private double pos = -30;

    Vehicle(char axis, int line) {
        this.id = ++counter;
        this.axis = axis;
        this.line = line;
    }

    abstract int getPriority();     // every subclass answers differently
    abstract String getType();
    abstract String prefix();

    void advance(double dt) { pos += 186 * dt; }
    int getId() { return id; }
    char getAxis() { return axis; }
    int getLine() { return line; }
    double getPos() { return pos; }
    String getTag() { return prefix() + "-" + id; }

    /** The vehicle with the higher priority comes first in the PriorityQueue. */
    public int compareTo(Vehicle other) { return Integer.compare(other.getPriority(), getPriority()); }
    public String alertText() { return getType() + " " + getTag(); }
    @Override public String toString() { return alertText(); }    // from the Object class

    static Vehicle create(String type, char axis, int line) {
        if (type.equals("Ambulance")) return new Ambulance(axis, line);
        if (type.equals("Fire truck")) return new FireTruck(axis, line);
        if (type.equals("Police car")) return new PoliceCar(axis, line);
        throw new IllegalArgumentException("Unknown vehicle type: " + type);
    }
}

class Ambulance extends Vehicle {
    Ambulance(char axis, int line) { super(axis, line); }
    int getPriority() { return 3; }
    String getType() { return "Ambulance"; }
    String prefix() { return "A"; }
}
class FireTruck extends Vehicle {
    FireTruck(char axis, int line) { super(axis, line); }
    int getPriority() { return 2; }
    String getType() { return "Fire truck"; }
    String prefix() { return "F"; }
}
class PoliceCar extends Vehicle {
    PoliceCar(char axis, int line) { super(axis, line); }
    int getPriority() { return 1; }
    String getType() { return "Police car"; }
    String prefix() { return "P"; }
}
