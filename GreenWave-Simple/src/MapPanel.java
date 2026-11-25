import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/** Interface for the object that wants to know when a junction is clicked. */
interface JunctionListener { void junctionClicked(Junction j, int x, int y); }

/** Draws the city: roads, signals, ordinary cars and emergency vehicles. */
class MapPanel extends JPanel {

    /** An ordinary car (a nested class). */
    static class Car {
        char axis; int line; double pos, speed; boolean waiting;
        Car(char axis, int line, double pos, double speed) { this.axis = axis; this.line = line; this.pos = pos; this.speed = speed; }
    }

    private final City city;
    private final List<Car> cars = new ArrayList<>();
    private JunctionListener listener;
    private Junction selected;

    MapPanel(City city) {
        this.city = city;
        setPreferredSize(new Dimension(900, 520));
        for (int line = 0; line < 3; line++)
            for (int i = 0; i < 3; i++) {
                cars.add(new Car('h', line, i * 300 - 20, 1.6 + 0.3 * i));
                cars.add(new Car('v', line, i * 180 - 20, 1.6 + 0.3 * i));
            }
        addMouseListener(new MouseAdapter() {                      // adapter class, anonymous inner class
            @Override public void mouseClicked(MouseEvent e) {
                selected = null;
                for (int i = 0; i < 9; i++)
                    if (Math.hypot(City.XS[i % 3] - e.getX(), City.YS[i / 3] - e.getY()) < 45) selected = city.getJunctions().get(i);
                if (selected != null && listener != null) listener.junctionClicked(selected, e.getX(), e.getY());
                repaint();
            }
        });
    }

    void setListener(JunctionListener listener) { this.listener = listener; }

    int waitingCount() { int n = 0; for (Car c : cars) if (c.waiting) n++; return n; }
    int carCount() { return cars.size(); }

    /** Moves every car one step. Cars stop at red and yellow signals and keep a gap from the car ahead. */
    void moveCars() {
        if (city.isPaused()) return;
        for (Car c : cars) {
            boolean go = true;
            double[] points = c.axis == 'h' ? City.XS : City.YS;
            for (int k = 0; k < 3; k++) {
                Junction j = c.axis == 'h' ? city.getJunctions().get(c.line * 3 + k) : city.getJunctions().get(k * 3 + c.line);
                double distance = points[k] - 26 - c.pos;
                Light light = j.state(c.axis);
                if (distance >= -2 && distance < c.speed + 6 && (light == Light.RED || light == Light.YELLOW)) go = false;
            }
            for (Car other : cars)
                if (other != c && other.axis == c.axis && other.line == c.line && other.pos > c.pos && other.pos - c.pos < 28) go = false;
            c.waiting = !go;
            if (go) {
                c.pos += c.speed;
                if (c.pos > (c.axis == 'h' ? 900 : 520) + 30) c.pos = -30;
            }
        }
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(new Color(205, 222, 200)); g.fillRect(0, 0, 900, 520);                 // grass
        g.setColor(new Color(70, 70, 70));                                                 // roads
        for (double y : City.YS) g.fillRect(0, (int) y - 24, 900, 48);
        for (double x : City.XS) g.fillRect((int) x - 24, 0, 48, 520);
        g.setColor(Color.WHITE);                                                           // lane marks
        for (int p = 0; p < 900; p += 28) for (double y : City.YS) g.fillRect(p, (int) y - 1, 14, 2);
        for (int p = 0; p < 520; p += 28) for (double x : City.XS) g.fillRect((int) x - 1, p, 2, 14);

        List<Vehicle> vehicles = city.getActive();
        for (Vehicle v : vehicles) {                                                       // green corridor
            g.setColor(new Color(0, 200, 0, 110));
            if (v.getAxis() == 'h') g.fillRect((int) v.getPos(), (int) City.YS[v.getLine()] - 6, 230, 12);
            else g.fillRect((int) City.XS[v.getLine()] - 6, (int) v.getPos(), 12, 230);
        }
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        for (int i = 0; i < 9; i++) {                                                      // signals
            Junction j = city.getJunctions().get(i);
            int x = (int) City.XS[i % 3], y = (int) City.YS[i / 3];
            g.setColor(colour(j.state('h'), blink)); g.fillOval(x - 40, y - 40, 14, 14);
            g.setColor(colour(j.state('v'), blink)); g.fillOval(x + 26, y - 40, 14, 14);
            g.setColor(Color.BLACK); g.drawString(j.getName() + (j.control().equals("Auto") ? "" : " " + j.control()), x + 30, y + 44);
            if (j == selected) { g.setColor(Color.BLUE); g.drawRect(x - 42, y - 42, 84, 84); }
        }
        for (Car c : cars) {                                                               // ordinary cars
            g.setColor(c.axis == 'h' ? new Color(60, 110, 200) : new Color(230, 140, 40));
            if (c.axis == 'h') g.fillRect((int) c.pos - 9, (int) City.YS[c.line] - 16, 18, 10);
            else g.fillRect((int) City.XS[c.line] + 6, (int) c.pos - 9, 10, 18);
        }
        boolean siren = (System.currentTimeMillis() / 160) % 2 == 0;
        for (Vehicle v : vehicles) {                                                       // emergency vehicles
            int x = v.getAxis() == 'h' ? (int) v.getPos() : (int) City.XS[v.getLine()];
            int y = v.getAxis() == 'h' ? (int) City.YS[v.getLine()] : (int) v.getPos();
            g.setColor(Color.WHITE); g.fillRect(x - 13, y - 7, 26, 14);
            g.setColor(Color.BLACK); g.drawRect(x - 13, y - 7, 26, 14);
            g.setColor(siren ? Color.RED : Color.BLUE); g.fillOval(x - 5, y - 5, 10, 10);
        }
        if (!vehicles.isEmpty()) {                                                         // banner
            g.setColor(new Color(200, 40, 40)); g.fillRect(250, 8, 400, 26);
            g.setColor(Color.WHITE);
            g.drawString(vehicles.get(0) + " is on its way: green corridor active", 262, 26);
        }
    }

    private Color colour(Light light, boolean blink) {
        if (light == Light.GREEN) return new Color(0, 200, 0);
        if (light == Light.YELLOW) return new Color(255, 190, 0);
        if (light == Light.FLASHING) return blink ? new Color(255, 190, 0) : new Color(90, 90, 90);
        return new Color(220, 0, 0);
    }
}
