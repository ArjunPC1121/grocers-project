package com.oracle.requestapp.exceptions;

public class InvalidRequestStateException extends RuntimeException {
    public InvalidRequestStateException(String message) { super(message); }
}
