import java.io.*;
import java.util.*;

/** The whole city: junctions, emergency vehicles, settings and the event log. */
class City {
    static final double[] XS = {190, 450, 710}, YS = {120, 260, 400};   // positions of the avenues and roads

    private final List<Junction> junctions = new ArrayList<>();
    private final Map<String, Junction> byName = new HashMap<>();
    private final PriorityQueue<Vehicle> bay = new PriorityQueue<>();    // most urgent vehicle first
    private final List<Vehicle> active = new ArrayList<>();
    private final Set<String> seen = new TreeSet<>();                    // junctions already cleared
    private final Map<String, Integer> counts = new TreeMap<>();         // vehicles dispatched per type
    private final LinkedList<String> events = new LinkedList<>();        // newest first
    private boolean paused, night;
    private double clock, release;
    private int saved;
    private final Random random = new Random();

    City() {
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++) {
                Junction j = new Junction("J" + (r * 3 + c + 1), r, c, this);
                junctions.add(j);
                byName.put(j.getName(), j);
                Thread t = new Thread(j);          // one thread for every junction
                t.setDaemon(true);
                t.start();
            }
        Thread emergency = new Thread(() -> emergencyLoop());
        emergency.setDaemon(true);
        emergency.setPriority(Thread.MAX_PRIORITY);
        emergency.start();
        log("System ready");
    }

    // ---- read-only views ----
    List<Junction> getJunctions() { return junctions; }
    synchronized boolean isPaused() { return paused; }
    synchronized boolean isNight() { return night; }
    synchronized int getSaved() { return saved; }
    synchronized List<Vehicle> getActive() { return new ArrayList<>(active); }
    synchronized List<String> getEvents() { return new ArrayList<>(events); }
    synchronized String getCounts() {
        if (counts.isEmpty()) return "none";
        String s = "";
        for (Map.Entry<String, Integer> e : counts.entrySet()) s += e.getKey() + " " + e.getValue() + "   ";
        return s.trim();
    }

    // ---- commands used by the buttons ----
    void dispatch(String type) throws RouteNotFoundException { dispatch(type, "random"); }    // overloaded method

    synchronized void dispatch(String type, String route) throws RouteNotFoundException {
        if (route.equals("random")) route = (random.nextBoolean() ? "h" : "v") + random.nextInt(3);
        if (!route.matches("[hv][0-2]")) throw new RouteNotFoundException("No such route: " + route);
        Vehicle v;
        try {
            v = Vehicle.create(type, route.charAt(0), route.charAt(1) - '0');
        } catch (IllegalArgumentException ex) {
            throw new RouteNotFoundException(ex.getMessage());
        }
        bay.add(v);
        counts.put(type, counts.getOrDefault(type, 0) + 1);       // autoboxing of the int
        log(v + " dispatched on " + (v.getAxis() == 'h' ? "Road " : "Avenue ") + (v.getLine() + 1));
    }

    synchronized void setManual(String name, String choice) throws SignalFailureException {
        Junction j = byName.get(name);
        if (j == null) throw new SignalFailureException("No such junction: " + name);
        j.setManual(choice);
        log(name + " set to " + choice);
    }
    synchronized void togglePause() { paused = !paused; log(paused ? "Simulation paused" : "Simulation resumed"); }
    synchronized void setNight(boolean value) { night = value; log(value ? "Night mode: signals flash" : "Normal mode"); }
    synchronized void reset() {
        bay.clear(); active.clear(); seen.clear(); counts.clear();
        for (Junction j : junctions) j.reset();
        saved = 0; clock = 0;
        log("City reset");
    }
    synchronized void log(String message) {
        events.addFirst(String.format("[%02d:%02d] %s", (int) clock / 60, (int) clock % 60, message));
        if (events.size() > 200) events.removeLast();
    }

    /** Saves the event log to a text file with a character stream. */
    void saveLog(String fileName) throws IOException {
        List<String> lines = getEvents();
        FileWriter writer = null;
        try {
            writer = new FileWriter(fileName);
            for (int i = lines.size() - 1; i >= 0; i--) writer.write(lines.get(i) + "\n");
        } finally {
            if (writer != null) writer.close();
        }
    }

    // ---- the emergency thread ----
    private void emergencyLoop() {
        while (true) {
            try { Thread.sleep(16); } catch (InterruptedException e) { return; }
            if (isPaused()) continue;
            synchronized (this) {
                clock += 0.016;
                release += 0.016;
                if (release > 0.3 && !bay.isEmpty()) { active.add(bay.poll()); release = 0; }   // most urgent first
                tick(0.016);
            }
        }
    }

    private Junction at(Vehicle v, int k) {
        return v.getAxis() == 'h' ? junctions.get(v.getLine() * 3 + k) : junctions.get(k * 3 + v.getLine());
    }

    private void tick(double dt) {
        Map<Junction, Vehicle> best = new HashMap<>();
        for (Vehicle v : active) {
            v.advance(dt);
            double[] points = v.getAxis() == 'h' ? XS : YS;
            for (int k = 0; k < 3; k++) {
                double distance = points[k] - v.getPos();
                if (distance > -30 && distance < 230) {                    // the junction is just ahead
                    Junction j = at(v, k);
                    Vehicle other = best.get(j);
                    if (other == null || v.getPriority() > other.getPriority()) best.put(j, v);   // higher priority wins
                }
            }
        }
        for (Junction j : junctions) {
            Vehicle v = best.get(j);
            j.force(v == null ? null : v.getAxis());
            if (v != null && seen.add(j.getName() + v.getId())) {
                saved += 9;
                log(j.getName() + " turned green for " + v);
            }
        }
        active.removeIf(v -> {                                              // lambda expression
            boolean gone = v.getPos() > (v.getAxis() == 'h' ? 900 : 520) + 40;
            if (gone) log(v + " reached its destination");
            return gone;
        });
    }
}
