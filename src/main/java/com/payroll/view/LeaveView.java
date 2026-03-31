package com.payroll.view;

import com.payroll.model.Leave;
import com.payroll.util.UIHelper;
import com.payroll.util.Session;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LeaveView — updated for payrolldbs schema.
 *
 * leave_types are dynamic rows in the DB, not hard-coded columns.
 * employee_leave_balances: one row per (empid, leave_type_id).
 * Supports all leave types returned from DB (Annual, Casual, Optional + any future types).
 */
public class LeaveView {

    private final VBox root = new VBox(20);
    private final Leave objLeave = new Leave();

    // Employee info
    private final TextField tfEmpId = UIHelper.readOnly("");
    private final TextField tfFname = UIHelper.readOnly("");
    private final TextField tfLname = UIHelper.readOnly("");
    private final TextField tfDesig = UIHelper.readOnly("");
    private final TextField tfDept  = UIHelper.readOnly("");

    // Dynamic balance + apply fields, keyed by leave type name
    private final Map<String, TextField> balanceFields = new LinkedHashMap<>();
    private final Map<String, TextField> applyFields   = new LinkedHashMap<>();

    // Panels rebuilt after search
    private HBox leavePanel = new HBox(20);

    public LeaveView() {
        root.setPadding(new Insets(24));
        root.getStyleClass().add("tab-content");

        Label heading = UIHelper.heading("Leave Management");
        Label sub     = UIHelper.subheading("Search an employee and apply leave against their balance.");

        root.getChildren().addAll(heading, sub, buildEmpStrip(), leavePanel, buildButtons());
    }

    private HBox buildEmpStrip() {
        HBox strip = new HBox(20);
        strip.getStyleClass().add("checkout-bar");
        strip.setPadding(new Insets(14, 20, 14, 20));
        GridPane g = new GridPane(); g.setHgap(10); g.setVgap(8);
        UIHelper.addRow(g, 0, "Employee ID", tfEmpId);
        UIHelper.addRow(g, 1, "First Name",  tfFname);
        UIHelper.addRow(g, 2, "Last Name",   tfLname);
        GridPane g2 = new GridPane(); g2.setHgap(10); g2.setVgap(8);
        UIHelper.addRow(g2, 0, "Designation", tfDesig);
        UIHelper.addRow(g2, 1, "Department",  tfDept);
        strip.getChildren().addAll(g, g2);
        return strip;
    }

    /** Rebuild the leave cards after loading employee data. */
    private void rebuildLeavePanel() {
        leavePanel.getChildren().clear();
        balanceFields.clear();
        applyFields.clear();

        VBox balCard = UIHelper.formCard();
        balCard.setPrefWidth(300);
        Label h1 = new Label("Available Leave Balance (Days)");
        h1.getStyleClass().add("panel-heading");
        GridPane g1 = new GridPane(); g1.setHgap(10); g1.setVgap(10);

        VBox applyCard = UIHelper.formCard();
        applyCard.setPrefWidth(300);
        Label h2 = new Label("Apply Leave (Days to Deduct)");
        h2.getStyleClass().add("panel-heading");
        GridPane g2 = new GridPane(); g2.setHgap(10); g2.setVgap(10);

        int row = 0;
        for (Map.Entry<String, Integer> entry : objLeave.getBalances().entrySet()) {
            String type = entry.getKey();

            TextField balField = UIHelper.readOnly("—");
            balField.setText(String.valueOf(entry.getValue()));
            balanceFields.put(type, balField);
            UIHelper.addRow(g1, row, type + " Leave", balField);

            TextField applyField = UIHelper.field("Days");
            applyFields.put(type, applyField);
            UIHelper.addRow(g2, row, type + " Leave", applyField);
            row++;
        }

        balCard.getChildren().addAll(h1, g1);
        applyCard.getChildren().addAll(h2, g2);
        leavePanel.getChildren().addAll(balCard, applyCard);
    }

    private HBox buildButtons() {
        Button btnSearch = UIHelper.secondaryBtn("🔍 Search Employee");
        Button btnApply  = UIHelper.primaryBtn("✅ Apply Leave");
        Button btnClear  = UIHelper.secondaryBtn("Clear");
        if (!Session.isAdmin()) {
    
   
}
        btnSearch.setOnAction(e -> handleSearch());
        btnApply.setOnAction(e -> handleApply());
        btnClear.setOnAction(e -> clearForm());
        HBox row = new HBox(12, btnSearch, btnApply, btnClear);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

  //  private void handleSearch() {
    //    String idStr = UIHelper.prompt("Search Employee", "Enter Employee ID:");
      //  if (idStr == null || idStr.isBlank()) return;
     //   try {
       //     int id = Integer.parseInt(idStr.trim());
         //   if (!Session.isAdmin()) {
    //id = Session.getEmpId();
//}
  //          if (objLeave.getLeaveDetails(id)) {
    //            tfEmpId.setText(String.valueOf(id));
      //          tfFname.setText(objLeave.objEmployee.getFname());
        //        tfLname.setText(objLeave.objEmployee.getLname());
          //      tfDesig.setText(objLeave.objEmployee.getDesignation());
            //    tfDept.setText(objLeave.objEmployee.getDepartment());
              //  rebuildLeavePanel();
     //       } else {
       //         UIHelper.showAlert(Alert.AlertType.ERROR, "Not Found", objLeave.getLastMessage());
         //   }
     //   } catch (NumberFormatException e) {
     //       UIHelper.showAlert(Alert.AlertType.ERROR, "Input Error", "Please enter a valid numeric ID.");
     //  }
   // }
private void handleSearch() {

    try {

        int id;

        /* If employee → automatically use logged-in empid */
        if (!Session.isAdmin()) {

            id = Session.getEmpId();

        } else {

            String idStr = UIHelper.prompt("Search Employee", "Enter Employee ID:");
            if (idStr == null || idStr.isBlank()) return;

            id = Integer.parseInt(idStr.trim());
        }

        if (objLeave.getLeaveDetails(id)) {

            tfEmpId.setText(String.valueOf(id));
            tfFname.setText(objLeave.objEmployee.getFname());
            tfLname.setText(objLeave.objEmployee.getLname());
            tfDesig.setText(objLeave.objEmployee.getDesignation());
            tfDept.setText(objLeave.objEmployee.getDepartment());

            rebuildLeavePanel();

        } else {

            UIHelper.showAlert(Alert.AlertType.ERROR,
                    "Not Found",
                    objLeave.getLastMessage());
        }

    } catch (NumberFormatException e) {

        UIHelper.showAlert(Alert.AlertType.ERROR,
                "Input Error",
                "Please enter a valid numeric ID.");
    }
}
    private void handleApply() {
        if (tfEmpId.getText().isBlank()) {
            UIHelper.showAlert(Alert.AlertType.WARNING, "No Employee", "Search an employee first.");
            return;
        }
        try {
            Map<String, Integer> deductions = new LinkedHashMap<>();
            for (String type : balanceFields.keySet()) {
                TextField applyField = applyFields.get(type);
                String text = applyField.getText().trim();
                if (text.isBlank()) continue;
                int days = Integer.parseInt(text);
                int available = Integer.parseInt(balanceFields.get(type).getText().trim());
                if (days < 0) {
                    UIHelper.showAlert(Alert.AlertType.ERROR, "Invalid Input",
                        "Days to apply cannot be negative for " + type + " leave.");
                    return;
                }
                if (days > available) {
                    UIHelper.showAlert(Alert.AlertType.ERROR, "Insufficient Leave",
                        type + " leave: requested " + days + " days but only " + available + " available.");
                    return;
                }
                if (days > 0) deductions.put(type, days);
            }

            if (deductions.isEmpty()) {
                UIHelper.showAlert(Alert.AlertType.WARNING, "Nothing to Apply", "Enter days for at least one leave type.");
                return;
            }

            if (objLeave.applyLeave(deductions)) {
                UIHelper.showAlert(Alert.AlertType.INFORMATION, "Success", objLeave.getLastMessage());
                // Refresh balance displays
                for (String type : balanceFields.keySet()) {
                    balanceFields.get(type).setText(String.valueOf(objLeave.getBalances().get(type)));
                    applyFields.get(type).clear();
                }
            } else {
                UIHelper.showAlert(Alert.AlertType.ERROR, "Error", objLeave.getLastMessage());
            }
        } catch (NumberFormatException e) {
            UIHelper.showAlert(Alert.AlertType.ERROR, "Input Error", "Please enter valid whole numbers for leave days.");
        }
    }

    private void clearForm() {
        tfEmpId.clear(); tfFname.clear(); tfLname.clear();
        tfDesig.clear(); tfDept.clear();
        leavePanel.getChildren().clear();
        balanceFields.clear();
        applyFields.clear();
    }

    public VBox getRoot() { return root; }
}
