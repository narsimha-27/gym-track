package com.gymtrack.exception;

/** Thrown when an action breaks a gym rule (expired plan, double check-in, duplicate phone...). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
