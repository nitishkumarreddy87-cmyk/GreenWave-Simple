import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Small helpers and custom components shared by every screen. */
final class Ui {
    static final List<Runnable> restylers = new ArrayList<>();   // re-colour everything when the theme changes
    static final int TEXT = 0, MUTE = 1, ACCENT = 2, ACC2 = 3;

    static Font font(int size, boolean bold) { return new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size); }
    static Color role(int r) { Theme t = Theme.current; return r == MUTE ? t.mute : r == ACCENT ? t.accent : r == ACC2 ? t.accent2 : t.text; }
    static void style(Runnable r) { restylers.add(r); r.run(); }
    static void applyTheme(Theme t, Component root) { Theme.current = t; for (Runnable r : restylers) r.run(); root.repaint(); }
    static void aa(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
    static JLabel label(String text, int size, boolean bold, int role) {
        JLabel l = new JLabel(text); l.setFont(font(size, bold)); style(() -> l.setForeground(role(role))); return l;
    }
    static JPanel clear(LayoutManager lm) { JPanel p = new JPanel(lm); p.setOpaque(false); return p; }
    static JTextArea area() {
        JTextArea a = new JTextArea(); a.setEditable(false); a.setLineWrap(true); a.setWrapStyleWord(true);
        a.setFont(font(13, false)); a.setBorder(new EmptyBorder(8, 10, 8, 10));
        style(() -> { a.setBackground(Theme.current.stage); a.setForeground(Theme.current.text); });
        return a;
    }
    static JScrollPane scroll(Component c) {
        JScrollPane s = new JScrollPane(c);
        if (c instanceof JTable) s.setColumnHeaderView(((JTable) c).getTableHeader());
        style(() -> { s.getViewport().setBackground(Theme.current.stage); s.setBorder(BorderFactory.createLineBorder(Theme.current.line)); });
        return s;
    }
    static void field(JTextField f) {
        f.setFont(font(14, false));
        style(() -> { f.setBackground(Theme.current.stage); f.setForeground(Theme.current.text); f.setCaretColor(Theme.current.text);
            f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.current.line), new EmptyBorder(8, 10, 8, 10))); });
    }
}

/** Rounded button that follows the theme. */
class FlatButton extends JButton {
    private boolean on;
    FlatButton(String text) {
        super(text);
        setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false); setOpaque(false);
        setFont(Ui.font(13, true)); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(9, 14, 9, 14)); setRolloverEnabled(true);
        Ui.style(this::restyle);
    }
    void setOn(boolean v) { on = v; restyle(); repaint(); }
    private void restyle() { setForeground(on ? Theme.current.on : Theme.current.text); }
    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create(); Ui.aa(g); Theme t = Theme.current;
        g.setColor(on ? t.accent : t.stage); g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        g.setColor(getModel().isRollover() && isEnabled() ? t.accent : t.line); g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        g.dispose();
        super.paintComponent(g0);
    }
}

class DotIcon implements Icon {
    private final Color c;
    DotIcon(Color c) { this.c = c; }
    public int getIconWidth() { return 16; }
    public int getIconHeight() { return 16; }
    public void paintIcon(Component k, Graphics g0, int x, int y) {
        Graphics2D g = (Graphics2D) g0.create(); Ui.aa(g); g.setColor(c); g.fillOval(x + 1, y + 1, 13, 13); g.dispose();
    }
}

/** Little sunrise, sun, sunset and moon pictures for the time-of-day buttons. */
class TimeIcon implements Icon {
    private final int kind;
    TimeIcon(int kind) { this.kind = kind; }
    public int getIconWidth() { return 24; }
    public int getIconHeight() { return 18; }
    public void paintIcon(Component k, Graphics g0, int x, int y) {
        Graphics2D g = (Graphics2D) g0.create(); Ui.aa(g);
        g.setColor(k.getForeground()); g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int cx = x + 12, cy = y + 9;
        if (kind == 0 || kind == 2) {
            g.draw(new Arc2D.Double(cx - 6, cy - 3, 12, 12, 0, 180, Arc2D.OPEN));
            g.drawLine(x, cy + 3, x + 24, cy + 3);
            if (kind == 0) { g.drawLine(cx, y, cx, y + 3); g.drawLine(cx - 8, cy - 5, cx - 6, cy - 3); g.drawLine(cx + 8, cy - 5, cx + 6, cy - 3); }
            else { g.drawLine(x + 5, cy + 6, x + 19, cy + 6); g.drawLine(x + 9, cy + 9, x + 15, cy + 9); }
        } else if (kind == 1) {
            g.draw(new Ellipse2D.Double(cx - 4, cy - 4, 8, 8));
            for (int i = 0; i < 8; i++) { double a = i * Math.PI / 4; g.draw(new Line2D.Double(cx + Math.cos(a) * 6, cy + Math.sin(a) * 6, cx + Math.cos(a) * 9, cy + Math.sin(a) * 9)); }
        } else {
            Area a = new Area(new Ellipse2D.Double(cx - 6, cy - 7, 13, 13));
            a.subtract(new Area(new Ellipse2D.Double(cx - 1, cy - 9, 12, 12))); g.draw(a);
        }
        g.dispose();
    }
}

class Card extends JPanel {
    Card(LayoutManager lm) { super(lm); setOpaque(false); setBorder(new EmptyBorder(16, 18, 16, 18)); }
    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create(); Ui.aa(g);
        g.setColor(Theme.current.panel); g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
        g.setColor(Theme.current.line); g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
        g.dispose();
    }
}

class StatCard extends JPanel {
    final JLabel value = Ui.label("0", 26, true, Ui.TEXT);
    private final Function<Theme, Color> stripe;
    StatCard(String caption, Function<Theme, Color> stripe) {
        super(new BorderLayout(0, 2)); this.stripe = stripe; setOpaque(false); setBorder(new EmptyBorder(14, 14, 10, 14));
        add(value, BorderLayout.CENTER); add(Ui.label(caption, 12, false, Ui.MUTE), BorderLayout.SOUTH);
        Ui.style(() -> value.setForeground(stripe.apply(Theme.current)));
    }
    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create(); Ui.aa(g); Theme t = Theme.current;
        g.setColor(t.stage); g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        g.setColor(t.line); g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        g.setClip(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 14, 14));
        g.setColor(stripe.apply(t)); g.fillRect(0, 0, getWidth(), 4); g.dispose();
    }
}

/** Line graph of vehicles on the road over the last 90 seconds. */
class Spark extends JPanel {
    private final TrafficSim sim;
    Spark(TrafficSim sim) { this.sim = sim; setOpaque(false); setPreferredSize(new Dimension(420, 220)); }
    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create(); Ui.aa(g); Theme t = Theme.current; int w = getWidth(), h = getHeight();
        g.setColor(t.stage); g.fillRoundRect(0, 0, w - 1, h - 1, 14, 14); g.setColor(t.line); g.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
        List<Integer> hist = new ArrayList<>(sim.hist);
        int max = 10; for (int v : hist) max = Math.max(max, v);
        g.setColor(t.line); for (int i = 1; i < 4; i++) g.drawLine(10, h * i / 4, w - 10, h * i / 4);
        if (hist.size() > 1) {
            Path2D line = new Path2D.Double();
            for (int i = 0; i < hist.size(); i++) {
                double x = 12 + i * (w - 24.0) / 89, y = h - 14 - hist.get(i) * (h - 30.0) / max;
                if (i == 0) line.moveTo(x, y); else line.lineTo(x, y);
            }
            Path2D fill = new Path2D.Double(line); fill.lineTo(12 + (hist.size() - 1) * (w - 24.0) / 89, h - 14); fill.lineTo(12, h - 14); fill.closePath();
            g.setColor(new Color(t.accent.getRed(), t.accent.getGreen(), t.accent.getBlue(), 45)); g.fill(fill);
            g.setColor(t.accent); g.setStroke(new BasicStroke(2.4f)); g.draw(line);
        }
        g.dispose();
    }
}
