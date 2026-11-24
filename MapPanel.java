import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.List;
import java.util.function.BiConsumer;

/** Draws the city: roads, nine signals, cars and emergency vehicles. Click a junction to control it. */
class MapPanel extends JPanel {
    private final City city;
    private final TrafficSim sim;
    Junction selected;
    BiConsumer<Junction, Point> onPick;

    MapPanel(City city, TrafficSim sim) {
        this.city = city; this.sim = sim;
        setPreferredSize(new Dimension(900, 520));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                double k = scale(), x = (e.getX() - ox()) / k, y = (e.getY() - oy()) / k;
                Junction hit = null;
                for (int i = 0; i < 9; i++) if (Math.hypot(City.XS[i % 3] - x, City.YS[i / 3] - y) < 45) hit = city.getJunctions().get(i);
                selected = hit;
                if (onPick != null) onPick.accept(hit, e.getPoint());
                repaint();
            }
        });
    }

    double scale() { return Math.min(getWidth() / 900.0, getHeight() / 520.0); }
    int ox() { return (int) ((getWidth() - 900 * scale()) / 2); }
    int oy() { return (int) ((getHeight() - 520 * scale()) / 2); }

    private static void glow(Graphics2D g, double x, double y, float r, Color c) {
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), r, new float[]{0f, 1f}, new Color[]{c, new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)}));
        g.fill(new Rectangle2D.Double(x - r, y - r, 2 * r, 2 * r));
    }

    private static Color signal(String s, boolean blink) {
        switch (s) {
            case "g": return new Color(0x2EE59D);
            case "y": return new Color(0xFFB933);
            case "f": return blink ? new Color(0xFFB933) : new Color(0x3A3320);
            default: return new Color(0xFF4D5E);
        }
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create(); Ui.aa(g); Theme t = Theme.current;
        g.setColor(t.stage); g.fillRect(0, 0, getWidth(), getHeight());
        AffineTransform base = g.getTransform(); double k = scale();
        g.translate(ox(), oy()); g.scale(k, k); g.setClip(0, 0, 900, 520);
        g.setColor(t.mapBg); g.fillRect(0, 0, 900, 520);
        g.setColor(t.road);
        for (double y : City.YS) g.fillRect(0, (int) y - 24, 900, 48);
        for (double x : City.XS) g.fillRect((int) x - 24, 0, 48, 520);
        g.setColor(t.dash); g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{14, 14}, 0));
        for (double y : City.YS) g.drawLine(0, (int) y, 900, (int) y);
        for (double x : City.XS) g.drawLine((int) x, 0, (int) x, 520);
        g.setStroke(new BasicStroke(1f));

        List<Vehicle> evs = city.getActive();
        long now = System.currentTimeMillis(); boolean blink = (now / 500) % 2 == 0, siren = (now / 160) % 2 == 0;
        for (Vehicle e : evs) {            // green corridor
            g.setColor(new Color(46, 229, 157, 90)); g.setStroke(new BasicStroke(10f));
            if (e.getAxis() == 'h') { int y = (int) City.YS[e.getLine()]; g.drawLine((int) e.getPos(), y, (int) Math.min(e.getPos() + 230, 900), y); }
            else { int x = (int) City.XS[e.getLine()]; g.drawLine(x, (int) e.getPos(), x, (int) Math.min(e.getPos() + 230, 520)); }
        }
        g.setStroke(new BasicStroke(1f));
        g.setFont(Ui.font(11, true));
        for (int i = 0; i < 9; i++) {
            Junction j = city.getJunctions().get(i); double x = City.XS[i % 3], y = City.YS[i / 3]; String m = j.marker();
            if (j == selected) { g.setColor(new Color(0x4CC9FF)); g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{5, 5}, 0)); g.drawRect((int) x - 40, (int) y - 40, 80, 80); g.setStroke(new BasicStroke(1f)); }
            String[] st = {j.state('h'), j.state('v')}; double[] dx = {-34, 34};
            for (int a = 0; a < 2; a++) {
                Color c = signal(st[a], blink);
                glow(g, x + dx[a], y - 34, 16, new Color(c.getRed(), c.getGreen(), c.getBlue(), 110));
                g.setColor(c); g.fill(new Ellipse2D.Double(x + dx[a] - 6, y - 40, 12, 12));
            }
            g.setColor(m.equals("f") ? new Color(0xFFB933) : t.mute);
            g.drawString(j.getName() + (m.equals("f") ? " FAULT" : m.equals("m") ? " MANUAL" : ""), (int) x + 30, (int) y + 44);
        }
        for (TrafficSim.Car c : sim.cars) {
            g.setColor(t.cars[c.ci]);
            if (c.axis == 'h') g.fillRect((int) c.pos - 9, (int) City.YS[c.line] - 16, 18, 10); else g.fillRect((int) City.XS[c.line] + 6, (int) c.pos - 9, 10, 18);
        }
        for (Vehicle e : evs) {
            double px = e.getAxis() == 'h' ? e.getPos() : City.XS[e.getLine()], py = e.getAxis() == 'h' ? City.YS[e.getLine()] : e.getPos();
            Rectangle2D body = e.getAxis() == 'h' ? new Rectangle2D.Double(px - 13, py - 6, 26, 12) : new Rectangle2D.Double(px - 6, py - 13, 12, 26);
            g.setColor(Color.WHITE); g.fill(body); g.setColor(new Color(0, 0, 0, 130)); g.draw(body);
            g.setColor(siren ? new Color(0xFF4D5E) : new Color(0x4CC9FF)); g.fill(new Ellipse2D.Double(px - 5, py - 5, 10, 10));
        }
        if (t.lights) {
            for (int i = 0; i < 9; i++) glow(g, City.XS[i % 3], City.YS[i / 3], 110, new Color(255, 190, 110, 34));
            for (TrafficSim.Car c : sim.cars) {
                if (c.axis == 'h') glow(g, c.pos + 18, City.YS[c.line] - 11, 34, new Color(255, 240, 190, 70));
                else glow(g, City.XS[c.line] + 11, c.pos + 18, 34, new Color(255, 240, 190, 70));
            }
        }
        for (Vehicle e : evs) {
            double px = e.getAxis() == 'h' ? e.getPos() : City.XS[e.getLine()], py = e.getAxis() == 'h' ? City.YS[e.getLine()] : e.getPos();
            glow(g, px, py, 85, siren ? new Color(255, 77, 94, 110) : new Color(76, 201, 255, 110));
        }
        g.setColor(t.tint); g.fillRect(0, 0, 900, 520);
        g.setTransform(base); g.setClip(null);

        // status pill (top left) and emergency banner (top centre)
        g.setFont(Ui.font(13, true)); FontMetrics fm = g.getFontMetrics();
        String mode = city.getMode().equals("adaptive") ? "Adaptive" : city.getMode().equals("fixed") ? "Fixed timer" : "Flashing";
        String hud = String.format("Live %02d:%02d  |  %s%s", (int) city.getClock() / 60, (int) city.getClock() % 60, mode, city.isPaused() ? "  |  Paused" : "");
        int w = fm.stringWidth(hud) + 40;
        g.setColor(new Color(t.panel.getRed(), t.panel.getGreen(), t.panel.getBlue(), 215)); g.fillRoundRect(16, 16, w, 30, 30, 30);
        g.setColor(t.line); g.drawRoundRect(16, 16, w, 30, 30, 30);
        g.setColor(new Color(0x2EE59D)); g.fillOval(28, 26, 10, 10);
        g.setColor(t.text); g.drawString(hud, 46, 36);
        if (!evs.isEmpty()) {
            Vehicle e = evs.get(0);
            String msg = e.alertText() + " en route. Green corridor active" + (evs.size() > 1 ? " (+" + (evs.size() - 1) + " more)" : "");
            int bw = fm.stringWidth(msg) + 50, bx = Math.max((getWidth() - bw) / 2, 16 + w + 12);
            g.setPaint(new GradientPaint(bx, 0, new Color(0xFF4D5E), bx + bw, 0, new Color(0xFF8A3D)));
            g.fillRoundRect(bx, 14, bw, 34, 34, 34);
            g.setPaint(null); g.setColor(Color.WHITE); g.fillOval(bx + 16, 26, 10, 10); g.drawString(msg, bx + 34, 36);
        }
        g.dispose();
    }

    /** Screen position of a junction, used to open the pop-up next to it. */
    Point screenPos(Junction j) {
        int i = city.getJunctions().indexOf(j); double k = scale();
        return new Point((int) (ox() + City.XS[i % 3] * k), (int) (oy() + City.YS[i / 3] * k));
    }
}
