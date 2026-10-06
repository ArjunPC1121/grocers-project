/**
 * Component role: Declares the persistence queries used by the service layer. Spring Data derives or implements these queries against the owning database tables.
 *
 * Maintainer note: this file belongs to adminapp. See backend/adminapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.adminapp.repositories;

import com.oracle.adminapp.entities.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Integer> {
    Optional<Admin> findByEmailIgnoreCase(String email);
}
