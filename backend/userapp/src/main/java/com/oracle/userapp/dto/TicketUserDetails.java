package com.oracle.userapp.dto;

/** Safe customer data for employee ticket review. Passwords and hashes are deliberately excluded. */
public record TicketUserDetails(int userId, String firstName, String lastName, String email,
                                boolean accountLocked, int failedLoginAttempts, String lockedReason) { }
