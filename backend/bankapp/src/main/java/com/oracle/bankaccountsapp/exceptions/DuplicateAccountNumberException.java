package com.oracle.bankaccountsapp.exceptions;

public class DuplicateAccountNumberException extends RuntimeException {
    public DuplicateAccountNumberException(String accountNumber) {
        super("Bank account already exists for account number: " + accountNumber);
    }
}
