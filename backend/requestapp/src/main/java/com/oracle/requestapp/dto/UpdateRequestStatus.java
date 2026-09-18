package com.oracle.requestapp.dto;

import com.oracle.requestapp.entities.RequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateRequestStatus(
        @NotNull RequestStatus status,
        @Size(max = 1000) String rejectionReason) {
}
