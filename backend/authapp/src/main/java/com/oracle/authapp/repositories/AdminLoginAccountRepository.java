package com.oracle.authapp.repositories;

import com.oracle.authapp.entities.AdminLoginAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminLoginAccountRepository extends JpaRepository<AdminLoginAccount, Integer> {
    Optional<AdminLoginAccount> findByEmailIgnoreCase(String email);
}
