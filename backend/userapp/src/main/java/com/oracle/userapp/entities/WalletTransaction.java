package com.oracle.userapp.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identifies the user whose wallet changed.
    @Column(nullable = false)
    private Integer userId;

    // Explains why money was added or removed.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WalletTransactionType type;

    // Stores the money involved in this transaction.
    @Column(nullable = false)
    private double amount;

    // Stores the wallet balance after this transaction is completed.
    @Column(nullable = false)
    private double balanceAfterTransaction;

    // Stores an order number or another useful source reference.
    @Column(length = 100)
    private String reference;

    // Stores when this transaction was created.
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void setCreatedAt() {
        createdAt = LocalDateTime.now();
    }
}