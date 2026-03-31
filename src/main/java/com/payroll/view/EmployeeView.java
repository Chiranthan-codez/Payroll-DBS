package com.payroll.view;
import com.payroll.util.Session;
import com.payroll.model.Employee;
import com.payroll.util.UIHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;



public class EmployeeView {

    private final HBox root = new HBox(24);
    private final Employee objEmp = new Employee();

    // Form fields
    private final TextField    tfEmpId       = UIHelper.readOnly("Auto-generated");
    private final TextField    tfNic         = UIHelper.field("e.g. 931552171V");
    private final TextField    tfFname       = UIHelper.field("First name");
    private final TextField    tfLname       = UIHelper.field("Last name");
    private final TextField    tfDob         = UIHelper.field("YYYY-MM-DD");
    private final TextField    tfAddrLine1   = UIHelper.field("Address line 1");
    private final TextField    tfAddrLine2   = UIHelper.field("Address line 2 (optional)");
    private final TextField    tfCity        = UIHelper.field("City");
    private final TextField    tfPostalCode  = UIHelper.field("Postal code");
    private final TextField    tfTelHome     = UIHelper.field("Home phone (optional)");
    private final TextField    tfTelMob      = UIHelper.field("Mobile (optional)");
    private final TextField    tfDateJoin    = UIHelper.field("YYYY-MM-DD");
    private final ComboBox<String> cbDept    = UIHelper.combo("Select department");
    private final ComboBox<String> cbDesig   = UIHelper.combo("Select designation");

    // Gender radio buttons — 3 options matching DB CHECK: ('Male','Female','Other')
    private final ToggleGroup  genderGroup   = new ToggleGroup();
    private final RadioButton  rdMale        = new RadioButton("Male");
    private final RadioButton  rdFemale      = new RadioButton("Female");
    private final RadioButton  rdOther       = new RadioButton("Other");

    // Performance Rating Spinner — matches employees.performance_rating TINYINT CHECK (1-5)
    private final Spinner<Integer> spRating  = new Spinner<>(1, 5, 3);

    // Table
    private final TableView<Employee> table    = new TableView<>();
    private final TextField           tfSearch = new TextField();
    private ObservableList<Employee>  data;

    public EmployeeView() {
        root.setPadding(new Insets(24));
        root.getStyleClass().add("tab-content");

        rdMale.setToggleGroup(genderGroup);
        rdFemale.setToggleGroup(genderGroup);
        rdOther.setToggleGroup(genderGroup);
        rdMale.setSelected(true);

        spRating.setEditable(true);
        spRating.setPrefWidth(80);

        cbDept.getItems().addAll(objEmp.getAllDepartments());
        cbDesig.getItems().addAll(objEmp.getAllDesignations());

        root.getChildren().addAll(buildForm(), buildTablePanel());
        refreshTable();
    }

    // ── Form Builder ──────────────────────────────────────────────────────────
    private VBox buildForm() {
        VBox card = UIHelper.formCard();
        card.setPrefWidth(440);

        Label heading = UIHelper.heading("Employee Management");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        UIHelper.addRow(grid, 0,  "Employee ID",       tfEmpId);
        UIHelper.addRow(grid, 1,  "NIC *",             tfNic);
        UIHelper.addRow(grid, 2,  "First Name *",      tfFname);
        UIHelper.addRow(grid, 3,  "Last Name *",       tfLname);
        UIHelper.addRow(grid, 4,  "Date of Birth",     tfDob);
        UIHelper.addRow(grid, 5,  "Address Line 1",    tfAddrLine1);
        UIHelper.addRow(grid, 6,  "Address Line 2",    tfAddrLine2);
        UIHelper.addRow(grid, 7,  "City",              tfCity);
        UIHelper.addRow(grid, 8,  "Postal Code",       tfPostalCode);
        UIHelper.addRow(grid, 9,  "Home Phone",        tfTelHome);
        UIHelper.addRow(grid, 10, "Mobile",            tfTelMob);
        UIHelper.addRow(grid, 11, "Department *",      cbDept);
        UIHelper.addRow(grid, 12, "Designation *",     cbDesig);
        UIHelper.addRow(grid, 13, "Date of Joining *", tfDateJoin);

        // Gender row — 3 options matching DB CHECK constraint (SQL Section 1 Table 3)
        HBox genderRow = new HBox(12, rdMale, rdFemale, rdOther);
        genderRow.setAlignment(Pos.CENTER_LEFT);
        grid.add(UIHelper.label("Gender"), 0, 14);
        grid.add(genderRow, 1, 14);

        // Performance Rating spinner — DB column: performance_rating TINYINT DEFAULT 3 CHECK(1-5)
        HBox ratingRow = new HBox(8, spRating, new Label("(1 = Poor  …  5 = Excellent)"));
        ratingRow.setAlignment(Pos.CENTER_LEFT);
        grid.add(UIHelper.label("Perf. Rating"), 0, 15);
        grid.add(ratingRow, 1, 15);

        Button btnAdd    = UIHelper.primaryBtn("➕ Add Employee");
        Button btnSearch = UIHelper.secondaryBtn("🔍 Load by ID");
        Button btnUpdate = UIHelper.secondaryBtn("💾 Update");
        Button btnDelete = UIHelper.dangerBtn("🗑 Delete");
        Button btnClear  = UIHelper.secondaryBtn("Clear");
        if (!Session.isAdmin()) {
    btnAdd.setDisable(true);
    btnDelete.setDisable(true);
    btnUpdate.setDisable(true);
}
        btnAdd.setOnAction(e    -> handleAdd());
        btnSearch.setOnAction(e -> handleSearch());
        btnUpdate.setOnAction(e -> handleUpdate());
        btnDelete.setOnAction(e -> handleDelete());
        btnClear.setOnAction(e  -> clearForm());

        HBox row1 = new HBox(10, btnAdd, btnClear);
        HBox row2 = new HBox(10, btnSearch, btnUpdate, btnDelete);
        row1.setAlignment(Pos.CENTER_LEFT);
        row2.setAlignment(Pos.CENTER_LEFT);

        Label hint = new Label("* Required  |  Salary configured in the Payroll tab");
        hint.getStyleClass().add("hint-label");

        card.getChildren().addAll(heading, grid, row1, row2, hint);
        return card;
    }

    // ── Table Panel ───────────────────────────────────────────────────────────
    private VBox buildTablePanel() {
        VBox panel = new VBox(12);
        HBox.setHgrow(panel, Priority.ALWAYS);
        panel.getStyleClass().add("table-panel");

        Label heading = UIHelper.heading("All Employees");

        tfSearch.setPromptText("🔍 Search by ID, name, dept, designation…");
        tfSearch.getStyleClass().add("search-field");
        tfSearch.textProperty().addListener((o, old, q) -> {
            if (q == null || q.isBlank()) refreshTable();
            else data.setAll(objEmp.searchEmployees(q));
        });

        buildColumns();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        table.getSelectionModel().selectedItemProperty().addListener((o, old, sel) -> {
            if (sel != null) fillForm(sel);
        });

        panel.getChildren().addAll(heading, tfSearch, table);
        return panel;
    }

    @SuppressWarnings("unchecked")
    private void buildColumns() {
        TableColumn<Employee, Integer> colId    = col("ID",          "empId",           55);
        TableColumn<Employee, String>  colName  = new TableColumn<>("Name");
        colName.setMinWidth(140);
        colName.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getFname() + " " + cd.getValue().getLname()));
        TableColumn<Employee, String>  colDesig  = col("Designation",    "designation",     120);
        TableColumn<Employee, String>  colDept   = col("Department",     "department",      110);
        TableColumn<Employee, String>  colGend   = col("Gender",         "gender",           70);
        TableColumn<Employee, String>  colNic    = col("NIC",            "nic",             110);
        TableColumn<Employee, String>  colCity   = col("City",           "city",             80);
        TableColumn<Employee, String>  colDoj    = col("Joined",         "dateOfJoining",   100);
        // New column: performanceRating (SQL Section 2 ALTER TABLE)
        TableColumn<Employee, Integer> colRating = col("Rating",         "performanceRating", 60);

        table.getColumns().addAll(colId, colName, colDesig, colDept, colGend,
                                  colNic, colCity, colDoj, colRating);
    }

    // ── Handlers ──────────────────────────────────────────────────────────────
    private void handleAdd() {
        if (!validate()) return;
        populateObj();
        if (objEmp.insertEmployee()) {
            UIHelper.showAlert(Alert.AlertType.INFORMATION, "Success", objEmp.getLastMessage());
            tfEmpId.setText(String.valueOf(objEmp.getEmpId()));
            refreshTable();
            clearForm();
        } else {
            UIHelper.showAlert(Alert.AlertType.ERROR, "Error", objEmp.getLastMessage());
        }
    }

    private void handleSearch() {
        String idStr = UIHelper.prompt("Search Employee", "Enter Employee ID:");
        if (idStr == null || idStr.isBlank()) return;
        try {
            //int id = Integer.parseInt(idStr.trim());
          //  if (objEmp.getEmployeeDetails(id)) fillForm(objEmp);
           // else UIHelper.showAlert(Alert.AlertType.ERROR, "Not Found", objEmp.getLastMessage());
        int id = Integer.parseInt(idStr.trim());

/* Employee restriction */
if (!Session.isAdmin()) {
    id = Session.getEmpId(); // force employee to only see themselves
}

if (objEmp.getEmployeeDetails(id))
    fillForm(objEmp);
else
    UIHelper.showAlert(Alert.AlertType.ERROR, "Not Found", objEmp.getLastMessage());
        } catch (NumberFormatException e) {
            UIHelper.showAlert(Alert.AlertType.ERROR, "Input Error", "Please enter a valid numeric ID.");
        }
    }

    private void handleUpdate() {
        if (tfEmpId.getText().isBlank()) {
            UIHelper.showAlert(Alert.AlertType.WARNING, "No Employee", "Search/select an employee first.");
            return;
        }
        if (!validate()) return;
        populateObj();
        if (objEmp.updateEmployee()) {
            UIHelper.showAlert(Alert.AlertType.INFORMATION, "Updated", objEmp.getLastMessage());
            refreshTable();
            clearForm();
        } else {
            UIHelper.showAlert(Alert.AlertType.ERROR, "Error", objEmp.getLastMessage());
        }
    }

    private void handleDelete() {
        String idStr = tfEmpId.getText();
        if (idStr == null || idStr.isBlank())
            idStr = UIHelper.prompt("Delete Employee", "Enter Employee ID to delete:");
        if (idStr == null || idStr.isBlank()) return;
        try {
            int id = Integer.parseInt(idStr.trim());
            if (!UIHelper.confirm("Confirm Delete",
                    "Delete Employee ID " + id + "?\nThis cannot be undone.\n"
                    + "Note: Admin users cannot be deleted (DB trigger protection).")) return;
            Employee del = new Employee();
            if (del.deleteEmployee(id)) {
                UIHelper.showAlert(Alert.AlertType.INFORMATION, "Deleted", del.getLastMessage());
                clearForm();
                refreshTable();
            } else {
                // Message may contain trigger error (SQLSTATE 45000) for Admin users
                UIHelper.showAlert(Alert.AlertType.ERROR, "Error", del.getLastMessage());
            }
        } catch (NumberFormatException e) {
            UIHelper.showAlert(Alert.AlertType.ERROR, "Input Error", "Please enter a valid numeric ID.");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private boolean validate() {
        if (tfNic.getText().isBlank() || tfFname.getText().isBlank() ||
            tfLname.getText().isBlank() || tfDateJoin.getText().isBlank() ||
            cbDept.getValue() == null || cbDesig.getValue() == null) {
            UIHelper.showAlert(Alert.AlertType.WARNING, "Validation",
                "Fields marked * are required.");
            return false;
        }
        // NIC minimum length validated by trg_emp_before_insert (SQL Section 9) — show friendly message
        if (tfNic.getText().trim().length() < 10) {
            UIHelper.showAlert(Alert.AlertType.WARNING, "Validation",
                "NIC must be at least 10 characters long.");
            return false;
        }
        return true;
    }

    private void populateObj() {
        if (!tfEmpId.getText().isBlank()) {
            try { objEmp.setEmpId(Integer.parseInt(tfEmpId.getText())); } catch (Exception ignored) {}
        }
        objEmp.setNic(tfNic.getText().trim());
        objEmp.setFname(tfFname.getText().trim());
        objEmp.setLname(tfLname.getText().trim());
        objEmp.setDob(tfDob.getText().trim());
        objEmp.setAddressLine1(tfAddrLine1.getText());
        objEmp.setAddressLine2(tfAddrLine2.getText());
        objEmp.setCity(tfCity.getText().trim());
        objEmp.setPostalCode(tfPostalCode.getText().trim());
        objEmp.setTelHome(tfTelHome.getText().trim());
        objEmp.setTelMobile(tfTelMob.getText().trim());
        objEmp.setDepartment(cbDept.getValue());
        objEmp.setDesignation(cbDesig.getValue());
        objEmp.setDateOfJoining(tfDateJoin.getText().trim());
        // Gender — includes 'Other' to satisfy DB CHECK: IN('Male','Female','Other')
        if (rdFemale.isSelected())     objEmp.setGender("Female");
        else if (rdOther.isSelected()) objEmp.setGender("Other");
        else                           objEmp.setGender("Male");
        // Performance rating — clamped to 1-5 by Spinner range; DB also has CHECK constraint
        objEmp.setPerformanceRating(spRating.getValue());
    }

    private void fillForm(Employee e) {
        tfEmpId.setText(String.valueOf(e.getEmpId()));
        tfNic.setText(e.getNic());
        tfFname.setText(e.getFname());
        tfLname.setText(e.getLname());
        tfDob.setText(e.getDob());
        tfAddrLine1.setText(e.getAddressLine1());
        tfAddrLine2.setText(e.getAddressLine2());
        tfCity.setText(e.getCity());
        tfPostalCode.setText(e.getPostalCode());
        tfTelHome.setText(e.getTelHome());
        tfTelMob.setText(e.getTelMobile());
        cbDept.setValue(e.getDepartment());
        cbDesig.setValue(e.getDesignation());
        tfDateJoin.setText(e.getDateOfJoining());
        if ("Female".equalsIgnoreCase(e.getGender()))     rdFemale.setSelected(true);
        else if ("Other".equalsIgnoreCase(e.getGender())) rdOther.setSelected(true);
        else                                               rdMale.setSelected(true);
        // Restore performance rating within valid 1-5 range
        int rating = e.getPerformanceRating();
        spRating.getValueFactory().setValue(Math.max(1, Math.min(5, rating == 0 ? 3 : rating)));
    }

    private void clearForm() {
        tfEmpId.clear(); tfNic.clear(); tfFname.clear(); tfLname.clear();
        tfDob.clear(); tfAddrLine1.clear(); tfAddrLine2.clear();
        tfCity.clear(); tfPostalCode.clear();
        tfTelHome.clear(); tfTelMob.clear(); tfDateJoin.clear();
        cbDept.setValue(null); cbDesig.setValue(null);
        rdMale.setSelected(true);
        spRating.getValueFactory().setValue(3);   // reset to DEFAULT 3
        table.getSelectionModel().clearSelection();
    }

    private void refreshTable() {
        data = FXCollections.observableArrayList(objEmp.getAllEmployees());
        table.setItems(data);
    }

    @SuppressWarnings("unchecked")
    private <S, T> TableColumn<S, T> col(String title, String prop, double minW) {
        TableColumn<S, T> c = new TableColumn<>(title);
        c.setMinWidth(minW);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        return c;
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
