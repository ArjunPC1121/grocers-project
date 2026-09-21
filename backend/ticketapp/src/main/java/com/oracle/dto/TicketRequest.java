package com.oracle.dto;

import com.oracle.entity.LockedReason;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TicketRequest(
        @Positive int userId,
        @NotNull LockedReason lockedReason
) {
}