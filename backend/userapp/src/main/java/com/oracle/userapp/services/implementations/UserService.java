package com.oracle.userapp.services.implementations;

import com.oracle.userapp.dto.UserRequest;
import com.oracle.userapp.dto.UserResponse;
import com.oracle.userapp.entities.User;
import com.oracle.userapp.repositories.UserRepository;
import com.oracle.userapp.services.abstractions.UserServiceManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UserService implements UserServiceManager<UserRequest,UserResponse,Integer> {

    private final UserRepository repository;

    public UserService(UserRepository repository)
    {
        this.repository = repository;
    }
    @Override
    public UserResponse add(UserRequest data) {
        return null;
    }

    @Override
    public Collection<UserResponse> getAll() {
        return List.of();
    }

    @Override
    public UserResponse get(Integer id) {
        return null;
    }

    @Override
    public UserResponse update(Integer id, UserRequest data) {
        return null;
    }

    @Override
    public UserResponse delete(Integer id) {
        return null;
    }

    @Override
    public UserResponse lock(Integer id) {
        return null;
    }
}
