package com.oracle.orderapp.exceptions;

import org.springframework.http.HttpStatus;

public class DownstreamRejectedException extends OrderAppException {
    public DownstreamRejectedException(String code, String message) {
        super(code, message, HttpStatus.BAD_GATEWAY);
    }
}
