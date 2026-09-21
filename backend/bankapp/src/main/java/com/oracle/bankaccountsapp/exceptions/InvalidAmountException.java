package com.oracle.bankaccountsapp.exceptions;

public class InvalidAmountException extends RuntimeException {
    public InvalidAmountException() {
        super("Amount must be a positive finite number");
    }
}
