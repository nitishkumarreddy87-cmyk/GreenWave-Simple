import java.util.*;

/** The whole city: junctions, emergency dispatch, log and settings. */
class City {
    static final double[] XS = {190, 450, 710}, YS = {120, 260, 400};
    private final List<Junction> junctions = new ArrayList<>();               // Collections
    private final Map<String, Junction> byName = new HashMap<>();
    private final PriorityQueue<Vehicle> bay = new PriorityQueue<>();         // most urgent first
    private final List<Vehicle> active = new ArrayList<>();
    private final Set<String> seen = new HashSet<>();
    private final LinkedList<String> view = new LinkedList<>();
    private final List<String> csv = new ArrayList<>();
    private volatile boolean paused;
    private volatile double speed = 1, rate = 0.03;
    private volatile String mode = "adaptive";
    private double clock, release;
    private int saved, resets;
    private final Random rnd = new Random();

    City() {
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++) {
                Junction j = new Junction("J" + (r * 3 + c + 1), r, c, this);
                junctions.add(j); byName.put(j.getName(), j);
                Thread t = new Thread(j, "signal-" + j.getName()); t.setDaemon(true); t.start();
            }
        Thread e = new Thread(this::emergencyLoop, "emergency-thread"); e.setDaemon(true); e.start();
        log("System ready. Dispatch a vehicle to begin.");
    }

    boolean isPaused() { return paused; }
    double getSpeed() { return speed; }
    String getMode() { return mode; }

    // ---------- commands used by the buttons ----------
    synchronized void dispatch(String type, String route) throws RouteNotFoundException {
        if (route == null || route.equals("rand")) route = (rnd.nextBoolean() ? "h" : "v") + rnd.nextInt(3);
        if (!route.matches("[hv][0-2]")) throw new RouteNotFoundException("No such route: " + route);
        Vehicle v;
        try { v = Vehicle.create(String.valueOf(type), route.charAt(0), route.charAt(1) - '0'); }
        catch (IllegalArgumentException ex) { throw new RouteNotFoundException(ex.getMessage()); }
        bay.add(v);
        log(v.alertText() + " dispatched on " + (v.getAxis() == 'h' ? "Road " : "Avenue ") + (v.getLine() + 1));
    }
    synchronized void togglePause() { paused = !paused; }
    synchronized void setSpeed(double s) { speed = Math.max(0.25, Math.min(8, s)); }
    synchronized void setRate(double r) {
        rate = Math.max(0, Math.min(0.2, r));
        log("Traffic density changed to " + (r < .02 ? "Low" : r < .04 ? "Medium" : r < .06 ? "High" : "Rush hour"));
    }
    synchronized void setMode(String m) throws SignalFailureException {
        if (!m.matches("adaptive|fixed|night")) throw new SignalFailureException("Unknown signal mode: " + m);
        mode = m; log("Signal mode: " + (m.equals("night") ? "Flashing" : m.equals("fixed") ? "Fixed timer" : "Adaptive"));
    }
    synchronized void setManual(String name, String k) throws SignalFailureException {
        Junction j = byName.get(name);
        if (j == null) throw new SignalFailureException("No such junction: " + name);
        j.setManual(k);
        log(name + " set to " + (k.equals("auto") ? "Auto" : k.equals("h") ? "East-West green" : k.equals("v") ? "North-South green" : "Fault"));
    }
    synchronized void reset() {
        bay.clear(); active.clear(); seen.clear(); view.clear(); csv.clear();
        junctions.forEach(Junction::reset); saved = 0; clock = 0; resets++;
        log("City reset");
    }
    synchronized void clearLog() { view.clear(); csv.clear(); }
    synchronized void log(String m) {
        String t = String.format("%02d:%02d", (int) clock / 60, (int) clock % 60);
        view.addFirst("[" + t + "] " + m); if (view.size() > 60) view.removeLast();
        csv.add(t + ",\"" + m + "\"");
    }
    synchronized String csv() { return "time,event\n" + String.join("\n", csv) + "\n"; }

    // ---------- emergency thread ----------
    private void emergencyLoop() {
        while (true) {
            try { Thread.sleep(16); } catch (InterruptedException e) { return; }
            if (paused) continue;
            synchronized (this) {
                double dt = 0.016 * speed; clock += dt; release += dt;
                if (release > 0.3 && !bay.isEmpty()) { active.add(bay.poll()); release = 0; }
                tick(dt);
            }
        }
    }
    private Junction at(Vehicle v, int k) { return v.getAxis() == 'h' ? junctions.get(v.getLine() * 3 + k) : junctions.get(k * 3 + v.getLine()); }
    private void tick(double dt) {
        Map<Junction, Vehicle> best = new HashMap<>();
        for (Vehicle v : active) {
            v.advance(dt);
            double[] pts = v.getAxis() == 'h' ? XS : YS;
            for (int k = 0; k < 3; k++) {
                double d = pts[k] - v.getPos();
                if (d > -30 && d < 230) {
                    Junction j = at(v, k); Vehicle b = best.get(j);
                    if (b == null || v.getPriority() > b.getPriority()) best.put(j, v);   // polymorphism decides who wins
                }
            }
        }
        for (Junction j : junctions) {
            Vehicle b = best.get(j);
            j.force(b == null ? null : b.getAxis());
            if (b != null && seen.add(j.getName() + b.getId())) { saved += 9; log(j.getName() + " turned green for " + b.alertText()); }
        }
        for (Iterator<Vehicle> it = active.iterator(); it.hasNext(); ) {
            Vehicle v = it.next();
            if (v.getPos() > (v.getAxis() == 'h' ? 900 : 520) + 40) { log(v.alertText() + " reached its destination"); it.remove(); }
        }
    }

    // ---------- read-only views used by the Swing screens ----------
    List<Junction> getJunctions() { return junctions; }
    synchronized List<Vehicle> getActive() { return new ArrayList<>(active); }
    synchronized List<String> getLogView() { return new ArrayList<>(view); }
    synchronized int getSaved() { return saved; }
    synchronized double getClock() { return clock; }
    double getRate() { return rate; }
    int getResets() { return resets; }
}
