package com.payroll.model;

import java.sql.*;

/**
 * PaySlip — updated for payrolldbs_project.sql schema.
 *
 * Reads last_revised_date (new column from SQL Section 2 ALTER TABLE).
 * Matches vw_payslip view (SQL Section 4) logic.
 *
 * Gross / Net pay calculations done in Java (same as DB view):
 *   gross_pay        = base_salary + travel_allowance + food_allowance + bonus
 *   total_deductions = epf + tax + paye
 *   net_pay          = gross_pay - total_deductions
 */
public class PaySlip {

    public Employee objEmployee = new Employee();
    public Payroll  objPayroll  = new Payroll();

    private double grossPay        = 0;
    private double totalDeductions = 0;
    private double netPay          = 0;
    private String lastRevisedDate = "";

    private String lastMessage = "";
    public  String getLastMessage()     { return lastMessage; }
    public  double getGrossPay()        { return grossPay; }
    public  double getTotalDeductions() { return totalDeductions; }
    public  double getNetPay()          { return netPay; }
    public  String getLastRevisedDate() { return lastRevisedDate; }

    /**
     * Fetch full pay details including last_revised_date.
     * Can alternatively SELECT * FROM vw_payslip WHERE empid=? for same result.
     */
    public boolean getPayDetails(int empId) {
        String sql = "SELECT e.empid, e.fname, e.lname, "
                   + "d.dept_name, ds.designation_name, "
                   + "es.base_salary, es.epf, es.tax, es.paye, es.last_revised_date, "
                   + "ss.travel_allowance, ss.food_allowance, ss.bonus "
                   + "FROM employees e "
                   + "LEFT JOIN departments d ON e.current_dept_id=d.dept_id "
                   + "LEFT JOIN designations ds ON e.current_designation_id=ds.designation_id "
                   + "JOIN employee_salaries es ON e.empid=es.empid "
                   + "JOIN salary_structures ss ON es.structure_id=ss.structure_id "
                   + "WHERE e.empid=?";
        try (Connection c = DbConnection.getDbConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, empId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                objEmployee.setEmpId(rs.getInt("empid"));
                objEmployee.setFname(rs.getString("fname"));
                objEmployee.setLname(rs.getString("lname"));
                String desig = rs.getString("designation_name");
                String dept  = rs.getString("dept_name");
                objEmployee.setDesignation(desig == null ? "" : desig);
                objEmployee.setDepartment(dept  == null ? "" : dept);
                double base = rs.getDouble("base_salary");
                objEmployee.setSalAmount(base);
                objPayroll.setTravelAmount(rs.getDouble("travel_allowance"));
                objPayroll.setFoodAmount(rs.getDouble("food_allowance"));
                objPayroll.setBonusAmount(rs.getDouble("bonus"));
                objPayroll.setEpfAmount(rs.getDouble("epf"));
                objPayroll.setTaxAmount(rs.getDouble("tax"));
                objPayroll.setPayeAmount(rs.getDouble("paye"));
                lastRevisedDate = rs.getString("last_revised_date") != null
                                  ? rs.getString("last_revised_date") : "";
                grossPay        = base + objPayroll.getTravelAmount()
                                + objPayroll.getFoodAmount() + objPayroll.getBonusAmount();
                totalDeductions = objPayroll.getEpfAmount()
                                + objPayroll.getTaxAmount() + objPayroll.getPayeAmount();
                netPay = grossPay - totalDeductions;
                return true;
            }
            lastMessage = "No salary record found for Employee ID: " + empId;
            return false;
        } catch (Exception ex) {
            lastMessage = "Error fetching pay details: " + ex.getMessage();
            return false;
        }
    }
}
