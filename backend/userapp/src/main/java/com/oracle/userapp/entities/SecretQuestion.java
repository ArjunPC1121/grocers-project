package com.oracle.userapp.entities;


/*
Enum to have fixed Secret security questions which the user will choose during registration and
use in case of three failed password attempts.
 */

public enum SecretQuestion {
    FIRST_PET_NAME,
    FIRST_SCHOOL_NAME,
    CHILDHOOD_NICKNAME
}
