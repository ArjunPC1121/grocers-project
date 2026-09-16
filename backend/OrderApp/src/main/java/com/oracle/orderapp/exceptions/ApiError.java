package com.oracle.orderapp.exceptions;
import java.time.Instant;
import java.util.List;
public record ApiError(Instant timestamp, int status, String code, String message, String path,
        String correlationId, List<FieldErrorDetail> fieldErrors) {
    public record FieldErrorDetail(String field, String message) {}
}
