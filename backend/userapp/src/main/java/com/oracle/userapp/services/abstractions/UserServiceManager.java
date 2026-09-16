package com.oracle.userapp.services.abstractions;

import com.oracle.userapp.dto.UpdateUserRequest;

import java.util.Collection;

public  interface UserServiceManager<TRequest,TResponse,TUpdateRequest,Id> {

    TResponse add(TRequest data);
    Collection<TResponse> getAll();
    TResponse get(Id id) throws RuntimeException;

    TResponse update(Integer id, TUpdateRequest data)throws RuntimeException;

    TResponse delete(Id id)throws RuntimeException;
    int incFailedAttempts(Id id) throws RuntimeException;
}
