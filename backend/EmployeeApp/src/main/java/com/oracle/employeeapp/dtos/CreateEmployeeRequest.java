package com.oracle.employeeapp.dtos;

import jakarta.validation.constraints.NotBlank;

public record CreateEmployeeRequest(@NotBlank String firstName, @NotBlank String lastName,
                                    @NotBlank String email, @NotBlank String defaultPassword) { }
