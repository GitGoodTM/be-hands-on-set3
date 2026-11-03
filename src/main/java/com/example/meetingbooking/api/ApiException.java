package com.example.meetingbooking.api;

import org.springframework.http.HttpStatus;
import java.util.Map;

public class ApiException extends RuntimeException {
    public final HttpStatus status;
    public final Map<String, String> details;
    public ApiException(HttpStatus status, String message) {
        super(message); this.status = status; this.details = Map.of("message", message);
    }
    public ApiException(HttpStatus status, Map<String, String> details) {
        super(details.toString()); this.status = status; this.details = details;
    }
}
