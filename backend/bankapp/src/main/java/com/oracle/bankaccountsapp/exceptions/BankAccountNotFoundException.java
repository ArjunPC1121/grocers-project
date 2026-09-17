package com.oracle.bankaccountsapp.exceptions;

public class BankAccountNotFoundException extends RuntimeException {
    public BankAccountNotFoundException(int userId) {
        super("No bank account found for user id: " + userId);
    }
}
