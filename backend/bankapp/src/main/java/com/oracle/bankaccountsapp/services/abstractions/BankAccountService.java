package com.oracle.bankaccountsapp.services.abstractions;

import com.oracle.bankaccountsapp.dtos.BankAccountRequest;

public interface BankAccountService {
    void add(BankAccountRequest request);
    boolean isAccountLinkedToPhone(String accountNumber, String phoneNumber);
    Double deduct(String accountNumber, String pin, Double amount);
}
