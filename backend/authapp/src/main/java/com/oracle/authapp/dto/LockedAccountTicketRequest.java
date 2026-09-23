package com.oracle.authapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Contains no password or recovery answer; it is only used to request employee assistance. */
public record LockedAccountTicketRequest(@NotBlank @Email String email, @Size(max = 1000) String note) { }
