package lifelink;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;

public class SignupFrame extends JFrame {
    private JTextField nameField, emailField, phoneField, dobField, cityField;
    private JPasswordField passwordField, confirmField;
    private JComboBox roleBox, bloodBox;
    private JLabel bloodLabel, dobLabel, cityLabel;

    public SignupFrame() {
        setTitle("LifeLink - Create Account");
        setSize(540, 580);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(Color.WHITE);
        JLabel heading = new JLabel("CREATE YOUR LIFELINK ACCOUNT", SwingConstants.CENTER);
        heading.setOpaque(true);
        heading.setBackground(new Color(132, 12, 35));
        heading.setForeground(Color.WHITE);
        heading.setFont(new Font("Arial", Font.BOLD, 20));
        heading.setPreferredSize(new Dimension(540, 58));
        outer.add(heading, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        roleBox = new JComboBox(new String[] {"DONOR", "HOSPITAL"});
        nameField = new JTextField(20);
        emailField = new JTextField(20);
        phoneField = new JTextField(20);
        passwordField = new JPasswordField(20);
        confirmField = new JPasswordField(20);
        bloodBox = new JComboBox(new String[] {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"});
        dobField = new JTextField("YYYY-MM-DD");
        cityField = new JTextField(20);

        int y = 0;
        addRow(form, c, y++, "Account type", roleBox);
        addRow(form, c, y++, "Full name / Hospital name", nameField);
        addRow(form, c, y++, "Email", emailField);
        addRow(form, c, y++, "Phone", phoneField);
        addRow(form, c, y++, "Password", passwordField);
        addRow(form, c, y++, "Confirm password", confirmField);
        
        bloodLabel = new JLabel("Blood group");
        addRow(form, c, y++, bloodLabel, bloodBox);
        
        dobLabel = new JLabel("Date of birth (YYYY-MM-DD)");
        addRow(form, c, y++, dobLabel, dobField);
        
        cityLabel = new JLabel("City / area");
        addRow(form, c, y++, cityLabel, cityField);

        c.gridx = 0; c.gridy = y++; c.gridwidth = 2;
        JButton create = new JButton("CREATE ACCOUNT");
        styleButton(create);
        form.add(create, c);

        c.gridy++;
        JButton back = new JButton("Back to Login");
        back.setForeground(new Color(132, 12, 35));
        back.setContentAreaFilled(false);
        back.setBorderPainted(false);
        form.add(back, c);

        outer.add(form, BorderLayout.CENTER);
        add(new JScrollPane(outer));

        roleBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { updateFields(); }
        });
        create.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { createAccount(); }
        });
        back.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new LoginFrame().setVisible(true);
                dispose();
            }
        });
        updateFields();
    }

    private void addRow(JPanel p, GridBagConstraints c, int y, String label, JComponent field) {
        addRow(p, c, y, new JLabel(label), field);
    }

    private void addRow(JPanel p, GridBagConstraints c, int y, JLabel label, JComponent field) {
        c.gridwidth = 1; c.gridx = 0; c.gridy = y;
        p.add(label, c);
        c.gridx = 1;
        p.add(field, c);
    }

    private void updateFields() {
        boolean donor = "DONOR".equals(String.valueOf(roleBox.getSelectedItem()));
        bloodLabel.setVisible(donor); bloodBox.setVisible(donor);
        dobLabel.setVisible(donor); dobField.setVisible(donor);
        cityLabel.setVisible(donor); cityField.setVisible(donor);
        revalidate(); 
        repaint();
    }

    private void createAccount() {
        String role = String.valueOf(roleBox.getSelectedItem());
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmField.getPassword());

        if (name.length() == 0 || email.length() == 0 || phone.length() == 0 ||
            password.length() < 6 || !password.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Fill all required fields. Password must be at least 6 characters and match confirmation.");
            return;
        }

        Connection con = null;
        PreparedStatement userPs = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);
            String status = "DONOR".equals(role) ? "ACTIVE" : "PENDING";
            String userSql = "INSERT INTO users(full_name, email, phone, password_hash, role, status) VALUES(?,?,?,?,?,?)";
            
            userPs = con.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS);
            userPs.setString(1, name);
            userPs.setString(2, email);
            userPs.setString(3, phone);
            userPs.setString(4, PasswordUtil.hash(password));
            userPs.setString(5, role);
            userPs.setString(6, status);
            userPs.executeUpdate();
            
            ResultSet keys = userPs.getGeneratedKeys();
            int userId = 0;
            if (keys.next()) {
                userId = keys.getInt(1);
            }
            keys.close();

            if ("DONOR".equals(role)) {
                String blood = String.valueOf(bloodBox.getSelectedItem());
                String dob = dobField.getText().trim();
                String city = cityField.getText().trim();
                if (city.length() == 0 || !isDate(dob)) {
                    con.rollback();
                    JOptionPane.showMessageDialog(this, "Enter city and date of birth in YYYY-MM-DD format.");
                    return;
                }
                String donorSql = "INSERT INTO donors(user_id, blood_group, date_of_birth, city, available) VALUES(?,?,?,?,1)";
                PreparedStatement donorPs = con.prepareStatement(donorSql);
                donorPs.setInt(1, userId);
                donorPs.setString(2, blood);
                donorPs.setDate(3, java.sql.Date.valueOf(dob));
                donorPs.setString(4, city);
                donorPs.executeUpdate();
                donorPs.close();
            } else if ("HOSPITAL".equals(role)) {
                String hospSql = "INSERT INTO hospitals(user_id, verified) VALUES(?, 0)";
                PreparedStatement hospPs = con.prepareStatement(hospSql);
                hospPs.setInt(1, userId);
                hospPs.executeUpdate();
                hospPs.close();
            }
            con.commit();
            JOptionPane.showMessageDialog(this, "Account created successfully!" + 
                ("HOSPITAL".equals(role) ? "\nHospital accounts require admin verification before login." : ""));
            new LoginFrame().setVisible(true);
            dispose();
        } catch (Exception ex) {
            try { if (con != null) con.rollback(); } catch (SQLException e) {}
            JOptionPane.showMessageDialog(this, "Error creating account:\n" + ex.getMessage());
        } finally {
            try { if (userPs != null) userPs.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
    }

    private boolean isDate(String dateStr) {
        try {
            java.sql.Date.valueOf(dateStr);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void styleButton(JButton b) {
        b.setBackground(new Color(132, 12, 35));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Arial", Font.BOLD, 13));
    }
}
