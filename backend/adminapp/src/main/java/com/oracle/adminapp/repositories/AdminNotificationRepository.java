/**
 * Component role: Declares the persistence queries used by the service layer. Spring Data derives or implements these queries against the owning database tables.
 *
 * Maintainer note: this file belongs to adminapp. See backend/adminapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.adminapp.repositories;

import com.oracle.adminapp.entities.AdminNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminNotificationRepository
        extends JpaRepository<AdminNotification, Integer> {

    boolean existsByRequestId(Integer requestId);

    List<AdminNotification> findAllByOrderByCreatedAtDesc();
}
