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
    private int failedLoginAttempts = 0;

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean accountLocked = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private LockedReason lockedReason;

    @Column(nullable = false)
    private double funds = 200;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SecretQuestion secretQuestion;

    @Column(nullable = false)
    private String secretAnswerHash;

    private String passwordResetTokenHash;

    private LocalDateTime passwordResetTokenExpiresAt;

}
