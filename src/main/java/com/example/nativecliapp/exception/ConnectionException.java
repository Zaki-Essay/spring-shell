package com.example.nativecliapp.exception;

public class ConnectionException extends DatabaseException {
    private final String connectionName;

    public ConnectionException(String connectionName, String message) {
        super("CONNECTION", message);
        this.connectionName = connectionName;
    }

    public ConnectionException(String connectionName, String message, Throwable cause) {
        super("CONNECTION", message, cause);
        this.connectionName = connectionName;
    }

    public String getConnectionName() {
        return connectionName;
    }
}
