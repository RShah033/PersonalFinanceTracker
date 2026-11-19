
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;

public class PersonalFinanceTracker extends JFrame {

    private static final String DB =
            "jdbc:sqlite:finance.db";

    private final Color navy = new Color(17, 35, 57);
    private final Color blue = new Color(0, 105, 255);
    private final Color background = new Color(244, 247, 251);

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);

    private JTable transactionTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    private JLabel incomeLabel;
    private JLabel expenseLabel;
    private JLabel savingsLabel;
    private JLabel budgetLabel;

    private final String[] categories = {
        "Salary", "Freelance", "Food", "Housing",
        "Transport", "Entertainment", "Utilities",
        "Health", "Shopping", "Other"
    };

    public PersonalFinanceTracker() {
        createDatabase();

        setTitle("Personal Finance Tracker");
        setSize(1200, 760);
        setMinimumSize(new Dimension(950, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().setBackground(background);

        buildSidebar();
        buildPages();

        refreshAll();
        setVisible(true);
    }
/*Method which establishes a connection between Java & SQLite */
    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB);
    }

    private void createDatabase() {
        try (Connection c = connect();
             Statement s = c.createStatement()) {

            s.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    date TEXT NOT NULL,
                    description TEXT NOT NULL,
                    category TEXT NOT NULL,
                    type TEXT NOT NULL,
                    amount REAL NOT NULL,
                    recurring INTEGER DEFAULT 0
                )
            """);

            s.execute("""
                CREATE TABLE IF NOT EXISTS budgets (
                    category TEXT PRIMARY KEY,
                    amount REAL NOT NULL
                )
            """);

        } catch (SQLException e) {
            showError(e);
        }
    }
/*The sidebar - is the dark section on the left of the application */
    private void buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(navy);
        sidebar.setPreferredSize(new Dimension(185, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        JLabel brand = new JLabel("  ◈ Finance Tracker");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("SansSerif", Font.BOLD, 17));
        brand.setBorder(BorderFactory.createEmptyBorder(24, 5, 28, 5));
        sidebar.add(brand);

        String[][] navigation = {
            {"Dashboard", "Dashboard"},
            {"Transactions", "Transactions"},
            {"Budgets", "Budgets"},
            {"Analytics", "Analytics"}
        };

        for (String[] item : navigation) {
            JButton button = new JButton(item[0]);
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            button.setMaximumSize(new Dimension(170, 43));
            button.setForeground(Color.WHITE);
            button.setBackground(navy);
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setFont(new Font("SansSerif", Font.PLAIN, 14));

            button.addActionListener(e -> {
                cardLayout.show(content, item[1]);
                refreshAll();
            });

            sidebar.add(button);
        }

        sidebar.add(Box.createVerticalGlue());

        JButton export = new JButton("Export CSV");
        export.addActionListener(e -> exportCSV());
        sidebar.add(export);
        sidebar.add(Box.createVerticalStrut(20));

        getContentPane().add(sidebar, BorderLayout.WEST);
        getContentPane().add(content, BorderLayout.CENTER);
    }

    private void buildPages() {
        content.add(buildDashboard(), "Dashboard");
        content.add(buildTransactions(), "Transactions");
        content.add(buildBudgets(), "Budgets");
        content.add(buildAnalytics(), "Analytics");
    }

    private JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 25));
        label.setForeground(navy);
        return label;
    }

    private JPanel page(String title) {
        JPanel panel = new JPanel(new BorderLayout(15, 18));
        panel.setBackground(background);
        panel.setBorder(
            BorderFactory.createEmptyBorder(25, 25, 25, 25)
        );
        panel.add(heading(title), BorderLayout.NORTH);
        return panel;
    }

    // DASHBOARD
    //Creates a dashboard panel - arranges contents vertically
    private JPanel buildDashboard() {
        JPanel panel = page("Dashboard");

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 12));
        cards.setOpaque(false);

        incomeLabel = new JLabel("£0.00");
        expenseLabel = new JLabel("£0.00");
        savingsLabel = new JLabel("£0.00");
        budgetLabel = new JLabel("£0.00");

        cards.add(summaryCard("Total Income", incomeLabel,
                new Color(15, 165, 105)));
        cards.add(summaryCard("Total Expenses", expenseLabel,
                new Color(230, 65, 80)));
        cards.add(summaryCard("Savings", savingsLabel, blue));
        cards.add(summaryCard("Monthly Budget", budgetLabel,
                new Color(130, 80, 220)));

        body.add(cards);
        body.add(Box.createVerticalStrut(20));

        JPanel recent = new JPanel(new BorderLayout(10, 10));
        recent.setBackground(Color.WHITE);
        recent.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel recentTitle = new JLabel("Recent Transactions");
        recentTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        recent.add(recentTitle, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
            new String[]{"Date", "Description", "Category",
                         "Type", "Amount"}, 0
        ) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        transactionTable = new JTable(tableModel);
        transactionTable.setRowHeight(32);
        transactionTable.setFillsViewportHeight(true);

        recent.add(new JScrollPane(transactionTable),
                   BorderLayout.CENTER);

        JButton add = new JButton("+ Add Transaction");
        add.setBackground(blue);
        add.setForeground(Color.WHITE);
        add.addActionListener(e -> addTransaction());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);
        actions.add(add);
        recent.add(actions, BorderLayout.SOUTH);

        body.add(recent);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private JPanel summaryCard(String title, JLabel value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(5, 15));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
            BorderFactory.createEmptyBorder(17, 15, 17, 15)
        ));

        JLabel label = new JLabel(title);
        label.setForeground(Color.GRAY);
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));

        value.setForeground(navy);
        value.setFont(new Font("SansSerif", Font.BOLD, 22));

        card.add(label, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    // TRANSACTIONS
    //Make transactions
    private JPanel buildTransactions() {
        JPanel panel = page("Transactions");

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setOpaque(false);

        searchField = new JTextField(22);
        searchField.setToolTipText("Search description or category");

        JButton search = new JButton("Search");
        search.addActionListener(e -> loadTransactions(
                searchField.getText()));

        JButton add = new JButton("Add Transaction");
        add.addActionListener(e -> addTransaction());

        top.add(searchField);
        top.add(search);
        top.add(add);

        JPanel wrapper = new JPanel(new BorderLayout(10, 10));
        wrapper.setOpaque(false);
        wrapper.add(top, BorderLayout.NORTH);

        JTable table = new JTable();
        wrapper.add(new JScrollPane(table), BorderLayout.CENTER);

        panel.add(wrapper, BorderLayout.CENTER);

        // Replace the placeholder table with the shared table model.
        // A separate model is used for the full transaction list.
        DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Date", "Description",
                         "Category", "Type", "Amount"}, 0
        ) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        table.setModel(model);
        table.setRowHeight(30);

        JButton delete = new JButton("Delete Selected");
        delete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;

            int id = Integer.parseInt(
                table.getValueAt(row, 0).toString());

            try (Connection c = connect();
                 PreparedStatement p = c.prepareStatement(
                     "DELETE FROM transactions WHERE id=?")) {
                p.setInt(1, id);
                p.executeUpdate();
                loadTransactions(searchField.getText());
                refreshAll();
            } catch (SQLException ex) {
                showError(ex);
            }
        });

        JButton export = new JButton("Export CSV");
        export.addActionListener(e -> exportCSV());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        bottom.add(delete);
        bottom.add(export);
        wrapper.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    private void loadTransactions(String search) {
        // Refresh the full transaction list on the Transactions page.
        if (content.getComponentCount() < 2) return;

        Component[] pages = content.getComponents();
        // The full list is populated by refreshAll().
        // Search filtering is applied in populateTransactionPage().
        populateTransactionPage(search);
    }

    private void populateTransactionPage(String search) {
        JPanel page = (JPanel) content.getComponent(1);
        JPanel wrapper = null;

        for (Component c : page.getComponents()) {
            if (c instanceof JPanel) {
                wrapper = (JPanel) c;
                break;
            }
        }

        if (wrapper == null) return;

        JTable table = findTable(wrapper);
        if (table == null) return;

        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        String sql = """
            SELECT id, date, description, category, type, amount
            FROM transactions
            WHERE date LIKE ?
              AND (description LIKE ? OR category LIKE ?)
            ORDER BY date DESC, id DESC
        """;

        String term = "%" + (search == null ? "" : search.trim()) + "%";

        try (Connection c = connect();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, YearMonth.now().toString() + "%");
            p.setString(2, term);
            p.setString(3, term);

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    model.addRow(new Object[]{
                        r.getInt("id"),
                        r.getString("date"),
                        r.getString("description"),
                        r.getString("category"),
                        r.getString("type"),
                        String.format("£%.2f", r.getDouble("amount"))
                    });
                }
            }
        } catch (SQLException e) {
            showError(e);
        }
    }

    // ADD TRANSACTION

    private void addTransaction() {
        JTextField description = new JTextField(18);
        JTextField amount = new JTextField("0.00", 18);
        JTextField date = new JTextField(LocalDate.now().toString(), 18);

        JComboBox<String> category = new JComboBox<>(categories);
        JComboBox<String> type = new JComboBox<>(
            new String[]{"Expense", "Income"});
        JCheckBox recurring = new JCheckBox("Recurring payment");

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 10));
        form.add(new JLabel("Date (YYYY-MM-DD):"));
        form.add(date);
        form.add(new JLabel("Description:"));
        form.add(description);
        form.add(new JLabel("Category:"));
        form.add(category);
        form.add(new JLabel("Type:"));
        form.add(type);
        form.add(new JLabel("Amount (£):"));
        form.add(amount);
        form.add(new JLabel("Recurrence:"));
        form.add(recurring);

        int result = JOptionPane.showConfirmDialog(
            this, form, "Add Transaction",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) return;

        try {
            LocalDate.parse(date.getText().trim());
            double value = Double.parseDouble(amount.getText().trim());

            if (description.getText().trim().isEmpty() || value <= 0) {
                throw new IllegalArgumentException(
                    "Enter a description and an amount greater than zero.");
            }

            try (Connection c = connect();
                 PreparedStatement p = c.prepareStatement("""
                    INSERT INTO transactions
                    (date, description, category, type, amount, recurring)
                    VALUES (?, ?, ?, ?, ?, ?)
                 """)) {

                p.setString(1, date.getText().trim());
                p.setString(2, description.getText().trim());
                p.setString(3, category.getSelectedItem().toString());
                p.setString(4, type.getSelectedItem().toString());
                p.setDouble(5, value);
                p.setInt(6, recurring.isSelected() ? 1 : 0);
                p.executeUpdate();
            }

            refreshAll();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                this, "Invalid transaction: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // MONTHLY BUDGETS

    private JPanel buildBudgets() {
        JPanel panel = page("Monthly Budgets");

        JPanel body = new JPanel(new BorderLayout(12, 12));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        DefaultTableModel model = new DefaultTableModel(
            new String[]{"Category", "Monthly Limit"}, 0
        );

        JTable table = new JTable(model);
        table.setRowHeight(32);

        JButton setBudget = new JButton("Set / Update Budget");
        setBudget.addActionListener(e -> {
            JComboBox<String> cat = new JComboBox<>(categories);
            JTextField limit = new JTextField("500");

            JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
            form.add(new JLabel("Category:"));
            form.add(cat);
            form.add(new JLabel("Monthly limit (£):"));
            form.add(limit);

            int result = JOptionPane.showConfirmDialog(
                this, form, "Monthly Budget",
                JOptionPane.OK_CANCEL_OPTION);

            if (result != JOptionPane.OK_OPTION) return;

            try {
                double amount = Double.parseDouble(limit.getText());

                if (amount < 0) throw new IllegalArgumentException(
                    "Budget cannot be negative.");

                try (Connection c = connect();
                     PreparedStatement p = c.prepareStatement("""
                        INSERT INTO budgets(category, amount)
                        VALUES(?, ?)
                        ON CONFLICT(category) DO UPDATE SET amount=excluded.amount
                     """)) {
                    p.setString(1, cat.getSelectedItem().toString());
                    p.setDouble(2, amount);
                    p.executeUpdate();
                }

                refreshAll();
            } catch (Exception ex) {
                showError(ex);
            }
        });

        body.add(new JScrollPane(table), BorderLayout.CENTER);
        body.add(setBudget, BorderLayout.SOUTH);
        panel.add(body, BorderLayout.CENTER);

        panel.putClientProperty("budgetTable", table);
        return panel;
    }

    private void refreshBudgets() {
        JPanel page = (JPanel) content.getComponent(2);
        JTable table = findTable(page);
        if (table == null) return;

        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        try (Connection c = connect();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery(
                 "SELECT category, amount FROM budgets ORDER BY category")) {

            while (r.next()) {
                model.addRow(new Object[]{
                    r.getString("category"),
                    String.format("£%.2f", r.getDouble("amount"))
                });
            }
        } catch (SQLException e) {
            showError(e);
        }
    }

    // ANALYTICS

    private JPanel buildAnalytics() {
        JPanel panel = page("Spending Analytics");

        JPanel chartArea = new JPanel(new BorderLayout(10, 10));
        chartArea.setBackground(Color.WHITE);
        chartArea.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Expenses by Category");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        chartArea.add(title, BorderLayout.NORTH);
        chartArea.add(new SpendingChart(), BorderLayout.CENTER);

        panel.add(chartArea, BorderLayout.CENTER);
        return panel;
    }

    private class SpendingChart extends JPanel {
        SpendingChart() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(500, 400));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

            double[] values = new double[categories.length];

            try (Connection c = connect();
                 PreparedStatement p = c.prepareStatement("""
                    SELECT category, SUM(amount) total
                    FROM transactions
                    WHERE type='Expense' AND date LIKE ?
                    GROUP BY category
                 """)) {

                p.setString(1, YearMonth.now().toString() + "%");

                try (ResultSet r = p.executeQuery()) {
                    while (r.next()) {
                        for (int i = 0; i < categories.length; i++) {
                            if (categories[i].equals(r.getString("category"))) {
                                values[i] = r.getDouble("total");
                            }
                        }
                    }
                }
            } catch (SQLException ignored) {
                // Chart remains available if the query fails.
            }

            double max = 1;
            for (double v : values) max = Math.max(max, v);

            int left = 90;
            int chartWidth = Math.max(100, getWidth() - left - 30);
            int rowHeight = Math.max(26, (getHeight() - 20) / categories.length);

            for (int i = 0; i < categories.length; i++) {
                int y = 10 + i * rowHeight;

                g2.setColor(navy);
                g2.drawString(categories[i], 10, y + 17);

                int width = (int) (chartWidth * values[i] / max);
                g2.setColor(blue);
                g2.fillRoundRect(left, y, width, 19, 7, 7);

                g2.setColor(Color.DARK_GRAY);
                g2.drawString(String.format("£%.2f", values[i]),
                              left + width + 7, y + 15);
            }

            g2.dispose();
        }
    }

    // TOTALS AND REFRESH

    private double total(String type) throws SQLException {
        try (Connection c = connect();
             PreparedStatement p = c.prepareStatement("""
                SELECT COALESCE(SUM(amount), 0)
                FROM transactions
                WHERE type=? AND date LIKE ?
             """)) {

            p.setString(1, type);
            p.setString(2, YearMonth.now().toString() + "%");

            try (ResultSet r = p.executeQuery()) {
                return r.next() ? r.getDouble(1) : 0;
            }
        }
    }

    private double totalBudgets() throws SQLException {
        try (Connection c = connect();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery(
                 "SELECT COALESCE(SUM(amount), 0) FROM budgets")) {
            return r.next() ? r.getDouble(1) : 0;
        }
    }

    private void refreshAll() {
        try {
            double income = total("Income");
            double expenses = total("Expense");

            incomeLabel.setText(String.format("£%,.2f", income));
            expenseLabel.setText(String.format("£%,.2f", expenses));
            savingsLabel.setText(String.format("£%,.2f", income - expenses));
            budgetLabel.setText(String.format("£%,.2f", totalBudgets()));

            tableModel.setRowCount(0);

            try (Connection c = connect();
                 PreparedStatement p = c.prepareStatement("""
                    SELECT date, description, category, type, amount
                    FROM transactions
                    WHERE date LIKE ?
                    ORDER BY date DESC, id DESC LIMIT 10
                 """)) {

                p.setString(1, YearMonth.now().toString() + "%");

                try (ResultSet r = p.executeQuery()) {
                    while (r.next()) {
                        tableModel.addRow(new Object[]{
                            r.getString("date"),
                            r.getString("description"),
                            r.getString("category"),
                            r.getString("type"),
                            String.format("£%,.2f", r.getDouble("amount"))
                        });
                    }
                }
            }

            refreshBudgets();
            populateTransactionPage("");

            // Repaint charts when their page becomes visible.
            for (Component component : content.getComponents()) {
                component.repaint();
            }

        } catch (SQLException e) {
            showError(e);
        }
    }

    private JTable findTable(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JTable) {
                return (JTable) component;
            }
            if (component instanceof Container) {
                JTable result = findTable((Container) component);
                if (result != null) return result;
            }
        }
        return null;
    }

    // CSV EXPORT

    private void exportCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("transactions.csv"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try (Connection c = connect();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("""
                SELECT date, description, category, type, amount, recurring
                FROM transactions ORDER BY date DESC
             """);
             PrintWriter out = new PrintWriter(
                 new FileWriter(chooser.getSelectedFile()))) {

            out.println("Date,Description,Category,Type,Amount,Recurring");

            while (r.next()) {
                out.printf("%s,%s,%s,%s,%.2f,%s%n",
                    csv(r.getString("date")),
                    csv(r.getString("description")),
                    csv(r.getString("category")),
                    csv(r.getString("type")),
                    r.getDouble("amount"),
                    r.getInt("recurring") == 1 ? "Yes" : "No");
            }

            JOptionPane.showMessageDialog(this, "CSV exported successfully.");

        } catch (Exception e) {
            showError(e);
        }
    }

    private String csv(String text) {
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private void showError(Exception e) {
        JOptionPane.showMessageDialog(
            this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PersonalFinanceTracker::new);
    }
}
