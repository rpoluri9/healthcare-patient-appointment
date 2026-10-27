package com.healthcare.appointment.exception;

/** A booking that breaks a scheduling rule (HTTP 400). */
public class BookingValidationException extends RuntimeException {
    public BookingValidationException(String message) {
        super(message);
    }
}
