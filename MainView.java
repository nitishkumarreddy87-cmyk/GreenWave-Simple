import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/** The whole control room: sidebar, six pages, and the two timers that keep it alive. */
class MainView extends JPanel {
    interface Op { void run() throws Exception; }

    final City city; final User user; final TrafficSim sim; final MapPanel map;
    private final Runnable onLogout;
    private final Map<JComponent, String> perms = new LinkedHashMap<>();
    private final CardLayout cards = new CardLayout();
    private final JPanel host = new JPanel(cards);
    private final Map<String, FlatButton> nav = new LinkedHashMap<>();
    private final Map<String, String> titles = new LinkedHashMap<>();
    private final JLabel pageTitle = Ui.label("Dashboard", 26, true, Ui.TEXT), status = Ui.label(" ", 13, false, Ui.MUTE);
    private final StatCard sCars = new StatCard("Vehicles on road", t -> t.accent), sWait = new StatCard("Waiting at red", t -> new Color(0xFF4D5E)),
            sGreen = new StatCard("Signals green", t -> new Color(0x2EE59D)), sEmg = new StatCard("Active emergencies", t -> t.accent3),
            sSaved = new StatCard("Wait time saved", t -> t.accent2), sFault = new StatCard("Signal faults", t -> new Color(0xFFB933));
    private final JTextArea missions = Ui.area(), missions2 = Ui.area(), recent = Ui.area(), done = Ui.area(), logArea = Ui.area();
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Junction", "Location", "East-West", "North-South", "Control"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; } };
    private final JTable table = new JTable(model);
    private final List<FlatButton> dens = new ArrayList<>(), modes = new ArrayList<>(), speeds = new ArrayList<>(), times = new ArrayList<>();
    private final double[] densVals = {0.012, 0.03, 0.048, 0.075}, speedVals = {0.5, 1, 2, 4};
    private final String[] modeVals = {"adaptive", "fixed", "night"};
    private final String[] routeCodes = {"rand", "h0", "h1", "h2", "v0", "v1", "v2"};
    private final String[] routeNames = {"Random route", "Road 1 (east)", "Road 2 (east)", "Road 3 (east)", "Avenue 1 (south)", "Avenue 2 (south)", "Avenue 3 (south)"};
    private FlatButton pauseBtn;
    private final Spark spark;
    private javax.swing.Timer simTimer, uiTimer, toastTimer;
    private double acc; private int lastResets; private String lastKey = "";
    private JPopupMenu popup;

    MainView(City city, User user, Runnable onLogout) {
        super(new BorderLayout());
        this.city = city; this.user = user; this.onLogout = onLogout;
        sim = new TrafficSim(city); map = new MapPanel(city, sim); spark = new Spark(sim);
        lastResets = city.getResets();
        map.onPick = (j, p) -> showPopup(j);
        setOpaque(true); Ui.style(() -> setBackground(Theme.current.bg));
        add(sidebar(), BorderLayout.WEST);
        JPanel right = Ui.clear(new BorderLayout(0, 8)); right.setBorder(new EmptyBorder(14, 20, 8, 20));
        right.add(topBar(), BorderLayout.NORTH);
        host.setOpaque(false); host.add(dashboard(), "dash"); host.add(mapPage(), "map"); host.add(signalsPage(), "sig");
        host.add(emergencyPage(), "emg"); host.add(logsPage(), "logs"); host.add(aboutPage(), "about");
        right.add(host, BorderLayout.CENTER); right.add(status, BorderLayout.SOUTH);
        add(right, BorderLayout.CENTER);
        styleTable();
        for (Map.Entry<JComponent, String> e : perms.entrySet()) {
            boolean ok = user.can(e.getValue()); e.getKey().setEnabled(ok); if (!ok) e.getKey().setToolTipText("Your role is not allowed to do this");
        }
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "pause");
        getActionMap().put("pause", new AbstractAction() { public void actionPerformed(ActionEvent e) { if (user.can("pause")) act("pause", city::togglePause); } });
        show("dash"); setTime(java.util.Arrays.asList(Theme.ALL).indexOf(Theme.current), false);
        refresh();
    }

    void start() {
        simTimer = new javax.swing.Timer(16, e -> tick()); simTimer.start();
        uiTimer = new javax.swing.Timer(250, e -> refresh()); uiTimer.start();
    }
    void stop() { if (simTimer != null) simTimer.stop(); if (uiTimer != null) uiTimer.stop(); }
    void tick() {
        if (city.getResets() != lastResets) { lastResets = city.getResets(); sim.clear(); }
        if (!city.isPaused()) { acc += city.getSpeed(); while (acc >= 1) { sim.step(); acc -= 1; } }
        map.repaint();
    }

    // ---------- helpers ----------
    void act(String action, Op op) {
        try {
            if (!user.can(action)) throw new AccessDeniedException("Your role (" + user.role() + ") is not allowed to do this");
            op.run();
        } catch (AccessDeniedException | RouteNotFoundException | SignalFailureException ex) { toast(ex.getMessage(), true);
        } catch (Exception ex) { toast("Error: " + ex, true); }
        refresh();
    }
    void toast(String msg, boolean err) {
        status.setText(msg); status.setForeground(err ? new Color(0xFF4D5E) : Theme.current.mute);
        if (toastTimer != null) toastTimer.stop();
        toastTimer = new javax.swing.Timer(4000, e -> status.setText(" ")); toastTimer.setRepeats(false); toastTimer.start();
    }
    private FlatButton btn(String text, String action, Op op) {
        FlatButton b = new FlatButton(text); b.addActionListener(e -> act(action, op)); if (action != null) perms.put(b, action); return b;
    }
    private JPanel titled(String title, JComponent c) {
        JPanel p = Ui.clear(new BorderLayout(0, 6)); p.add(Ui.label(title, 15, true, Ui.TEXT), BorderLayout.NORTH); p.add(c, BorderLayout.CENTER); return p;
    }
    private JPanel group(String title, JComponent c) {
        JPanel p = Ui.clear(new BorderLayout(0, 8)); p.add(Ui.label(title, 13, false, Ui.MUTE), BorderLayout.NORTH); p.add(c, BorderLayout.CENTER); return p;
    }
    private JPanel row() { return Ui.clear(new FlowLayout(FlowLayout.LEFT, 6, 0)); }
    void show(String key) {
        cards.show(host, key); pageTitle.setText(titles.get(key));
        for (Map.Entry<String, FlatButton> e : nav.entrySet()) e.getValue().setOn(e.getKey().equals(key));
        if (!key.equals("map") && popup != null) popup.setVisible(false);
    }
    void setTime(int i, boolean log) {
        Ui.applyTheme(Theme.ALL[i], this); styleTable();
        for (int k = 0; k < times.size(); k++) times.get(k).setOn(k == i);
        if (log && user.can("note")) city.log("Time of day: " + Theme.ALL[i].name);
    }

    // ---------- frame parts ----------
    private JPanel sidebar() {
        JPanel s = new JPanel() { @Override protected void paintComponent(Graphics g) {
            g.setColor(Theme.current.panel); g.fillRect(0, 0, getWidth(), getHeight()); g.setColor(Theme.current.line); g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight()); } };
        s.setLayout(new BoxLayout(s, BoxLayout.Y_AXIS)); s.setBorder(new EmptyBorder(22, 14, 18, 14)); s.setPreferredSize(new Dimension(210, 100));
        JPanel brand = Ui.clear(new FlowLayout(FlowLayout.LEFT, 5, 0));
        for (int c : new int[]{0xFF4D5E, 0xFFB933, 0x2EE59D}) brand.add(new JLabel(new DotIcon(new Color(c))));
        brand.add(Ui.label("GreenWave", 18, true, Ui.TEXT)); brand.setAlignmentX(0); brand.setMaximumSize(new Dimension(300, 34));
        s.add(brand); s.add(Box.createVerticalStrut(22));
        String[][] pg = {{"dash", "Dashboard"}, {"map", "Live map"}, {"sig", "Signals"}, {"emg", "Emergency"}, {"logs", "Logs"}, {"about", "About"}};
        for (String[] p : pg) {
            titles.put(p[0], p[1]);
            FlatButton b = new FlatButton(p[1]); b.setHorizontalAlignment(SwingConstants.LEFT); b.setMaximumSize(new Dimension(300, 40)); b.setAlignmentX(0);
            b.addActionListener(e -> show(p[0])); nav.put(p[0], b); s.add(b); s.add(Box.createVerticalStrut(6));
        }
        s.add(Box.createVerticalGlue());
        JLabel who = Ui.label("<html><b>" + user.getName() + "</b><br>" + (user.role().equals("viewer") ? "Public viewer" : user.role()) + "</html>", 13, false, Ui.MUTE);
        who.setAlignmentX(0); s.add(who); s.add(Box.createVerticalStrut(10));
        FlatButton out = new FlatButton("Sign out"); out.setMaximumSize(new Dimension(300, 40)); out.setAlignmentX(0); out.addActionListener(e -> onLogout.run()); s.add(out);
        return s;
    }
    private JPanel topBar() {
        JPanel bar = Ui.clear(new BorderLayout()); bar.add(pageTitle, BorderLayout.WEST);
        JPanel t = Ui.clear(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        for (int i = 0; i < 4; i++) {
            final int idx = i; FlatButton b = new FlatButton(Theme.ALL[i].name); b.setIcon(new TimeIcon(i)); b.addActionListener(e -> setTime(idx, true)); times.add(b); t.add(b);
        }
        bar.add(t, BorderLayout.EAST); return bar;
    }

    // ---------- pages ----------
    private JPanel dashboard() {
        JPanel p = Ui.clear(new BorderLayout(0, 14));
        JPanel stats = Ui.clear(new GridLayout(1, 6, 12, 0));
        for (StatCard c : new StatCard[]{sCars, sWait, sGreen, sEmg, sSaved, sFault}) stats.add(c);
        stats.setPreferredSize(new Dimension(100, 84)); p.add(stats, BorderLayout.NORTH);
        JPanel mid = Ui.clear(new GridLayout(1, 2, 14, 0));
        Card flow = new Card(new BorderLayout(0, 10)); flow.add(Ui.label("Traffic flow", 16, true, Ui.TEXT), BorderLayout.NORTH); flow.add(spark, BorderLayout.CENTER);
        flow.add(Ui.label("Vehicles on the road, last 90 seconds", 12, false, Ui.MUTE), BorderLayout.SOUTH);
        Card right = new Card(new BorderLayout()); JPanel two = Ui.clear(new GridLayout(2, 1, 0, 12));
        two.add(titled("Active emergencies", Ui.scroll(missions))); two.add(titled("Latest events", Ui.scroll(recent))); right.add(two);
        mid.add(flow); mid.add(right); p.add(mid, BorderLayout.CENTER); return p;
    }

    private Card dispatchCard() {
        Card c = new Card(new BorderLayout(0, 12)); c.setPreferredSize(new Dimension(290, 100));
        JPanel top = Ui.clear(new GridLayout(0, 1, 0, 8));
        top.add(Ui.label("Emergency dispatch", 20, true, Ui.TEXT));
        top.add(Ui.label("<html><body style='width:200px'>Choose a route, then send a vehicle. Every signal ahead of it turns green.</body></html>", 13, false, Ui.MUTE));
        JComboBox<String> combo = new JComboBox<>(routeNames); combo.setFont(Ui.font(13, false));
        Ui.style(() -> { combo.setBackground(Theme.current.stage); combo.setForeground(Theme.current.text); });
        perms.put(combo, "dispatch"); top.add(combo);
        JPanel btns = Ui.clear(new GridLayout(3, 1, 0, 12));
        String[] types = {"Ambulance", "Fire truck", "Police car"}; int[] cols = {0x4CC9FF, 0xFF4D5E, 0xFFB933};
        for (int i = 0; i < 3; i++) {
            final String type = types[i];
            FlatButton b = btn(type, "dispatch", () -> city.dispatch(type, routeCodes[combo.getSelectedIndex()]));
            b.setIcon(new DotIcon(new Color(cols[i]))); b.setIconTextGap(12); b.setHorizontalAlignment(SwingConstants.LEFT); b.setFont(Ui.font(16, true)); btns.add(b);
        }
        c.add(top, BorderLayout.NORTH); c.add(btns, BorderLayout.CENTER); return c;
    }

    private JPanel mapPage() {
        JPanel p = Ui.clear(new BorderLayout(0, 12)); JPanel rowp = Ui.clear(new BorderLayout(14, 0));
        Card mapCard = new Card(new BorderLayout()); mapCard.setBorder(new EmptyBorder(8, 8, 8, 8)); mapCard.add(map);
        rowp.add(mapCard, BorderLayout.CENTER); rowp.add(dispatchCard(), BorderLayout.EAST); p.add(rowp, BorderLayout.CENTER);
        Card deck = new Card(new GridLayout(1, 3, 28, 0));
        JPanel d1 = row(), d2 = row(), d3 = row(); String[] dn = {"Low", "Medium", "High", "Rush hour"};
        for (int i = 0; i < 4; i++) { final int k = i; FlatButton b = btn(dn[i], "density", () -> city.setRate(densVals[k])); dens.add(b); d1.add(b); }
        String[] mn = {"Adaptive", "Fixed timer", "Flashing"};
        for (int i = 0; i < 3; i++) { final int k = i; FlatButton b = btn(mn[i], "mode", () -> city.setMode(modeVals[k])); modes.add(b); d2.add(b); }
        pauseBtn = btn("Pause", "pause", city::togglePause); d3.add(pauseBtn);
        String[] sn = {"0.5x", "1x", "2x", "4x"};
        for (int i = 0; i < 4; i++) { final int k = i; FlatButton b = btn(sn[i], "speed", () -> city.setSpeed(speedVals[k])); speeds.add(b); d3.add(b); }
        d3.add(btn("Reset city", "reset", () -> { city.reset(); sim.clear(); }));
        deck.add(group("Traffic density", d1)); deck.add(group("Signal mode", d2)); deck.add(group("Simulation (Space pauses)", d3));
        p.add(deck, BorderLayout.SOUTH); return p;
    }

    private Junction selectedRow() throws SignalFailureException {
        int r = table.getSelectedRow(); if (r < 0) throw new SignalFailureException("Select a junction row first");
        return city.getJunctions().get(r);
    }
    private JPanel signalsPage() {
        Card c = new Card(new BorderLayout(0, 12)); c.add(Ui.label("All junctions", 16, true, Ui.TEXT), BorderLayout.NORTH);
        for (Junction j : city.getJunctions()) { int i = city.getJunctions().indexOf(j); model.addRow(new Object[]{j.getName(), "Road " + (i / 3 + 1) + ", Avenue " + (i % 3 + 1), "", "", ""}); }
        c.add(Ui.scroll(table), BorderLayout.CENTER);
        JPanel b = row(); String[][] o = {{"Auto", "auto"}, {"East-West green", "h"}, {"North-South green", "v"}, {"Mark fault", "fault"}};
        for (String[] x : o) b.add(btn(x[0], "manual", () -> city.setManual(selectedRow().getName(), x[1])));
        b.add(Ui.label("Select a row, then choose an override.", 12, false, Ui.MUTE)); c.add(b, BorderLayout.SOUTH);
        JPanel p = Ui.clear(new BorderLayout()); p.add(c); return p;
    }
    private void styleTable() {
        Theme t = Theme.current; table.setRowHeight(32); table.setFont(Ui.font(13, false)); table.setBackground(t.stage); table.setForeground(t.text);
        table.setGridColor(t.line); table.setSelectionBackground(t.accent); table.setSelectionForeground(t.on); table.setShowVerticalLines(false);
        table.getTableHeader().setBackground(t.panel); table.getTableHeader().setForeground(t.text); table.getTableHeader().setFont(Ui.font(13, true));
        DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(tb, v, sel, foc, row, col);
                String s = String.valueOf(v);
                if (!sel && (col == 2 || col == 3)) c.setForeground(s.equals("Green") ? new Color(0x2EE59D) : s.equals("Red") ? new Color(0xFF4D5E) : new Color(0xFFB933));
                return c;
            } };
        table.setDefaultRenderer(Object.class, r);
    }

    private JPanel emergencyPage() {
        JPanel p = Ui.clear(new GridLayout(1, 2, 14, 0)); p.add(dispatchCard());
        Card c = new Card(new BorderLayout()); JPanel two = Ui.clear(new GridLayout(2, 1, 0, 12));
        two.add(titled("Active missions", Ui.scroll(missions2))); two.add(titled("Recently completed", Ui.scroll(done))); c.add(two); p.add(c); return p;
    }
    private JPanel logsPage() {
        Card c = new Card(new BorderLayout(0, 12)); JPanel b = row();
        FlatButton exp = new FlatButton("Export CSV"); exp.addActionListener(e -> exportCsv()); b.add(exp);
        b.add(btn("Clear log", "log/clear", city::clearLog)); c.add(b, BorderLayout.NORTH); c.add(Ui.scroll(logArea), BorderLayout.CENTER);
        JPanel p = Ui.clear(new BorderLayout()); p.add(c); return p;
    }
    private void exportCsv() {
        JFileChooser fc = new JFileChooser(); fc.setSelectedFile(new java.io.File("greenwave-log.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try { Files.writeString(fc.getSelectedFile().toPath(), city.csv()); toast("Log saved to " + fc.getSelectedFile().getName(), false); }
            catch (IOException ex) { toast("Could not save the file: " + ex.getMessage(), true); }
        }
    }
    private JPanel aboutPage() {
        Card c = new Card(new BorderLayout(0, 12)); c.add(Ui.label("Built on object-oriented Java", 18, true, Ui.TEXT), BorderLayout.NORTH);
        String[][] k = {{"Inheritance", "Vehicle is extended by Ambulance, FireTruck and PoliceCar. User is extended by four roles."},
            {"Polymorphism", "getPriority() differs per vehicle and can() differs per role."}, {"Abstraction", "Vehicle and User are abstract classes."},
            {"Encapsulation", "Signal state is private inside Junction."}, {"Interfaces", "SignalController, Alertable, Comparable and Swing listeners."},
            {"Exception handling", "SignalFailure, RouteNotFound and AccessDenied exceptions."}, {"Collections", "ArrayList, HashMap, HashSet, LinkedList, PriorityQueue."},
            {"Swing and threads", "JPanel screens, a Timer for animation, and a thread per junction."}};
        JPanel g = Ui.clear(new GridLayout(2, 4, 12, 12));
        for (String[] x : k) { Card cc = new Card(new BorderLayout(0, 6)); cc.add(Ui.label(x[0], 15, true, Ui.ACCENT), BorderLayout.NORTH);
            JLabel dl = Ui.label("<html><body style='width:150px'>" + x[1] + "</body></html>", 13, false, Ui.MUTE); dl.setVerticalAlignment(SwingConstants.TOP);
            cc.add(dl, BorderLayout.CENTER); g.add(cc); }
        c.add(g, BorderLayout.CENTER); JPanel p = Ui.clear(new BorderLayout()); p.add(c); return p;
    }

    // ---------- junction pop-up ----------
    private void showPopup(Junction j) {
        if (popup != null) popup.setVisible(false);
        if (j == null) return;
        popup = new JPopupMenu(); popup.setBorder(BorderFactory.createEmptyBorder()); popup.add(junctionPanel(j));
        Point p = map.screenPos(j); popup.show(map, p.x + 50, Math.max(4, p.y - 90));
    }
    JPanel junctionPanel(Junction j) {
        Card c = new Card(new BorderLayout(0, 8)); c.setPreferredSize(new Dimension(330, 240));
        int i = city.getJunctions().indexOf(j); String h = j.state('h'), v = j.state('v'), m = j.marker();
        c.add(Ui.label(j.getName() + "  |  Road " + (i / 3 + 1) + ", Avenue " + (i % 3 + 1), 15, true, Ui.TEXT), BorderLayout.NORTH);
        JPanel info = Ui.clear(new GridLayout(3, 1, 0, 2));
        info.add(Ui.label("East-West: " + name(h), 13, false, Ui.MUTE)); info.add(Ui.label("North-South: " + name(v), 13, false, Ui.MUTE));
        info.add(Ui.label("Control: " + (m.equals("f") ? "Fault" : m.equals("m") ? "Manual override" : "Automatic"), 13, true, Ui.TEXT)); c.add(info, BorderLayout.CENTER);
        JPanel b = Ui.clear(new GridLayout(2, 2, 6, 6)); String[][] o = {{"Auto", "auto"}, {"Mark fault", "fault"}, {"East-West green", "h"}, {"North-South green", "v"}};
        String active = m.equals("f") ? "fault" : m.equals("m") ? (h.equals("g") ? "h" : "v") : "auto";
        for (String[] x : o) { FlatButton f = btn(x[0], "manual", () -> { city.setManual(j.getName(), x[1]); if (popup != null) popup.setVisible(false); });
            f.setFont(Ui.font(12, true)); f.setOn(x[1].equals(active)); f.setEnabled(user.can("manual")); b.add(f); }
        c.add(b, BorderLayout.SOUTH); return c;
    }
    private static String name(String s) { return s.equals("g") ? "Green" : s.equals("y") ? "Yellow" : s.equals("r") ? "Red" : "Flashing"; }

    // ---------- live refresh ----------
    void refresh() {
        List<Vehicle> act = city.getActive(); int green = 0;
        for (Junction j : city.getJunctions()) { if (j.state('h').equals("g")) green++; if (j.state('v').equals("g")) green++; }
        long faults = city.getJunctions().stream().filter(Junction::isFault).count();
        sCars.value.setText(String.valueOf(sim.cars.size() + act.size())); sWait.value.setText(String.valueOf(sim.waiting())); sGreen.value.setText(String.valueOf(green));
        sEmg.value.setText(String.valueOf(act.size())); sSaved.value.setText(city.getSaved() + "s"); sFault.value.setText(String.valueOf(faults));
        StringBuilder ms = new StringBuilder();
        for (Vehicle e : act) {
            double lim = e.getAxis() == 'h' ? 900 : 520, pr = Math.max(0, Math.min(100, e.getPos() / lim * 100)), eta = Math.max(0, (lim - e.getPos()) / (186 * city.getSpeed()));
            ms.append(e.alertText()).append("  |  ").append(e.getAxis() == 'h' ? "Road " : "Avenue ").append(e.getLine() + 1)
              .append("  |  ").append(String.format("%.0f%%  |  ETA %.0fs\n", pr, eta));
        }
        String m = ms.length() == 0 ? "No active emergencies." : ms.toString(); setText(missions, m); setText(missions2, m);
        List<String> log = city.getLogView();
        setText(recent, String.join("\n", log.subList(0, Math.min(5, log.size()))));
        StringBuilder dn = new StringBuilder(); int n = 0; for (String s : log) if (s.contains("reached") && n++ < 5) dn.append(s).append('\n');
        setText(done, n == 0 ? "None yet." : dn.toString()); setText(logArea, String.join("\n", log));
        for (int i = 0; i < 9; i++) {
            Junction j = city.getJunctions().get(i); String mk = j.marker();
            set(i, 2, name(j.state('h'))); set(i, 3, name(j.state('v'))); set(i, 4, mk.equals("f") ? "Fault" : mk.equals("m") ? "Manual" : "Auto");
        }
        if (pauseBtn != null) pauseBtn.setText(city.isPaused() ? "Resume" : "Pause");
        for (int i = 0; i < dens.size(); i++) dens.get(i).setOn(Math.abs(densVals[i] - city.getRate()) < 1e-6);
        for (int i = 0; i < modes.size(); i++) modes.get(i).setOn(modeVals[i].equals(city.getMode()));
        for (int i = 0; i < speeds.size(); i++) speeds.get(i).setOn(speedVals[i] == city.getSpeed());
        spark.repaint();
    }
    private void set(int r, int c, String v) { if (!v.equals(model.getValueAt(r, c))) model.setValueAt(v, r, c); }
    private static void setText(JTextArea a, String s) { if (!s.equals(a.getText())) a.setText(s); }
}
