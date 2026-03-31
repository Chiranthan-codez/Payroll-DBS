package com.payroll.view;
import com.payroll.util.*;
import com.payroll.MainApp;
import com.payroll.model.User;
import com.payroll.util.UIHelper;
import com.payroll.model.Employee;
import com.payroll.util.Session;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;


/**
 * LoginView — updated for payrolldbs_project.sql.
 *
 * Password change (SQL Section 3):
 *   Seed data now stores PLAIN-TEXT passwords (not bcrypt hashes).
 *   aarav.hr   → password: admin123   (Admin role)
 *   sneha.se   → password: sneha123   (Employee role)
 *   rahul.jse  → password: rahul123   (Employee role)
 *   priya.finance → password: priya123
 *   arjun.marketing → password: arjun123
 *   anjali.hr  → password: anjali123  (Employee + HR Manager)
 *   vikram.finance → password: vikram123
 *   deepa.marketing → password: deepa123
 *   karan.devops → password: karan123
 *   swati.qa   → password: swati123
 *
 * Auth flow uses system_users + user_role_assignments + user_roles (vw_user_auth logic).
 * Admin role detected via MAX(CASE WHEN role_name='Admin' THEN 1 ELSE 0 END).
 *
 * trg_user_after_insert trigger (SQL Section 9):
 *   Automatically assigns 'Employee' role to every new user created via CreateUserView.
 */
public class LoginView {

    private final StackPane root = new StackPane();
    private final TextField     tfUser = UIHelper.field("Username");
    private final PasswordField pfPass = UIHelper.passField("Password");
    private final Label         lblErr = new Label();

    public LoginView() {
        root.getStyleClass().add("login-bg");

        VBox card = new VBox(18);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(400);
        card.setPadding(new Insets(36, 40, 36, 40));
        card.setAlignment(Pos.CENTER);

        Label title    = new Label("Payroll Management");
        title.getStyleClass().add("login-title");
        Label subtitle = new Label("Sign in to continue");
        subtitle.getStyleClass().add("login-subtitle");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(14);
        grid.setAlignment(Pos.CENTER);
        UIHelper.addRow(grid, 0, "Username", tfUser);
        UIHelper.addRow(grid, 1, "Password", pfPass);

        lblErr.getStyleClass().add("error-label");
        lblErr.setWrapText(true);

        Button btnLogin = UIHelper.primaryBtn("Log In");
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setOnAction(e -> handleLogin());
        pfPass.setOnAction(e -> handleLogin());

        Button btnCreate = UIHelper.secondaryBtn("Create User");
        btnCreate.setMaxWidth(Double.MAX_VALUE);
        btnCreate.setOnAction(e -> openCreateUser());

        Button btnExit = UIHelper.secondaryBtn("Exit");
        btnExit.setMaxWidth(Double.MAX_VALUE);
        btnExit.setOnAction(e -> System.exit(0));

        HBox btns = new HBox(12, btnLogin, btnCreate, btnExit);
        btns.setAlignment(Pos.CENTER);
        HBox.setHgrow(btnLogin,  Priority.ALWAYS);
        HBox.setHgrow(btnCreate, Priority.ALWAYS);
        HBox.setHgrow(btnExit,   Priority.ALWAYS);

        // Credential hint — updated to match plain-text passwords from SQL Section 3
        Label credHint = new Label(
            "Dev credentials (plain-text passwords from payrolldbs_project.sql):\n" +
            "  Admin  → username: aarav.hr      password: admin123\n" +
            "  Employee → username: sneha.se    password: sneha123"
        );
        credHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
        credHint.setWrapText(true);

        card.getChildren().addAll(title, subtitle, grid, lblErr, btns, credHint);
        root.getChildren().add(card);
    }

    private void handleLogin() {
        lblErr.setText("");
        String user = tfUser.getText().trim();
        String pass = pfPass.getText();
        if (user.isEmpty() || pass.isEmpty()) {
            lblErr.setText("⚠ Username and Password cannot be empty.");
            return;
        }
        try {
            User u = new User();
            u.setUserName(user);
            u.setPassword(pass);
            int result = u.authenticate();
            if (result == -1) {
                lblErr.setText("⚠ Invalid username or password.");
            } else {
                Session.start(u.getEmpId(), u.isAdmin()); 
                HomeView home = new HomeView(result == 1);
                javafx.scene.Scene scene = new javafx.scene.Scene(home.getRoot(), 1200, 700);
                scene.getStylesheets().add(
                    getClass().getResource("/com/payroll/css/styles.css").toExternalForm());
                MainApp.primaryStage.setScene(scene);
                MainApp.primaryStage.setTitle("Payroll Management System | Home");
            }
        } catch (Exception ex) {
            lblErr.setText("⚠ DB Error: " + ex.getMessage());
        }
    }

    private void openCreateUser() {
        CreateUserView createView = new CreateUserView();
        javafx.scene.Scene scene = new javafx.scene.Scene(createView.getRoot(), 620, 500);
        scene.getStylesheets().add(
            getClass().getResource("/com/payroll/css/styles.css").toExternalForm());
        MainApp.primaryStage.setScene(scene);
        MainApp.primaryStage.setTitle("Create User | Payroll Management System");
    }

    public StackPane getRoot() { return root; }
}
