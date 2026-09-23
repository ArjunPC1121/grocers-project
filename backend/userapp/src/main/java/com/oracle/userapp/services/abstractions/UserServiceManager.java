package com.oracle.userapp.services.abstractions;

import com.oracle.userapp.dto.ChangePasswordRequest;
import com.oracle.userapp.dto.ResetPasswordRequest;
import com.oracle.userapp.dto.SecretAnswerRequest;
import com.oracle.userapp.entities.SecretQuestion;

import java.util.Collection;

public  interface UserServiceManager<TRequest,TResponse,TUpdateRequest,TTicketResponse, Id> {

    TResponse add(TRequest data);
    Collection<TResponse> getAll();
    TResponse get(Id id) throws RuntimeException;

    TResponse update(Integer id, TUpdateRequest data)throws RuntimeException;

    TResponse delete(Id id)throws RuntimeException;
    int incFailedAttempts(Id id) throws RuntimeException;

    double addFunds(Id id, double amount)throws RuntimeException;
    double deductFunds(Id id, double amount)throws RuntimeException;

    TTicketResponse raiseTicket(Id id);

    Double refund(Id id, double amount);

    Id unlock(Id id);

    String verifySecretAnswer(Id id, SecretAnswerRequest request);

    void resetPassword(Id id, ResetPasswordRequest request);

    void clearFailedAttempts(Id id);

    SecretQuestion getSecretQuestion(Id id);
    public void changePassword(Integer userId, ChangePasswordRequest request);
}
