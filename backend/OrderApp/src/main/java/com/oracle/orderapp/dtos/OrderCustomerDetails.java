package com.oracle.orderapp.dtos;

/** Safe customer information required by employees fulfilling an order. */
public record OrderCustomerDetails(
        Integer id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String address
) { }
