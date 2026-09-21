package com.oracle.userapp.services.implementations;

import com.oracle.userapp.dto.*;
import com.oracle.userapp.entities.LockedReason;
import com.oracle.userapp.entities.User;
import com.oracle.userapp.repositories.UserRepository;
import com.oracle.userapp.services.abstractions.UserServiceManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Service
public class UserService implements UserServiceManager<UserRequest,UserResponse,UpdateUserRequest, TicketResponse,Integer> {

    private static final String CREATE_BANK_ACCOUNT_URL =
            "http://localhost:8089/grocers/api/banks/add/{userId}";
    private static final String DEDUCT_BANK_FUNDS_URL =
            "http://localhost:8089/grocers/api/banks/{userId}/deduct";
    private static final String RAISE_TICKET_URL =
            "http://localhost:8087/grocers/api/tickets";


    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder, RestTemplate restTemplate)
    {
        this.repository = repository;
        this.passwordEncoder=passwordEncoder;
        this.restTemplate = restTemplate;
    }
    @Override
    @Transactional
    public UserResponse add(UserRequest data) {
        User user = new User();
        mapRequestToEntity(user, data);
        user.setPassword(passwordEncoder.encode(data.getPassword()));
        user.setSecretQuestion(data.getSecretQuestion());
        user.setSecretAnswerHash(
                passwordEncoder.encode(data.getSecretAnswer())
        );
        user = repository.save(user);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(
                Map.of("accountNumber", user.getAccountNumber()),
                headers
        );

        restTemplate.postForEntity(
                CREATE_BANK_ACCOUNT_URL,
                request,
                Void.class,
                user.getId()
        );

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
    public UserResponse update(Integer id, UpdateUserRequest data)throws RuntimeException {
        User user = repository.findById(id).orElseThrow(()-> new RuntimeException("User not found"));
        mapUpdateRequestToEntity(user, data);
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
    @Override
    public double addFunds(Integer id, double amount) throws RuntimeException
    {
        User user = repository.findById(id).orElseThrow(()-> new RuntimeException("User not found"));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Double>> request = new HttpEntity<>(
                Map.of("amount", amount),
                headers
        );


        Double deductedAmount = restTemplate.postForObject(
                DEDUCT_BANK_FUNDS_URL,
                request,
                Double.class,
                id
        );

        if (deductedAmount == null) {
            throw new RuntimeException("Bank service returned no deducted amount");
        }

        user.setFunds(user.getFunds() + deductedAmount);
        user = repository.save(user);
        return user.getFunds();
    }

    @Override
    public double deductFunds(Integer id, double amount) throws RuntimeException
    {
        User user = repository.findById(id).orElseThrow(()-> new RuntimeException("User not found"));
        user.setFunds(user.getFunds()-amount);
        repository.save(user);
        return user.getFunds();
    }

    @Override
    public TicketResponse raiseTicket(Integer id)
    {
        User user = repository.findById(id).orElseThrow(()-> new RuntimeException("User not found"));

        if (!user.isAccountLocked() || user.getLockedReason() == null) {
            throw new RuntimeException("User account is not locked");
        }

        TicketRequest ticketRequest = new TicketRequest(user.getId(), user.getLockedReason());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<TicketRequest> request = new HttpEntity<>(
                ticketRequest, headers);

        TicketServiceResponse ticketServiceResponse = restTemplate.postForObject(
                RAISE_TICKET_URL,
                request,
                TicketServiceResponse.class
        );

        if(ticketServiceResponse==null || ticketServiceResponse.ticketId()<=0)
        {
            throw new RuntimeException("Ticket couldn't be generated");
        }

        int ticketId = ticketServiceResponse.ticketId();
        return new TicketResponse(ticketId,id);

    }

    public Double refund(Integer id, double amount)
    {
        User user = repository.findById(id).orElseThrow(()-> new RuntimeException("User not found"));
        user.setFunds(user.getFunds()+amount);
        repository.save(user);
        return user.getFunds();
    }

    public Integer unlock(Integer id)
    {
        User user = repository.findById(id).orElseThrow(()-> new RuntimeException("User not found"));
        user.setAccountLocked(false);
        user.setLockedReason(null);
        repository.save(user);
        return user.getId();
    }

    public String verifySecretAnswer(Integer id, SecretAnswerRequest request)
    {
        User user = repository.findById(id).orElseThrow(()-> new RuntimeException("User not found"));

        if(!user.isAccountLocked() ||
            user.getLockedReason() != LockedReason.THREE_FAILED_ATTEMPTS)
        {
            throw new RuntimeException("Secret-question recovery is unavailable");
        }

        boolean correct = passwordEncoder.matches(
                request.answer(),
                user.getSecretAnswerHash()
        );

        if(!correct)
        {
            user.setLockedReason(LockedReason.SECURITY_ESCALATION);
            repository.save(user);
            throw new RuntimeException("Answer is incorrect");
        }

        String rawResetToken = UUID.randomUUID().toString();

        user.setPasswordResetTokenHash(
                passwordEncoder.encode(rawResetToken)
        );
        user.setPasswordResetTokenExpiresAt(
                LocalDateTime.now().plusMinutes(15)
        );
        repository.save(user);

        return rawResetToken;

    }

    public void resetPassword(Integer id, ResetPasswordRequest request)
    {
        User user = repository.findById(id).orElseThrow(()-> new RuntimeException("User not found"));

        boolean validToken =
                user.getPasswordResetTokenHash() != null
                && user.getPasswordResetTokenExpiresAt() != null
                && LocalDateTime.now().isBefore(user.getPasswordResetTokenExpiresAt())
                && passwordEncoder.matches(request.resetToken(), user.getPasswordResetTokenHash());

        if(!validToken)
        {
            throw new RuntimeException("Invalid or expired reset token");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setAccountLocked(false);
        user.setLockedReason(null);
        user.setFailedLoginAttempts(0);
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetTokenExpiresAt(null);

        repository.save(user);

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
        response.setFunds(user.getFunds());

        return response;
    }

    private static void mapUpdateRequestToEntity(User user, UpdateUserRequest data)
    {
        if (data.getFirstName() != null) {
            user.setFirstName(data.getFirstName());
        }
        if (data.getLastName() != null) {
            user.setLastName(data.getLastName());
        }
        if (data.getEmail() != null) {
            user.setEmail(data.getEmail());
        }
        if (data.getDob() != null) {
            user.setDob(data.getDob());
        }
        if (data.getPhoneNumber() != null) {
            user.setPhoneNumber(data.getPhoneNumber());
        }
        if (data.getAddress() != null) {
            user.setAddress(data.getAddress());
        }
        if (data.getAccountNumber() != null) {
            user.setAccountNumber(data.getAccountNumber());
        }
    }
}