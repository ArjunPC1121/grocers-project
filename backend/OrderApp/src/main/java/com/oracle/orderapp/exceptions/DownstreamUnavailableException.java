package com.oracle.orderapp.exceptions;
import org.springframework.http.HttpStatus;
public class DownstreamUnavailableException extends OrderAppException {
    public DownstreamUnavailableException(String service, Throwable cause) {
        super("DOWNSTREAM_UNAVAILABLE", service + " service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        initCause(cause);
    }
}
