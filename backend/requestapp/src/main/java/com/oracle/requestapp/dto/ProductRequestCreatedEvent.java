/**
 * Component role: Defines a transport contract used at an API or service boundary. Keep it free of persistence and business side effects.
 *
 * Maintainer note: this file belongs to requestapp. See backend/requestapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.requestapp.dto;



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
