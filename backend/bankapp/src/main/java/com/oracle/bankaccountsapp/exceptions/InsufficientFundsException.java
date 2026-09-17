package com.oracle.bankaccountsapp.exceptions;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException() {
        super("Insufficient funds for the requested deduction");
    }
}
