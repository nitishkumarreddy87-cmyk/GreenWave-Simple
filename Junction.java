/** Interface for anything that controls a signal (Interfaces). */
interface SignalController {
    String state(char axis);                         // "g", "y", "r" or "f" (flashing)
    void setManual(String k) throws SignalFailureException;
}

/** One junction. It owns its timer and runs on its own thread (Multithreading). */
class Junction implements SignalController, Runnable {
    private final String name;
    private final double offset;
    private final City city;
    private volatile double clock;
    private volatile double share = 0.5;      // share of the cycle given to East-West
    private volatile String manual;           // null, "h", "v" or "fault"  (Encapsulation)
    private volatile Character forced;        // set by an emergency vehicle

    Junction(String name, int row, int col, City city) {
        this.name = name; this.offset = (row + col) * 2.2; this.city = city;
    }

    public void run() {
        while (true) {
            try { Thread.sleep(20); } catch (InterruptedException e) { return; }
            if (!city.isPaused()) clock += 0.02 * city.getSpeed();
        }
    }

    public String state(char axis) {
        Character f = forced;
        if (f != null) return f.charValue() == axis ? "g" : "r";
        String m = manual;
        if ("fault".equals(m) || "night".equals(city.getMode())) return "f";
        if (m != null) return m.charAt(0) == axis ? "g" : "r";
        double p = (clock + offset) % 14;
        double len = "adaptive".equals(city.getMode()) ? 14 * share : 7;
        if (axis == 'h') return p < len - 1 ? "g" : p < len ? "y" : "r";
        return p < len ? "r" : p < 13 ? "g" : "y";
    }

    public void setManual(String k) throws SignalFailureException {
        if (k == null || !k.matches("auto|h|v|fault"))
            throw new SignalFailureException("Invalid override '" + k + "' for " + name);
        manual = k.equals("auto") ? null : k;
    }

    void force(Character axis) { forced = axis; }
    void setShare(int waitingH, int waitingV) {
        share = Math.min(0.7, Math.max(0.3, (waitingH + 1.0) / (waitingH + waitingV + 2)));
    }
    void reset() { manual = null; forced = null; clock = 0; share = 0.5; }
    boolean isFault() { return "fault".equals(manual); }
    String marker() { return manual == null ? "-" : isFault() ? "f" : "m"; }
    String getName() { return name; }
}
