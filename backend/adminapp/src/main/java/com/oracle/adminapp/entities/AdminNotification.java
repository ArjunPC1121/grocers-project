/**
 * Component role: Maps a domain concept to persistent storage and contains the state that the service owns.
 *
 * Maintainer note: this file belongs to adminapp. See backend/adminapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.adminapp.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@Entity
@Table(name = "admin_notifications")
public class AdminNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private Integer requestId;

    @Column(nullable = false)
    private Integer employeeId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private boolean read = false;

    @Column(nullable = false)
    private Instant createdAt;

    public AdminNotification(
            Integer requestId,
            Integer employeeId,
            String title,
            String message,
            Instant createdAt
    ) {
        this.requestId = requestId;
        this.employeeId = employeeId;
        this.title = title;
        this.message = message;
        this.createdAt = createdAt;
        this.read = false;
    }
}
