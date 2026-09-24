package com.oracle.bankaccountsapp.controllers;

import com.oracle.bankaccountsapp.dtos.BankAccountRequest;
import com.oracle.bankaccountsapp.dtos.DeductionRequest;
import com.oracle.bankaccountsapp.services.abstractions.BankAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/grocers/api/banks")
public class BankAccountController {
    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    /** Public endpoint used to populate the dummy bank database. */
    @PostMapping("/add")
    public ResponseEntity<Void> add(@RequestBody BankAccountRequest request) {
        bankAccountService.add(request);
        return ResponseEntity.status(201).build();
    }

    @GetMapping("/validate")
    public ResponseEntity<Boolean> validate(
            @RequestParam String accountNumber,
            @RequestParam String phoneNumber) {
        return ResponseEntity.ok(bankAccountService.isAccountLinkedToPhone(accountNumber, phoneNumber));
    }

    @PostMapping("/{accountNumber}/deduct")
    public ResponseEntity<Double> deduct(@PathVariable String accountNumber, @RequestBody DeductionRequest request) {
        Double deductedAmount = bankAccountService.deduct(accountNumber, request.pin(), request.amount());
        return ResponseEntity.ok(deductedAmount);
    }
}
