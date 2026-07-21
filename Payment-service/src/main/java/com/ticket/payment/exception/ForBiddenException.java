package com.ticket.payment.exception;


public class ForBiddenException extends RuntimeException {
    public ForBiddenException(String message) {
        super(message);
    }
}