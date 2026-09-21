package com.oracle.authapp.services;

import com.oracle.authapp.dto.AuthResponse;
import com.oracle.authapp.dto.LoginRequest;
import com.oracle.authapp.entities.AdminLoginAccount;
import com.oracle.authapp.entities.EmployeeLoginAccount;
import com.oracle.authapp.entities.LoginRole;
import com.oracle.authapp.entities.UserLoginAccount;
import com.oracle.authapp.exceptions.AccountLockedException;
import com.oracle.authapp.exceptions.InvalidCredentialsException;
import com.oracle.authapp.repositories.AdminLoginAccountRepository;
import com.oracle.authapp.repositories.EmployeeLoginAccountRepository;
import com.oracle.authapp.repositories.UserLoginAccountRepository;
import com.oracle.authapp.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthService {
    private final UserLoginAccountRepository userAccounts;
    private final EmployeeLoginAccountRepository employeeAccounts;
    private final AdminLoginAccountRepository adminAccounts;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RestTemplate restTemplate;

    @Value("${app.gateway.internal-secret}")
    private String internalSecret;

    private static final String FAILED_ATTEMPTS_URL =
            "http://localhost:8091/grocers/api/users/{id}/failed-attempts";

    public AuthService(UserLoginAccountRepository userAccounts,
                       EmployeeLoginAccountRepository employeeAccounts,
                       AdminLoginAccountRepository adminAccounts,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService, RestTemplate restTemplate) {
        this.userAccounts = userAccounts;
        this.employeeAccounts = employeeAccounts;
        this.adminAccounts = adminAccounts;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.restTemplate = restTemplate;
    }

    public AuthResponse loginUser(LoginRequest request) {
        UserLoginAccount account = userAccounts.findByEmailIgnoreCase(request.email())
                .orElseThrow(InvalidCredentialsException::new);
        if (account.isAccountLocked()) {
            throw new AccountLockedException();
        }
        try{
            verifyPassword(request.password(), account.getPassword());
            return response(account.getId(), account.getEmail(), LoginRole.USER, false);
        }
        catch(InvalidCredentialsException e)
        {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Service", "authapp");
            headers.set("X-Internal-Secret", internalSecret);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.postForEntity(
                    FAILED_ATTEMPTS_URL,
                    entity,
                    Integer.class,
                    account.getId()
            );
            throw e;
        }

    }

    public AuthResponse loginEmployee(LoginRequest request) {
        EmployeeLoginAccount account = employeeAccounts.findByEmailIgnoreCase(request.email())
                .orElseThrow(InvalidCredentialsException::new);
        verifyPassword(request.password(), account.getPassword());
        return response(account.getId(), account.getEmail(), LoginRole.EMPLOYEE, account.isMustChangePassword());
    }

    public AuthResponse loginAdmin(LoginRequest request) {
        AdminLoginAccount account = adminAccounts.findByEmailIgnoreCase(request.email())
                .orElseThrow(InvalidCredentialsException::new);
        verifyPassword(request.password(), account.getPassword());
        return response(account.getId(), account.getEmail(), LoginRole.ADMIN, false);
    }

    private void verifyPassword(String rawPassword, String storedHash) {
        if (!passwordEncoder.matches(rawPassword, storedHash)) {
            throw new InvalidCredentialsException();
        }
    }

    private AuthResponse response(Integer id, String email, LoginRole role, boolean mustChangePassword) {
        return new AuthResponse(
                jwtService.createToken(id, email, role, mustChangePassword),
                "Bearer",
                jwtService.expirationFor(role) / 1000,
                role.name(),
                mustChangePassword,
                mustChangePassword ? "You must change your default password before continuing." : "Login successful."
        );
    }
}
