package com.oracle.employeeapp.dtos;

/** Customer identity and lock status made available to an employee handling a ticket. */
public record TicketUserDetails(Integer userId, String firstName, String lastName, String email,
                                boolean accountLocked, int failedLoginAttempts, String lockedReason) { }
