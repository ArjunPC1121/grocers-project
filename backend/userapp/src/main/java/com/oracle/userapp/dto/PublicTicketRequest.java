package com.oracle.userapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Internal AuthApp-to-UserApp request for employee-assisted locked-account recovery. */
public record PublicTicketRequest(@NotBlank @Email String email, @Size(max = 1000) String note) { }
