package com.vkarun.foodcircle.repository;

import com.vkarun.foodcircle.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    // Login. Uses the unique index on username, so it is one index lookup.
    Optional<User> findByUsername(String username);
}
