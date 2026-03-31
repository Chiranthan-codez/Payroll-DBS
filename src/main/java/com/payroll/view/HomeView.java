package com.payroll.view;

import com.payroll.MainApp;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * HomeView — updated for payrolldbs schema.
 *
 * Admin role (from user_role_assignments) unlocks Employee + Payroll tabs.
 * HR Manager role also gets Employee + Payroll access (treated as admin-level here).
 */
public class HomeView {

    private final BorderPane root = new BorderPane();
    private final boolean isAdmin;

    private final EmployeeView employeeView;
    private final PayrollView  payrollView;
    private final LeaveView    leaveView;
    private final PaySlipView  paySlipView;

    public HomeView(boolean isAdmin) {
        this.isAdmin = isAdmin;

        employeeView = new EmployeeView();
        payrollView  = new PayrollView();
        leaveView    = new LeaveView();
        paySlipView  = new PaySlipView();

        buildHeader();
        buildTabs();
    }

    private void buildHeader() {
        HBox header = new HBox(16);
        header.getStyleClass().add("app-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 24, 0, 24));

        Label brand   = new Label("💼 PayrollFX");
        brand.getStyleClass().add("brand");
        Label tagline = new Label("Payroll Management System");
        tagline.getStyleClass().add("tagline");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label roleBadge = new Label(isAdmin ? "👑 Admin" : "👤 Employee");
        roleBadge.getStyleClass().add("version-badge");
        roleBadge.setStyle(isAdmin
            ? "-fx-background-color: #f59e0b;"
            : "-fx-background-color: #3b82f6;");

        Button btnLogout = new Button("Logout");
        btnLogout.getStyleClass().addAll("btn", "btn-secondary");
        btnLogout.setStyle("-fx-font-size:11px; -fx-padding: 4 12 4 12;");
        btnLogout.setOnAction(e -> MainApp.showLogin());

        header.getChildren().addAll(brand, tagline, spacer, roleBadge, btnLogout);
        root.setTop(header);
    }

    private void buildTabs() {
        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("main-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tabDash     = tab("🏠  Dashboard",  buildDashboard());
        Tab tabEmployee = tab("👤  Employees",  employeeView.getRoot());
        Tab tabPayroll  = tab("💰  Payroll",    payrollView.getRoot());
        Tab tabLeave    = tab("📅  Leave",      leaveView.getRoot());
        Tab tabPaySlip  = tab("🧾  Pay Slip",   paySlipView.getRoot());

        // Only Admin/HR Manager can manage employees and payroll
        if (!isAdmin) {
            tabEmployee.setDisable(true);
            tabPayroll.setDisable(true);
        }

        tabs.getTabs().addAll(tabDash, tabEmployee, tabPayroll, tabLeave, tabPaySlip);
        root.setCenter(tabs);
    }

    private Tab tab(String title, javafx.scene.Node content) {
        return new Tab(title, content);
    }

    private VBox buildDashboard() {
        VBox pane = new VBox(20);
        pane.setPadding(new Insets(40));
        pane.getStyleClass().add("tab-content");
        pane.setAlignment(Pos.TOP_LEFT);

        Label h   = new Label("Welcome to Payroll Management System");
        h.getStyleClass().add("page-heading");
        Label sub = new Label("Use the tabs above to manage employees, payroll, leave, and pay slips.");
        sub.getStyleClass().add("page-subheading");

        HBox cards = new HBox(20);
        cards.getChildren().addAll(
            dashCard("👤", "Employees",  "View & manage\nemployee records",    "card-blue"),
            dashCard("💰", "Payroll",    "Configure allowances\n& deductions",  "card-green"),
            dashCard("📅", "Leave",      "Track & apply\nemployee leave",       "card-amber"),
            dashCard("🧾", "Pay Slip",   "Generate & print\npay slips",         "card-red")
        );

        // Schema info card
        VBox schemaCard = new VBox(6);
        schemaCard.getStyleClass().addAll("stat-card", "card-blue");
        schemaCard.setPadding(new Insets(12, 16, 12, 16));
        schemaCard.setMaxWidth(500);
        Label schemaTitle = new Label("📊 Database: payrolldbs");
        schemaTitle.getStyleClass().add("panel-heading");
        Label schemaDesc = new Label(
            "12-table normalised schema · employees · departments · designations\n" +
            "employee_addresses · employee_contact_numbers · salary_structures\n" +
            "employee_salaries · leave_types · employee_leave_balances\n" +
            "system_users · user_roles · user_role_assignments");
        schemaDesc.getStyleClass().add("hint-label");
        schemaCard.getChildren().addAll(schemaTitle, schemaDesc);

        pane.getChildren().addAll(h, sub, cards, schemaCard);
        return pane;
    }

    private VBox dashCard(String icon, String title, String desc, String style) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("stat-card", style);
        card.setPrefSize(210, 130);
        card.setAlignment(Pos.CENTER);
        Label ico = new Label(icon); ico.getStyleClass().add("stat-icon");
        Label t   = new Label(title); t.getStyleClass().add("panel-heading");
        Label d   = new Label(desc);  d.getStyleClass().add("hint-label");
        d.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        card.getChildren().addAll(ico, t, d);
        return card;
    }

    public BorderPane getRoot() { return root; }
}
