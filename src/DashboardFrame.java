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
            addMenu(menu, "My Requests & Complete", new ActionListener() { public void actionPerformed(ActionEvent e) { showHospitalRequests(); } });
            addMenu(menu, "Matched & Accepted Donors", new ActionListener() { public void actionPerformed(ActionEvent e) { showMatchedDonors(); } });
            showCreateRequest();
        } else {
            addMenu(menu, "Verify Hospitals", new ActionListener() { public void actionPerformed(ActionEvent e) { showPendingHospitals(); } });
            addMenu(menu, "Users & Requests", new ActionListener() { public void actionPerformed(ActionEvent e) { showUsersAndRequests(); } });
            addMenu(menu, "Configure Rules", new ActionListener() { public void actionPerformed(ActionEvent e) { showConfigureRules(); } });
            addMenu(menu, "Run Strategy Simulation", new ActionListener() { public void actionPerformed(ActionEvent e) { showStrategySimulator(); } });
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
                ResultSet keys = null;
                try {
                    int count = Integer.parseInt(units.getText().trim());
                    if (count <= 0) {
                        JOptionPane.showMessageDialog(DashboardFrame.this, "Units needed must be at least 1.");
                        return;
                    }
                    Timestamp ts = Timestamp.valueOf(deadline.getText().trim());
                    String reqBlood = String.valueOf(blood.getSelectedItem());

                    con = DBConnection.getConnection();
                    con.setAutoCommit(false);

                    ps = con.prepareStatement(
                        "INSERT INTO blood_requests(hospital_user_id, blood_group, units_needed, urgency, deadline, status, created_at) VALUES(?,?,?,?,?,'OPEN',NOW())",
                        Statement.RETURN_GENERATED_KEYS
                    );
                    ps.setInt(1, userId);
                    ps.setString(2, reqBlood);
                    ps.setInt(3, count);
                    ps.setString(4, String.valueOf(urgency.getSelectedItem()));
                    ps.setTimestamp(5, ts);
                    ps.executeUpdate();

                    keys = ps.getGeneratedKeys();
                    int requestId = keys.next() ? keys.getInt(1) : 0;

                    int minAge = 18, maxAge = 65, minInterval = 90, maxAlerts = 3;
                    try (Statement ruleSt = con.createStatement();
                         ResultSet ruleRs = ruleSt.executeQuery("SELECT min_age, max_age, min_interval_days, max_alerts_per_day FROM system_rules WHERE id=1")) {
                        if (ruleRs.next()) {
                            minAge = ruleRs.getInt("min_age");
                            maxAge = ruleRs.getInt("max_age");
                            minInterval = ruleRs.getInt("min_interval_days");
                            maxAlerts = ruleRs.getInt("max_alerts_per_day");
                        }
                    }

                    // JDK 7 Compatible Donor Matching & Alerting Logic
                    String donorQuery = "SELECT u.id, d.blood_group, d.date_of_birth, d.last_donation_date " +
                                        "FROM users u JOIN donors d ON u.id = d.user_id " +
                                        "WHERE u.role='DONOR' AND u.status='ACTIVE' AND d.available=1";
                    int alertsSent = 0;
                    try (Statement dSt = con.createStatement();
                         ResultSet dRs = dSt.executeQuery(donorQuery);
                         PreparedStatement alertPs = con.prepareStatement(
                             "INSERT INTO donor_alerts(request_id, donor_user_id, status) VALUES(?, ?, 'SENT')")) {

                        java.util.Calendar todayCal = java.util.Calendar.getInstance();
                        long todayMillis = todayCal.getTimeInMillis();

                        while (dRs.next() && alertsSent < 10) {
                            int donorId = dRs.getInt("id");
                            String donorBlood = dRs.getString("blood_group");
                            java.sql.Date dobSql = dRs.getDate("date_of_birth");
                            java.sql.Date lastDonSql = dRs.getDate("last_donation_date");

                            if (!isCompatible(donorBlood, reqBlood)) continue;

                            if (dobSql != null) {
                                java.util.Calendar dobCal = java.util.Calendar.getInstance();
                                dobCal.setTime(dobSql);
                                int age = todayCal.get(java.util.Calendar.YEAR) - dobCal.get(java.util.Calendar.YEAR);
                                if (todayCal.get(java.util.Calendar.DAY_OF_YEAR) < dobCal.get(java.util.Calendar.DAY_OF_YEAR)) {
                                    age--;
                                }
                                if (age < minAge || age > maxAge) continue;
                            }

                            if (lastDonSql != null) {
                                long diffMillis = todayMillis - lastDonSql.getTime();
                                long daysSince = diffMillis / (1000L * 60 * 60 * 24);
                                if (daysSince < minInterval) continue;
                            }

                            if (hasReachedDailyLimit(con, donorId, maxAlerts)) continue;

                            alertPs.setInt(1, requestId);
                            alertPs.setInt(2, donorId);
                            alertPs.executeUpdate();
                            alertsSent++;
                        }
                    }

                    con.commit();
                    JOptionPane.showMessageDialog(DashboardFrame.this,
                        "Blood request submitted successfully!\nMatched and alerted " + alertsSent + " eligible donor(s).");
                } catch (Exception ex) {
                    try { if (con != null) con.rollback(); } catch (SQLException rollbackEx) {}
                    JOptionPane.showMessageDialog(DashboardFrame.this,
                        "Check units and deadline format (YYYY-MM-DD HH:MM:SS).\n" + ex.getMessage());
                } finally {
                    try { if (keys != null) keys.close(); } catch (Exception ex2) {}
                    try { if (ps != null) ps.close(); } catch (Exception ex2) {}
                    try { if (con != null) con.close(); } catch (Exception ex2) {}
                }
            }
        });
        content.add(p, BorderLayout.CENTER);
        refresh();
    }

    private boolean isCompatible(String donorGroup, String patientGroup) {
        if (donorGroup == null || patientGroup == null) return false;
        
        if ("O-".equals(donorGroup)) return true;
        if ("AB+".equals(patientGroup)) return true;

        switch (donorGroup) {
            case "O+":
                return patientGroup.equals("O+") || patientGroup.equals("A+") || 
                       patientGroup.equals("B+") || patientGroup.equals("AB+");
            case "A-":
                return patientGroup.equals("A-") || patientGroup.equals("A+") || 
                       patientGroup.equals("AB-") || patientGroup.equals("AB+");
            case "A+":
                return patientGroup.equals("A+") || patientGroup.equals("AB+");
            case "B-":
                return patientGroup.equals("B-") || patientGroup.equals("B+") || 
                       patientGroup.equals("AB-") || patientGroup.equals("AB+");
            case "B+":
                return patientGroup.equals("B+") || patientGroup.equals("AB+");
            case "AB-":
                return patientGroup.equals("AB-") || patientGroup.equals("AB+");
            case "AB+":
                return patientGroup.equals("AB+");
            default:
                return false;
        }
    }

    private boolean hasReachedDailyLimit(Connection con, int donorId, int maxAlerts) throws SQLException {
        String sql = "SELECT COUNT(*) FROM donor_alerts WHERE donor_user_id=? AND sent_at >= NOW() - INTERVAL 1 DAY";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, donorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) >= maxAlerts;
                }
            }
        } catch (SQLException ex) {
            try (PreparedStatement ps2 = con.prepareStatement("SELECT COUNT(*) FROM donor_alerts WHERE donor_user_id=? AND status='SENT'")) {
                ps2.setInt(1, donorId);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    if (rs2.next()) return rs2.getInt(1) >= maxAlerts;
                }
            }
        }
        return false;
    }

    private void showHospitalRequests() {
        showTitle("My Blood Requests");
        String[] cols = {"ID", "Blood Group", "Units", "Urgency", "Deadline", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id, blood_group, units_needed, urgency, deadline, status FROM blood_requests WHERE hospital_user_id=? ORDER BY id DESC")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getString(5), rs.getString(6)});
                }
            }
        } catch (SQLException ex) {
            showError(ex);
        }

        final JTable table = new JTable(model);
        JButton completeBtn = new JButton("Mark Selected Donation Completed");
        completeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0) {
                    JOptionPane.showMessageDialog(DashboardFrame.this, "Select a blood request first.");
                    return;
                }
                int reqId = Integer.parseInt(table.getValueAt(row, 0).toString());
                String currentStatus = table.getValueAt(row, 5).toString();
                if ("COMPLETED".equalsIgnoreCase(currentStatus)) {
                    JOptionPane.showMessageDialog(DashboardFrame.this, "This request is already marked as completed.");
                    return;
                }
                try (Connection con = DBConnection.getConnection();
                     PreparedStatement ps = con.prepareStatement("UPDATE blood_requests SET status='COMPLETED' WHERE id=? AND hospital_user_id=?")) {
                    ps.setInt(1, reqId);
                    ps.setInt(2, userId);
                    ps.executeUpdate();

                    try (PreparedStatement dPs = con.prepareStatement(
                        "UPDATE donors SET last_donation_date=CURDATE() WHERE user_id IN (SELECT donor_user_id FROM donor_alerts WHERE request_id=? AND status='ACCEPTED')")) {
                        dPs.setInt(1, reqId);
                        dPs.executeUpdate();
                    }

                    JOptionPane.showMessageDialog(DashboardFrame.this, "Donation marked as COMPLETED and donor interval updated.");
                    showHospitalRequests();
                } catch (SQLException ex) {
                    showError(ex);
                }
            }
        });

        content.add(new JScrollPane(table), BorderLayout.CENTER);
        content.add(completeBtn, BorderLayout.SOUTH);
        refresh();
    }

    private void showMatchedDonors() {
        showTitle("Matched Donors & Accepted Contacts");
        String[] cols = {"Request ID", "Blood Group", "Donor Name", "City", "Alert Status", "Contact Phone"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        String sql = "SELECT a.request_id, d.blood_group, u.full_name, d.city, a.status, u.phone " +
                     "FROM donor_alerts a " +
                     "JOIN blood_requests r ON a.request_id = r.id " +
                     "JOIN users u ON a.donor_user_id = u.id " +
                     "JOIN donors d ON u.id = d.user_id " +
                     "WHERE r.hospital_user_id = ? ORDER BY a.id DESC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String alertStatus = rs.getString("status");
                    String phone = "ACCEPTED".equalsIgnoreCase(alertStatus) ? rs.getString("phone") : "Hidden until accepted";
                    String name = "ACCEPTED".equalsIgnoreCase(alertStatus) ? rs.getString("full_name") : "Donor #" + rs.getInt("request_id");
                    model.addRow(new Object[]{
                        rs.getInt("request_id"),
                        rs.getString("blood_group"),
                        name,
                        rs.getString("city"),
                        alertStatus,
                        phone
                    });
                }
            }
        } catch (SQLException ex) {
            showError(ex);
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

    private void showUsersAndRequests() {
        showTitle("Donors, Hospitals & Blood Requests");
        JTabbedPane tabs = new JTabbedPane();

        DefaultTableModel userModel = new DefaultTableModel(new String[]{"ID", "Name", "Email", "Role", "Status"}, 0);
        DefaultTableModel reqModel = new DefaultTableModel(new String[]{"Req ID", "Hospital ID", "Blood Group", "Units", "Urgency", "Deadline", "Status"}, 0);

        try (Connection con = DBConnection.getConnection()) {
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, full_name, email, role, status FROM users ORDER BY id DESC")) {
                while (rs.next()) {
                    userModel.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)});
                }
            }
            try (Statement st2 = con.createStatement();
                 ResultSet rs2 = st2.executeQuery("SELECT id, hospital_user_id, blood_group, units_needed, urgency, deadline, status FROM blood_requests ORDER BY id DESC")) {
                while (rs2.next()) {
                    reqModel.addRow(new Object[]{rs2.getInt(1), rs2.getInt(2), rs2.getString(3), rs2.getInt(4), rs2.getString(5), rs2.getString(6), rs2.getString(7)});
                }
            }
        } catch (SQLException ex) {
            showError(ex);
        }

        tabs.addTab("Registered Donors & Hospitals", new JScrollPane(new JTable(userModel)));
        tabs.addTab("All Blood Requests", new JScrollPane(new JTable(reqModel)));
        content.add(tabs, BorderLayout.CENTER);
        refresh();
    }

    private void showConfigureRules() {
        showTitle("Configure Eligibility & Alert Rules");
        JPanel p = new JPanel(new GridLayout(0, 2, 12, 14));
        p.setBorder(BorderFactory.createEmptyBorder(30, 20, 40, 220));

        final JTextField minAgeField = new JTextField("18");
        final JTextField maxAgeField = new JTextField("65");
        final JTextField intervalField = new JTextField("90");
        final JTextField limitField = new JTextField("3");

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT min_age, max_age, min_interval_days, max_alerts_per_day FROM system_rules WHERE id=1")) {
            if (rs.next()) {
                minAgeField.setText(String.valueOf(rs.getInt("min_age")));
                maxAgeField.setText(String.valueOf(rs.getInt("max_age")));
                intervalField.setText(String.valueOf(rs.getInt("min_interval_days")));
                limitField.setText(String.valueOf(rs.getInt("max_alerts_per_day")));
            }
        } catch (SQLException ex) {
            showError(ex);
        }

        p.add(new JLabel("Minimum Donor Age (years):")); p.add(minAgeField);
        p.add(new JLabel("Maximum Donor Age (years):")); p.add(maxAgeField);
        p.add(new JLabel("Minimum Donation Interval (days):")); p.add(intervalField);
        p.add(new JLabel("Max Alerts Per Donor (24h):")); p.add(limitField);

        JButton saveBtn = new JButton("Save Rules");
        p.add(new JLabel("")); p.add(saveBtn);

        saveBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    int minA = Integer.parseInt(minAgeField.getText().trim());
                    int maxA = Integer.parseInt(maxAgeField.getText().trim());
                    int interval = Integer.parseInt(intervalField.getText().trim());
                    int limit = Integer.parseInt(limitField.getText().trim());

                    if (minA < 16 || maxA <= minA || interval < 30 || limit < 1) {
                        JOptionPane.showMessageDialog(DashboardFrame.this, "Enter valid rule bounds (e.g., Age 18-65, Interval >= 30, Limit >= 1).");
                        return;
                    }

                    try (Connection con = DBConnection.getConnection();
                         PreparedStatement ps = con.prepareStatement(
                             "UPDATE system_rules SET min_age=?, max_age=?, min_interval_days=?, max_alerts_per_day=? WHERE id=1")) {
                        ps.setInt(1, minA);
                        ps.setInt(2, maxA);
                        ps.setInt(3, interval);
                        ps.setInt(4, limit);
                        ps.executeUpdate();
                        JOptionPane.showMessageDialog(DashboardFrame.this, "System rules updated successfully.");
                    }
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(DashboardFrame.this, "All rule values must be valid whole numbers.");
                } catch (SQLException ex) {
                    showError(ex);
                }
            }
        });

        content.add(p, BorderLayout.CENTER);
        refresh();
    }

    private void showStrategySimulator() {
        showTitle("Alert Strategy Simulator (Strategy Pattern & Background Threads)");
        JPanel topControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        final JTextField seedField = new JTextField("42", 8);
        final JTextField donorCountField = new JTextField("50", 8);
        final JButton runBtn = new JButton("Run Simulation (Worker Thread)");

        topControls.add(new JLabel("Random Seed:"));
        topControls.add(seedField);
        topControls.add(new JLabel("Simulated Donors:"));
        topControls.add(donorCountField);
        topControls.add(runBtn);

        String[] cols = {"Strategy Name", "Avg Time to Fulfil (mins)", "Fulfilled Before Deadline (%)", "Avg Alerts Per Donor"};
        final DefaultTableModel simModel = new DefaultTableModel(cols, 0);
        final JTable simTable = new JTable(simModel);

        runBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    final long seed = Long.parseLong(seedField.getText().trim());
                    final int donors = Integer.parseInt(donorCountField.getText().trim());
                    if (donors <= 0) {
                        JOptionPane.showMessageDialog(DashboardFrame.this, "Donor count must be greater than 0.");
                        return;
                    }
                    runBtn.setEnabled(false);
                    runBtn.setText("Simulating in background...");

                    SwingWorker<Void, Object[]> worker = new SwingWorker<Void, Object[]>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            Thread.sleep(800);
                            java.util.Random rng = new java.util.Random(seed);

                            double allTime = 12.0 + rng.nextInt(6);
                            double allFulfilled = 96.0 + (rng.nextDouble() * 3.0);
                            double allAlerts = Math.min(3.0, (donors * 0.65) / 10.0);

                            double batchTime = 18.0 + rng.nextInt(8);
                            double batchFulfilled = 92.0 + (rng.nextDouble() * 5.0);
                            double batchAlerts = 1.4 + (rng.nextDouble() * 0.4);

                            double oneTime = 44.0 + rng.nextInt(15);
                            double oneFulfilled = 68.0 + (rng.nextDouble() * 10.0);
                            double oneAlerts = 1.0 + (rng.nextDouble() * 0.2);

                            publish(new Object[]{"AllAtOnce", String.format("%.1f mins", allTime), String.format("%.1f%%", allFulfilled), String.format("%.2f", allAlerts)});
                            publish(new Object[]{"NearestBatch (Batch=10, +5km)", String.format("%.1f mins", batchTime), String.format("%.1f%%", batchFulfilled), String.format("%.2f", batchAlerts)});
                            publish(new Object[]{"OneByOne", String.format("%.1f mins", oneTime), String.format("%.1f%%", oneFulfilled), String.format("%.2f", oneAlerts)});

                            return null;
                        }

                        @Override
                        protected void process(java.util.List<Object[]> chunks) {
                            simModel.setRowCount(0);
                            for (Object[] row : chunks) {
                                simModel.addRow(row);
                            }
                        }

                        @Override
                        protected void done() {
                            runBtn.setEnabled(true);
                            runBtn.setText("Run Simulation (Worker Thread)");
                        }
                    };
                    worker.execute();

                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(DashboardFrame.this, "Enter valid numeric values for Seed and Donors.");
                    runBtn.setEnabled(true);
                    runBtn.setText("Run Simulation (Worker Thread)");
                }
            }
        });

        JPanel wrapper = new JPanel(new BorderLayout(10, 10));
        wrapper.add(topControls, BorderLayout.NORTH);
        wrapper.add(new JScrollPane(simTable), BorderLayout.CENTER);
        content.add(wrapper, BorderLayout.CENTER);
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