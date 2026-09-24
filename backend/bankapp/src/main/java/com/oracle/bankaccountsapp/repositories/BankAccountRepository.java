package com.oracle.bankaccountsapp.repositories;

import com.oracle.bankaccountsapp.entities.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, String> {

    Optional<BankAccount> findByAccountNumberAndPhoneNumber(
            String accountNumber,
            String phoneNumber
    );
}
