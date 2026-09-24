package com.oracle.chatapp.dto;
import com.oracle.chatapp.entities.AvailabilityStatus;
import jakarta.validation.constraints.NotNull;
public record AvailabilityRequest(@NotNull AvailabilityStatus status) { }
