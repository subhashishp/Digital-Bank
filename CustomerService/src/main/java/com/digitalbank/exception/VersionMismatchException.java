package com.digitalbank.exception;

public class VersionMismatchException extends RuntimeException {
    public VersionMismatchException(String message) { super(message); };
}
