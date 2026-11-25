import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

/** The control room: a statistics bar, three tabs and a status line. */
class MainPanel extends JPanel {
    /** Something a button wants to do; it may throw one of our exceptions. */
    interface Task { void run() throws Exception; }

    private final City city;
    private final User user;
    final MapPanel map;
    final JTabbedPane tabs = new JTabbedPane();
    final JPopupMenu popup = new JPopupMenu();
    private final JMenuItem popupTitle = new JMenuItem();
    private final Map<JComponent, String> perms = new HashMap<>();
    private final JLabel emergencies = new JLabel(), greens = new JLabel(), saved = new JLabel(), faults = new JLabel(), cars = new JLabel(), counts = new JLabel();
    private final JLabel status = new JLabel(" ");
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Junction", "Location", "East-West", "North-South", "Control"}, 0);
    private final JTable table = new JTable(model);
    private final DefaultListModel<String> logModel = new DefaultListModel<>();
    private final JRadioButton normalMode = new JRadioButton("Normal signals", true), nightMode = new JRadioButton("Night mode (flashing)");
    private final JCheckBox pauseBox = new JCheckBox("Pause simulation");
    private Junction picked;
    private Thread animation;
    private List<String> lastEvents = new ArrayList<>();

    MainPanel(City city, User user) {
        super(new BorderLayout(0, 6));
        this.city = city;
        this.user = user;
        this.map = new MapPanel(city);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel stats = new JPanel(new GridLayout(2, 3, 8, 4));              // statistics bar
        for (JLabel l : new JLabel[]{emergencies, greens, saved, faults, cars, counts}) {
            l.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), BorderFactory.createEmptyBorder(4, 8, 4, 8)));
            stats.add(l);
        }
        add(stats, BorderLayout.NORTH);

        tabs.setFocusable(false);
        tabs.addTab("Live Map", mapTab());
        tabs.addTab("Signals", signalsTab());
        tabs.addTab("Event Log", logTab());
        add(tabs, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        popupTitle.setEnabled(false);                                      // pop-up menu for a junction
        popup.add(popupTitle);
        String[][] choices = {{"Auto", "auto"}, {"East-West green", "h"}, {"North-South green", "v"}, {"Mark fault", "fault"}};
        for (String[] c : choices) {
            JMenuItem item = new JMenuItem(c[0]);
            item.addActionListener(e -> act("manual", () -> city.setManual(picked.getName(), c[1])));
            popup.add(item);
        }
        map.setListener((j, x, y) -> { preparePopup(j); popup.show(map, x, y); });

        setFocusable(true);                                                // keyboard: Space pauses
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { if (e.getKeyCode() == KeyEvent.VK_SPACE) act("pause", city::togglePause); }
        });
        for (Map.Entry<JComponent, String> e : perms.entrySet()) e.getKey().setEnabled(user.can(e.getValue()));
        refresh();
    }

    private <T extends JComponent> T plain(T c) { c.setFocusable(false); return c; }   // generic method

    private JPanel mapTab() {
        JPanel tab = new JPanel(new BorderLayout(10, 0));
        tab.add(map, BorderLayout.CENTER);
        JPanel side = new JPanel(new GridLayout(0, 1, 0, 8));
        side.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
        side.add(new JLabel("Dispatch a vehicle"));
        String[] routeNames = {"Random route", "Road 1 (east)", "Road 2 (east)", "Road 3 (east)", "Avenue 1 (south)", "Avenue 2 (south)", "Avenue 3 (south)"};
        String[] routeCodes = {"random", "h0", "h1", "h2", "v0", "v1", "v2"};
        JComboBox<String> route = plain(new JComboBox<>(routeNames));
        perms.put(route, "dispatch");
        side.add(route);
        for (String type : new String[]{"Ambulance", "Fire truck", "Police car"}) {
            JButton b = plain(new JButton(type));
            b.addActionListener(e -> act("dispatch", () -> {
                if (route.getSelectedIndex() == 0) city.dispatch(type);             // random route
                else city.dispatch(type, routeCodes[route.getSelectedIndex()]);     // chosen route
            }));
            perms.put(b, "dispatch");
            side.add(b);
        }
        side.add(new JLabel("Signal mode"));
        ButtonGroup group = new ButtonGroup();
        group.add(normalMode); group.add(nightMode);
        normalMode.addActionListener(e -> act("mode", () -> city.setNight(false)));
        nightMode.addActionListener(e -> act("mode", () -> city.setNight(true)));
        perms.put(normalMode, "mode"); perms.put(nightMode, "mode");
        side.add(plain(normalMode)); side.add(plain(nightMode));
        pauseBox.addActionListener(e -> act("pause", city::togglePause));
        perms.put(pauseBox, "pause");
        side.add(plain(pauseBox));
        JButton reset = plain(new JButton("Reset city"));
        reset.addActionListener(e -> reset());
        perms.put(reset, "reset");
        side.add(reset);
        tab.add(side, BorderLayout.EAST);
        return tab;
    }

    private Junction selectedRow() throws SignalFailureException {
        int row = table.getSelectedRow();
        if (row < 0) throw new SignalFailureException("Select a junction row first");
        return city.getJunctions().get(row);
    }

    private JPanel signalsTab() {
        for (int i = 0; i < 9; i++) model.addRow(new Object[]{"J" + (i + 1), "Road " + (i / 3 + 1) + ", Avenue " + (i % 3 + 1), "", "", ""});
        table.setRowHeight(26);
        table.setFocusable(false);
        JPanel tab = new JPanel(new BorderLayout(0, 8));
        tab.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        String[][] choices = {{"Auto", "auto"}, {"East-West green", "h"}, {"North-South green", "v"}, {"Mark fault", "fault"}};
        for (String[] c : choices) {
            JButton b = plain(new JButton(c[0]));
            b.addActionListener(e -> act("manual", () -> city.setManual(selectedRow().getName(), c[1])));
            perms.put(b, "manual");
            buttons.add(b);
        }
        buttons.add(new JLabel("Select a row, then choose an override."));
        tab.add(buttons, BorderLayout.SOUTH);
        return tab;
    }

    private JPanel logTab() {
        JList<String> list = plain(new JList<>(logModel));
        JPanel tab = new JPanel(new BorderLayout(0, 8));
        tab.add(new JScrollPane(list), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton save = plain(new JButton("Save log to file"));
        save.addActionListener(e -> saveLog());
        buttons.add(save);
        tab.add(buttons, BorderLayout.SOUTH);
        return tab;
    }

    // ---- actions ----
    /** Every button goes through here: check the role, run the task, show any error. */
    void act(String action, Task task) {
        try {
            if (!user.can(action)) throw new AccessDeniedException("Your role (" + user.role() + ") is not allowed to do this");
            task.run();
            setStatus(" ", false);
        } catch (AccessDeniedException | RouteNotFoundException | SignalFailureException ex) {
            setStatus(ex.getMessage(), true);
        } catch (Exception ex) {
            setStatus("Error: " + ex, true);
        }
        refresh();
    }

    private void setStatus(String text, boolean error) { status.setText(text); status.setForeground(error ? Color.RED : Color.BLACK); }
    void reset() { act("reset", () -> city.reset()); }
    void saveLog() {
        try { city.saveLog("greenwave-log.txt"); setStatus("Log saved to greenwave-log.txt in the project folder", false); }
        catch (IOException ex) { setStatus("Could not save the log: " + ex.getMessage(), true); }
    }
    void preparePopup(Junction j) { picked = j; popupTitle.setText(j.getName() + " controls"); }

    // ---- threads and screen updates ----
    /** A thread moves the cars and redraws the map; every few frames it also refreshes the statistics. */
    void start() {
        requestFocusInWindow();
        animation = new Thread(() -> {
            int frame = 0;
            while (true) {
                try { Thread.sleep(30); } catch (InterruptedException e) { return; }
                map.moveCars();
                map.repaint();
                if (++frame % 8 == 0) SwingUtilities.invokeLater(() -> refresh());
            }
        });
        animation.setDaemon(true);
        animation.start();
    }
    void stop() { if (animation != null) animation.interrupt(); }

    void refresh() {
        List<Vehicle> active = city.getActive();
        int green = 0, fault = 0;
        for (Junction j : city.getJunctions()) {
            if (j.state('h') == Light.GREEN) green++;
            if (j.state('v') == Light.GREEN) green++;
            if (j.isFault()) fault++;
        }
        emergencies.setText("Active emergency vehicles: " + active.size());
        greens.setText("Signals showing green: " + green + " of 18");
        saved.setText("Waiting time saved: " + city.getSaved() + " s");
        faults.setText("Signal faults: " + fault);
        cars.setText("Cars on the road: " + map.carCount() + " (" + map.waitingCount() + " waiting)");
        counts.setText("Dispatched: " + city.getCounts());
        for (int i = 0; i < 9; i++) {
            Junction j = city.getJunctions().get(i);
            model.setValueAt(j.state('h'), i, 2);
            model.setValueAt(j.state('v'), i, 3);
            model.setValueAt(j.control(), i, 4);
        }
        List<String> events = city.getEvents();
        if (!events.equals(lastEvents)) {
            lastEvents = events;
            logModel.clear();
            for (String line : events) logModel.addElement(line);
        }
        pauseBox.setSelected(city.isPaused());
        nightMode.setSelected(city.isNight());
        normalMode.setSelected(!city.isNight());
    }
}
