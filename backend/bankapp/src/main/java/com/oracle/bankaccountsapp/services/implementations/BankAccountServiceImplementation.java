package com.oracle.bankaccountsapp.services.implementations;

import com.oracle.bankaccountsapp.dtos.BankAccountRequest;
import com.oracle.bankaccountsapp.entities.BankAccount;
import com.oracle.bankaccountsapp.exceptions.BankAccountNotFoundException;
import com.oracle.bankaccountsapp.exceptions.DuplicateAccountNumberException;
import com.oracle.bankaccountsapp.exceptions.InsufficientFundsException;
import com.oracle.bankaccountsapp.exceptions.InvalidAmountException;
import com.oracle.bankaccountsapp.repositories.BankAccountRepository;
import com.oracle.bankaccountsapp.services.abstractions.BankAccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BankAccountServiceImplementation implements BankAccountService {
    private final BankAccountRepository bankAccountRepository;

    public BankAccountServiceImplementation(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    @Override
    @Transactional
    public void add(BankAccountRequest request) {
        if (request.accountNumber() == null || request.accountNumber().isBlank()) {
            throw new IllegalArgumentException("Account number must not be blank");
        }
        if (request.phoneNumber() == null || request.phoneNumber().isBlank()) {
            throw new IllegalArgumentException("Phone number must not be blank");
        }
        if (request.pin() == null || request.pin().isBlank()) {
            throw new IllegalArgumentException("PIN must not be blank");
        }
        if (request.balance() == null || !Double.isFinite(request.balance()) || request.balance() < 0) {
            throw new IllegalArgumentException("Balance must be zero or greater");
        }
        if (bankAccountRepository.existsById(request.accountNumber())) {
            throw new DuplicateAccountNumberException(request.accountNumber());
        }
        BankAccount account = new BankAccount();
        account.setAccountNumber(request.accountNumber());
        account.setPhoneNumber(request.phoneNumber());
        account.setPin(request.pin());
        account.setBalance(request.balance());
        bankAccountRepository.save(account);
    }

    @Override
    public boolean isAccountLinkedToPhone(String accountNumber, String phoneNumber) {
        return bankAccountRepository.findByAccountNumberAndPhoneNumber(accountNumber, phoneNumber).isPresent();
    }

    @Override
    @Transactional
    public Double deduct(String accountNumber, String pin, Double amount) {
        if (amount == null || !Double.isFinite(amount) || amount <= 0) {
            throw new InvalidAmountException();
        }
        BankAccount account = bankAccountRepository.findById(accountNumber)
                .orElseThrow(() -> new BankAccountNotFoundException(accountNumber));
        if (!account.getPin().equals(pin)) {
            throw new IllegalArgumentException("Invalid bank PIN");
        }
        if (account.getBalance() < amount) {
            throw new InsufficientFundsException();
        }
        account.setBalance(account.getBalance() - amount);
        bankAccountRepository.save(account);
        return amount;
    }
}

