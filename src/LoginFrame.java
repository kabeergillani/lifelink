package lifelink;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;

public class LoginFrame extends JFrame {
    private JTextField emailField;
    private JPasswordField passwordField;

    public LoginFrame() {
        setTitle("LifeLink - Login");
        setSize(470, 390);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setBackground(new Color(132, 12, 35));
        JLabel title = new JLabel("LIFELINK", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 27));
        JLabel sub = new JLabel("Blood Donor Matching & Emergency Alert System", SwingConstants.CENTER);
        sub.setForeground(Color.WHITE);
        sub.setFont(new Font("Arial", Font.PLAIN, 12));
        header.add(title);
        header.add(sub);
        header.setPreferredSize(new Dimension(470, 90));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        JLabel heading = new JLabel("Sign in to your account", SwingConstants.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 19));
        form.add(heading, c);

        c.gridwidth = 1; c.gridy++;
        form.add(new JLabel("Email address"), c);
        c.gridx = 1;
        emailField = new JTextField(19);
        form.add(emailField, c);

        c.gridx = 0; c.gridy++;
        form.add(new JLabel("Password"), c);
        c.gridx = 1;
        passwordField = new JPasswordField(19);
        form.add(passwordField, c);

        c.gridx = 0; c.gridy++; c.gridwidth = 2;
        JButton login = new JButton("LOGIN");
        styleButton(login);
        form.add(login, c);

        c.gridy++;
        JButton signup = new JButton("Create a new account");
        signup.setForeground(new Color(132, 12, 35));
        signup.setBorderPainted(false);
        signup.setContentAreaFilled(false);
        form.add(signup, c);

        c.gridy++;
        JLabel roles = new JLabel("Donor  |  Hospital  |  Administrator", SwingConstants.CENTER);
        roles.setForeground(Color.GRAY);
        form.add(roles, c);

        root.add(header, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        add(root);

        login.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { doLogin(); }
        });
        passwordField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { doLogin(); }
        });
        signup.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new SignupFrame().setVisible(true);
                dispose();
            }
        });
    }

    private void doLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (email.length() == 0 || password.length() == 0) {
            JOptionPane.showMessageDialog(this, "Enter email and password.");
            return;
        }
        String sql = "SELECT id, full_name, role, status FROM users WHERE email=? AND password_hash=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, PasswordUtil.hash(password));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String status = rs.getString("status");
                String role = rs.getString("role");
                if ("PENDING".equals(status)) {
                    JOptionPane.showMessageDialog(this, "Your hospital account is waiting for admin verification.");
                    return;
                }
                if (!"ACTIVE".equals(status)) {
                    JOptionPane.showMessageDialog(this, "This account is not active. Contact administrator.");
                    return;
                }
                int userId = rs.getInt("id");
                String name = rs.getString("full_name");
                new DashboardFrame(userId, name, role).setVisible(true);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid email or password.");
            }
            rs.close();
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void showDatabaseError(SQLException ex) {
        JOptionPane.showMessageDialog(this,
            "Could not connect to LifeLink database.\n" + ex.getMessage()
            + "\n\nCheck XAMPP MySQL and MySQL Connector/J in Libraries.",
            "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    static void styleButton(JButton b) {
        b.setBackground(new Color(132, 12, 35));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Arial", Font.BOLD, 13));
    }
}
