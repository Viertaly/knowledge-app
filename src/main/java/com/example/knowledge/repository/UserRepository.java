package com.example.knowledge.repository;

import com.example.knowledge.model.User;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findByUsername(String username);

    Optional<User> findById(Long id);

}

