package com.oracle.userapp.dto;

// DTO used for adding money to wallet from the user's registered bank account.
public record AddFundsRequest(double amount, String pin) {
}
