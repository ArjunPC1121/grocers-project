package com.oracle.authapp.exceptions;

public class AccountLockedException extends RuntimeException {
    public AccountLockedException() {
        super("This user account is locked and cannot sign in");
    }
}
