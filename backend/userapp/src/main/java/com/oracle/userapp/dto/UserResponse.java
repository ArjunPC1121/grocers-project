package com.oracle.userapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private int id;

    private String firstName;

    private String lastName;

    private String email;

    private Date dob;

    private String phoneNumber;

    private String address;

    private String accountNumber;
}
