package com.oracle.orderapp.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(OrderAppException.class)
    ResponseEntity<ApiError> domain(OrderAppException e,HttpServletRequest request){return body(e.getStatus(),e.getCode(),e.getMessage(),request,List.of());}
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException e,HttpServletRequest request){
        List<ApiError.FieldErrorDetail> fields=e.getBindingResult().getFieldErrors().stream().map(f->new ApiError.FieldErrorDetail(f.getField(),f.getDefaultMessage())).toList();
        return body(HttpStatus.BAD_REQUEST,"VALIDATION_FAILED","Request validation failed",request,fields);
    }
    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<ApiError> header(MissingRequestHeaderException e,HttpServletRequest request){return body(HttpStatus.BAD_REQUEST,"MISSING_HEADER",e.getMessage(),request,List.of());}
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<ApiError> malformed(Exception e,HttpServletRequest request){return body(HttpStatus.BAD_REQUEST,"MALFORMED_REQUEST",e.getMessage(),request,List.of());}
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception e,HttpServletRequest request){LOGGER.error("Unhandled request failure",e);return body(HttpStatus.INTERNAL_SERVER_ERROR,"INTERNAL_ERROR","Unexpected server error",request,List.of());}
    private ResponseEntity<ApiError> body(HttpStatus status,String code,String message,HttpServletRequest r,List<ApiError.FieldErrorDetail> fields){
        String correlation=r.getHeader("Idempotency-Key");
        return ResponseEntity.status(status).body(new ApiError(Instant.now(),status.value(),code,message,r.getRequestURI(),correlation,fields));
    }
}
