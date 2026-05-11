package com.example.knowledge.test;

import com.example.knowledge.model.User;
import com.example.knowledge.repository.UserRepositoryImpl;
import com.example.knowledge.service.AuthService;

public class AuthServiceTest {

    public static void main(String[] args) {
        UserRepositoryImpl repo = new UserRepositoryImpl();
        AuthService auth = new AuthService(repo);

        try {
            User u = auth.register("testuser", "testpass");
            System.out.println("User registered successfully: " + u.getUsername());
        } catch (IllegalArgumentException e) {
            System.out.println("Registration skipped: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Registration error: " + e.getMessage());
        }

        try {
            User logged = auth.login("testuser", "testpass");
            System.out.println("Login successful: " + logged.getUsername());
        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage());
        }
    }
}
