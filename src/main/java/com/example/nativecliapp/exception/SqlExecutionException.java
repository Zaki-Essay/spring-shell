package com.example.nativecliapp.exception;

public class SqlExecutionException extends DatabaseException {
    private final String sql;

    public SqlExecutionException(String sql, String message) {
        super("SQL_EXECUTION", message);
        this.sql = sql;
    }

    public SqlExecutionException(String sql, String message, Throwable cause) {
        super("SQL_EXECUTION", message, cause);
        this.sql = sql;
    }

    public String getSql() {
        return sql;
    }
}
