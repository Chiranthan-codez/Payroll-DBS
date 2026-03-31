package com.payroll.model;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Employee — updated for payrolldbs_project.sql schema.
 *
 * New fields from SQL Section 2 (ALTER TABLE):
 *   employees.performance_rating  TINYINT NOT NULL DEFAULT 3 (CHECK 1-5)
 *
 * New columns included in SELECT and INSERT/UPDATE queries.
 * vw_employee_full view (SQL Section 4) is used for complex reads.
 *
 * Tables touched:
 *   employees, employee_addresses, employee_contact_numbers,
 *   departments, designations, employee_leave_balances (seed on insert)
 */
public class Employee extends Person {

    private int    empId         = 0;
    private int    deptId        = 0;
    private int    designationId = 0;
    private String department    = "";
    private String designation   = "";
    private String dateOfJoining = "";

    private String lastMessage = "";
    public  String getLastMessage() { return lastMessage; }

    public int    getEmpId()                 { return empId; }
    public void   setEmpId(int v)            { empId = v; }
    public int    getDeptId()                { return deptId; }
    public void   setDeptId(int v)           { deptId = v; }
    public int    getDesignationId()         { return designationId; }
    public void   setDesignationId(int v)    { designationId = v; }
    public String getDepartment()            { return department; }
    public void   setDepartment(String v)    { department = v; }
    public String getDesignation()           { return designation; }
    public void   setDesignation(String v)   { designation = v; }
    public String getDateOfJoining()         { return dateOfJoining; }
    public void   setDateOfJoining(String v) { dateOfJoining = v; }

    // ── Lookup helpers ────────────────────────────────────────────────────────

    public List<String> getAllDepartments() {
        List<String> list = new ArrayList<>();
        try (Connection c = DbConnection.getDbConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT dept_name FROM departments ORDER BY dept_name")) {
            while (rs.next()) list.add(rs.getString("dept_name"));
        } catch (Exception ignored) {}
        return list;
    }

    public List<String> getAllDesignations() {
        List<String> list = new ArrayList<>();
        try (Connection c = DbConnection.getDbConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT designation_name FROM designations ORDER BY designation_name")) {
            while (rs.next()) list.add(rs.getString("designation_name"));
        } catch (Exception ignored) {}
        return list;
    }
    public boolean getMyDetails(int loggedEmpId) {

    String sql = "SELECT e.*, d.dept_name, ds.designation_name, "
               + "ea.address_line1, ea.address_line2, ea.city, ea.postal_code "
               + "FROM employees e "
               + "LEFT JOIN departments d ON e.current_dept_id = d.dept_id "
               + "LEFT JOIN designations ds ON e.current_designation_id = ds.designation_id "
               + "LEFT JOIN employee_addresses ea ON e.empid = ea.empid "
               + "WHERE e.empid = ?";

    try (Connection c = DbConnection.getDbConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {

        ps.setInt(1, loggedEmpId);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            mapRow(rs);
            return true;
        }

        return false;

    } catch (Exception ex) {
        lastMessage = "Error: " + ex.getMessage();
        return false;
    }
}
    private int resolveDeptId(Connection c, String deptName) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT dept_id FROM departments WHERE dept_name = ?")) {
            ps.setString(1, deptName);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private int resolveDesignationId(Connection c, String desigName) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT designation_id FROM designations WHERE designation_name = ?")) {
            ps.setString(1, desigName);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── DB Operations ─────────────────────────────────────────────────────────

    /**
     * INSERT new employee across 5 tables in one transaction.
     * Includes performance_rating (new column from ALTER TABLE in SQL Section 2).
     * Uses INSERT...SELECT to seed leave balances (SQL Section 5, Q17 pattern).
     */
    public boolean insertEmployee() {
        // performance_rating included (new column - SQL Section 2)
        String sqlEmp  = "INSERT INTO employees "
                       + "(nic,fname,lname,dob,gender,date_of_joining,current_dept_id,current_designation_id,performance_rating) "
                       + "VALUES (?,?,?,?,?,?,?,?,?)";
        String sqlAddr = "INSERT INTO employee_addresses (empid,address_line1,address_line2,city,postal_code) VALUES (?,?,?,?,?)";
        String sqlHome = "INSERT INTO employee_contact_numbers (empid,contact_type,contact_number) VALUES (?,'home',?)";
        String sqlMob  = "INSERT INTO employee_contact_numbers (empid,contact_type,contact_number) VALUES (?,'mobile',?)";
        // INSERT...SELECT pattern from SQL Section 5 Q17
        String sqlLeave = "INSERT IGNORE INTO employee_leave_balances (empid,leave_type_id,available_leaves) "
                        + "SELECT ?,leave_type_id,CASE leave_type_name WHEN 'Annual' THEN 15 WHEN 'Casual' THEN 10 ELSE 5 END "
                        + "FROM leave_types";
        try (Connection c = DbConnection.getDbConnection()) {
            c.setAutoCommit(false);
            try {
                deptId        = resolveDeptId(c, department);
                designationId = resolveDesignationId(c, designation);
                if (deptId == 0)        { lastMessage = "Department not found: " + department; c.rollback(); return false; }
                if (designationId == 0) { lastMessage = "Designation not found: " + designation; c.rollback(); return false; }

                try (PreparedStatement ps = c.prepareStatement(sqlEmp, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, nic);
                    ps.setString(2, fName);
                    ps.setString(3, lName);
                    ps.setString(4, dob.isBlank() ? null : dob);
                    ps.setString(5, gender.isBlank() ? "Male" : gender);
                    ps.setString(6, dateOfJoining);
                    ps.setInt(7, deptId);
                    ps.setInt(8, designationId);
                    // Clamp performance_rating to DB CHECK constraint: BETWEEN 1 AND 5
                    ps.setInt(9, Math.max(1, Math.min(5, performanceRating)));
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) empId = keys.getInt(1); }
                }

                try (PreparedStatement ps = c.prepareStatement(sqlAddr)) {
                    ps.setInt(1, empId);
                    ps.setString(2, addressLine1);
                    ps.setString(3, addressLine2);
                    ps.setString(4, city.isBlank() ? "Unknown" : city);
                    ps.setString(5, postalCode);
                    ps.executeUpdate();
                }

                if (telHome != null && !telHome.isBlank()) {
                    try (PreparedStatement ps = c.prepareStatement(sqlHome)) {
                        ps.setInt(1, empId); ps.setString(2, telHome); ps.executeUpdate();
                    }
                }
                if (telMobile != null && !telMobile.isBlank()) {
                    try (PreparedStatement ps = c.prepareStatement(sqlMob)) {
                        ps.setInt(1, empId); ps.setString(2, telMobile); ps.executeUpdate();
                    }
                }

                try (PreparedStatement ps = c.prepareStatement(sqlLeave)) {
                    ps.setInt(1, empId); ps.executeUpdate();
                }

                c.commit();
                lastMessage = "Employee " + fName + " " + lName + " added (ID: " + empId + ").";
                return true;
            } catch (Exception ex) { c.rollback(); lastMessage = "Error adding employee: " + ex.getMessage(); return false; }
        } catch (Exception ex) { lastMessage = "DB error: " + ex.getMessage(); return false; }
    }

    /**
     * UPDATE existing employee — includes performance_rating.
     */
    public boolean updateEmployee() {
        String sqlEmp  = "UPDATE employees SET nic=?,fname=?,lname=?,dob=?,gender=?,date_of_joining=?,"
                       + "current_dept_id=?,current_designation_id=?,performance_rating=? WHERE empid=?";
        String sqlAddr = "UPDATE employee_addresses SET address_line1=?,address_line2=?,city=?,postal_code=? WHERE empid=?";
        String sqlDelContacts = "DELETE FROM employee_contact_numbers WHERE empid=?";
        String sqlHome = "INSERT INTO employee_contact_numbers (empid,contact_type,contact_number) VALUES (?,'home',?)";
        String sqlMob  = "INSERT INTO employee_contact_numbers (empid,contact_type,contact_number) VALUES (?,'mobile',?)";
        try (Connection c = DbConnection.getDbConnection()) {
            c.setAutoCommit(false);
            try {
                deptId        = resolveDeptId(c, department);
                designationId = resolveDesignationId(c, designation);
                if (deptId == 0)        { lastMessage = "Department not found: " + department; c.rollback(); return false; }
                if (designationId == 0) { lastMessage = "Designation not found: " + designation; c.rollback(); return false; }

                try (PreparedStatement ps = c.prepareStatement(sqlEmp)) {
                    ps.setString(1, nic);
                    ps.setString(2, fName);
                    ps.setString(3, lName);
                    ps.setString(4, dob.isBlank() ? null : dob);
                    ps.setString(5, gender.isBlank() ? "Male" : gender);
                    ps.setString(6, dateOfJoining);
                    ps.setInt(7, deptId);
                    ps.setInt(8, designationId);
                    ps.setInt(9, Math.max(1, Math.min(5, performanceRating)));
                    ps.setInt(10, empId);
                    ps.executeUpdate();
                }

                // Upsert address
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT address_id FROM employee_addresses WHERE empid=?")) {
                    ps.setInt(1, empId);
                    boolean exists = ps.executeQuery().next();
                    if (exists) {
                        try (PreparedStatement up = c.prepareStatement(sqlAddr)) {
                            up.setString(1, addressLine1); up.setString(2, addressLine2);
                            up.setString(3, city.isBlank() ? "Unknown" : city);
                            up.setString(4, postalCode);   up.setInt(5, empId);
                            up.executeUpdate();
                        }
                    } else {
                        try (PreparedStatement ins = c.prepareStatement(
                                "INSERT INTO employee_addresses (empid,address_line1,address_line2,city,postal_code) VALUES (?,?,?,?,?)")) {
                            ins.setInt(1, empId); ins.setString(2, addressLine1); ins.setString(3, addressLine2);
                            ins.setString(4, city.isBlank() ? "Unknown" : city); ins.setString(5, postalCode);
                            ins.executeUpdate();
                        }
                    }
                }

                // Re-insert contacts
                try (PreparedStatement ps = c.prepareStatement(sqlDelContacts)) { ps.setInt(1, empId); ps.executeUpdate(); }
                if (telHome != null && !telHome.isBlank()) {
                    try (PreparedStatement ps = c.prepareStatement(sqlHome)) { ps.setInt(1, empId); ps.setString(2, telHome); ps.executeUpdate(); }
                }
                if (telMobile != null && !telMobile.isBlank()) {
                    try (PreparedStatement ps = c.prepareStatement(sqlMob)) { ps.setInt(1, empId); ps.setString(2, telMobile); ps.executeUpdate(); }
                }

                c.commit();
                lastMessage = "Employee updated successfully.";
                return true;
            } catch (Exception ex) { c.rollback(); lastMessage = "Error updating employee: " + ex.getMessage(); return false; }
        } catch (Exception ex) { lastMessage = "DB error: " + ex.getMessage(); return false; }
    }

    /**
     * DELETE employee — ON DELETE CASCADE handles all child tables automatically.
     * trg_emp_before_delete trigger (SQL Section 9) will block deletion of Admin users.
     */
    public boolean deleteEmployee(int id) {
        String sql = "DELETE FROM employees WHERE empid=?";
        try (Connection c = DbConnection.getDbConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            lastMessage = "Employee ID " + id + " deleted.";
            return true;
        } catch (Exception ex) {
            // trg_emp_before_delete raises SQLSTATE 45000 for Admin users
            lastMessage = "Error deleting: " + ex.getMessage();
            return false;
        }
    }

    public boolean getEmployeeDetails(int id) {
        String sql =  "SELECT e.*, d.dept_name, ds.designation_name, "
                   + "ea.address_line1, ea.address_line2, ea.city, ea.postal_code "
                   + "FROM employees e "
                   + "LEFT JOIN departments d ON e.current_dept_id = d.dept_id "
                   + "LEFT JOIN designations ds ON e.current_designation_id = ds.designation_id "
                   + "LEFT JOIN employee_addresses ea ON e.empid = ea.empid "
                   + "WHERE e.empid = ?";
        try (Connection c = DbConnection.getDbConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) { mapRow(rs); return true; }
            lastMessage = "No employee found for ID " + id;
            return false;
        } catch (Exception ex) { lastMessage = "Error: " + ex.getMessage(); return false; }
    }

    public List<Employee> getAllEmployees() {
        List<Employee> list = new ArrayList<>();
        String sql = buildSelectSQL() + " ORDER BY e.empid";
        try (Connection c = DbConnection.getDbConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) { Employee e = new Employee(); e.mapRow(rs); list.add(e); }
        } catch (Exception ignored) {}
        return list;
    }

    public List<Employee> searchEmployees(String query) {
        List<Employee> list = new ArrayList<>();
        // LIKE + CAST pattern from SQL Section 5 Q16
        String sql = buildSelectSQL()
                   + " WHERE CAST(e.empid AS CHAR) LIKE ? OR e.fname LIKE ? OR e.lname LIKE ? "
                   + "OR d.dept_name LIKE ? OR ds.designation_name LIKE ?";
        try (Connection c = DbConnection.getDbConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            String q = "%" + query + "%";
            for (int i = 1; i <= 5; i++) ps.setString(i, q);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) { Employee e = new Employee(); e.mapRow(rs); list.add(e); }
        } catch (Exception ignored) {}
        return list;
    }

    /**
     * Builds the 5-table LEFT JOIN SELECT including performance_rating.
     * Matches vw_employee_full view definition (SQL Section 4).
     * Uses conditional aggregation MAX(CASE WHEN) to pivot contact types (SQL Section 5 Q15).
     */
    private String buildSelectSQL() {
        return "SELECT e.empid, e.nic, e.fname, e.lname, e.dob, e.gender, e.date_of_joining, "
             + "e.current_dept_id, e.current_designation_id, e.performance_rating, "
             + "d.dept_name, ds.designation_name, "
             + "a.address_line1, a.address_line2, a.city, a.postal_code, "
             + "MAX(CASE WHEN cn.contact_type='home'   THEN cn.contact_number END) AS tel_home, "
             + "MAX(CASE WHEN cn.contact_type='mobile' THEN cn.contact_number END) AS tel_mobile "
             + "FROM employees e "
             + "LEFT JOIN departments d ON e.current_dept_id=d.dept_id "
             + "LEFT JOIN designations ds ON e.current_designation_id=ds.designation_id "
             + "LEFT JOIN employee_addresses a ON e.empid=a.empid "
             + "LEFT JOIN employee_contact_numbers cn ON e.empid=cn.empid "
             + "GROUP BY e.empid, e.nic, e.fname, e.lname, e.dob, e.gender, e.date_of_joining, "
             + "e.current_dept_id, e.current_designation_id, e.performance_rating, "
             + "d.dept_name, ds.designation_name, "
             + "a.address_line1, a.address_line2, a.city, a.postal_code";
    }

    void mapRow(ResultSet rs) throws SQLException {
        empId             = rs.getInt("empid");
        nic               = rs.getString("nic");
        fName             = rs.getString("fname");
        lName             = rs.getString("lname");
        dob               = safe(rs, "dob");
        gender            = safe(rs, "gender");
        deptId            = rs.getInt("current_dept_id");
        designationId     = rs.getInt("current_designation_id");
        performanceRating = rs.getInt("performance_rating");
        department        = safe(rs, "dept_name");
        designation       = safe(rs, "designation_name");
        dateOfJoining     = safe(rs, "date_of_joining");
        addressLine1      = safe(rs, "address_line1");
        addressLine2      = safe(rs, "address_line2");
        city              = safe(rs, "city");
        postalCode        = safe(rs, "postal_code");
        telHome           = safe(rs, "tel_home");
        telMobile         = safe(rs, "tel_mobile");
    }

    private String safe(ResultSet rs, String col) {
        try { String v = rs.getString(col); return v == null ? "" : v; } catch (Exception e) { return ""; }
    }
}
