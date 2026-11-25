/** Abstract class: each role decides which actions it may do. */
abstract class User {
    private final String name;
    User(String name) { this.name = name; }
    String getName() { return name; }
    abstract String role();
    abstract boolean can(String action);
}
class Admin extends User {
    Admin(String name) { super(name); }
    String role() { return "Admin"; }
    boolean can(String action) { return true; }
}
class Operator extends User {
    Operator(String name) { super(name); }
    String role() { return "Operator"; }
    boolean can(String action) { return !action.equals("reset"); }
}
class Driver extends User {
    Driver(String name) { super(name); }
    String role() { return "Emergency driver"; }
    boolean can(String action) { return action.equals("dispatch"); }
}
class Viewer extends User {
    Viewer(String name) { super(name); }
    String role() { return "Public viewer"; }
    boolean can(String action) { return false; }
}
