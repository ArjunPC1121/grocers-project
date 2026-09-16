package com.oracle.orderapp.exceptions;
import org.springframework.http.HttpStatus;
public class InvalidStateException extends OrderAppException {
    public InvalidStateException(String code, String message) { super(code, message, HttpStatus.CONFLICT); }
}
