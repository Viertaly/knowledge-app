package com.example.knowledge.service;

import com.example.knowledge.model.User;
import com.example.knowledge.repository.UserRepository;
import com.example.knowledge.repository.UserRepositoryImpl;

import java.util.Optional;

public class AuthService {

    private final UserRepository userRepository;

    // No-arg constructor for existing callers (keeps backward compatibility)
    public AuthService() {
        this.userRepository = new UserRepositoryImpl();
    }

    // Constructor injection
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("username must not be empty");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("password must not be empty");
        }
        Optional<User> existing = userRepository.findByUsername(username);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("username already exists");
        }
        User u = new User(null, username, password);
        return userRepository.save(u);
    }

    public User login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("username must not be empty");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("password must not be empty");
        }
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("invalid username or password");
        }
        User u = opt.get();
        if (!password.equals(u.getPasswordHash())) {
            throw new IllegalArgumentException("invalid username or password");
        }
        return u;
    }
}

