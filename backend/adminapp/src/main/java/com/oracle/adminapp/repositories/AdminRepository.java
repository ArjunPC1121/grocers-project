package com.oracle.adminapp.repositories;

import com.oracle.adminapp.entities.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Integer> {
    Optional<Admin> findByEmailIgnoreCase(String email);
}
