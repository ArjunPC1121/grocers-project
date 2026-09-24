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
