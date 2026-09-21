package com.oracle.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TicketActionRequest(@NotNull @Positive Integer employeeId) { }
