package com.payroll.view;

import com.payroll.MainApp;
import com.payroll.model.User;
import com.payroll.util.UIHelper;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * CreateUserView — updated for payrolldbs_project.sql.
 *
 * Changes from old version:
 *  1. Role dropdown now correctly uses 'HR Manager' (matches user_roles.role_name in SQL Section 3).
 *     Old code had 'HR' which would fail the FK lookup against user_roles table.
 *  2. Password is stored as plain-text (matches seed data pattern in SQL Section 3).
 *  3. Delegates to User.createUser() method which handles:
 *     - INSERT into system_users
 *     - trg_user_after_insert trigger auto-assigns 'Employee' role
 *     - Additional role assignment if non-Employee role chosen
 *  4. Input validation: empId numeric check, required fields check.
 */
public class CreateUserView {

    private final StackPane root = new StackPane();

    private final TextField     tfEmpId = UIHelper.field("Employee ID (numeric)");
    private final TextField     tfUser  = UIHelper.field("Username");
    private final PasswordField pfPass  = UIHelper.passField("Password");
    private final Label         lblMsg  = new Label();

    // Role names MUST match user_roles.role_name in the database exactly
    // SQL Section 3: INSERT INTO user_roles VALUES ('Admin'),('Employee'),('HR Manager')
    private final ComboBox<String> roleBox = new ComboBox<>();

    public CreateUserView() {
        VBox card = new VBox(18);
        card.setPadding(new Insets(35));
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(420);
        card.getStyleClass().add("login-card");

        Label title    = new Label("Create New System User");
        title.getStyleClass().add("login-title");
        Label subtitle = new Label("Link an employee ID to a login account");
        subtitle.getStyleClass().add("login-subtitle");

        // Roles must exactly match user_roles.role_name values in DB
        roleBox.getItems().addAll("Admin", "Employee", "HR Manager");
        roleBox.setPromptText("Select Role");
        roleBox.setValue("Employee");  // default

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(14);
        grid.setAlignment(Pos.CENTER);
        UIHelper.addRow(grid, 0, "Employee ID *", tfEmpId);
        UIHelper.addRow(grid, 1, "Username *",    tfUser);
        UIHelper.addRow(grid, 2, "Password *",    pfPass);
        UIHelper.addRow(grid, 3, "Role *",        roleBox);

        lblMsg.getStyleClass().add("error-label");
        lblMsg.setWrapText(true);

        Label hint = new Label("Password is stored as plain text in development mode.");
        hint.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");

        Button btnCreate = UIHelper.primaryBtn(" Create User");
        btnCreate.setOnAction(e -> createUser());

        Button btnBack = UIHelper.secondaryBtn("← Back to Login");
        btnBack.setOnAction(e -> goBack());

        HBox btns = new HBox(12, btnCreate, btnBack);
        btns.setAlignment(Pos.CENTER);
        HBox.setHgrow(btnCreate, Priority.ALWAYS);
        HBox.setHgrow(btnBack,   Priority.ALWAYS);
        btnCreate.setMaxWidth(Double.MAX_VALUE);
        btnBack.setMaxWidth(Double.MAX_VALUE);

        card.getChildren().addAll(title, subtitle, grid, lblMsg, hint, btns);
        root.getChildren().add(card);
    }

    private void createUser() {
        lblMsg.setText("");

        // Validation
        if (tfEmpId.getText().isBlank() || tfUser.getText().isBlank() || pfPass.getText().isBlank()) {
            lblMsg.setStyle("-fx-text-fill: #ef4444;");
            lblMsg.setText("⚠ All fields are required.");
            return;
        }
        if (roleBox.getValue() == null) {
            lblMsg.setStyle("-fx-text-fill: #ef4444;");
            lblMsg.setText("⚠ Please select a role.");
            return;
        }

        int empId;
        try {
            empId = Integer.parseInt(tfEmpId.getText().trim());
        } catch (NumberFormatException ex) {
            lblMsg.setStyle("-fx-text-fill: #ef4444;");
            lblMsg.setText("⚠ Employee ID must be a valid number.");
            return;
        }

        try {
            User u = new User();
            // createUser() in User.java handles:
            //   INSERT into system_users (plain-text password)
            //   trg_user_after_insert auto-assigns Employee role
            //   Additional role if not Employee
            u.createUser(empId, tfUser.getText().trim(), pfPass.getText(), roleBox.getValue());
            lblMsg.setStyle("-fx-text-fill: #22c55e;");
            lblMsg.setText("✅ User '" + tfUser.getText().trim() + "' created successfully with role: " + roleBox.getValue());
            tfEmpId.clear(); tfUser.clear(); pfPass.clear();
            roleBox.setValue("Employee");
        } catch (Exception e) {
            lblMsg.setStyle("-fx-text-fill: #ef4444;");
            lblMsg.setText("⚠ Error: " + e.getMessage());
        }
    }

    private void goBack() {
        LoginView login = new LoginView();
        javafx.scene.Scene scene = new javafx.scene.Scene(login.getRoot(), 1200, 700);
        scene.getStylesheets().add(
            getClass().getResource("/com/payroll/css/styles.css").toExternalForm());
        MainApp.primaryStage.setScene(scene);
        MainApp.primaryStage.setTitle("Payroll Management System | Login");
    }

    public StackPane getRoot() { return root; }
}
