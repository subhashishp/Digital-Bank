package com.digitalbank.exception;

public class PreconditionRequiredException extends RuntimeException {
    public PreconditionRequiredException(String message) { super(message); }
}
