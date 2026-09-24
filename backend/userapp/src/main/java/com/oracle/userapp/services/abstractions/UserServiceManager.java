package com.oracle.userapp.services.abstractions;

import com.oracle.userapp.dto.ChangePasswordRequest;
import com.oracle.userapp.dto.ResetPasswordRequest;
import com.oracle.userapp.dto.SecretAnswerRequest;
import com.oracle.userapp.dto.WalletTransactionResponse;
import com.oracle.userapp.entities.SecretQuestion;

import java.util.Collection;
import java.util.List;

public  interface UserServiceManager<TRequest,TResponse,TUpdateRequest,TTicketResponse, Id> {

    TResponse add(TRequest data);
    Collection<TResponse> getAll();
    TResponse get(Id id) throws RuntimeException;

    TResponse update(Integer id, TUpdateRequest data)throws RuntimeException;

    TResponse delete(Id id)throws RuntimeException;
    int incFailedAttempts(Id id) throws RuntimeException;

    double addFunds(Id id, double amount, String pin)throws RuntimeException;
    double deductFunds(Id id, double amount,String reference)throws RuntimeException;

    TTicketResponse raiseTicket(Id id);

    Double refund(Id id, double amount, String reference);

    Id unlock(Id id);

    String verifySecretAnswer(Id id, SecretAnswerRequest request);

    void resetPassword(Id id, ResetPasswordRequest request);

    void clearFailedAttempts(Id id);

    SecretQuestion getSecretQuestion(Id id);
    public void changePassword(Integer userId, ChangePasswordRequest request);

    List<WalletTransactionResponse> getWalletTransactions(Id id);
}
