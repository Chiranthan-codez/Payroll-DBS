package com.payroll.model;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Payroll — updated for payrolldbs_project.sql schema.
 *
 * New column from SQL Section 2 (ALTER TABLE):
 *   employee_salaries.last_revised_date  DATE NOT NULL DEFAULT CURRENT_DATE
 *   → Written when updating salary, read back for display.
 *
 * Stored procedure sp_apply_salary_hike (SQL Section 7) used for hike operations.
 * Trigger trg_salary_after_update (SQL Section 9) auto-writes payroll_audit_log.
 *
 * Tables:
 *   employee_salaries  (salary_record_id, empid, structure_id, base_salary, epf, tax, paye, last_revised_date)
 *   salary_structures  (structure_id, travel_allowance, food_allowance, bonus)
 */
public class Payroll {

    public Employee objEmployee = new Employee();

    // From salary_structures
    private double travelAmount  = 0;
    private double foodAmount    = 0;
    private double bonusAmount   = 0;

    // From employee_salaries
    private double epfAmount     = 0;
    private double taxAmount     = 0;
    private double payeAmount    = 0;
    private double baseSalary    = 0;
    private int    structureId   = 0;
    // New column (SQL Section 2 - ALTER TABLE)
    private String lastRevisedDate = "";

    private String lastMessage = "";
    public  String getLastMessage()         { return lastMessage; }

    public double getTravelAmount()         { return travelAmount; }
    public void   setTravelAmount(double v) { travelAmount = v; }
    public double getFoodAmount()           { return foodAmount; }
    public void   setFoodAmount(double v)   { foodAmount = v; }
    public double getBonusAmount()          { return bonusAmount; }
    public void   setBonusAmount(double v)  { bonusAmount = v; }
    public double getEpfAmount()            { return epfAmount; }
    public void   setEpfAmount(double v)    { epfAmount = v; }
    public double getTaxAmount()            { return taxAmount; }
    public void   setTaxAmount(double v)    { taxAmount = v; }
    public double getPayeAmount()           { return payeAmount; }
    public void   setPayeAmount(double v)   { payeAmount = v; }
    public double getBaseSalary()           { return baseSalary; }
    public void   setBaseSalary(double v)   { baseSalary = v; }
    public int    getStructureId()          { return structureId; }
    public void   setStructureId(int v)     { structureId = v; }
    public String getLastRevisedDate()      { return lastRevisedDate; }

    /**
     * Fetch salary details — reads last_revised_date (new column).
     * Matches vw_payslip view (SQL Section 4).
     */
    public boolean getSalaryDetails(int empId) {
        String sql = "SELECT e.empid, e.fname, e.lname, d.dept_name, ds.designation_name, "
                   + "es.structure_id, es.base_salary, es.epf, es.tax, es.paye, es.last_revised_date, "
                   + "ss.travel_allowance, ss.food_allowance, ss.bonus "
                   + "FROM employees e "
                   + "LEFT JOIN departments d ON e.current_dept_id=d.dept_id "
                   + "LEFT JOIN designations ds ON e.current_designation_id=ds.designation_id "
                   + "LEFT JOIN employee_salaries es ON e.empid=es.empid "
                   + "LEFT JOIN salary_structures ss ON es.structure_id=ss.structure_id "
                   + "WHERE e.empid=?";
        try (Connection c = DbConnection.getDbConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, empId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                objEmployee.setEmpId(rs.getInt("empid"));
                objEmployee.setFname(rs.getString("fname"));
                objEmployee.setLname(rs.getString("lname"));
                objEmployee.setDesignation(safe(rs, "designation_name"));
                objEmployee.setDepartment(safe(rs, "dept_name"));
                structureId     = rs.getInt("structure_id");
                baseSalary      = rs.getDouble("base_salary");
                epfAmount       = rs.getDouble("epf");
                taxAmount       = rs.getDouble("tax");
                payeAmount      = rs.getDouble("paye");
                travelAmount    = rs.getDouble("travel_allowance");
                foodAmount      = rs.getDouble("food_allowance");
                bonusAmount     = rs.getDouble("bonus");
                lastRevisedDate = safe(rs, "last_revised_date");
                objEmployee.setSalAmount(baseSalary);
                return true;
            }
            lastMessage = "No record found for Employee ID: " + empId;
            return false;
        } catch (Exception ex) {
            lastMessage = "Error fetching salary: " + ex.getMessage();
            return false;
        }
    }

    /**
     * INSERT salary — CHECK constraints in DB (base_salary > 0, epf/tax/paye >= 0)
     * will reject invalid values. trg_salary_after_insert writes audit log automatically.
     */
    public boolean insertSalaryDetails() {
        String sql = "INSERT INTO employee_salaries (empid,structure_id,base_salary,epf,tax,paye) VALUES (?,?,?,?,?,?)";
        try (Connection c = DbConnection.getDbConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, objEmployee.getEmpId());
            ps.setInt(2, structureId);
            ps.setDouble(3, baseSalary);
            ps.setDouble(4, epfAmount);
            ps.setDouble(5, taxAmount);
            ps.setDouble(6, payeAmount);
            ps.executeUpdate();
            lastMessage = "Salary details added for Employee ID: " + objEmployee.getEmpId();
            return true;
        } catch (Exception ex) {
            lastMessage = "Error inserting salary: " + ex.getMessage();
            return false;
        }
    }

    /**
     * UPDATE salary — writes last_revised_date = CURDATE() (new column).
     * trg_salary_after_update (SQL Section 9) fires and writes audit log automatically.
     * Uses explicit transaction (SQL Section 6 pattern).
     */
    public boolean updateSalaryDetails(int empId) {
        // last_revised_date set to CURDATE() on every update (SQL Section 2 ALTER column)
        String sqlSal = "UPDATE employee_salaries SET structure_id=?,base_salary=?,epf=?,tax=?,paye=?,last_revised_date=CURDATE() WHERE empid=?";
        String sqlStr = "UPDATE salary_structures SET travel_allowance=?,food_allowance=?,bonus=? WHERE structure_id=?";
        try (Connection c = DbConnection.getDbConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(sqlSal)) {
                    ps.setInt(1, structureId);
                    ps.setDouble(2, baseSalary);
                    ps.setDouble(3, epfAmount);
                    ps.setDouble(4, taxAmount);
                    ps.setDouble(5, payeAmount);
                    ps.setInt(6, empId);
                    ps.executeUpdate();
                }
                if (structureId > 0) {
                    try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
                        ps.setDouble(1, travelAmount);
                        ps.setDouble(2, foodAmount);
                        ps.setDouble(3, bonusAmount);
                        ps.setInt(4, structureId);
                        ps.executeUpdate();
                    }
                }
                c.commit();
                lastMessage = "Salary details updated.";
                return true;
            } catch (Exception ex) {
                c.rollback();
                lastMessage = "Error updating salary: " + ex.getMessage();
                return false;
            }
        } catch (Exception ex) {
            lastMessage = "DB error: " + ex.getMessage();
            return false;
        }
    }

    /**
     * Apply salary hike using stored procedure sp_apply_salary_hike (SQL Section 7).
     * The procedure uses INOUT parameter pattern (Lab 9 parameter modes).
     *
     * @param empId    employee to hike
     * @param hikePct  percentage to raise (e.g. 10.0 for 10%)
     * @return new base salary, or -1 on error
     */
    public double applySalaryHike(int empId, double hikePct) {
        try (Connection c = DbConnection.getDbConnection();
             CallableStatement cs = c.prepareCall("{CALL sp_apply_salary_hike(?,?,?)}")) {
            cs.setInt(1, empId);
            cs.setDouble(2, hikePct);
            cs.setInt(3, (int) baseSalary);          // INOUT: pass current salary in
            cs.registerOutParameter(3, Types.INTEGER); // INOUT: get new salary back
            cs.execute();
            double newSalary = cs.getInt(3);
            baseSalary = newSalary;
            lastMessage = "Salary hike applied. New base salary: " + newSalary;
            return newSalary;
        } catch (Exception ex) {
            lastMessage = "Error applying hike: " + ex.getMessage();
            return -1;
        }
    }

    /**
     * Fetch audit log entries for an employee from payroll_audit_log table (SQL Section 1 Table 13).
     * Written automatically by triggers trg_salary_after_insert and trg_salary_after_update.
     */
    public List<String> getAuditLog(int empId) {
        List<String> log = new ArrayList<>();
        String sql = "SELECT action_type, old_base_salary, new_base_salary, changed_at "
                   + "FROM payroll_audit_log WHERE empid=? ORDER BY changed_at DESC LIMIT 10";
        try (Connection c = DbConnection.getDbConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, empId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                log.add(rs.getString("action_type")
                    + " | Old: " + rs.getString("old_base_salary")
                    + " → New: " + rs.getString("new_base_salary")
                    + " | " + rs.getString("changed_at"));
            }
        } catch (Exception ignored) {}
        return log;
    }

    public List<String> getSalaryStructures() {
        List<String> list = new ArrayList<>();
        try (Connection c = DbConnection.getDbConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                 "SELECT structure_id, travel_allowance, food_allowance, bonus FROM salary_structures ORDER BY structure_id")) {
            while (rs.next()) {
                list.add("Structure " + rs.getInt("structure_id")
                    + " (Travel:" + rs.getDouble("travel_allowance")
                    + " Food:"    + rs.getDouble("food_allowance")
                    + " Bonus:"   + rs.getDouble("bonus") + ")");
            }
        } catch (Exception ignored) {}
        return list;
    }

    public double calculate(double perc, double basicSal) {
        return basicSal * (perc / 100.0);
    }

    private String safe(ResultSet rs, String col) {
        try { String v = rs.getString(col); return v == null ? "" : v; } catch (Exception e) { return ""; }
    }
}
