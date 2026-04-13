package vehiclemaintenance;
 
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.sql.*;
import java.time.LocalDate;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
 
/**
 * DashboardFrame.java
 * Same JFrame design — MaintenanceDAO logic merged inside.
 * Uses LoginFrame.session* for session data (no SessionManager file needed).
 */
public class DashboardFrame extends javax.swing.JFrame {
 
    private static final Logger logger = Logger.getLogger(DashboardFrame.class.getName());
 
    private DefaultTableModel tableModel;
    private DefaultTableModel recentModel;
    private int selectedId = -1;
 
    // ── MAINTENANCE DAO (replaces MaintenanceDAO) ──────────────────
 
    private boolean dbAddRecord(String date, String vehicle, String serviceType,
            double cost, String notes) {
        String sql = "INSERT INTO maintenance_records (user_id, date, vehicle, service_type, cost, notes) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, LoginFrame.sessionUserId);
            ps.setString(2, date);
            ps.setString(3, vehicle);
            ps.setString(4, serviceType);
            ps.setDouble(5, cost);
            ps.setString(6, notes);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("AddRecord error: " + e.getMessage());
        }
        return false;
    }
 
    private DefaultTableModel dbGetAllRecords() {
        String[] columns = {"ID", "Date", "Vehicle", "Service Type", "Cost", "Notes"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        String sql = "SELECT id, date, vehicle, service_type, cost, notes "
                   + "FROM maintenance_records WHERE user_id = ? ORDER BY date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, LoginFrame.sessionUserId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("date"),
                    rs.getString("vehicle"),
                    rs.getString("service_type"),
                    String.format("%.2f", rs.getDouble("cost")),
                    rs.getString("notes")
                });
            }
        } catch (SQLException e) {
            System.err.println("GetAllRecords error: " + e.getMessage());
        }
        return model;
    }
 
    private boolean dbUpdateRecord(int id, String date, String vehicle,
            String serviceType, double cost, String notes) {
        String sql = "UPDATE maintenance_records SET date=?, vehicle=?, service_type=?, cost=?, notes=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date);
            ps.setString(2, vehicle);
            ps.setString(3, serviceType);
            ps.setDouble(4, cost);
            ps.setString(5, notes);
            ps.setInt(6, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("UpdateRecord error: " + e.getMessage());
        }
        return false;
    }
 
    private boolean dbDeleteRecord(int id) {
        String sql = "DELETE FROM maintenance_records WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("DeleteRecord error: " + e.getMessage());
        }
        return false;
    }
 
    private double dbGetTotalCost() {
        String sql = "SELECT SUM(cost) AS total FROM maintenance_records WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, LoginFrame.sessionUserId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble("total");
        } catch (SQLException e) {
            System.err.println("GetTotalCost error: " + e.getMessage());
        }
        return 0.0;
    }
 
    private int dbGetTotalRecords() {
        String sql = "SELECT COUNT(*) AS total FROM maintenance_records WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, LoginFrame.sessionUserId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("total");
        } catch (SQLException e) {
            System.err.println("GetTotalRecords error: " + e.getMessage());
        }
        return 0;
    }
 
    private String dbGetLatestActivity() {
        String sql = "SELECT vehicle, service_type, date FROM maintenance_records "
                   + "WHERE user_id = ? ORDER BY date DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, LoginFrame.sessionUserId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("vehicle") + " - "
                        + rs.getString("service_type")
                        + " (" + rs.getString("date") + ")";
            }
        } catch (SQLException e) {
            System.err.println("GetLatestActivity error: " + e.getMessage());
        }
        return "No records yet";
    }
 
    private DefaultTableModel dbSearchRecords(String keyword) {
        String[] columns = {"ID", "Date", "Vehicle", "Service Type", "Cost", "Notes"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        String sql = "SELECT id, date, vehicle, service_type, cost, notes FROM maintenance_records "
                   + "WHERE user_id = ? AND (vehicle LIKE ? OR service_type LIKE ?) ORDER BY date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, LoginFrame.sessionUserId);
            ps.setString(2, "%" + keyword + "%");
            ps.setString(3, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("date"),
                    rs.getString("vehicle"),
                    rs.getString("service_type"),
                    String.format("%.2f", rs.getDouble("cost")),
                    rs.getString("notes")
                });
            }
        } catch (SQLException e) {
            System.err.println("SearchRecords error: " + e.getMessage());
        }
        return model;
    }
 
    // ── CONSTRUCTOR ────────────────────────────────────────────────
    public DashboardFrame() {
        initComponents();
 
        setTitle("Vehicle Maintenance Log - Dashboard  |  User: "
                + LoginFrame.sessionFullName);
        lblWelcome.setText("  Welcome, " + LoginFrame.sessionFullName
                + "!  |  Vehicle Maintenance Log System");
 
        String[] cols = {"ID", "Date", "Vehicle", "Service Type", "Cost", "Notes"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        jTable1.setModel(tableModel);
 
        jTable1.getColumnModel().getColumn(0).setPreferredWidth(35);
        jTable1.getColumnModel().getColumn(1).setPreferredWidth(82);
        jTable1.getColumnModel().getColumn(2).setPreferredWidth(100);
        jTable1.getColumnModel().getColumn(3).setPreferredWidth(110);
        jTable1.getColumnModel().getColumn(4).setPreferredWidth(80);
        jTable1.getColumnModel().getColumn(5).setPreferredWidth(120);
 
        jTable1.getTableHeader().setBackground(new Color(0, 102, 204));
        jTable1.getTableHeader().setForeground(Color.WHITE);
        jTable1.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
 
        jTable1.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (isSelected) setBackground(new Color(184, 207, 229));
                else if (row % 2 == 0) setBackground(Color.WHITE);
                else setBackground(new Color(232, 240, 255));
                return this;
            }
        });
 
        jTable1.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) onRowSelected();
            }
        });
 
        String[] recCols = {"Date", "Vehicle", "Service Type", "Cost (PHP)"};
        recentModel = new DefaultTableModel(recCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblRecent.setModel(recentModel);
        tblRecent.getTableHeader().setBackground(new Color(0, 102, 204));
        tblRecent.getTableHeader().setForeground(Color.WHITE);
        tblRecent.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
 
        refreshDashboard();
        loadRecords();
        loadRecentTable(recentModel);
    }
 
    // ── LOGIC METHODS ──────────────────────────────────────────────
 
    private void refreshDashboard() {
        double totalCost = dbGetTotalCost();
        int totalRec = dbGetTotalRecords();
        String latest = dbGetLatestActivity();
 
        if (lblCostTitle != null)
            lblCostTitle.setText("PHP " + String.format("%.2f", totalCost));
        if (lblTotalRecords1 != null)
            lblTotalRecords1.setText(totalRec + " records");
        if (lblLatest != null)
            lblLatest.setText("<html><center>" + latest + "</center></html>");
    }
 
    private void loadRecords() {
        tableModel.setRowCount(0);
        DefaultTableModel data = dbGetAllRecords();
        for (int i = 0; i < data.getRowCount(); i++) {
            tableModel.addRow(new Object[]{
                data.getValueAt(i, 0),
                data.getValueAt(i, 1),
                data.getValueAt(i, 2),
                data.getValueAt(i, 3),
                "PHP " + data.getValueAt(i, 4),
                data.getValueAt(i, 5)
            });
        }
        selectedId = -1;
    }
 
    private void loadRecentTable(DefaultTableModel model) {
        model.setRowCount(0);
        DefaultTableModel data = dbGetAllRecords();
        int count = Math.min(data.getRowCount(), 8);
        for (int i = 0; i < count; i++) {
            model.addRow(new Object[]{
                data.getValueAt(i, 1),
                data.getValueAt(i, 2),
                data.getValueAt(i, 3),
                "PHP " + data.getValueAt(i, 4)
            });
        }
    }
 
    private void onRowSelected() {
        int row = jTable1.getSelectedRow();
        if (row == -1) return;
 
        selectedId = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
        txtDate.setText(tableModel.getValueAt(row, 1).toString());
        txtVehicle.setText(tableModel.getValueAt(row, 2).toString());
        cmbService.setSelectedItem(tableModel.getValueAt(row, 3).toString());
 
        String cost = tableModel.getValueAt(row, 4).toString().replace("PHP ", "").trim();
        txtCost.setText(cost);
 
        Object notes = tableModel.getValueAt(row, 5);
        txtNotes.setText(notes != null ? notes.toString() : "");
    }
 
    private void doAdd() {
        if (!validateForm()) return;
        boolean ok = dbAddRecord(
                txtDate.getText().trim(),
                txtVehicle.getText().trim(),
                cmbService.getSelectedItem().toString(),
                Double.parseDouble(txtCost.getText().trim()),
                txtNotes.getText().trim());
        if (ok) {
            JOptionPane.showMessageDialog(this, "Record added successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            clearForm(); loadRecords(); refreshDashboard();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to add record.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
 
    private void doUpdate() {
        if (selectedId == -1) {
            JOptionPane.showMessageDialog(this, "Please click a row in the table first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!validateForm()) return;
        boolean ok = dbUpdateRecord(
                selectedId,
                txtDate.getText().trim(),
                txtVehicle.getText().trim(),
                cmbService.getSelectedItem().toString(),
                Double.parseDouble(txtCost.getText().trim()),
                txtNotes.getText().trim());
        if (ok) {
            JOptionPane.showMessageDialog(this, "Record updated successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            clearForm(); loadRecords(); refreshDashboard();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update record.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
 
    private void doDelete() {
        if (selectedId == -1) {
            JOptionPane.showMessageDialog(this, "Please click a row first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this record?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            boolean ok = dbDeleteRecord(selectedId);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Record deleted successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                clearForm(); loadRecords(); refreshDashboard();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete record.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
 
    private void doSearch() {
        String keyword = txtSearch.getText().trim();
        if (keyword.equals("")) { loadRecords(); return; }
        tableModel.setRowCount(0);
        DefaultTableModel data = dbSearchRecords(keyword);
        for (int i = 0; i < data.getRowCount(); i++) {
            tableModel.addRow(new Object[]{
                data.getValueAt(i, 0),
                data.getValueAt(i, 1),
                data.getValueAt(i, 2),
                data.getValueAt(i, 3),
                "PHP " + data.getValueAt(i, 4),
                data.getValueAt(i, 5)
            });
        }
    }
 
    private void clearForm() {
        txtDate.setText(LocalDate.now().toString());
        txtVehicle.setText("");
        cmbService.setSelectedIndex(0);
        txtCost.setText("");
        txtNotes.setText("");
        selectedId = -1;
        jTable1.clearSelection();
    }
 
    private boolean validateForm() {
        if (txtDate.getText().trim().equals("")) {
            JOptionPane.showMessageDialog(this, "Please enter a date.",
                    "Missing Field", JOptionPane.WARNING_MESSAGE); return false; }
        if (!txtDate.getText().trim().matches("\\d{4}-\\d{2}-\\d{2}")) {
            JOptionPane.showMessageDialog(this,
                    "Date format must be YYYY-MM-DD\nExample: 2026-04-13",
                    "Invalid Date", JOptionPane.WARNING_MESSAGE); return false; }
        if (txtVehicle.getText().trim().equals("")) {
            JOptionPane.showMessageDialog(this, "Please enter the vehicle name.",
                    "Missing Field", JOptionPane.WARNING_MESSAGE); return false; }
        if (txtCost.getText().trim().equals("")) {
            JOptionPane.showMessageDialog(this, "Please enter the cost.",
                    "Missing Field", JOptionPane.WARNING_MESSAGE); return false; }
        try {
            double cost = Double.parseDouble(txtCost.getText().trim());
            if (cost < 0) {
                JOptionPane.showMessageDialog(this, "Cost cannot be negative.",
                        "Invalid", JOptionPane.WARNING_MESSAGE); return false; }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Cost must be a number.\nExample: 350.00",
                    "Invalid Cost", JOptionPane.WARNING_MESSAGE); return false; }
        return true;
    }
 
    private void doLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?",
                "Logout", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            LoginFrame.sessionLogout();
            new LoginFrame().setVisible(true);
            dispose();
        }
    }
 
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel3 = new javax.swing.JPanel();
        lblWelcome = new javax.swing.JLabel();
        btnLogout = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        lblRecent = new javax.swing.JPanel();
        tabs = new javax.swing.JTabbedPane();
        lblHead = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        boxCost = new javax.swing.JPanel();
        lblCostTitle = new javax.swing.JLabel();
        lblCostTitle2 = new javax.swing.JLabel();
        boxRec = new javax.swing.JPanel();
        lblRecTitle1 = new javax.swing.JLabel();
        lblTotalRecords1 = new javax.swing.JLabel();
        boxLatest = new javax.swing.JPanel();
        lblLatestTitle = new javax.swing.JLabel();
        lblLatest = new javax.swing.JLabel();
        btnRefresh = new javax.swing.JButton();
        lblHead1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblRecent = new javax.swing.JTable();
        formPanel = new javax.swing.JTabbedPane();
        jPanel2 = new javax.swing.JPanel();
        txtVehicle = new javax.swing.JTextField();
        lDate1 = new javax.swing.JLabel();
        lVeh = new javax.swing.JLabel();
        txtNotes = new javax.swing.JTextField();
        cmbService = new javax.swing.JComboBox<>();
        lSvc = new javax.swing.JLabel();
        txtDate = new javax.swing.JTextField();
        lCost = new javax.swing.JLabel();
        txtCost = new javax.swing.JTextField();
        lNotes1 = new javax.swing.JLabel();
        btnClear = new javax.swing.JButton();
        btnAdd = new javax.swing.JButton();
        btnUpdate = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();
        tablePanel = new javax.swing.JPanel();
        txtSearch = new javax.swing.JTextField();
        btnSearch = new javax.swing.JButton();
        btnShowAll = new javax.swing.JButton();
        tblRecords = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel3.setBackground(new java.awt.Color(0, 0, 0));
        jPanel3.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        lblWelcome.setBackground(new java.awt.Color(0, 0, 0));
        lblWelcome.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblWelcome.setForeground(new java.awt.Color(255, 255, 255));
        lblWelcome.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblWelcome.setText("Welcome, User!  |  Vehicle Maintenance Log System");

        btnLogout.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnLogout.setText("Logout");
        btnLogout.addActionListener(this::btnLogoutActionPerformed);

        jLabel4.setBackground(new java.awt.Color(0, 0, 0));
        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText("Dashboard");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(lblWelcome)
                        .addGap(0, 217, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnLogout)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnLogout))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblWelcome, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(12, Short.MAX_VALUE))
        );

        getContentPane().add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        lblRecent.setBackground(new java.awt.Color(255, 255, 255));
        lblRecent.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        tabs.setBackground(new java.awt.Color(255, 255, 255));
        tabs.setForeground(new java.awt.Color(0, 102, 255));
        tabs.setToolTipText("");
        tabs.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        lblHead.setBackground(new java.awt.Color(255, 255, 255));
        lblHead.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        boxCost.setBackground(new java.awt.Color(0, 0, 204));
        boxCost.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        boxCost.setForeground(new java.awt.Color(255, 255, 255));

        lblCostTitle.setBackground(new java.awt.Color(0, 0, 0));
        lblCostTitle.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblCostTitle.setForeground(new java.awt.Color(255, 255, 255));
        lblCostTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblCostTitle.setText("PHP 0.00");

        lblCostTitle2.setBackground(new java.awt.Color(0, 0, 0));
        lblCostTitle2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblCostTitle2.setForeground(new java.awt.Color(255, 255, 255));
        lblCostTitle2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblCostTitle2.setText("Total Expenses");

        javax.swing.GroupLayout boxCostLayout = new javax.swing.GroupLayout(boxCost);
        boxCost.setLayout(boxCostLayout);
        boxCostLayout.setHorizontalGroup(
            boxCostLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(boxCostLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(boxCostLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(boxCostLayout.createSequentialGroup()
                        .addComponent(lblCostTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(15, 15, 15))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, boxCostLayout.createSequentialGroup()
                        .addComponent(lblCostTitle2, javax.swing.GroupLayout.DEFAULT_SIZE, 134, Short.MAX_VALUE)
                        .addContainerGap())))
        );
        boxCostLayout.setVerticalGroup(
            boxCostLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, boxCostLayout.createSequentialGroup()
                .addComponent(lblCostTitle2, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 24, Short.MAX_VALUE)
                .addComponent(lblCostTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(14, 14, 14))
        );

        boxRec.setBackground(new java.awt.Color(0, 0, 204));
        boxRec.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        boxRec.setForeground(new java.awt.Color(255, 255, 255));

        lblRecTitle1.setBackground(new java.awt.Color(0, 0, 0));
        lblRecTitle1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblRecTitle1.setForeground(new java.awt.Color(255, 255, 255));
        lblRecTitle1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblRecTitle1.setText("Total Records");

        lblTotalRecords1.setBackground(new java.awt.Color(0, 0, 0));
        lblTotalRecords1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblTotalRecords1.setForeground(new java.awt.Color(255, 255, 255));
        lblTotalRecords1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTotalRecords1.setText("0 records");
        lblTotalRecords1.setPreferredSize(new java.awt.Dimension(61, 20));

        javax.swing.GroupLayout boxRecLayout = new javax.swing.GroupLayout(boxRec);
        boxRec.setLayout(boxRecLayout);
        boxRecLayout.setHorizontalGroup(
            boxRecLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, boxRecLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblRecTitle1, javax.swing.GroupLayout.DEFAULT_SIZE, 134, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(boxRecLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(boxRecLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(lblTotalRecords1, javax.swing.GroupLayout.DEFAULT_SIZE, 134, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        boxRecLayout.setVerticalGroup(
            boxRecLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(boxRecLayout.createSequentialGroup()
                .addComponent(lblRecTitle1, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(boxRecLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, boxRecLayout.createSequentialGroup()
                    .addContainerGap(65, Short.MAX_VALUE)
                    .addComponent(lblTotalRecords1, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(10, 10, 10)))
        );

        boxLatest.setBackground(new java.awt.Color(0, 0, 204));
        boxLatest.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        boxLatest.setForeground(new java.awt.Color(255, 255, 255));
        boxLatest.setPreferredSize(new java.awt.Dimension(61, 20));

        lblLatestTitle.setBackground(new java.awt.Color(0, 0, 0));
        lblLatestTitle.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblLatestTitle.setForeground(new java.awt.Color(255, 255, 255));
        lblLatestTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblLatestTitle.setText("Latest Activity");

        lblLatest.setBackground(new java.awt.Color(0, 0, 0));
        lblLatest.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblLatest.setForeground(new java.awt.Color(255, 255, 255));
        lblLatest.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblLatest.setText("None yet");

        javax.swing.GroupLayout boxLatestLayout = new javax.swing.GroupLayout(boxLatest);
        boxLatest.setLayout(boxLatestLayout);
        boxLatestLayout.setHorizontalGroup(
            boxLatestLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblLatestTitle, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, boxLatestLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblLatest, javax.swing.GroupLayout.DEFAULT_SIZE, 133, Short.MAX_VALUE)
                .addContainerGap())
        );
        boxLatestLayout.setVerticalGroup(
            boxLatestLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(boxLatestLayout.createSequentialGroup()
                .addComponent(lblLatestTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblLatest, javax.swing.GroupLayout.DEFAULT_SIZE, 63, Short.MAX_VALUE)
                .addContainerGap())
        );

        btnRefresh.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnRefresh.setText("Refresh");
        btnRefresh.addActionListener(this::btnRefreshActionPerformed);

        lblHead1.setBackground(new java.awt.Color(0, 0, 0));
        lblHead1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblHead1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblHead1.setText("Recent Maintenance Records:");

        tblRecent.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Date", "Vehicle", "Service Type", "Cost"
            }
        ));
        tblRecent.setColumnSelectionAllowed(true);
        jScrollPane1.setViewportView(tblRecent);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(34, 34, 34)
                        .addComponent(boxCost, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(59, 59, 59)
                        .addComponent(boxRec, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(boxLatest, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(17, 17, 17)
                        .addComponent(btnRefresh)))
                .addGap(26, 26, 26))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 644, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(lblHead1, javax.swing.GroupLayout.PREFERRED_SIZE, 204, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(boxLatest, javax.swing.GroupLayout.DEFAULT_SIZE, 114, Short.MAX_VALUE)
                    .addComponent(boxCost, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(boxRec, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(9, 9, 9)
                .addComponent(btnRefresh)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblHead1, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 261, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        lblHead.addTab("Dashboard Overview", jPanel1);

        tabs.addTab(" Dashboard ", lblHead);

        formPanel.setBackground(new java.awt.Color(255, 255, 255));
        formPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Maintenance Record Form"));

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setLayout(null);
        jPanel2.add(txtVehicle);
        txtVehicle.setBounds(70, 60, 180, 22);

        lDate1.setBackground(new java.awt.Color(0, 0, 0));
        lDate1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lDate1.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lDate1.setText("Date:");
        jPanel2.add(lDate1);
        lDate1.setBounds(10, 20, 40, 20);

        lVeh.setBackground(new java.awt.Color(0, 0, 0));
        lVeh.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lVeh.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lVeh.setText("Vehicle:");
        jPanel2.add(lVeh);
        lVeh.setBounds(10, 60, 60, 20);
        jPanel2.add(txtNotes);
        txtNotes.setBounds(70, 180, 180, 22);

        cmbService.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Oil Change", "Tire Repair", "Tire Replacement", "Brake Service", "Battery Replacement", "Air Filter", "Engine Tune-Up", "Coolant Flush", "General Inspection", "Lights / Electrical", "Body Repair", "Other" }));
        jPanel2.add(cmbService);
        cmbService.setBounds(70, 100, 180, 22);

        lSvc.setBackground(new java.awt.Color(0, 0, 0));
        lSvc.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lSvc.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lSvc.setText("Service:");
        jPanel2.add(lSvc);
        lSvc.setBounds(10, 100, 60, 20);
        jPanel2.add(txtDate);
        txtDate.setBounds(70, 20, 180, 22);

        lCost.setBackground(new java.awt.Color(0, 0, 0));
        lCost.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lCost.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lCost.setText("Cost:");
        jPanel2.add(lCost);
        lCost.setBounds(10, 140, 40, 20);
        jPanel2.add(txtCost);
        txtCost.setBounds(70, 140, 180, 22);

        lNotes1.setBackground(new java.awt.Color(0, 0, 0));
        lNotes1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lNotes1.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lNotes1.setText("Notes:");
        jPanel2.add(lNotes1);
        lNotes1.setBounds(10, 180, 50, 20);

        btnClear.setBackground(new java.awt.Color(204, 204, 204));
        btnClear.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnClear.setText("Clear Form");
        btnClear.addActionListener(this::btnClearActionPerformed);
        jPanel2.add(btnClear);
        btnClear.setBounds(130, 260, 120, 23);

        btnAdd.setBackground(new java.awt.Color(204, 255, 204));
        btnAdd.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnAdd.setText("Add Record");
        btnAdd.addActionListener(this::btnAddActionPerformed);
        jPanel2.add(btnAdd);
        btnAdd.setBounds(0, 220, 120, 23);

        btnUpdate.setBackground(new java.awt.Color(255, 204, 51));
        btnUpdate.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnUpdate.setText("Update");
        btnUpdate.addActionListener(this::btnUpdateActionPerformed);
        jPanel2.add(btnUpdate);
        btnUpdate.setBounds(130, 220, 120, 23);

        btnDelete.setBackground(new java.awt.Color(255, 51, 51));
        btnDelete.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnDelete.setText("Delete Record");
        btnDelete.addActionListener(this::btnDeleteActionPerformed);
        jPanel2.add(btnDelete);
        btnDelete.setBounds(0, 260, 120, 23);

        tablePanel.setBackground(new java.awt.Color(51, 102, 255));
        tablePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("All Records"));

        txtSearch.setText("Search by vehicle or service type");

        btnSearch.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnSearch.setText("Search");
        btnSearch.addActionListener(this::btnSearchActionPerformed);

        btnShowAll.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnShowAll.setText("Show All");
        btnShowAll.addActionListener(this::btnShowAllActionPerformed);

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "ID", "Date", "Vehicle", "Service Type", "Cost", "Note"
            }
        ));
        jTable1.setColumnSelectionAllowed(true);
        tblRecords.setViewportView(jTable1);

        javax.swing.GroupLayout tablePanelLayout = new javax.swing.GroupLayout(tablePanel);
        tablePanel.setLayout(tablePanelLayout);
        tablePanelLayout.setHorizontalGroup(
            tablePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tablePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 11, Short.MAX_VALUE)
                .addComponent(btnSearch)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnShowAll)
                .addContainerGap())
            .addComponent(tblRecords, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );
        tablePanelLayout.setVerticalGroup(
            tablePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tablePanelLayout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addGroup(tablePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtSearch, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(tablePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btnSearch)
                        .addComponent(btnShowAll)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(tblRecords, javax.swing.GroupLayout.PREFERRED_SIZE, 399, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(16, Short.MAX_VALUE))
        );

        jPanel2.add(tablePanel);
        tablePanel.setBounds(260, -30, 380, 500);

        formPanel.addTab("Details", jPanel2);

        tabs.addTab("Records", formPanel);

        javax.swing.GroupLayout lblRecentLayout = new javax.swing.GroupLayout(lblRecent);
        lblRecent.setLayout(lblRecentLayout);
        lblRecentLayout.setHorizontalGroup(
            lblRecentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lblRecentLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tabs)
                .addContainerGap())
        );
        lblRecentLayout.setVerticalGroup(
            lblRecentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lblRecentLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tabs, javax.swing.GroupLayout.PREFERRED_SIZE, 545, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        getContentPane().add(lblRecent, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 110, -1, -1));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnLogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLogoutActionPerformed
        doLogout();
    }//GEN-LAST:event_btnLogoutActionPerformed

    private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
        refreshDashboard(); loadRecentTable(recentModel);   // TODO add your handling code here:
    }//GEN-LAST:event_btnRefreshActionPerformed

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed
        clearForm();        // TODO add your handling code here:
    }//GEN-LAST:event_btnClearActionPerformed

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
        doAdd();
        // TODO add your handling code here:
    }//GEN-LAST:event_btnAddActionPerformed

    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateActionPerformed
        doUpdate();        // TODO add your handling code here:
    }//GEN-LAST:event_btnUpdateActionPerformed

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        doDelete();        // TODO add your handling code here:
    }//GEN-LAST:event_btnDeleteActionPerformed

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
        doSearch();        // TODO add your handling code here:
    }//GEN-LAST:event_btnSearchActionPerformed

    private void btnShowAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnShowAllActionPerformed
        txtSearch.setText(""); loadRecords();      // TODO add your handling code here:
    }//GEN-LAST:event_btnShowAllActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
 
        java.awt.EventQueue.invokeLater(() -> new vehiclemaintenance.DashboardFrame().setVisible(true));
    }
    //
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel boxCost;
    private javax.swing.JPanel boxLatest;
    private javax.swing.JPanel boxRec;
    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnClear;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnLogout;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton btnSearch;
    private javax.swing.JButton btnShowAll;
    private javax.swing.JButton btnUpdate;
    private javax.swing.JComboBox<String> cmbService;
    private javax.swing.JTabbedPane formPanel;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JLabel lCost;
    private javax.swing.JLabel lDate1;
    private javax.swing.JLabel lNotes1;
    private javax.swing.JLabel lSvc;
    private javax.swing.JLabel lVeh;
    private javax.swing.JLabel lblCostTitle;
    private javax.swing.JLabel lblCostTitle2;
    private javax.swing.JTabbedPane lblHead;
    private javax.swing.JLabel lblHead1;
    private javax.swing.JLabel lblLatest;
    private javax.swing.JLabel lblLatestTitle;
    private javax.swing.JLabel lblRecTitle1;
    private javax.swing.JPanel lblRecent;
    private javax.swing.JLabel lblTotalRecords1;
    private javax.swing.JLabel lblWelcome;
    private javax.swing.JPanel tablePanel;
    private javax.swing.JTabbedPane tabs;
    private javax.swing.JTable tblRecent;
    private javax.swing.JScrollPane tblRecords;
    private javax.swing.JTextField txtCost;
    private javax.swing.JTextField txtDate;
    private javax.swing.JTextField txtNotes;
    private javax.swing.JTextField txtSearch;
    private javax.swing.JTextField txtVehicle;
    // End of variables declaration//GEN-END:variables
}