import javax.swing.*;

/** The main window after login. */
class MainFrame extends JFrame {
    private final City city;
    private final MainView view;

    MainFrame(City city, User user) {
        super("GreenWave | Smart Traffic Signal & Emergency Vehicle Management");
        this.city = city;
        view = new MainView(city, user, this::logout);
        setContentPane(view);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1360, 840); setMinimumSize(new java.awt.Dimension(1100, 700)); setLocationRelativeTo(null);
        view.start();
    }
    private void logout() { view.stop(); dispose(); new LoginFrame(city).setVisible(true); }
}
