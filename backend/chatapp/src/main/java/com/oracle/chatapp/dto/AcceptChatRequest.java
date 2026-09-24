package com.oracle.chatapp.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record AcceptChatRequest(@NotBlank @Size(max=120) String employeeName) { }
