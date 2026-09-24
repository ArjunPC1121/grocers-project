package com.oracle.bankaccountsapp.config;

import com.oracle.bankaccountsapp.dtos.BankAccountRequest;
import com.oracle.bankaccountsapp.exceptions.DuplicateAccountNumberException;
import com.oracle.bankaccountsapp.services.abstractions.BankAccountService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Adds sample accounts when BankApp starts. Duplicate accounts are ignored. */
@Component
public class BankAccountDataSeeder implements CommandLineRunner {

    private final BankAccountService bankAccountService;

    public BankAccountDataSeeder(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @Override
    public void run(String... args) {
        addIfMissing(new BankAccountRequest("100000001", "9876543210", "1234", 50000.00));
        addIfMissing(new BankAccountRequest("100000002", "9876543211", "2345", 25000.00));
    }

    private void addIfMissing(BankAccountRequest request) {
        try {
            bankAccountService.add(request);
        } catch (DuplicateAccountNumberException ignored) {
            // The account already exists from a previous application start.
        }
    }
}
