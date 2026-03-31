package com.payroll.view;

import com.payroll.model.Payroll;
import com.payroll.util.UIHelper;
import com.payroll.util.Session;    
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * PayrollView — updated for payrolldbs_project.sql.
 *
 * New features added to match new SQL schema:
 *
 *  1. Last Revised Date field (read-only) — shows employee_salaries.last_revised_date,
 *     the column added via ALTER TABLE in SQL Section 2.
 *     Written automatically by updateSalaryDetails() with CURDATE().
 *
 *  2. "Apply Salary Hike" panel — calls Payroll.applySalaryHike() which uses
 *     stored procedure sp_apply_salary_hike(IN empId, IN hikePct, INOUT oldSalary)
 *     from SQL Section 7. The INOUT parameter returns the new salary.
 *
 *  3. "Salary Audit Log" button — reads payroll_audit_log table (SQL Section 1 Table 13)
 *     populated automatically by triggers:
 *       trg_salary_after_insert (SQL Section 9) — on INSERT
 *       trg_salary_after_update (SQL Section 9) — on UPDATE
 *
 *  4. CHECK constraint feedback: base_salary > 0 enforced by DB (SQL Section 1 Table 7).
 *     DB will reject 0 or negative values; error surfaced to user.
 */
public class PayrollView {

    private final VBox root = new VBox(20);
    private final Payroll objPayroll = new Payroll();

    // Employee info strip (read-only)
    private final TextField        tfEmpId         = UIHelper.readOnly("");
    private final TextField        tfFname         = UIHelper.readOnly("");
    private final TextField        tfLname         = UIHelper.readOnly("");
    private final TextField        tfDesig         = UIHelper.readOnly("");
    private final TextField        tfDept          = UIHelper.readOnly("");
    private final TextField        tfBaseSal       = UIHelper.field("Base salary (must be > 0)");
    private final ComboBox<String> cbStructure     = UIHelper.combo("Select salary structure");
    // New: last_revised_date — written by updateSalaryDetails() via CURDATE() (SQL Section 2)
    private final TextField        tfLastRevised   = UIHelper.readOnly("Not yet updated");

    // Allowances (salary_structures table)
    private final TextField tfTravel = UIHelper.field("Amount");
    private final TextField tfFood   = UIHelper.field("Amount");
    private final TextField tfBonus  = UIHelper.field("Amount");

    // Deductions (employee_salaries table)
    private final TextField tfEpf    = UIHelper.field("Amount");
    private final TextField tfTax    = UIHelper.field("Amount");
    private final TextField tfPaye   = UIHelper.field("Amount");

    // % → amount helpers
    private final TextField tfEpfPerc  = UIHelper.field("%");
    private final TextField tfTaxPerc  = UIHelper.field("%");
    private final TextField tfPayePerc = UIHelper.field("%");

    // Hike panel — calls sp_apply_salary_hike (SQL Section 7 stored procedure)
    private final TextField tfHikePct  = UIHelper.field("e.g. 10 for 10%");
    private final Label     lblHikeResult = new Label();

    public PayrollView() {
        root.setPadding(new Insets(24));
        root.getStyleClass().add("tab-content");

        Label heading = UIHelper.heading("Payroll — Salary & Allowances");
        Label sub     = UIHelper.subheading(
                "Search an employee, configure salary details, then save. " +
                "Audit log is written automatically by DB triggers.");

        cbStructure.getItems().addAll(objPayroll.getSalaryStructures());

        root.getChildren().addAll(
                heading, sub,
                buildEmpStrip(),
                buildSalaryRow(),
                buildHikePanel(),
                buildButtons()
        );
        wirePercFields();
    }

    // ── Employee info strip ───────────────────────────────────────────────────
    private HBox buildEmpStrip() {
        HBox strip = new HBox(16);
        strip.getStyleClass().add("checkout-bar");
        strip.setPadding(new Insets(12, 20, 12, 20));
        strip.setAlignment(Pos.CENTER_LEFT);

        GridPane g = new GridPane(); g.setHgap(10); g.setVgap(8);
        UIHelper.addRow(g, 0, "Employee ID",  tfEmpId);
        UIHelper.addRow(g, 1, "First Name",   tfFname);
        UIHelper.addRow(g, 2, "Last Name",    tfLname);

        GridPane g2 = new GridPane(); g2.setHgap(10); g2.setVgap(8);
        UIHelper.addRow(g2, 0, "Designation",         tfDesig);
        UIHelper.addRow(g2, 1, "Department",           tfDept);
        UIHelper.addRow(g2, 2, "Salary Structure",     cbStructure);
        UIHelper.addRow(g2, 3, "Base Salary (₹)",      tfBaseSal);
        // last_revised_date — new column from SQL Section 2 ALTER TABLE
        UIHelper.addRow(g2, 4, "Last Revised",         tfLastRevised);

        strip.getChildren().addAll(g, g2);
        return strip;
    }

    // ── Allowances + Deductions panels ───────────────────────────────────────
    private HBox buildSalaryRow() {
        HBox row = new HBox(20);

        // Allowances panel (salary_structures table)
        VBox allowPanel = UIHelper.formCard();
        HBox.setHgrow(allowPanel, Priority.ALWAYS);
        Label h1    = new Label("Salary Allowances (Salary Structure)");
        h1.getStyleClass().add("panel-heading");
        Label hint1 = new Label("Shared across all employees on this structure. Updating affects all.");
        hint1.getStyleClass().add("hint-label");
        GridPane g1 = new GridPane(); g1.setHgap(10); g1.setVgap(12);
        g1.add(UIHelper.label(""),                  0, 0);
        g1.add(new Label("Amount (₹)"),             1, 0);
        addAmtRow(g1, 1, "Travel Allowance",  tfTravel);
        addAmtRow(g1, 2, "Food Allowance",    tfFood);
        addAmtRow(g1, 3, "Performance Bonus", tfBonus);
        allowPanel.getChildren().addAll(h1, hint1, g1);

        // Deductions panel (employee_salaries table)
        VBox deductPanel = UIHelper.formCard();
        HBox.setHgrow(deductPanel, Priority.ALWAYS);
        Label h2    = new Label("Salary Deductions");
        h2.getStyleClass().add("panel-heading");
        // Note: DB CHECK constraints enforce epf >= 0, tax >= 0, paye >= 0 (SQL Section 1 Table 7)
        Label hint2 = new Label("Enter % then Tab to auto-calc, or type amount directly.\n"
                              + "DB CHECK: base_salary > 0, all deductions ≥ 0.");
        hint2.getStyleClass().add("hint-label");
        hint2.setWrapText(true);
        GridPane g2 = new GridPane(); g2.setHgap(8); g2.setVgap(12);
        g2.add(UIHelper.label(""),           0, 0);
        g2.add(new Label("%"),               1, 0);
        g2.add(new Label("Amount (₹)"),      2, 0);
        addPercAmtRow(g2, 1, "EPF",  tfEpfPerc,  tfEpf);
        addPercAmtRow(g2, 2, "Tax",  tfTaxPerc,  tfTax);
        addPercAmtRow(g2, 3, "PAYE", tfPayePerc, tfPaye);
        deductPanel.getChildren().addAll(h2, hint2, g2);

        row.getChildren().addAll(allowPanel, deductPanel);
        return row;
    }

    // ── Hike panel (calls sp_apply_salary_hike — SQL Section 7 stored procedure) ──
    private VBox buildHikePanel() {
        VBox panel = UIHelper.formCard();
        Label h = new Label("Apply Salary Hike  (via Stored Procedure sp_apply_salary_hike)");
        h.getStyleClass().add("panel-heading");
        Label hint = new Label(
            "Uses CALL sp_apply_salary_hike(empId, hikePct, INOUT salary). " +
            "Trigger trg_salary_after_update auto-writes the audit log.");
        hint.getStyleClass().add("hint-label");
        hint.setWrapText(true);

        GridPane g = new GridPane(); g.setHgap(10); g.setVgap(10);
        tfHikePct.setMaxWidth(100);
        UIHelper.addRow(g, 0, "Hike Percentage", tfHikePct);

        lblHikeResult.setWrapText(true);
        lblHikeResult.setStyle("-fx-font-size: 11px;");

        Button btnHike     = UIHelper.primaryBtn("🚀 Apply Hike");
        Button btnAuditLog = UIHelper.secondaryBtn("📋 View Audit Log");

        btnHike.setOnAction(e -> handleApplyHike());
        btnAuditLog.setOnAction(e -> handleViewAuditLog());

        HBox btns = new HBox(12, btnHike, btnAuditLog);
        btns.setAlignment(Pos.CENTER_LEFT);

        panel.getChildren().addAll(h, hint, g, lblHikeResult, btns);
        return panel;
    }

    // ── Main action buttons ────────────────────────────────────────────────────
    private HBox buildButtons() {
        Button btnSearch = UIHelper.secondaryBtn("🔍 Search Employee");
        Button btnAdd    = UIHelper.primaryBtn("➕ Add Salary Record");
        Button btnUpdate = UIHelper.secondaryBtn("💾 Update");
        Button btnClear  = UIHelper.secondaryBtn("Clear");
        if (!Session.isAdmin()) {
    btnAdd.setDisable(true);
    btnUpdate.setDisable(true);
}
        btnSearch.setOnAction(e -> handleSearch());
        btnAdd.setOnAction(e    -> handleAdd());
        btnUpdate.setOnAction(e -> handleUpdate());
        btnClear.setOnAction(e  -> clearForm());

        HBox row = new HBox(12, btnSearch, btnAdd, btnUpdate, btnClear);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ── Grid helpers ──────────────────────────────────────────────────────────
    private void addAmtRow(GridPane g, int row, String label, TextField amt) {
        g.add(UIHelper.label(label), 0, row);
        amt.setMaxWidth(130);
        g.add(amt, 1, row);
        GridPane.setMargin(amt, new Insets(0, 0, 0, 8));
    }

    private void addPercAmtRow(GridPane g, int row, String label, TextField perc, TextField amt) {
        g.add(UIHelper.label(label), 0, row);
        perc.setMaxWidth(65); amt.setMaxWidth(110);
        g.add(perc, 1, row); g.add(amt, 2, row);
        GridPane.setMargin(perc, new Insets(0, 0, 0, 8));
        GridPane.setMargin(amt,  new Insets(0, 0, 0, 8));
    }

    // ── % field wiring ────────────────────────────────────────────────────────
    private void wirePercFields() {
        wireOne(tfEpfPerc, tfEpf);
        wireOne(tfTaxPerc, tfTax);
        wireOne(tfPayePerc, tfPaye);
    }

    private void wireOne(TextField perc, TextField amt) {
        perc.focusedProperty().addListener((o, old, focused) -> {
            if (!focused) calcAmt(perc, amt);
        });
        perc.setOnAction(e -> calcAmt(perc, amt));
    }

    private void calcAmt(TextField perc, TextField amt) {
        try {
            double p   = Double.parseDouble(perc.getText().trim());
            double sal = Double.parseDouble(tfBaseSal.getText().trim());
            amt.setText(String.format("%.2f", objPayroll.calculate(p, sal)));
        } catch (Exception ignored) {}
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private void handleSearch() {
        String idStr = UIHelper.prompt("Search Employee", "Enter Employee ID:");
        if (idStr == null || idStr.isBlank()) return;
        try {
            int id = Integer.parseInt(idStr.trim());
            if (!Session.isAdmin()) {
    id = Session.getEmpId();
}
            if (objPayroll.getSalaryDetails(id)) {
                tfEmpId.setText(String.valueOf(objPayroll.objEmployee.getEmpId()));
                tfFname.setText(objPayroll.objEmployee.getFname());
                tfLname.setText(objPayroll.objEmployee.getLname());
                tfDesig.setText(objPayroll.objEmployee.getDesignation());
                tfDept.setText(objPayroll.objEmployee.getDepartment());
                tfBaseSal.setText(String.valueOf((int) objPayroll.getBaseSalary()));
                tfTravel.setText(String.valueOf(objPayroll.getTravelAmount()));
                tfFood.setText(String.valueOf(objPayroll.getFoodAmount()));
                tfBonus.setText(String.valueOf(objPayroll.getBonusAmount()));
                tfEpf.setText(String.valueOf(objPayroll.getEpfAmount()));
                tfTax.setText(String.valueOf(objPayroll.getTaxAmount()));
                tfPaye.setText(String.valueOf(objPayroll.getPayeAmount()));
                // Display last_revised_date (new column from SQL Section 2)
                String lrd = objPayroll.getLastRevisedDate();
                tfLastRevised.setText(lrd.isBlank() ? "Not yet updated" : lrd);
                int sid = objPayroll.getStructureId();
                if (sid > 0 && sid <= cbStructure.getItems().size())
                    cbStructure.getSelectionModel().select(sid - 1);
                lblHikeResult.setText("");
            } else {
                UIHelper.showAlert(Alert.AlertType.ERROR, "Not Found", objPayroll.getLastMessage());
            }
        } catch (NumberFormatException e) {
            UIHelper.showAlert(Alert.AlertType.ERROR, "Input Error", "Please enter a valid numeric ID.");
        }
    }

    private void handleAdd() {
        if (tfEmpId.getText().isBlank()) {
            UIHelper.showAlert(Alert.AlertType.WARNING, "No Employee", "Search an employee first.");
            return;
        }
        if (!collectValues()) return;
        if (objPayroll.insertSalaryDetails()) {
            UIHelper.showAlert(Alert.AlertType.INFORMATION, "Added", objPayroll.getLastMessage());
            // Refresh to show auto-generated last_revised_date from DB DEFAULT
            handleSearch();
        } else {
            UIHelper.showAlert(Alert.AlertType.ERROR, "Error", objPayroll.getLastMessage());
        }
    }

    private void handleUpdate() {
        if (tfEmpId.getText().isBlank()) {
            UIHelper.showAlert(Alert.AlertType.WARNING, "No Employee", "Search an employee first.");
            return;
        }
        if (!collectValues()) return;
        int id = Integer.parseInt(tfEmpId.getText());
        if (objPayroll.updateSalaryDetails(id)) {
            UIHelper.showAlert(Alert.AlertType.INFORMATION, "Updated",
                    objPayroll.getLastMessage() +
                    "\nAudit log entry written automatically by DB trigger.");
            // Refresh last_revised_date — it is now CURDATE()
            handleSearch();
        } else {
            UIHelper.showAlert(Alert.AlertType.ERROR, "Error", objPayroll.getLastMessage());
        }
    }

    /**
     * Apply salary hike using stored procedure sp_apply_salary_hike.
     * SQL Section 7 — INOUT parameter mode:
     *   IN  p_empid   INT
     *   IN  p_hike_pct DOUBLE
     *   INOUT p_old_salary INT  → returns new salary
     * The SP also sets last_revised_date = CURDATE(), which triggers the audit log.
     */
    private void handleApplyHike() {
        if (tfEmpId.getText().isBlank()) {
            UIHelper.showAlert(Alert.AlertType.WARNING, "No Employee", "Search an employee first.");
            return;
        }
        String hikeTxt = tfHikePct.getText().trim();
        if (hikeTxt.isBlank()) {
            UIHelper.showAlert(Alert.AlertType.WARNING, "Input Required", "Enter a hike percentage.");
            return;
        }
        double hikePct;
        try {
            hikePct = Double.parseDouble(hikeTxt);
            if (hikePct <= 0 || hikePct > 100) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            UIHelper.showAlert(Alert.AlertType.ERROR, "Invalid Input",
                    "Hike % must be a positive number between 0 and 100.");
            return;
        }
        int empId = Integer.parseInt(tfEmpId.getText());
        double newSalary = objPayroll.applySalaryHike(empId, hikePct);
        if (newSalary > 0) {
            lblHikeResult.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
            lblHikeResult.setText("✅ " + objPayroll.getLastMessage()
                    + "  |  Audit log updated by trigger.");
            // Refresh form to show new base salary and updated last_revised_date
            handleSearch();
        } else {
            lblHikeResult.setStyle("-fx-text-fill: #ef4444;");
            lblHikeResult.setText("⚠ " + objPayroll.getLastMessage());
        }
    }

    /**
     * Read payroll_audit_log for this employee (SQL Section 1 Table 13).
     * Entries are written by triggers trg_salary_after_insert and trg_salary_after_update
     * (SQL Section 9) — never by Java directly.
     */
    private void handleViewAuditLog() {
        if (tfEmpId.getText().isBlank()) {
            UIHelper.showAlert(Alert.AlertType.WARNING, "No Employee", "Search an employee first.");
            return;
        }
        int empId = Integer.parseInt(tfEmpId.getText());
        List<String> log = objPayroll.getAuditLog(empId);
        if (log.isEmpty()) {
            UIHelper.showAlert(Alert.AlertType.INFORMATION, "Audit Log",
                    "No audit records found for Employee ID: " + empId +
                    "\n(Records are created automatically when salary is inserted or updated.)");
        } else {
            StringBuilder sb = new StringBuilder(
                    "Salary Audit Log for Employee ID: " + empId + "\n");
            for (String entry : log) sb.append(entry).append("\n");
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
alert.setTitle("Audit Log");
alert.setHeaderText("Salary Audit Log for Employee ID: " + empId);

TextArea area = new TextArea(sb.toString());
area.setEditable(false);
area.setWrapText(true);
area.setPrefWidth(500);
area.setPrefHeight(300);

alert.getDialogPane().setContent(area);
alert.showAndWait();
            //UIHelper.showAlert(Alert.AlertType.INFORMATION, "Audit Log", sb.toString());
        }
    }

    private boolean collectValues() {
        try {
            objPayroll.objEmployee.setEmpId(Integer.parseInt(tfEmpId.getText()));
            objPayroll.setBaseSalary(Double.parseDouble(
                    tfBaseSal.getText().isEmpty() ? "0" : tfBaseSal.getText()));
            int sid = cbStructure.getSelectionModel().getSelectedIndex() + 1;
            objPayroll.setStructureId(sid > 0 ? sid : 1);
            objPayroll.setTravelAmount(Double.parseDouble(
                    tfTravel.getText().isEmpty() ? "0" : tfTravel.getText()));
            objPayroll.setFoodAmount(Double.parseDouble(
                    tfFood.getText().isEmpty() ? "0" : tfFood.getText()));
            objPayroll.setBonusAmount(Double.parseDouble(
                    tfBonus.getText().isEmpty() ? "0" : tfBonus.getText()));
            objPayroll.setEpfAmount(Double.parseDouble(
                    tfEpf.getText().isEmpty() ? "0" : tfEpf.getText()));
            objPayroll.setTaxAmount(Double.parseDouble(
                    tfTax.getText().isEmpty() ? "0" : tfTax.getText()));
            objPayroll.setPayeAmount(Double.parseDouble(
                    tfPaye.getText().isEmpty() ? "0" : tfPaye.getText()));
            return true;
        } catch (Exception e) {
            UIHelper.showAlert(Alert.AlertType.ERROR, "Input Error",
                    "Please fill all amount fields with valid numbers.\n" +
                    "Note: base_salary must be > 0 (DB CHECK constraint).");
            return false;
        }
    }

    private void clearForm() {
        tfEmpId.clear(); tfFname.clear(); tfLname.clear();
        tfDesig.clear(); tfDept.clear();
        tfBaseSal.clear(); cbStructure.setValue(null);
        tfLastRevised.setText("Not yet updated");
        tfTravel.clear(); tfFood.clear(); tfBonus.clear();
        tfEpf.clear(); tfTax.clear(); tfPaye.clear();
        tfEpfPerc.clear(); tfTaxPerc.clear(); tfPayePerc.clear();
        tfHikePct.clear();
        lblHikeResult.setText("");
    }

    public ScrollPane getRoot() {
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        return scroll;
    }
}
