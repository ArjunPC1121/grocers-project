/**
 * Component role: Defines a transport contract used at an API or service boundary. Keep it free of persistence and business side effects.
 *
 * Maintainer note: this file belongs to adminapp. See backend/adminapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.adminapp.dto;


import java.time.Instant;

public record ProductRequestCreatedEvent(
        Integer requestId,
        Integer employeeId,
        String action,
        String productName,
        Integer quantity,
        String reason,
        Instant createdAt
) {}
