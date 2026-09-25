package com.oracle.adminapp.repositories;

import com.oracle.adminapp.entities.AdminNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminNotificationRepository
        extends JpaRepository<AdminNotification, Integer> {

    boolean existsByRequestId(Integer requestId);

    List<AdminNotification> findAllByOrderByCreatedAtDesc();
}
