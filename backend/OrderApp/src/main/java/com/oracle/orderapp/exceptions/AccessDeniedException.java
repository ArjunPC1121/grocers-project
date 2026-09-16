package com.oracle.orderapp.exceptions;
import org.springframework.http.HttpStatus;
public class AccessDeniedException extends OrderAppException {
    public AccessDeniedException(String code, String message) { super(code, message, HttpStatus.FORBIDDEN); }
}
