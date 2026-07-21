package com.ticket.auth.exception;


public class ForBiddenException extends RuntimeException {
    public ForBiddenException(String message) {
        super(message);
    }
}