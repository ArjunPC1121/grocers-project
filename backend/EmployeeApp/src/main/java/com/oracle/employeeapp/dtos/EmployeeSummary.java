package com.oracle.employeeapp.dtos;

import com.oracle.employeeapp.entities.EmployeeStatus;

public record EmployeeSummary(Integer id, String firstName, String lastName, String email,
                              Boolean mustChangePassword, EmployeeStatus status) { }
