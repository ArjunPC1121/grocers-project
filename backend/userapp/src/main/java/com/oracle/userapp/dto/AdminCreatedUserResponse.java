package com.oracle.userapp.dto;

//Currently not in use as we are not exposing add user to admin
public record AdminCreatedUserResponse(UserResponse user, String temporaryPassword) {
}
