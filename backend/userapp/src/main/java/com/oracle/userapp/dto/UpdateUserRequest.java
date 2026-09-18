package com.oracle.userapp.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    private String firstName;
    private String lastName;

    @Email
    private String email;

    private Date dob;
    private String phoneNumber;
    private String address;
    private String accountNumber;
}
