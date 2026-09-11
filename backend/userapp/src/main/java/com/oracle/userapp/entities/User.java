package com.oracle.userapp.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;

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

    @NotBlank(message="Can't be blank")
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

}
