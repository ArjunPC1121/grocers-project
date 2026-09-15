package com.oracle.userapp.services.abstractions;

import java.util.Collection;

public  interface UserServiceManager<T,Id> {

    T add(T data);
    Collection<T> getAll();
    T get(Id id);
    T update(Id id, T data);
    T delete(Id id);
    T lock(Id id);
}
