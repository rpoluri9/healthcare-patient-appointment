package com.healthcare.appointment.exception;

/** A booking that conflicts with an existing appointment (HTTP 409). */
public class BookingConflictException extends RuntimeException {
    public BookingConflictException(String message) {
        super(message);
    }
}
