package com.oracle.authapp.repositories;

import com.oracle.authapp.entities.EmployeeLoginAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeLoginAccountRepository extends JpaRepository<EmployeeLoginAccount, Integer> {
    Optional<EmployeeLoginAccount> findByEmailIgnoreCase(String email);
}
