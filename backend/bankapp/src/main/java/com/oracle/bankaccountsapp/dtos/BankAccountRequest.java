package com.oracle.bankaccountsapp.dtos;

/** Input used to add an account to the dummy bank database. */
public record BankAccountRequest(
        String accountNumber,
        String phoneNumber,
        String pin,
        Double balance
) {
}
