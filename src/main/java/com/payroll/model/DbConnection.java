package com.payroll.model;

import java.io.File;
import java.io.FileInputStream;
import java.sql.*;
import java.util.Properties;

public class DbConnection {
    private static Connection conn = null;
    private static String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static String DB_USER     = "root";
    private static String DB_PASSWORD = "xzvb##1234A";
    private static String DB_NAME     = "payrolldbs";
    private static String DB_HOST     = "127.0.0.1";
    private static String DB_PORT     = "3307";

    private static void loadConfig() {
        try {
            File cfg = new File("config/db.properties");
            if (!cfg.exists()) return;
            Properties p = new Properties();
            try (FileInputStream fis = new FileInputStream(cfg)) { p.load(fis); }
            JDBC_DRIVER = p.getProperty("jdbc.driver",  JDBC_DRIVER);
            DB_USER     = p.getProperty("db.user",      DB_USER);
            DB_PASSWORD = p.getProperty("db.password",  DB_PASSWORD);
            DB_NAME     = p.getProperty("db.name",      DB_NAME);
            DB_HOST     = p.getProperty("db.host",      DB_HOST);
            DB_PORT     = p.getProperty("db.port",      DB_PORT);
        } catch (Exception ignored) {}
    }

    public static Connection getDbConnection() {
        try {
            loadConfig();
            Class.forName(JDBC_DRIVER);
            try { if (conn != null && !conn.isClosed()) return conn; } catch (SQLException ignored) {}
            String url = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
                       + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            conn = DriverManager.getConnection(url, DB_USER, DB_PASSWORD);
            return conn;
        } catch (Exception ex) {
            throw new RuntimeException("DB connection failed: " + ex.getMessage(), ex);
        }
    }

    public static void close() {
        try { if (conn != null && !conn.isClosed()) conn.close(); }
        catch (SQLException ignored) {}
        conn = null;
    }
}
