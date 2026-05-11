package com.example.knowledge.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database configuration helper.
 *
 * Connection parameters are loaded from `src/main/resources/db.properties` on the classpath.
 */
public final class DatabaseConfig {

    private static String DB_URL;
    private static String DB_USERNAME;
    private static String DB_PASSWORD;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found", e);
        }

        Properties props = new Properties();
        try (InputStream in = DatabaseConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new RuntimeException("db.properties not found on classpath");
            }
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load db.properties", e);
        }

        DB_URL = props.getProperty("db.url");
        DB_USERNAME = props.getProperty("db.user");
        DB_PASSWORD = props.getProperty("db.password");

        if (DB_URL == null || DB_URL.isBlank()) throw new RuntimeException("db.url is missing in db.properties");
        if (DB_USERNAME == null || DB_USERNAME.isBlank()) throw new RuntimeException("db.user is missing in db.properties");
        if (DB_PASSWORD == null) DB_PASSWORD = ""; // allow empty password but not null
    }

    private DatabaseConfig() { }

    /**
     * Obtain a new JDBC Connection. Caller is responsible for closing the Connection.
     *
     * @return a new {@link Connection}
     * @throws SQLException if obtaining the connection fails
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
    }
}
