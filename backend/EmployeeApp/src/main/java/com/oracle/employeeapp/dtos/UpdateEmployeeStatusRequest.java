package com.oracle.employeeapp.dtos;

import com.oracle.employeeapp.entities.EmployeeStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateEmployeeStatusRequest(@NotNull EmployeeStatus status) { }
