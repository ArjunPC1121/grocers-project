package com.oracle.bankaccountsapp.exceptions;

public class BankAccountNotFoundException extends RuntimeException {
    public BankAccountNotFoundException(String accountNumber) {
        super("No bank account found for account number: " + accountNumber);
    }
}
