package com.oracle.employeeapp.dtos;

public record EmployeeSummary(Integer id, String firstName, String lastName, String email,
                              Boolean mustChangePassword) { }
