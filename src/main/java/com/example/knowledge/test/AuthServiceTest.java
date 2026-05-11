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
            System.out.println("Пользователь успешно зарегистрирован: " + u.getUsername());
        } catch (IllegalArgumentException e) {
            System.out.println("Registration skipped: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Registration error: " + e.getMessage());
        }

        try {
            User logged = auth.login("testuser", "testpass");
            System.out.println("Вход выполнен: " + logged.getUsername());
        } catch (Exception e) {
            System.out.println("Ошибка входа: " + e.getMessage());
        }
    }
}
