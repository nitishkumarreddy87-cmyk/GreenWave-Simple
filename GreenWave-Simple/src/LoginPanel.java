import javax.swing.*;
import java.awt.*;

/** Interface for the object that wants to know who signed in. */
interface LoginListener { void loggedIn(User user); }

/** The sign-in screen: a name and a role. */
class LoginPanel extends JPanel {
    LoginPanel(LoginListener listener) {
        super(new GridBagLayout());
        JPanel box = new JPanel(new GridBagLayout());
        box.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY), BorderFactory.createEmptyBorder(25, 35, 25, 35)));
        JLabel title = new JLabel("GreenWave");
        title.setFont(new Font("SansSerif", Font.BOLD, 32));
        JLabel subtitle = new JLabel("Smart Traffic Signal & Emergency Vehicle Management System");
        JTextField name = new JTextField(20);
        JComboBox<String> role = new JComboBox<>(new String[]{"Admin", "Operator", "Emergency driver", "Public viewer"});
        JButton enter = new JButton("Enter");
        enter.addActionListener(e -> {
            String n = name.getText().trim().isEmpty() ? "Guest" : name.getText().trim();
            User user;
            switch (role.getSelectedIndex()) {
                case 0: user = new Admin(n); break;
                case 1: user = new Operator(n); break;
                case 2: user = new Driver(n); break;
                default: user = new Viewer(n);
            }
            listener.loggedIn(user);
        });
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.fill = GridBagConstraints.HORIZONTAL; gc.insets = new Insets(6, 0, 6, 0);
        Component[] parts = {title, subtitle, new JLabel("Your name"), name, new JLabel("Role"), role, enter};
        for (int i = 0; i < parts.length; i++) { gc.gridy = i; box.add(parts[i], gc); }
        add(box);
    }
}
