package com.oracle.requestapp.exceptions;

public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) { super(message); }
}
