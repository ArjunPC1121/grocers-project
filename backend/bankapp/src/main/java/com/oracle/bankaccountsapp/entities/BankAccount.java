package com.oracle.bankaccountsapp.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "bank_account")
@Getter
@Setter
@NoArgsConstructor
public class BankAccount {

    @Id
    @Column(name = "account_number", nullable = false, length = 30)
    private String accountNumber; // PK; String preserves leading zeroes

    @Column(name = "balance", nullable = false)
    private Double balance;

    @Column(name = "user_id", nullable = false)
    private int  userId; // FK to the user record
}
