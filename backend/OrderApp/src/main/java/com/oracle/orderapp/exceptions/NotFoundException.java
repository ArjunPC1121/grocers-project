package com.oracle.orderapp.exceptions;
import org.springframework.http.HttpStatus;
public class NotFoundException extends OrderAppException {
    public NotFoundException(String code, String message) { super(code, message, HttpStatus.NOT_FOUND); }
}
