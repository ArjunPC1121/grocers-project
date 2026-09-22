package com.oracle.userapp.dto;

public record AdminCreatedUserResponse(UserResponse user, String temporaryPassword) {
}
