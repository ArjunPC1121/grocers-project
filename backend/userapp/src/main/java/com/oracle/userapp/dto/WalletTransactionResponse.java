package com.oracle.userapp.dto;

import com.oracle.userapp.entities.WalletTransaction;
import com.oracle.userapp.entities.WalletTransactionType;

import java.time.LocalDateTime;

// Returns safe wallet transaction information to the frontend.
public record WalletTransactionResponse(
        Long id,
        WalletTransactionType type,
        double amount,
        double balanceAfterTransaction,
        String reference,
        LocalDateTime createdAt
) {
    public static WalletTransactionResponse from(WalletTransaction transaction) {
        return new WalletTransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getBalanceAfterTransaction(),
                transaction.getReference(),
                transaction.getCreatedAt()
        );
    }
}