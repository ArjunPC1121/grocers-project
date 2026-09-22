package com.oracle.userapp.entities;

/** Reasons assigned by application security rules when an account is locked. */
public enum LockedReason {
    THREE_FAILED_ATTEMPTS,
    SECURITY_ESCALATION
}
