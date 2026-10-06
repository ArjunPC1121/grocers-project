/**
 * Component role: Declares the persistence queries used by the service layer. Spring Data derives or implements these queries against the owning database tables.
 *
 * Maintainer note: this file belongs to authapp. See backend/authapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.authapp.repositories;

import com.oracle.authapp.entities.EmployeeLoginAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeLoginAccountRepository extends JpaRepository<EmployeeLoginAccount, Integer> {
    Optional<EmployeeLoginAccount> findByEmailIgnoreCase(String email);
}
