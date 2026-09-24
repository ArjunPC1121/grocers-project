package com.oracle.userapp.repositories;

import com.oracle.userapp.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
// Provides database access to user records.
public interface UserRepository extends JpaRepository<User,Integer> {
    // Finds a user by their unique email address during account recovery.
    Optional<User> findByEmail(String email);
}
