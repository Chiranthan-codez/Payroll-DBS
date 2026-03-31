package com.payroll.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** Reusable factory for styled JavaFX controls. */
public final class UIHelper {

    private UIHelper() {}

    public static Label label(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("form-label");
        return l;
    }

    public static Label heading(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("page-heading");
        return l;
    }

    public static Label subheading(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("page-subheading");
        return l;
    }

    public static TextField field(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("form-field");
        tf.setMaxWidth(260);
        return tf;
    }

    public static PasswordField passField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.getStyleClass().add("form-field");
        pf.setMaxWidth(260);
        return pf;
    }

    public static TextField readOnly(String prompt) {
        TextField tf = field(prompt);
        tf.setEditable(false);
        tf.setFocusTraversable(false);
        tf.setStyle("-fx-background-color: #f1f5f9;");
        return tf;
    }

    public static <T> ComboBox<T> combo(String prompt) {
        ComboBox<T> cb = new ComboBox<>();
        cb.setPromptText(prompt);
        cb.getStyleClass().add("form-field");
        cb.setMaxWidth(260);
        return cb;
    }

    public static Button primaryBtn(String text) {
        Button b = new Button(text);
        b.getStyleClass().addAll("btn", "btn-primary");
        b.setMinWidth(130);
        return b;
    }

    public static Button secondaryBtn(String text) {
        Button b = new Button(text);
        b.getStyleClass().addAll("btn", "btn-secondary");
        b.setMinWidth(110);
        return b;
    }

    public static Button dangerBtn(String text) {
        Button b = new Button(text);
        b.getStyleClass().addAll("btn", "btn-danger");
        b.setMinWidth(110);
        return b;
    }

    /** Add a label+control row to a GridPane. */
    public static void addRow(GridPane g, int row, String lbl, Control ctrl) {
        g.add(label(lbl), 0, row);
        g.add(ctrl,       1, row);
        GridPane.setMargin(ctrl, new Insets(4, 0, 4, 12));
    }

    public static VBox formCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("form-card");
        card.setPadding(new Insets(24));
        return card;
    }

    public static void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    public static boolean confirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    /** Prompt dialog that returns entered string or null. */
    public static String prompt(String title, String label) {
        TextInputDialog d = new TextInputDialog();
        d.setTitle(title); d.setHeaderText(null); d.setContentText(label);
        return d.showAndWait().orElse(null);
    }
}
