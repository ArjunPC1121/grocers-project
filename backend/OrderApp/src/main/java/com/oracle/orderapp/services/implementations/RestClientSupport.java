package com.oracle.orderapp.services.implementations;

import com.oracle.orderapp.exceptions.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.converter.HttpMessageConversionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RestClientSupport {
    private static final Pattern CODE = Pattern.compile("\\\"code\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern MESSAGE = Pattern.compile("\\\"message\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private RestClientSupport() {}
    static RuntimeException translate(String service, RestClientException error) {
        if (error instanceof RestClientResponseException response) {
            int status=response.getStatusCode().value();
            String body = response.getResponseBodyAsString();
            String code = value(CODE, body, service.toUpperCase()+"_HTTP_"+status);
            String message = value(MESSAGE, body, service+" returned HTTP "+status);
            if(status==404) return new NotFoundException(code,message);
            if(status==409) return new DownstreamConflictException(code,message);
            if(status>=400 && status<500) return new DownstreamRejectedException(code,message);
            return new DownstreamUnavailableException(service,error);
        }
        if (hasCause(error, HttpMessageConversionException.class))
            return new DownstreamContractException(service + " returned malformed JSON");
        return new DownstreamUnavailableException(service,error);
    }
    private static String value(Pattern pattern, String json, String fallback) {
        if (json == null) return fallback;
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : fallback;
    }
    private static boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        for (Throwable current = error; current != null; current = current.getCause())
            if (type.isInstance(current)) return true;
        return false;
    }
}
