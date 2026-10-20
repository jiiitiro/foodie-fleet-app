package com.foodiefleet.app.exception;

// The request is valid but conflicts with the current state (e.g. an illegal order status change)
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
