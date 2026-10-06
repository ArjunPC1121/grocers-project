/**
 * Component role: Defines a transport contract used at an API or service boundary. Keep it free of persistence and business side effects.
 *
 * Maintainer note: this file belongs to requestapp. See backend/requestapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.requestapp.dto;

import com.oracle.requestapp.entities.RequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateRequestStatus(
        @NotNull RequestStatus status,
        @Size(max = 1000) String rejectionReason) {
}
