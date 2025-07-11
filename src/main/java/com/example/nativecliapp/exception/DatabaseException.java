package com.example.nativecliapp.exception;

public class DatabaseException extends RuntimeException {
    private final String operation;

    public DatabaseException(String operation, String message) {
        super(message);
        this.operation = operation;
    }

    public DatabaseException(String operation, String message, Throwable cause) {
        super(message, cause);
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }
}
