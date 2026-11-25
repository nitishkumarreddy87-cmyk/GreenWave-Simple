/** Interface for anything that controls a signal. */
interface SignalController {
    Light state(char axis);
    void setManual(String choice) throws SignalFailureException;
}

/** One junction. It runs on its own thread and keeps its signal timer. */
class Junction implements SignalController, Runnable {
    private final String name;
    private final double offset;      // so that the junctions do not all change together
    private final City city;
    private double clock;             // private: only the methods below can change it
    private String manual;            // null, "h", "v" or "fault"
    private Character forced;         // set by an emergency vehicle: 'h', 'v' or null

    Junction(String name, int row, int col, City city) {
        this.name = name;
        this.offset = (row + col) * 2.2;
        this.city = city;
    }

    public void run() {
        while (true) {
            try { Thread.sleep(20); } catch (InterruptedException e) { return; }
            boolean paused = city.isPaused();
            synchronized (this) { if (!paused) clock += 0.02; }
        }
    }

    public Light state(char axis) {
        boolean night = city.isNight();          // read before taking this junction's lock
        synchronized (this) {
            if (forced != null) return forced.charValue() == axis ? Light.GREEN : Light.RED;   // emergency vehicle
            if ("fault".equals(manual) || night) return Light.FLASHING;
            if (manual != null) return manual.charAt(0) == axis ? Light.GREEN : Light.RED;     // manual choice
            double p = (clock + offset) % 14;
            if (axis == 'h') return p < 6 ? Light.GREEN : p < 7 ? Light.YELLOW : Light.RED;
            return p < 7 ? Light.RED : p < 13 ? Light.GREEN : Light.YELLOW;
        }
    }

    public synchronized void setManual(String choice) throws SignalFailureException {
        if (!choice.equals("auto") && !choice.equals("h") && !choice.equals("v") && !choice.equals("fault"))
            throw new SignalFailureException("Invalid choice '" + choice + "' for " + name);
        manual = choice.equals("auto") ? null : choice;
    }

    synchronized void force(Character axis) { forced = axis; }
    synchronized void reset() { manual = null; forced = null; clock = 0; }
    synchronized boolean isFault() { return "fault".equals(manual); }
    synchronized String control() { return manual == null ? "Auto" : isFault() ? "Fault" : "Manual"; }
    String getName() { return name; }
}
