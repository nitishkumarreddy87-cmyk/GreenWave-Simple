import java.util.*;

/** The ordinary cars. They stop at red and yellow signals and keep a gap from the car ahead. */
class TrafficSim {
    static class Car { char axis; int line; double pos, sp; int ci; boolean waiting; }
    final List<Car> cars = new ArrayList<>();
    final List<Integer> hist = new ArrayList<>();
    private final City city;
    private final Random rnd = new Random();
    private int frame;

    TrafficSim(City city) { this.city = city; }
    void clear() { cars.clear(); hist.clear(); }
    int waiting() { int n = 0; for (Car c : cars) if (c.waiting) n++; return n; }

    private Junction at(char axis, int line, int k) { return city.getJunctions().get(axis == 'h' ? line * 3 + k : k * 3 + line); }

    void step() {
        frame++;
        if (frame % 120 == 0) adapt();
        if (frame % 60 == 0) { hist.add(cars.size() + city.getActive().size()); if (hist.size() > 90) hist.remove(0); }
        if (rnd.nextDouble() < city.getRate()) spawn();
        for (Car c : cars) {
            boolean go = true;
            double[] pts = c.axis == 'h' ? City.XS : City.YS;
            for (int k = 0; k < 3; k++) {
                double d = pts[k] - 26 - c.pos;
                String s = at(c.axis, c.line, k).state(c.axis);
                if (d >= -2 && d < c.sp + 6 && (s.equals("r") || s.equals("y"))) go = false;
            }
            for (Car o : cars) if (o != c && o.axis == c.axis && o.line == c.line && o.pos > c.pos && o.pos - c.pos < 28) go = false;
            c.waiting = !go;
            if (go) c.pos += c.sp;
        }
        cars.removeIf(c -> c.pos > (c.axis == 'h' ? 900 : 520) + 30);
    }

    private void spawn() {
        char axis = rnd.nextBoolean() ? 'h' : 'v'; int line = rnd.nextInt(3);
        for (Car c : cars) if (c.axis == axis && c.line == line && c.pos < 50) return;
        Car c = new Car(); c.axis = axis; c.line = line; c.pos = -20; c.sp = 1.4 + rnd.nextDouble() * 0.8; c.ci = rnd.nextInt(4);
        cars.add(c);
    }

    /** Tell each junction how many cars wait in each direction, so adaptive mode can favour the busier road. */
    private void adapt() {
        for (int i = 0; i < 9; i++) {
            int r = i / 3, col = i % 3, h = 0, v = 0;
            for (Car c : cars) {
                if (!c.waiting) continue;
                if (c.axis == 'h' && c.line == r && c.pos > City.XS[col] - 170 && c.pos < City.XS[col]) h++;
                if (c.axis == 'v' && c.line == col && c.pos > City.YS[r] - 170 && c.pos < City.YS[r]) v++;
            }
            city.getJunctions().get(i).setShare(h, v);
        }
    }
}
