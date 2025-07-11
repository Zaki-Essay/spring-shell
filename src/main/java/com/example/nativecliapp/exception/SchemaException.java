package com.example.nativecliapp.exception;

public class SchemaException extends DatabaseException {
    public SchemaException(String message) {
        super("SCHEMA", message);
    }

    public SchemaException(String message, Throwable cause) {
        super("SCHEMA", message, cause);
    }
}
