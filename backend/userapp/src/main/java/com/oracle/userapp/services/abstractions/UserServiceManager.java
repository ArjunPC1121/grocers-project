package com.oracle.userapp.services.abstractions;

import java.util.Collection;

public  interface UserServiceManager<TRequest,TResponse,Id> {

    TResponse add(TRequest data);
    Collection<TResponse> getAll();
    TResponse get(Id id);
    TResponse update(Id id, TRequest data);
    TResponse delete(Id id);
    TResponse lock(Id id);
}
