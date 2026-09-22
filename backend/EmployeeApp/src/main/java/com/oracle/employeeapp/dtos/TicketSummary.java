package com.oracle.employeeapp.dtos;

import java.time.Instant;

public record TicketSummary(Integer ticketId, Integer userId, Integer employeeId, String status,
                            String lockedReason, Instant createdAt, Instant updatedAt) { }
