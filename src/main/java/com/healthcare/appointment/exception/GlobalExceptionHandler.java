package com.healthcare.appointment.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Centralized exception handling: every error leaves the API in the same
 * clean shape, and error logs carry no patient details (only messages that
 * the service layer already scrubbed).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message,
                                                     HttpServletRequest req) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message,
                "path", req.getRequestURI()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(ResourceNotFoundException ex,
                                                        HttpServletRequest req) {
        return body(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(BookingConflictException.class)
    public ResponseEntity<Map<String, Object>> conflict(BookingConflictException ex,
                                                        HttpServletRequest req) {
        log.info("Booking conflict: {}", ex.getMessage());
        return body(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(BookingValidationException.class)
    public ResponseEntity<Map<String, Object>> validation(BookingValidationException ex,
                                                          HttpServletRequest req) {
        return body(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> forbidden(ForbiddenException ex,
                                                         HttpServletRequest req) {
        return body(HttpStatus.FORBIDDEN, ex.getMessage(), req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> denied(AccessDeniedException ex,
                                                      HttpServletRequest req) {
        return body(HttpStatus.FORBIDDEN, "Access denied for this resource.", req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> beanValidation(MethodArgumentNotValidException ex,
                                                              HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst().orElse("Validation failed.");
        return body(HttpStatus.BAD_REQUEST, message, req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception ex,
                                                          HttpServletRequest req) {
        log.error("Unexpected error on {}", req.getRequestURI(), ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.", req);
    }
}
