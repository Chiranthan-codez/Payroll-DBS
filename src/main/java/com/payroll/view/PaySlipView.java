package com.payroll.view;

import com.payroll.model.PaySlip;
import com.payroll.util.UIHelper;
import com.payroll.util.Session;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Scale;

/**
 * PaySlipView — updated for payrolldbs_project.sql.
 *
 * New additions from SQL Section 2 (ALTER TABLE):
 *   employee_salaries.last_revised_date — displayed on slip as "Pay Period / Revised"
 *   Written automatically by updateSalaryDetails() with CURDATE().
 *   Also updated by stored procedure sp_apply_salary_hike (SQL Section 7).
 *
 * Calculation matches vw_payslip view definition (SQL Section 4):
 *   gross_pay        = base_salary + travel_allowance + food_allowance + bonus
 *   total_deductions = epf + tax + paye
 *   net_pay          = gross_pay - total_deductions
 *
 * Trigger awareness:
 *   trg_salary_after_update (SQL Section 9) writes payroll_audit_log
 *   every time salary is saved — not shown on slip but confirmed in PayrollView.
 */
public class PaySlipView {

    private final VBox root = new VBox(16);
    private final PaySlip objPaySlip = new PaySlip();

    // Employee details
    private final Label lblEmpId      = valueLabel("—");
    private final Label lblName       = valueLabel("—");
    private final Label lblDesig      = valueLabel("—");
    private final Label lblDept       = valueLabel("—");
    // New: last_revised_date from employee_salaries (SQL Section 2 ALTER TABLE)
    private final Label lblRevised    = valueLabel("—");

    // Earnings
    private final Label lblBasic      = valueLabel("—");
    private final Label lblTravel     = valueLabel("—");
    private final Label lblFood       = valueLabel("—");
    private final Label lblBonus      = valueLabel("—");
    private final Label lblGross      = valueLabel("—");

    // Deductions
    private final Label lblEpf        = valueLabel("—");
    private final Label lblTax        = valueLabel("—");
    private final Label lblPaye       = valueLabel("—");
    private final Label lblDeduct     = valueLabel("—");

    // Net
    private final Label lblNet        = valueLabel("—");

    private VBox printArea;

    public PaySlipView() {
        root.setPadding(new Insets(24));
        root.getStyleClass().add("tab-content");

        Label heading = UIHelper.heading("Pay Slip");
        Label sub     = UIHelper.subheading(
            "Search an employee to generate the pay slip. " +
            "Last Revised date shows when salary was last updated.");

        printArea = buildPrintArea();
        root.getChildren().addAll(heading, sub, printArea, buildButtons());
    }

    private VBox buildPrintArea() {
        VBox area = new VBox(12);

        // Company header
        VBox compHeader = new VBox(4);
        compHeader.setAlignment(Pos.CENTER);
        compHeader.getStyleClass().add("payslip-header");
        compHeader.setPadding(new Insets(14));
        Label compName  = new Label("PAYROLL MANAGEMENT SYSTEM");
        compName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        compName.setStyle("-fx-text-fill: #1a2332;");
        Label slipTitle = new Label("SALARY SLIP");
        slipTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        slipTitle.setStyle("-fx-text-fill: #64748b;");
        compHeader.getChildren().addAll(compName, slipTitle);

        // Employee info grid — includes last_revised_date (new SQL Section 2 column)
        GridPane empGrid = new GridPane();
        empGrid.setHgap(16); empGrid.setVgap(8);
        empGrid.setPadding(new Insets(12, 16, 12, 16));
        empGrid.getStyleClass().add("payslip-section");
        addSlipRow(empGrid, 0, "Employee ID :",  lblEmpId,  "Name :",            lblName);
        addSlipRow(empGrid, 1, "Designation :",  lblDesig,  "Department :",      lblDept);
        // last_revised_date row — new in payrolldbs_project.sql (Section 2 ALTER TABLE)
        addSlipRow(empGrid, 2, "Last Revised :", lblRevised, "", new Label(""));

        // Earnings + deductions side by side
        HBox midRow = new HBox(12, earningsPanel(), deductionsPanel());

        // Net pay bar
        HBox netRow = new HBox();
        netRow.getStyleClass().add("net-pay-bar");
        netRow.setPadding(new Insets(14, 20, 14, 20));
        netRow.setAlignment(Pos.CENTER_RIGHT);
        Label netLbl = new Label("NET PAY : ");
        netLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        netLbl.setStyle("-fx-text-fill: #1a2332;");
        lblNet.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lblNet.setStyle("-fx-text-fill: #1a6e3c;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        netRow.getChildren().addAll(sp, netLbl, lblNet);

        area.getChildren().addAll(compHeader, empGrid, midRow, netRow);
        return area;
    }

    private GridPane earningsPanel() {
        GridPane g = new GridPane();
        g.setHgap(16); g.setVgap(8);
        g.setPadding(new Insets(12));
        g.getStyleClass().add("payslip-section");
        HBox.setHgrow(g, Priority.ALWAYS);

        Label h = new Label("EARNINGS");
        h.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        h.setStyle("-fx-text-fill: #1a2332;");
        g.add(h, 0, 0); GridPane.setColumnSpan(h, 2);

        Label amtHdr = new Label("Amount (₹)");
        amtHdr.setStyle("-fx-font-weight: bold; -fx-text-fill: #64748b; -fx-font-size: 11px;");
        g.add(new Label(""), 0, 1);
        g.add(amtHdr, 1, 1);

        addEarningRow(g, 2, "Basic Pay :",            lblBasic);
        addEarningRow(g, 3, "+ Travel Allowance :",   lblTravel);
        addEarningRow(g, 4, "+ Food Allowance :",     lblFood);
        addEarningRow(g, 5, "+ Performance Bonus :",  lblBonus);

        Separator sep = new Separator(); GridPane.setColumnSpan(sep, 2);
        g.add(sep, 0, 6);

        Label grossLbl = new Label("Gross Pay :");
        grossLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblGross.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblGross.setStyle("-fx-text-fill: #1a6e3c;");
        g.add(grossLbl, 0, 7); g.add(lblGross, 1, 7);
        return g;
    }

    private GridPane deductionsPanel() {
        GridPane g = new GridPane();
        g.setHgap(16); g.setVgap(8);
        g.setPadding(new Insets(12));
        g.getStyleClass().add("payslip-section");
        HBox.setHgrow(g, Priority.ALWAYS);

        Label h = new Label("DEDUCTIONS");
        h.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        h.setStyle("-fx-text-fill: #1a2332;");
        g.add(h, 0, 0); GridPane.setColumnSpan(h, 2);

        Label amtHdr = new Label("Amount (₹)");
        amtHdr.setStyle("-fx-font-weight: bold; -fx-text-fill: #64748b; -fx-font-size: 11px;");
        g.add(new Label(""), 0, 1);
        g.add(amtHdr, 1, 1);

        // DB CHECK constraints: epf >= 0, tax >= 0, paye >= 0 (SQL Section 1 Table 7)
        addEarningRow(g, 2, "- EPF :",            lblEpf);
        addEarningRow(g, 3, "- Employee Tax :",   lblTax);
        addEarningRow(g, 4, "- PAYE :",           lblPaye);

        Separator sep = new Separator(); GridPane.setColumnSpan(sep, 2);
        g.add(sep, 0, 5);

        Label deductLbl = new Label("Total Deductions :");
        deductLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblDeduct.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblDeduct.setStyle("-fx-text-fill: #c0392b;");
        g.add(deductLbl, 0, 6); g.add(lblDeduct, 1, 6);
        return g;
    }

    private HBox buildButtons() {
        Button btnSearch = UIHelper.secondaryBtn("🔍 Search Employee");
      //  Button btnPrint  = UIHelper.primaryBtn("🖨 Print Pay Slip");
        Button btnClear  = UIHelper.secondaryBtn("Clear");
        btnSearch.setOnAction(e -> handleSearch());
        //btnPrint.setOnAction(e  -> handlePrint());, btnPrint
        btnClear.setOnAction(e  -> clearSlip());
        HBox row = new HBox(12, btnSearch, btnClear);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
private void handleSearch() {

    int id;

    try {

        /* If employee → automatically use logged-in empid */
        if (!Session.isAdmin()) {

            id = Session.getEmpId();

        } else {

            String idStr = UIHelper.prompt("Search Employee", "Enter Employee ID:");
            if (idStr == null || idStr.isBlank()) return;

            id = Integer.parseInt(idStr.trim());
        }

        if (objPaySlip.getPayDetails(id)) {
            populateSlip();
        } else {
            UIHelper.showAlert(Alert.AlertType.ERROR, "Not Found",
                    objPaySlip.getLastMessage());
        }

    } catch (NumberFormatException ex) {

        UIHelper.showAlert(Alert.AlertType.ERROR, "Input Error",
                "Please enter a valid numeric Employee ID.");
    }
}
  //  private void handleSearch() {
     //   String idStr = UIHelper.prompt("Search Employee", "Enter Employee ID:");
     //   if (idStr == null || idStr.isBlank()) return;
     //   try {
       //     int id = Integer.parseInt(idStr.trim());
     //       if (!Session.isAdmin()) {
   // id = Session.getEmpId();
//}
   //         if (objPaySlip.getPayDetails(id)) populateSlip();
    //        else UIHelper.showAlert(Alert.AlertType.ERROR, "Not Found", objPaySlip.getLastMessage());
     //   } catch (NumberFormatException ex) {
      //      UIHelper.showAlert(Alert.AlertType.ERROR, "Input Error",
      ////          "Please enter a valid numeric Employee ID.");
      //  }
  //  }

    private void handlePrint() {
        if (lblNet.getText().equals("—") || lblNet.getText().isBlank()) {
            UIHelper.showAlert(Alert.AlertType.WARNING, "No Pay Slip",
                "Search an employee first to generate the pay slip.");
            return;
        }
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            UIHelper.showAlert(Alert.AlertType.ERROR, "Print Error", "No printer found.");
            return;
        }
        if (job.showPrintDialog(root.getScene().getWindow())) {
            PageLayout pageLayout = job.getJobSettings().getPageLayout();
            double pgW    = pageLayout.getPrintableWidth();
            double pgH    = pageLayout.getPrintableHeight();
            double scaleX = pgW / printArea.getBoundsInParent().getWidth();
            double scaleY = pgH / printArea.getBoundsInParent().getHeight();
            double scale  = Math.min(scaleX, scaleY);
            Scale scaleTransform = new Scale(scale, scale);
            printArea.getTransforms().add(scaleTransform);
            boolean success = job.printPage(printArea);
            printArea.getTransforms().remove(scaleTransform);
            if (success) {
                job.endJob();
                UIHelper.showAlert(Alert.AlertType.INFORMATION, "Printed",
                    "Pay slip sent to printer.");
            } else {
                UIHelper.showAlert(Alert.AlertType.ERROR, "Print Failed",
                    "Could not print the pay slip.");
            }
        }
    }

    private void populateSlip() {
        
            lblEmpId.setText(String.valueOf(objPaySlip.objEmployee.getEmpId()));
        lblName.setText(objPaySlip.objEmployee.getFname() + " " + objPaySlip.objEmployee.getLname());
        lblDesig.setText(objPaySlip.objEmployee.getDesignation());
        lblDept.setText(objPaySlip.objEmployee.getDepartment());

        // last_revised_date — new column from SQL Section 2 ALTER TABLE
        String rev = objPaySlip.getLastRevisedDate();
        lblRevised.setText(rev.isBlank() ? "Not recorded" : rev);

        // Earnings — matches vw_payslip view (SQL Section 4)
        lblBasic.setText(fmt(objPaySlip.objEmployee.getSalAmount()));
        lblTravel.setText(fmt(objPaySlip.objPayroll.getTravelAmount()));
        lblFood.setText(fmt(objPaySlip.objPayroll.getFoodAmount()));
        lblBonus.setText(fmt(objPaySlip.objPayroll.getBonusAmount()));
        lblGross.setText(fmt(objPaySlip.getGrossPay()));

        // Deductions
        lblEpf.setText(fmt(objPaySlip.objPayroll.getEpfAmount()));
        lblTax.setText(fmt(objPaySlip.objPayroll.getTaxAmount()));
        lblPaye.setText(fmt(objPaySlip.objPayroll.getPayeAmount()));
        lblDeduct.setText(fmt(objPaySlip.getTotalDeductions()));

        lblNet.setText(fmt(objPaySlip.getNetPay()));
    }

    private void clearSlip() {
        for (Label l : new Label[]{lblEmpId, lblName, lblDesig, lblDept, lblRevised,
                                   lblBasic, lblTravel, lblFood, lblBonus, lblGross,
                                   lblEpf, lblTax, lblPaye, lblDeduct, lblNet}) {
            l.setText("—");
        }
    }

    private static Label valueLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 13px;");
        return l;
    }

    private static String fmt(double v) { return String.format("%,.2f", v); }

    private void addSlipRow(GridPane g, int row,
                            String lbl1, Label val1,
                            String lbl2, Label val2) {
        Label l1 = new Label(lbl1); l1.setStyle("-fx-font-weight: bold; -fx-text-fill: #64748b;");
        Label l2 = new Label(lbl2); l2.setStyle("-fx-font-weight: bold; -fx-text-fill: #64748b;");
        g.add(l1, 0, row); g.add(val1, 1, row);
        g.add(l2, 2, row); g.add(val2, 3, row);
        GridPane.setMargin(val1, new Insets(0, 30, 0, 6));
        GridPane.setMargin(val2, new Insets(0, 0,  0, 6));
    }

    private void addEarningRow(GridPane g, int row, String lbl, Label val) {
        Label l = new Label(lbl); l.setStyle("-fx-text-fill: #374151;");
        val.setStyle("-fx-text-fill: #374151;");
        g.add(l, 0, row); g.add(val, 1, row);
        GridPane.setMargin(val, new Insets(0, 0, 0, 10));
    }

   // public VBox getRoot() { return root; }
   public ScrollPane getRoot() {

    ScrollPane scroll = new ScrollPane(root);
    scroll.setFitToWidth(true);   // makes content stretch to width
    scroll.setPannable(true);     // optional smoother scrolling
    scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

    return scroll;
 }
}
