package com.healthcare.appointment.exception;

/** The caller is authenticated but may not touch this record (HTTP 403). */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
