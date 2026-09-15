package com.oracle.userapp.services.implementations;

import com.oracle.userapp.entities.User;
import com.oracle.userapp.repositories.UserRepository;
import com.oracle.userapp.services.abstractions.UserServiceManager;

import java.util.Collection;
import java.util.List;

public class UserService implements UserServiceManager<User,Integer> {

    private final UserRepository repository;

    public UserService(UserRepository repository)
    {
        this.repository = repository;
    }
    @Override
    public User add(User data) {
        return null;
    }

    @Override
    public Collection<User> getAll() {
        return List.of();
    }

    @Override
    public User get(Integer id) {
        return null;
    }

    @Override
    public User update(Integer id, User data) {
        return null;
    }

    @Override
    public User delete(Integer id) {
        return null;
    }

    @Override
    public User lock(Integer id) {
        return null;
    }
}
