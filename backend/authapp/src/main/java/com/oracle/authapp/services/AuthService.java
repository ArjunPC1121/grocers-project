package com.oracle.authapp.services;

import com.oracle.authapp.dto.AuthResponse;
import com.oracle.authapp.dto.LockedAccountTicketRequest;
import com.oracle.authapp.dto.LockedAccountRecoveryStatus;
import com.oracle.authapp.dto.LoginRequest;
import com.oracle.authapp.dto.SecurityRecoveryAnswerRequest;
import com.oracle.authapp.dto.SecurityRecoveryAnswerResponse;
import com.oracle.authapp.dto.SecurityRecoveryResetPasswordRequest;
import com.oracle.authapp.entities.AdminLoginAccount;
import com.oracle.authapp.entities.EmployeeLoginAccount;
import com.oracle.authapp.entities.LoginRole;
import com.oracle.authapp.entities.UserLoginAccount;
import com.oracle.authapp.exceptions.AccountLockedException;
import com.oracle.authapp.exceptions.EmployeeInactiveException;
import com.oracle.authapp.exceptions.InvalidCredentialsException;
import com.oracle.authapp.exceptions.SecurityRecoveryEscalatedException;
import com.oracle.authapp.repositories.AdminLoginAccountRepository;
import com.oracle.authapp.repositories.EmployeeLoginAccountRepository;
import com.oracle.authapp.repositories.UserLoginAccountRepository;
import com.oracle.authapp.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
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
    private static final String LOCKED_ACCOUNT_TICKET_URL =
            "http://localhost:8091/grocers/api/users/tickets";
    private static final String OPEN_TICKET_URL =
            "http://localhost:8087/grocers/api/tickets/user/{userId}/open";
    private static final String SECRET_QUESTION_URL = "http://localhost:8091/grocers/api/users/{id}/secret-question";
    private static final String SECRET_ANSWER_URL = "http://localhost:8091/grocers/api/users/{id}/secret-answer";
    private static final String PASSWORD_RESET_URL = "http://localhost:8091/grocers/api/users/{id}/password-reset";

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

            Integer attempts = restTemplate.postForEntity(
                    FAILED_ATTEMPTS_URL,
                    entity,
                    Integer.class,
                    account.getId()
            ).getBody();
            if (attempts != null && attempts >= 3) throw new AccountLockedException();
            throw e;
        }

    }

    /** Starts employee-assisted recovery after the customer cannot use the security-question path. */
    public void raiseLockedAccountTicket(LockedAccountTicketRequest request) {
        UserLoginAccount account = userAccounts.findByEmailIgnoreCase(request.email())
                .orElseThrow(InvalidCredentialsException::new);
        if (!account.isAccountLocked()) {
            throw new AccountLockedException();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Service", "authapp");
        headers.set("X-Internal-Secret", internalSecret);

        restTemplate.postForEntity(LOCKED_ACCOUNT_TICKET_URL,
                new HttpEntity<>(request, headers), Void.class);
    }

    public LockedAccountRecoveryStatus lockedAccountStatus(String email) {
        UserLoginAccount account = userAccounts.findByEmailIgnoreCase(email)
                .orElseThrow(InvalidCredentialsException::new);
        if (!account.isAccountLocked()) {
            return new LockedAccountRecoveryStatus(true, false);
        }
        return new LockedAccountRecoveryStatus(false, hasOpenTicket(account.getId()));
    }

    public String securityQuestion(String email) {
        UserLoginAccount account = lockedUser(email);
        try {
            return restTemplate.exchange(SECRET_QUESTION_URL, org.springframework.http.HttpMethod.GET,
                    new HttpEntity<Void>(internalHeaders()), String.class, account.getId()).getBody();
        } catch (HttpServerErrorException exception) {
            throw new SecurityRecoveryEscalatedException();
        }
    }

    public SecurityRecoveryAnswerResponse verifySecurityAnswer(SecurityRecoveryAnswerRequest request) {
        UserLoginAccount account = lockedUser(request.email());
        String resetToken;
        try {
            resetToken = restTemplate.postForEntity(SECRET_ANSWER_URL,
                    new HttpEntity<>(java.util.Map.of("answer", request.answer()), internalHeaders()), String.class, account.getId()).getBody();
        } catch (HttpServerErrorException exception) {
            throw new SecurityRecoveryEscalatedException();
        }
        if (resetToken == null || resetToken.isBlank()) throw new IllegalStateException("Recovery could not be started");
        return new SecurityRecoveryAnswerResponse(resetToken);
    }

    public void resetPasswordFromSecurityQuestion(SecurityRecoveryResetPasswordRequest request) {
        UserLoginAccount account = lockedUser(request.email());
        restTemplate.postForEntity(PASSWORD_RESET_URL,
                new HttpEntity<>(java.util.Map.of("resetToken", request.resetToken(), "newPassword", request.newPassword()), internalHeaders()),
                Void.class, account.getId());
    }

    private UserLoginAccount lockedUser(String email) {
        UserLoginAccount account = userAccounts.findByEmailIgnoreCase(email).orElseThrow(InvalidCredentialsException::new);
        if (!account.isAccountLocked()) throw new AccountLockedException();
        return account;
    }

    private HttpHeaders internalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Service", "authapp");
        headers.set("X-Internal-Secret", internalSecret);
        return headers;
    }

    private boolean hasOpenTicket(Integer userId) {
        try {
            restTemplate.getForEntity(OPEN_TICKET_URL, Object.class, userId);
            return true;
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) return false;
            throw exception;
        }
    }

    public AuthResponse loginEmployee(LoginRequest request) {
        EmployeeLoginAccount account = employeeAccounts.findByEmailIgnoreCase(request.email())
                .orElseThrow(InvalidCredentialsException::new);
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new EmployeeInactiveException();
        }
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
