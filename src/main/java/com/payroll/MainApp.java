package com.payroll;

import com.payroll.view.LoginView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * MainApp — JavaFX entry point.
 * Launches the Login screen; on success it transitions to the main Home view.
 */
public class MainApp extends Application {

    public static final String TITLE   = "Payroll Management System";
    public static final int    W       = 900;
    public static final int    H       = 560;

    /** Shared primary stage — views swap scenes on it. */
    public static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        stage.setTitle(TITLE + " | Login");
        stage.setResizable(false);
        showLogin();
        stage.show();
    }

    public static void showLogin() {
        LoginView lv = new LoginView();
        Scene scene = new Scene(lv.getRoot(), W, H);
        scene.getStylesheets().add(
            MainApp.class.getResource("/com/payroll/css/styles.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle(TITLE + " | Login");
    }

    public static void main(String[] args) { launch(args); }
}
