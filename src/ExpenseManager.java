import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.text.SimpleDateFormat;

/* ================= DB ================= */
class DBConnection {
    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/expense_manager", "root", "root"
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

/* ================= GRADIENT PANEL ================= */
class GradientPanel extends JPanel {
    public GradientPanel() { setLayout(null); }
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setPaint(new GradientPaint(0, 0, new Color(10, 10, 30),
                getWidth(), getHeight(), new Color(0, 120, 215)));
        g2.fillRect(0, 0, getWidth(), getHeight());
    }
}

/* ================= MAIN ================= */
public class ExpenseManager extends JFrame implements ActionListener {

    JComboBox<String> categoryBox;
    JTextField amountField, budgetField;
    JSpinner dateSpinner;
    JButton addBtn, updateBtn, deleteBtn, refreshBtn, graphBtn;
    JTable table;
    DefaultTableModel model;
    JLabel totalLabel, countLabel;
    JProgressBar budgetBar;
    GradientPanel panel;

    public ExpenseManager() {
        setTitle("Expense Manager");
        setSize(1200, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        panel = new GradientPanel();
        setContentPane(panel);

        /* ── Title ── */
        JLabel title = new JLabel("EXPENSE MANAGER");
        title.setBounds(420, 10, 450, 40);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(Color.WHITE);
        panel.add(title);

        /* ── Cards ── */
        totalLabel = new JLabel("Rs.0.00", SwingConstants.CENTER);
        countLabel = new JLabel("0", SwingConstants.CENTER);
        panel.add(makeCard("Total Expense", totalLabel, new Color(0, 120, 215), 20, 60));
        panel.add(makeCard("Transactions",  countLabel, new Color(0, 180, 90),  240, 60));

        /* ── Inputs ── */
        int lx = 20, fw = 290, y = 170, gap = 72;

        addLabel("Category:", lx, y);
        categoryBox = new JComboBox<>(new String[]{
            "Food","Transport","Shopping","Education",
            "Entertainment","Health","Bills","Others"
        });
        categoryBox.setBounds(lx, y+22, fw, 30);
        panel.add(categoryBox);

        addLabel("Amount (Rs.):", lx, y+gap);
        amountField = new JTextField();
        amountField.setBounds(lx, y+gap+22, fw, 30);
        panel.add(amountField);

        addLabel("Date:", lx, y+gap*2);
        SpinnerDateModel sdm = new SpinnerDateModel();
        dateSpinner = new JSpinner(sdm);
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd"));
        dateSpinner.setBounds(lx, y+gap*2+22, fw, 30);
        panel.add(dateSpinner);

        addLabel("Budget Limit (Rs.):", lx, y+gap*3);
        budgetField = new JTextField();
        budgetField.setBounds(lx, y+gap*3+22, fw, 30);
        panel.add(budgetField);

        budgetBar = new JProgressBar(0, 100);
        budgetBar.setBounds(lx, y+gap*3+58, fw, 25);
        budgetBar.setStringPainted(true);
        panel.add(budgetBar);

        JButton chkBtn = makeButton("Check Budget", lx, y+gap*3+90, fw);
        chkBtn.addActionListener(e -> checkBudget());
        panel.add(chkBtn);

        /* ── Buttons ── */
        addBtn    = makeButton("Add",         360, 160, 110);
        updateBtn = makeButton("Update",      480, 160, 110);
        deleteBtn = makeButton("Delete",      600, 160, 110);
        refreshBtn= makeButton("Refresh",     720, 160, 110);
        graphBtn  = makeButton("Trend Graph", 840, 160, 130);

        for (JButton b : new JButton[]{addBtn,updateBtn,deleteBtn,refreshBtn,graphBtn}) {
            panel.add(b);
            b.addActionListener(this);
        }

        /* ── Table ── */
        model = new DefaultTableModel(new String[]{"ID","Category","Amount","Date"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setRowHeight(24);
        table.setSelectionBackground(new Color(0,120,215,120));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(0,80,160));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getSelectionModel().addListSelectionListener(e -> fillFromRow());

        JScrollPane sp = new JScrollPane(table);
        sp.setBounds(360, 210, 810, 450);
        panel.add(sp);

        loadData();
    }

    /* ═══ HELPERS ═══ */
    JPanel makeCard(String lbl, JLabel val, Color bg, int x, int y) {
        JPanel c = new JPanel(new GridLayout(2,1));
        c.setBounds(x, y, 200, 80);
        c.setBackground(bg);
        JLabel l = new JLabel(lbl, SwingConstants.CENTER);
        l.setForeground(Color.WHITE);
        val.setForeground(Color.WHITE);
        val.setFont(new Font("Segoe UI", Font.BOLD, 20));
        c.add(l); c.add(val);
        return c;
    }

    void addLabel(String text, int x, int y) {
        JLabel l = new JLabel(text);
        l.setBounds(x, y, 290, 20);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(l);
    }

    JButton makeButton(String text, int x, int y, int w) {
        JButton b = new JButton(text);
        b.setBounds(x, y, w, 35);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setBackground(new Color(0,100,200));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    void fillFromRow() {
        int row = table.getSelectedRow();
        if (row == -1) return;
        categoryBox.setSelectedItem(model.getValueAt(row, 1));
        amountField.setText(String.valueOf(model.getValueAt(row, 2)));
        try {
            dateSpinner.setValue(new SimpleDateFormat("yyyy-MM-dd")
                .parse((String) model.getValueAt(row, 3)));
        } catch (Exception ignored) {}
    }

    String getSelectedDate() {
        return new SimpleDateFormat("yyyy-MM-dd")
            .format((java.util.Date) dateSpinner.getValue());
    }

    /* ═══ ACTIONS ═══ */
    public void actionPerformed(ActionEvent e) {
        if      (e.getSource() == addBtn)     addExpense();
        else if (e.getSource() == updateBtn)  updateExpense();
        else if (e.getSource() == deleteBtn)  deleteExpense();
        else if (e.getSource() == refreshBtn) loadData();
        else if (e.getSource() == graphBtn)   showGraph();
    }

    void addExpense() {
        try {
            if (amountField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter an amount."); return;
            }
            Connection con = DBConnection.getConnection();
            if (con == null) return;
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO expenses(category, amount, expense_date) VALUES (?,?,?)");
            ps.setString(1, (String) categoryBox.getSelectedItem());
            ps.setDouble(2, Double.parseDouble(amountField.getText().trim()));
            ps.setString(3, getSelectedDate());
            ps.executeUpdate();
            amountField.setText("");
            JOptionPane.showMessageDialog(this, "Added successfully!");
            loadData();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Amount must be a number.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    void updateExpense() {
        try {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this,"Select a row first."); return; }
            if (amountField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,"Enter amount."); return;
            }
            int id = (int) model.getValueAt(row, 0);
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(
                "UPDATE expenses SET category=?, amount=?, expense_date=? WHERE id=?");
            ps.setString(1, (String) categoryBox.getSelectedItem());
            ps.setDouble(2, Double.parseDouble(amountField.getText().trim()));
            ps.setString(3, getSelectedDate());
            ps.setInt(4, id);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Updated successfully!");
            loadData();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Amount must be a number.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    void deleteExpense() {
        try {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this,"Select a row first."); return; }
            if (JOptionPane.showConfirmDialog(this,"Delete this expense?","Confirm",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            int id = (int) model.getValueAt(row, 0);
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM expenses WHERE id=?");
            ps.setInt(1, id); ps.executeUpdate();
            loadData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    void loadData() {
        try {
            model.setRowCount(0);
            Connection con = DBConnection.getConnection();
            if (con == null) return;
            ResultSet rs = con.createStatement().executeQuery(
                "SELECT * FROM expenses ORDER BY expense_date ASC");
            double total = 0; int count = 0;
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"), rs.getString("category"),
                    rs.getDouble("amount"), rs.getString("expense_date")
                });
                total += rs.getDouble("amount"); count++;
            }
            totalLabel.setText("Rs. " + String.format("%.2f", total));
            countLabel.setText(String.valueOf(count));
            checkBudget();
        } catch (Exception e) { e.printStackTrace(); }
    }

    void checkBudget() {
        try {
            String bt = budgetField.getText().trim();
            if (bt.isEmpty()) return;
            double budget = Double.parseDouble(bt), total = 0;
            for (int i = 0; i < model.getRowCount(); i++)
                total += (double) model.getValueAt(i, 2);
            int pct = (int) Math.min((total / budget) * 100, 100);
            budgetBar.setValue(pct);
            budgetBar.setString(pct + "% used");
            if (pct >= 100) {
                budgetBar.setForeground(Color.RED);
                JOptionPane.showMessageDialog(this, "Budget exceeded!", "Warning",
                    JOptionPane.WARNING_MESSAGE);
            } else if (pct >= 75) budgetBar.setForeground(Color.ORANGE);
            else budgetBar.setForeground(new Color(0,180,90));
        } catch (NumberFormatException ignored) {}
    }

    /* ══════════════════════════════════════
       TREND GRAPH — shown inside JOptionPane
       using ImageIcon so it MUST open
    ══════════════════════════════════════ */
    void showGraph() {
        try {
            Connection con = DBConnection.getConnection();
            if (con == null) return;

            ResultSet rs = con.createStatement().executeQuery(
                "SELECT category, SUM(amount) AS total " +
                "FROM expenses GROUP BY category"
            );

            List<String> categories = new ArrayList<>();
            List<Double> values = new ArrayList<>();

            while (rs.next()) {
                categories.add(rs.getString("category"));
                values.add(rs.getDouble("total"));
            }

            if (categories.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No data found!");
                return;
            }

            int w = 700, h = 500;
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();

            // background
            g.setColor(new Color(20, 20, 40));
            g.fillRect(0, 0, w, h);

            double total = 0;
            for (double v : values) total += v;

            int cx = 250, cy = 250, radius = 150;

            double startAngle = 0;

            Color[] colors = {
                new Color(0, 180, 255),
                new Color(0, 255, 150),
                new Color(255, 200, 0),
                new Color(255, 80, 80),
                new Color(180, 100, 255),
                new Color(255, 140, 0),
                new Color(0, 255, 255),
                new Color(200, 200, 200)
            };

            // PIE DRAW
            for (int i = 0; i < values.size(); i++) {

                double angle = (values.get(i) / total) * 360;

                g.setColor(colors[i % colors.length]);
                g.fillArc(cx - radius, cy - radius,
                        radius * 2, radius * 2,
                        (int) startAngle, (int) angle);

                startAngle += angle;
            }

            // TITLE
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 18));
            g.drawString("Expense Category Distribution", 20, 30);

            // LEGEND
            int lx = 450, ly = 80;
            g.setFont(new Font("Arial", Font.PLAIN, 14));

            for (int i = 0; i < categories.size(); i++) {

                g.setColor(colors[i % colors.length]);
                g.fillRect(lx, ly + i * 25, 15, 15);

                g.setColor(Color.WHITE);
                String text = categories.get(i) + " - Rs." + values.get(i);
                g.drawString(text, lx + 25, ly + i * 25 + 12);
            }

            g.dispose();

            JOptionPane.showMessageDialog(
                this,
                new JLabel(new ImageIcon(img)),
                "Expense Pie Chart",
                JOptionPane.PLAIN_MESSAGE
            );

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ExpenseManager().setVisible(true));
    }
}