package com.example.knowledge.service;

import com.example.knowledge.model.User;

public final class Session {
    private static volatile User currentUser;

    private Session() { }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }
}
