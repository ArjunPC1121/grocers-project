package com.oracle.userapp.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.ColumnDefault;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="users")
// Stores a customer's profile, wallet balance, and account-recovery information.
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotBlank(message = "First name can't be blank")
    @Column(nullable = false)
    private String firstName;

    @NotBlank(message="Can't be blank")
    @Column(nullable = false)
    private String lastName;

    @NotBlank
    @Column(nullable = false,unique = true)
    private String email;

    @NotBlank(message="Can't be blank")
    @Column(nullable = false)
    private String password;

    @NotNull(message="Can't be blank")
    @Column(nullable = false)
    private Date dob;

    @NotBlank(message="Can't be blank")
    @Column(nullable = false)
    private String phoneNumber;

    @NotBlank(message="Can't be blank")
    @Column(nullable = false)
    private String address;

    @NotBlank(message="Can't be blank")
    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    // Counts unsuccessful logins so the account can be locked after repeated failures.
    private int failedLoginAttempts = 0;

    @Column(nullable = false)
    @ColumnDefault("false")
    // Prevents sign-in while the user resolves a security issue.
    private boolean accountLocked = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    // Records why the account was locked, if it is currently locked.
    private LockedReason lockedReason;

    @Column(nullable = false)
    // Holds the money available for purchases in the application wallet.
    private double funds = 200;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SecretQuestion secretQuestion;

    @Column(nullable = false)
    // Stores an encrypted security-answer value, never the answer itself.
    private String secretAnswerHash;

    // Stores an encrypted, temporary token used during password recovery.
    private String passwordResetTokenHash;

    // Marks the time after which the password recovery token can no longer be used.
    private LocalDateTime passwordResetTokenExpiresAt;

}
