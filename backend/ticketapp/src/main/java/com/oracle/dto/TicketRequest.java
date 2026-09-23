package com.oracle.dto;

import com.oracle.entity.LockedReason;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TicketRequest(
        @Positive int userId,
        @NotNull LockedReason lockedReason,
        @Size(max = 1000) String requestNote
) {
}
