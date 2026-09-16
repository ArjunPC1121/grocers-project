package com.oracle.bankaccountsapp.services.abstractions;

public interface BankAccountService {
    void createAccount(int userId, String accountNumber);
    Double deduct(int userId, Double amount);
}
