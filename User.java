import java.util.Set;

/** Abstract user. Each role decides what it may do (Abstraction, Polymorphism). */
abstract class User {
    private final String name;
    User(String name) { this.name = name; }
    String getName() { return name; }
    abstract String role();
    abstract boolean can(String action);
}
class Admin extends User {
    Admin(String n) { super(n); }
    String role() { return "admin"; }
    boolean can(String a) { return true; }
}
class Operator extends User {
    Operator(String n) { super(n); }
    String role() { return "operator"; }
    boolean can(String a) { return !a.equals("reset"); }
}
class Driver extends User {
    Driver(String n) { super(n); }
    String role() { return "driver"; }
    boolean can(String a) { return Set.of("dispatch", "note").contains(a); }
}
class Viewer extends User {
    Viewer(String n) { super(n); }
    String role() { return "viewer"; }
    boolean can(String a) { return false; }
}
