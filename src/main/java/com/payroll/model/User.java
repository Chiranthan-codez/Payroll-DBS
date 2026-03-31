package com.payroll.model;

import java.sql.*;

/**
 * User — updated for payrolldbs_project.sql schema.
 *
 * Password storage change (SQL Section 3):
 *   Seed data now stores plain-text passwords (e.g. 'admin123', 'sneha123').
 *   Authentication tries plain-text first, then SHA-256 hash fallback.
 *
 * Tables: system_users (user_id, empid, username, password_hash)
 *         user_role_assignments (user_id, role_id)
 *         user_roles (role_id, role_name)
 *
 * Uses vw_user_auth view (SQL Section 4) logic inlined here for clarity.
 * Admin check uses MAX(CASE WHEN role_name='Admin') aggregate pattern (SQL Section 5 Q19).
 *
 * trg_user_after_insert trigger (SQL Section 9) auto-assigns 'Employee' role on user creation.
 */
public class User extends Person {

    private String  userName = "";
    private String  password = "";
    private boolean admin    = false;
    private int empId = 0;

public int getEmpId() { 
    return empId; 
}

    public String  getUserName()         { return userName; }
    public void    setUserName(String v) { userName = v; }
    public String  getPassword()         { return password; }
    public void    setPassword(String v) { password = v; }
    public boolean isAdmin()             { return admin; }

    /**
     * Authenticate against system_users + user_role_assignments + user_roles.
     * Plain-text comparison matches seed data from payrolldbs_project.sql Section 3.
     * SHA-256 fallback retained for passwords hashed by older versions.
     *
     * Uses SQL vw_user_auth logic:
     *   SELECT ... MAX(CASE WHEN role_name='Admin' THEN 1 ELSE 0 END) AS is_admin ...
     *
     * @return 1 = Admin role, 0 = regular user, -1 = not found / error
     */
    public int authenticate() {
        String sql = "SELECT su.user_id, su.empid, "
                   + "MAX(CASE WHEN ur.role_id IN (1,3) THEN 1 ELSE 0 END) AS is_admin "
                   + "FROM system_users su "
                   + "JOIN user_role_assignments ura ON su.user_id=ura.user_id "
                   + "JOIN user_roles ur ON ura.role_id=ur.role_id "
                   + "WHERE su.username=? AND (su.password_hash=? OR su.password_hash=?) "
                   + "GROUP BY su.user_id";
        try (Connection c = DbConnection.getDbConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userName);
            ps.setString(2, password);              // plain-text match (seed data)
            ps.setString(3, hashPassword(password)); // SHA-256 fallback
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                empId = rs.getInt("empid");
                admin = rs.getInt("is_admin") == 1;
                return admin ? 1 : 0;
            }
            return -1;
        } catch (Exception ex) {
            throw new RuntimeException("Authentication error: " + ex.getMessage(), ex);
        }
    }

    /**
     * Create a new system user with the given role.
     * NOTE: trg_user_after_insert (SQL Section 9) automatically assigns the
     * 'Employee' role on INSERT — the explicit role assignment here adds a
     * SECOND role if a non-Employee role is chosen (e.g. Admin).
     *
     * @param empId    employee id to link
     * @param username login username
     * @param rawPass  plain-text password (stored as-is in dev build)
     * @param roleName role name string (must match user_roles.role_name)
     * @return true on success
     */
    public boolean createUser(int empId, String username, String rawPass, String roleName) {
        String sqlInsertUser = "INSERT INTO system_users (empid, username, password_hash) VALUES (?,?,?)";
        String sqlGetRole    = "SELECT role_id FROM user_roles WHERE role_name=?";
        String sqlAssign     = "INSERT IGNORE INTO user_role_assignments (user_id, role_id) VALUES (?,?)";
        try (Connection c = DbConnection.getDbConnection()) {
            c.setAutoCommit(false);
            try {
                int userId;
                try (PreparedStatement ps = c.prepareStatement(sqlInsertUser, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, empId);
                    ps.setString(2, username);
                    ps.setString(3, rawPass);   // plain-text, matches seed data pattern
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) { c.rollback(); return false; }
                        userId = keys.getInt(1);
                    }
                }
                // trg_user_after_insert already inserts 'Employee' role.
                // Only add explicit role if it is different from 'Employee'.
                if (roleName != null && !roleName.equalsIgnoreCase("Employee")) {
                    int roleId = 0;
                    try (PreparedStatement ps = c.prepareStatement(sqlGetRole)) {
                        ps.setString(1, roleName);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) roleId = rs.getInt("role_id");
                    }
                    if (roleId > 0) {
                        try (PreparedStatement ps = c.prepareStatement(sqlAssign)) {
                            ps.setInt(1, userId); ps.setInt(2, roleId); ps.executeUpdate();
                        }
                    }
                }
                c.commit();
                return true;
            } catch (Exception ex) { c.rollback(); throw ex; }
        } catch (Exception ex) {
            throw new RuntimeException("Create user error: " + ex.getMessage(), ex);
        }
    }

    /** SHA-256 hash — fallback for passwords stored as hashes in older data. */
    private String hashPassword(String plain) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return plain; }
    }
}
