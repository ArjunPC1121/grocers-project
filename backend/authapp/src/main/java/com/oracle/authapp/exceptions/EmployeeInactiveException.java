package com.oracle.authapp.exceptions;

public class EmployeeInactiveException extends RuntimeException {
    public EmployeeInactiveException() {
        super("This employee account is inactive and cannot sign in");
    }
}
