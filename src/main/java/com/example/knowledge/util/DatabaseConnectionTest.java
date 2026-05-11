package com.example.knowledge.util;

import com.example.knowledge.config.DatabaseConfig;

import java.sql.Connection;

public class DatabaseConnectionTest {
    public static void main(String[] args) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            System.out.println("Database connection successful");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
