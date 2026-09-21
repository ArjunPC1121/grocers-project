package com.oracle.bankaccountsapp.services.implementations;

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
    private static final Double INITIAL_BALANCE = 100000.00;
    private final BankAccountRepository bankAccountRepository;

    public BankAccountServiceImplementation(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    @Override
    @Transactional
    public void createAccount(int userId, String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number must not be blank");
        }
        if (bankAccountRepository.existsById(accountNumber)) {
            throw new DuplicateAccountNumberException(accountNumber);
        }
        BankAccount account = new BankAccount();
        account.setUserId(userId);
        account.setAccountNumber(accountNumber);
        account.setBalance(INITIAL_BALANCE);
        bankAccountRepository.save(account);
    }

    @Override
    @Transactional
    public Double deduct(int userId, Double amount) {
        if (amount == null || !Double.isFinite(amount) || amount <= 0) {
            throw new InvalidAmountException();
        }
        BankAccount account = bankAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new BankAccountNotFoundException(userId));
        if (account.getBalance() < amount) {
            throw new InsufficientFundsException();
        }
        account.setBalance(account.getBalance() - amount);
        bankAccountRepository.save(account);
        return amount;
    }
}

