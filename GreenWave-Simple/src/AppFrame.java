import javax.swing.*;
import java.awt.*;

/** The window. A CardLayout switches between the sign-in screen and the control room. */
class AppFrame extends JFrame {
    private final City city;
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private MainPanel main;

    AppFrame(City city) {
        super("GreenWave - Smart Traffic Signal & Emergency Vehicle Management System");
        this.city = city;
        root.add(new LoginPanel(user -> signIn(user)), "login");
        setContentPane(root);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1180, 780);
        setLocationRelativeTo(null);
    }

    private void signIn(User user) {
        main = new MainPanel(city, user);
        root.add(main, "main");
        setJMenuBar(buildMenu());
        cards.show(root, "main");
        main.start();
        revalidate();
    }

    private void signOut() {
        main.stop();
        root.remove(main);
        setJMenuBar(null);
        cards.show(root, "login");
        revalidate();
        repaint();
    }

    private JMenuBar buildMenu() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem save = new JMenuItem("Save log"), out = new JMenuItem("Sign out"), exit = new JMenuItem("Exit");
        save.addActionListener(e -> main.saveLog());
        out.addActionListener(e -> signOut());
        exit.addActionListener(e -> System.exit(0));
        file.add(save); file.add(new JSeparator()); file.add(out); file.add(exit);
        JMenu sim = new JMenu("Simulation");
        JMenuItem reset = new JMenuItem("Reset city");
        reset.addActionListener(e -> main.reset());
        sim.add(reset);
        bar.add(file); bar.add(sim);
        return bar;
    }
}
