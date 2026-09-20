package com.oracle.userapp.services.abstractions;

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
    void unlockAccount(Id id);
}
