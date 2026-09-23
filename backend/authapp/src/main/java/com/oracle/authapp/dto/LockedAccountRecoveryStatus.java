package com.oracle.authapp.dto;

/** Indicates whether employee-assisted recovery has restored access to a locked account. */
public record LockedAccountRecoveryStatus(boolean unlocked, boolean ticketOpen) { }
