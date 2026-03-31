package com.payroll.util;

public class Session {

    private static int empId;
    private static boolean admin;

    public static void start(int id, boolean isAdmin) {
        empId = id;
        admin = isAdmin;
    }

    public static int getEmpId() {
        return empId;
    }

    public static boolean isAdmin() {
        return admin;
    }
}