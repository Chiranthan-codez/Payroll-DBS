package com.payroll.model;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Leave — updated for payrolldbs schema.
 *
 * Tables:
 *   employee_leave_balances (balance_id, empid, leave_type_id, available_leaves)
 *   leave_types             (leave_type_id, leave_type_name, leave_description)
 *
 * Balances stored as one row per leave type (normalised), not columns.
 */
public class Leave {

    public Employee objEmployee = new Employee();

    // key = leave_type_name, value = available_leaves
    private Map<String, Integer> balances = new LinkedHashMap<>();

    private String lastMessage = "";
    public  String getLastMessage() { return lastMessage; }

    public Map<String, Integer> getBalances() { return balances; }

    /** Convenience getters for the three standard leave types. */
    public int getAnnual()   { return balances.getOrDefault("Annual",   0); }
    public int getCasual()   { return balances.getOrDefault("Casual",   0); }
    public int getOptional() { return balances.getOrDefault("Optional", 0); }

    public boolean getLeaveDetails(int empId) {
        String sql = "SELECT e.empid, e.fname, e.lname, "
                   + "d.dept_name, ds.designation_name, "
                   + "lt.leave_type_name, lb.available_leaves "
                   + "FROM employees e "
                   + "LEFT JOIN departments d ON e.current_dept_id=d.dept_id "
                   + "LEFT JOIN designations ds ON e.current_designation_id=ds.designation_id "
                   + "JOIN employee_leave_balances lb ON e.empid=lb.empid "
                   + "JOIN leave_types lt ON lb.leave_type_id=lt.leave_type_id "
                   + "WHERE e.empid=? "
                   + "ORDER BY lt.leave_type_id";
        try (Connection c = DbConnection.getDbConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, empId);
            ResultSet rs = ps.executeQuery();
            boolean found = false;
            balances.clear();
            while (rs.next()) {
                if (!found) {
                    objEmployee.setEmpId(empId);
                    objEmployee.setFname(rs.getString("fname"));
                    objEmployee.setLname(rs.getString("lname"));
                    objEmployee.setDepartment(rs.getString("dept_name"));
                    objEmployee.setDesignation(rs.getString("designation_name"));
                    found = true;
                }
                balances.put(rs.getString("leave_type_name"), rs.getInt("available_leaves"));
            }
            if (!found) { lastMessage = "No leave record for Employee ID: " + empId; return false; }
            return true;
        } catch (Exception ex) {
            lastMessage = "Error fetching leave: " + ex.getMessage();
            return false;
        }
    }

    /**
     * Update leave balance for a specific leave type.
     * @param leaveTypeName  e.g. "Annual"
     * @param newBalance     new available_leaves value
     */
    public boolean updateLeaveBalance(String leaveTypeName, int newBalance) {
        String sql = "UPDATE employee_leave_balances lb "
                   + "JOIN leave_types lt ON lb.leave_type_id=lt.leave_type_id "
                   + "SET lb.available_leaves=? "
                   + "WHERE lb.empid=? AND lt.leave_type_name=?";
        try (Connection c = DbConnection.getDbConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, newBalance);
            ps.setInt(2, objEmployee.getEmpId());
            ps.setString(3, leaveTypeName);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                balances.put(leaveTypeName, newBalance);
                lastMessage = leaveTypeName + " leave updated.";
                return true;
            }
            lastMessage = "No leave record updated for " + leaveTypeName;
            return false;
        } catch (Exception ex) {
            lastMessage = "Error updating leave: " + ex.getMessage();
            return false;
        }
    }

    /**
     * Apply (deduct) leave days across all types at once.
     * deductions map: leave_type_name -> days to deduct
     */
    public boolean applyLeave(Map<String, Integer> deductions) {
        for (Map.Entry<String, Integer> e : deductions.entrySet()) {
            if (e.getValue() <= 0) continue;
            int current = balances.getOrDefault(e.getKey(), 0);
            if (!updateLeaveBalance(e.getKey(), current - e.getValue())) return false;
        }
        lastMessage = "Leave applied successfully.";
        return true;
    }
}
