package com.oracle.userapp.services.abstractions;

import java.util.Collection;

public  interface UserServiceManager<TRequest,TResponse,Id> {

    TResponse add(TRequest data);
    Collection<TResponse> getAll();
    TResponse get(Id id) throws RuntimeException;
    TResponse update(Id id, TRequest data)throws RuntimeException;
    TResponse delete(Id id)throws RuntimeException;
    int incFailedAttempts(Id id) throws RuntimeException;
}
