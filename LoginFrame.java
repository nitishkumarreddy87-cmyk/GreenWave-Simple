import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Consumer;

/** The sign-in screen (a panel, so it can also be drawn without a window). */
class LoginView extends JPanel {
    LoginView(City city, Consumer<User> onLogin) {
        super(new GridBagLayout());
        Card box = new Card(null); box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS)); box.setBorder(new EmptyBorder(28, 32, 28, 32));
        JPanel brand = Ui.clear(new FlowLayout(FlowLayout.LEFT, 6, 0));
        for (int c : new int[]{0xFF4D5E, 0xFFB933, 0x2EE59D}) brand.add(new JLabel(new DotIcon(new Color(c))));
        brand.add(Ui.label("GreenWave", 20, true, Ui.TEXT));
        JLabel h1 = Ui.label("<html>Every second counts.<br>Every signal listens.</html>", 26, true, Ui.TEXT);
        JLabel sub = Ui.label("<html><body style='width:280px'>Smart Traffic Signal &amp; Emergency Vehicle Management System. Sign in to run the city, or watch it as a public viewer.</body></html>", 13, false, Ui.MUTE);
        JTextField user = new JTextField(); JPasswordField pass = new JPasswordField(); Ui.field(user); Ui.field(pass);
        JLabel err = Ui.label(" ", 13, false, Ui.TEXT); err.setForeground(new Color(0xFF4D5E));
        FlatButton in = new FlatButton("Sign in"); in.setOn(true);
        FlatButton guest = new FlatButton("Continue as public viewer");
        Runnable submit = () -> {
            User u = Main.login(user.getText().trim(), new String(pass.getPassword()));
            if (u == null) err.setText("Wrong username or password"); else onLogin.accept(u);
        };
        in.addActionListener(e -> submit.run()); pass.addActionListener(e -> submit.run()); user.addActionListener(e -> pass.requestFocus());
        guest.addActionListener(e -> onLogin.accept(new Viewer("Guest")));
        JLabel demo = Ui.label("<html><body style='width:280px'>Demo logins: admin / admin123, operator / op123, driver / drive123</body></html>", 12, false, Ui.MUTE);
        for (JComponent c : new JComponent[]{brand, h1, sub, Ui.label("Username", 13, false, Ui.MUTE), user, Ui.label("Password", 13, false, Ui.MUTE), pass, err, in, guest, demo}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT); box.add(c); box.add(Box.createVerticalStrut(c instanceof JLabel ? 4 : 10));
        }
        user.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); pass.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        in.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); guest.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        box.setPreferredSize(new Dimension(420, 560));
        add(box);
    }
    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setPaint(new GradientPaint(0, 0, Theme.current.bg, getWidth(), getHeight(), Theme.current.stage)); g.fillRect(0, 0, getWidth(), getHeight());
        g.dispose();
    }
}

class LoginFrame extends JFrame {
    LoginFrame(City city) {
        super("GreenWave | Sign in");
        setContentPane(new LoginView(city, u -> { dispose(); new MainFrame(city, u).setVisible(true); }));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(720, 700); setLocationRelativeTo(null);
    }
}
