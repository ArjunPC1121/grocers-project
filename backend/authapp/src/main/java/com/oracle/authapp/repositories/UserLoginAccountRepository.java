package com.oracle.authapp.repositories;

import com.oracle.authapp.entities.UserLoginAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserLoginAccountRepository extends JpaRepository<UserLoginAccount, Integer> {
    Optional<UserLoginAccount> findByEmailIgnoreCase(String email);
}
