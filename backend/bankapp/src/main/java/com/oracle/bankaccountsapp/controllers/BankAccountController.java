package com.oracle.bankaccountsapp.controllers;

import com.oracle.bankaccountsapp.dtos.AccountCreationRequest;
import com.oracle.bankaccountsapp.dtos.DeductionRequest;
import com.oracle.bankaccountsapp.dtos.DeductionResponse;
import com.oracle.bankaccountsapp.services.abstractions.BankAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/grocers/api/banks")
public class BankAccountController {
    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @PostMapping("/add/{userId}")
    public ResponseEntity<Void> createAccount(@PathVariable int userId, @RequestBody AccountCreationRequest request) {
        bankAccountService.createAccount(userId, request.accountNumber());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/deduct")
    public ResponseEntity<DeductionResponse> deduct(@PathVariable int userId, @RequestBody DeductionRequest request) {
        Double deductedAmount = bankAccountService.deduct(userId, request.amount());
        return ResponseEntity.ok(new DeductionResponse(deductedAmount));
    }
}
