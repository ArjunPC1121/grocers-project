package com.oracle.userapp.services.implementations;

import com.oracle.userapp.dto.UserRequest;
import com.oracle.userapp.dto.UserResponse;
import com.oracle.userapp.entities.LockedReason;
import com.oracle.userapp.entities.User;
import com.oracle.userapp.repositories.UserRepository;
import com.oracle.userapp.services.abstractions.UserServiceManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class UserService implements UserServiceManager<UserRequest,UserResponse,Integer> {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    public UserService(UserRepository repository, PasswordEncoder passwordEncoder)
    {
        this.repository = repository;
        this.passwordEncoder=passwordEncoder;
    }
    @Override
    public UserResponse add(UserRequest data) {
        User user = new User();
        mapRequestToEntity(user, data);
        user.setPassword(passwordEncoder.encode(data.getPassword()));
        user = repository.save(user);
        return mapEntityToResponse(user);

    }

    @Override
    public Collection<UserResponse> getAll() {
        Collection<User> allUsers = repository.findAll();

        return allUsers.stream().map(UserService::mapEntityToResponse).toList();
    }

    @Override
    public UserResponse get(Integer id) throws RuntimeException {
        User user = repository.findById(id).orElseThrow(()-> new RuntimeException("User not found"));
        return mapEntityToResponse(user);
    }

    @Override
    public UserResponse update(Integer id, UserRequest data)throws RuntimeException {
        User user = repository.findById(id).orElseThrow(()-> new RuntimeException("User not found"));
        mapRequestToEntity(user, data);
        user.setPassword(passwordEncoder.encode(data.getPassword()));
        user = repository.save(user);
        return mapEntityToResponse(user);
    }

    @Override
    public UserResponse delete(Integer id) throws RuntimeException{
        User user = repository.findById(id).orElseThrow(()-> new RuntimeException("User not found"));
        repository.deleteById(id);
        return mapEntityToResponse(user);
    }


    public int incFailedAttempts(Integer id) throws RuntimeException{
        User user = repository.findById(id).orElseThrow(()->new RuntimeException("User not found"));
        int failedAttempts = user.getFailedLoginAttempts()+1;
        user.setFailedLoginAttempts(failedAttempts);

        if(failedAttempts>=3)
        {
            user.setAccountLocked(true);
            user.setLockedReason(LockedReason.THREE_FAILED_ATTEMPTS);
        }
        repository.save(user);
        return failedAttempts;
    }

    private static void mapRequestToEntity(User user, UserRequest data)
    {
        user.setFirstName(data.getFirstName());
        user.setLastName(data.getLastName());
        user.setAccountNumber(data.getAccountNumber());
        user.setAddress(data.getAddress());
        user.setDob(data.getDob());
        user.setEmail(data.getEmail());
        user.setPhoneNumber(data.getPhoneNumber());
    }

    private static UserResponse mapEntityToResponse(User user)
    {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setAccountNumber(user.getAccountNumber());
        response.setAddress(user.getAddress());
        response.setDob(user.getDob());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setEmail(user.getEmail());

        return response;
    }
}
