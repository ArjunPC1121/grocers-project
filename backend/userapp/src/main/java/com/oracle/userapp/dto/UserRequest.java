package com.oracle.userapp.dto;


import com.oracle.userapp.entities.SecretQuestion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor

/*
User registration DTO
 */
public class UserRequest {

    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    @NotBlank
    private String email;
    @NotBlank
    private String password;
    @NotNull
    private Date dob;
    @NotBlank
    private String phoneNumber;
    @NotBlank
    private String address;
    @NotBlank
    private String accountNumber;
    @NotNull
    private SecretQuestion secretQuestion;
    @NotBlank
    private String secretAnswer;
}
