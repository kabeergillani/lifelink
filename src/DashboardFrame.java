package lifelink;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class DashboardFrame extends JFrame {
    private int userId;
    private String userName;
    private String role;
    private JPanel content;

    public DashboardFrame(int userId, String userName, String role) {
        this.userId = userId;
        this.userName = userName;
        this.role = role;
        setTitle("LifeLink - " + role + " Dashboard");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(new Color(132, 12, 35));
        JLabel welcome = new JLabel("   LIFELINK      Welcome, " + userName + " (" + role + ")");
        welcome.setForeground(Color.WHITE);
        welcome.setFont(new Font("Arial", Font.BOLD, 17));
        top.add(welcome, BorderLayout.CENTER);
        JButton logout = new JButton("Logout");
        top.add(logout, BorderLayout.EAST);
        root.add(top, BorderLayout.NORTH);

        JPanel menu = new JPanel(new GridLayout(0, 1, 5, 5));
        menu.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        menu.setPreferredSize(new Dimension(190, 500));
        content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        root.add(menu, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        add(root);

        if ("DONOR".equals(role)) {
            addMenu(menu, "My Profile", new ActionListener() { public void actionPerformed(ActionEvent e) { showDonorProfile(); } });
            addMenu(menu, "My Blood Alerts", new ActionListener() { public void actionPerformed(ActionEvent e) { showDonorAlerts(); } });
            addMenu(menu, "Availability", new ActionListener() { public void actionPerformed(ActionEvent e) { updateAvailability(); } });
            showDonorProfile();
        } else if ("HOSPITAL".equals(role)) {
            addMenu(menu, "Create Blood Request", new ActionListener() { public void actionPerformed(ActionEvent e) { showCreateRequest(); } });
            addMenu(menu, "My Requests", new ActionListener() { public void actionPerformed(ActionEvent e) { showHospitalRequests(); } });
            showCreateRequest();
        } else {
            addMenu(menu, "Verify Hospitals", new ActionListener() { public void actionPerformed(ActionEvent e) { showPendingHospitals(); } });
            addMenu(menu, "All Users", new ActionListener() { public void actionPerformed(ActionEvent e) { showUsers(); } });
            showPendingHospitals();
        }
        logout.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new LoginFrame().setVisible(true);
                dispose();
            }
        });
    }

    private void addMenu(JPanel menu, String text, ActionListener listener) {
        JButton b = new JButton(text);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        menu.add(b);
        b.addActionListener(listener);
    }

    private void showTitle(String title) {
        content.removeAll();
        JLabel label = new JLabel(title);
        label.setFont(new Font("Arial", Font.BOLD, 22));
        content.add(label, BorderLayout.NORTH);
    }

    private void showDonorProfile() {
        showTitle("Donor Profile");
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DBConnection.getConnection();
            String sql = "SELECT u.full_name, u.email, u.phone, d.blood_group, d.date_of_birth, d.city, d.last_donation_date, d.available FROM users u LEFT JOIN donors d ON u.id=d.user_id WHERE u.id=?";
            ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            if (rs.next()) {
                JPanel p = new JPanel(new GridLayout(0, 2, 12, 12));
                p.setBorder(BorderFactory.createEmptyBorder(30, 10, 10, 250));
                addValue(p, "Name", rs.getString("full_name"));
                addValue(p, "Email", rs.getString("email"));
                addValue(p, "Phone", rs.getString("phone"));
                addValue(p, "Blood group", rs.getString("blood_group"));
                addValue(p, "Date of birth", rs.getString("date_of_birth"));
                addValue(p, "City", rs.getString("city"));
                String lastDonation = rs.getString("last_donation_date");
                addValue(p, "Last donation", lastDonation == null ? "Not provided" : lastDonation);
                addValue(p, "Available", rs.getInt("available") == 1 ? "Yes" : "No");
                content.add(p, BorderLayout.CENTER);
            } else {
                JLabel errorMsg = new JLabel("Donor profile details not found.");
                errorMsg.setForeground(Color.RED);
                content.add(errorMsg, BorderLayout.CENTER);
            }
        } catch (SQLException ex) { 
            showError(ex); 
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
        refresh();
    }

    private void addValue(JPanel p, String label, String value) {
        p.add(new JLabel(label + ":"));
        p.add(new JLabel(value == null ? "N/A" : value));
    }

    private void showDonorAlerts() {
        showTitle("My Blood Alerts");
        String[] cols = {"Alert ID", "Request ID", "Blood Group", "Units", "Urgency", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DBConnection.getConnection();
            String sql = "SELECT a.id, a.request_id, r.blood_group, r.units_needed, r.urgency, a.status FROM donor_alerts a JOIN blood_requests r ON a.request_id=r.id WHERE a.donor_user_id=? ORDER BY a.id DESC";
            ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[] {rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getInt(4), rs.getString(5), rs.getString(6)});
            }
        } catch (SQLException ex) { 
            showError(ex); 
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
        final JTable table = new JTable(model);
        JPanel buttons = new JPanel();
        JButton accept = new JButton("Accept Selected");
        JButton decline = new JButton("Decline Selected");
        buttons.add(accept); 
        buttons.add(decline);
        accept.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { respondToAlert(table, "ACCEPTED"); } });
        decline.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { respondToAlert(table, "DECLINED"); } });
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        refresh();
    }

    private void respondToAlert(JTable table, String status) {
        int row = table.getSelectedRow();
        if (row < 0) { 
            JOptionPane.showMessageDialog(this, "Select an alert first."); 
            return; 
        }
        int alertId = Integer.parseInt(table.getValueAt(row, 0).toString());
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement("UPDATE donor_alerts SET status=?, responded_at=NOW() WHERE id=? AND donor_user_id=? AND status='SENT'");
            ps.setString(1, status); 
            ps.setInt(2, alertId); 
            ps.setInt(3, userId);
            int changed = ps.executeUpdate();
            JOptionPane.showMessageDialog(this, changed > 0 ? "Your response has been saved." : "This alert has already been answered.");
            showDonorAlerts();
        } catch (SQLException ex) { 
            showError(ex); 
        } finally {
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
    }

    private void updateAvailability() {
        Object[] options = {"Available", "Unavailable"};
        int choice = JOptionPane.showOptionDialog(this, "Choose your donation availability:", "Availability",
            JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (choice < 0) return;
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement("UPDATE donors SET available=? WHERE user_id=?");
            ps.setInt(1, choice == 0 ? 1 : 0); 
            ps.setInt(2, userId); 
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Availability updated.");
            showDonorProfile();
        } catch (SQLException ex) { 
            showError(ex); 
        } finally {
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
    }

    private void showCreateRequest() {
        showTitle("Create Blood Request");
        JPanel p = new JPanel(new GridLayout(0, 2, 10, 12));
        p.setBorder(BorderFactory.createEmptyBorder(30, 20, 20, 250));
        
        final JComboBox blood = new JComboBox(new String[] {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"});
        final JTextField units = new JTextField("1");
        final JComboBox urgency = new JComboBox(new String[] {"Critical", "Urgent", "Normal"});
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentDateTime = sdf.format(new java.util.Date(System.currentTimeMillis() + 86400000L));
        final JTextField deadline = new JTextField(currentDateTime);

        p.add(new JLabel("Required blood group")); p.add(blood);
        p.add(new JLabel("Units needed")); p.add(units);
        p.add(new JLabel("Urgency")); p.add(urgency);
        p.add(new JLabel("Deadline (YYYY-MM-DD HH:MM:SS)")); p.add(deadline);
        JButton submit = new JButton("Submit Request");
        p.add(new JLabel("")); p.add(submit);
        
        submit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Connection con = null;
                PreparedStatement ps = null;
                try {
                    int count = Integer.parseInt(units.getText().trim());
                    Timestamp ts = Timestamp.valueOf(deadline.getText().trim());
                    
                    con = DBConnection.getConnection();
                    ps = con.prepareStatement("INSERT INTO blood_requests(hospital_user_id, blood_group, units_needed, urgency, deadline, status, created_at) VALUES(?,?,?,?,?,'OPEN',NOW())");
                    ps.setInt(1, userId); 
                    ps.setString(2, String.valueOf(blood.getSelectedItem()));
                    ps.setInt(3, count); 
                    ps.setString(4, String.valueOf(urgency.getSelectedItem())); 
                    ps.setTimestamp(5, ts);
                    ps.executeUpdate();
                    JOptionPane.showMessageDialog(DashboardFrame.this, "Blood request submitted successfully.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DashboardFrame.this, "Check units and deadline format (YYYY-MM-DD HH:MM:SS).\n" + ex.getMessage());
                } finally {
                    try { if (ps != null) ps.close(); } catch (Exception ex2) {}
                    try { if (con != null) con.close(); } catch (Exception ex2) {}
                }
            }
        });
        content.add(p, BorderLayout.CENTER);
        refresh();
    }

    private void showHospitalRequests() {
        showTitle("My Blood Requests");
        String[] cols = {"ID", "Blood Group", "Units", "Urgency", "Deadline", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement("SELECT id, blood_group, units_needed, urgency, deadline, status FROM blood_requests WHERE hospital_user_id=? ORDER BY id DESC");
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[] {rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getString(5), rs.getString(6)});
            }
        } catch (SQLException ex) { 
            showError(ex); 
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
        content.add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);
        refresh();
    }

    private void showPendingHospitals() {
        showTitle("Hospital Verification");
        String[] cols = {"User ID", "Hospital Name", "Email", "Phone", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DBConnection.getConnection();
            String sql = "SELECT u.id, u.full_name, u.email, u.phone, u.status FROM users u JOIN hospitals h ON u.id=h.user_id WHERE u.role='HOSPITAL' AND u.status='PENDING'";
            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[] {rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)});
            }
        } catch (SQLException ex) { 
            showError(ex); 
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
        final JTable table = new JTable(model);
        JButton verify = new JButton("Verify Selected Hospital");
        verify.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0) { 
                    JOptionPane.showMessageDialog(DashboardFrame.this, "Select a hospital."); 
                    return; 
                }
                int id = Integer.parseInt(table.getValueAt(row, 0).toString());
                Connection innerCon = null;
                PreparedStatement innerPs = null;
                PreparedStatement innerHp = null;
                try {
                    innerCon = DBConnection.getConnection();
                    innerPs = innerCon.prepareStatement("UPDATE users SET status='ACTIVE' WHERE id=? AND role='HOSPITAL'");
                    innerPs.setInt(1, id); 
                    innerPs.executeUpdate();
                    
                    innerHp = innerCon.prepareStatement("UPDATE hospitals SET verified=1 WHERE user_id=?");
                    innerHp.setInt(1, id); 
                    innerHp.executeUpdate();
                    
                    JOptionPane.showMessageDialog(DashboardFrame.this, "Hospital verified successfully.");
                    showPendingHospitals();
                } catch (SQLException ex) { 
                    showError(ex); 
                } finally {
                    try { if (innerPs != null) innerPs.close(); } catch (Exception ex2) {}
                    try { if (innerHp != null) innerHp.close(); } catch (Exception ex2) {}
                    try { if (innerCon != null) innerCon.close(); } catch (Exception ex2) {}
                }
            }
        });
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        content.add(verify, BorderLayout.SOUTH);
        refresh();
    }

    private void showUsers() {
        showTitle("Registered Users");
        String[] cols = {"ID", "Name", "Email", "Role", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            con = DBConnection.getConnection();
            st = con.createStatement();
            rs = st.executeQuery("SELECT id, full_name, email, role, status FROM users ORDER BY id DESC");
            while (rs.next()) {
                model.addRow(new Object[] {rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)});
            }
        } catch (SQLException ex) { 
            showError(ex); 
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (st != null) st.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
        content.add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);
        refresh();
    }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    private void refresh() {
        content.revalidate();
        content.repaint();
    }
}
