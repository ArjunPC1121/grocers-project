package com.oracle.orderapp.exceptions;
import org.springframework.http.HttpStatus;
public class DownstreamContractException extends OrderAppException {
    public DownstreamContractException(String message) { super("DOWNSTREAM_CONTRACT_ERROR", message, HttpStatus.BAD_GATEWAY); }
}
