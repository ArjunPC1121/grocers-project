package com.oracle.userapp.dto;

import com.oracle.userapp.entities.LockedReason;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketRequest {
    @Positive
    private int userId;

    @NotNull
    private LockedReason lockedReason;

    private String requestNote;
}
