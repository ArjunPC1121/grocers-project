/**
 * Component role: Defines a transport contract used at an API or service boundary. Keep it free of persistence and business side effects.
 *
 * Maintainer note: this file belongs to adminapp. See backend/adminapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.adminapp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

/** Contract expected from RequestApp for an employee inventory request. */
public record EmployeeProductRequest(
        Integer requestId,
        Integer employeeId,
        Integer productId,
        String action,
        String status,
        String description,
        String name,
        BigDecimal price,
        Integer quantity,
        @Min(0) @Max(100) Integer discount) {
}
