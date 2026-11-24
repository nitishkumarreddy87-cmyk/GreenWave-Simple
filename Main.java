import javax.swing.*;

/** Starts GreenWave: one City (the model) and the Swing screens (the view). */
public class Main {
    static User login(String name, String pass) {
        if (name.equals("admin") && pass.equals("admin123")) return new Admin("Admin");
        if (name.equals("operator") && pass.equals("op123")) return new Operator("Operator");
        if (name.equals("driver") && pass.equals("drive123")) return new Driver("Driver");
        return null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) { }
            Theme.current = Theme.forHour(java.time.LocalTime.now().getHour());
            new LoginFrame(new City()).setVisible(true);
        });
    }
}
