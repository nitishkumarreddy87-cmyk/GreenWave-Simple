import javax.swing.*;

/** Starts GreenWave. */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AppFrame(new City()).setVisible(true));
    }
}
