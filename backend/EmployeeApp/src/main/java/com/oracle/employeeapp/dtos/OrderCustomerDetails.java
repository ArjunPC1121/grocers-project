package com.oracle.employeeapp.dtos;

public record OrderCustomerDetails(
        Integer id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String address
) { }
