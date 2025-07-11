package com.example.nativecliapp.constant;

public final class DatabaseConstants {

    // Connection Pool Configuration
    public static final int DEFAULT_MAX_POOL_SIZE = 10;
    public static final int DEFAULT_MIN_IDLE = 2;
    public static final long DEFAULT_CONNECTION_TIMEOUT = 30_000L;
    public static final long DEFAULT_IDLE_TIMEOUT = 600_000L;
    public static final long DEFAULT_MAX_LIFETIME = 1_800_000L;

    // Database Types
    public static final String DB_TYPE_H2 = "h2";
    public static final String DB_TYPE_MYSQL = "mysql";
    public static final String DB_TYPE_POSTGRESQL = "postgresql";
    public static final String DB_TYPE_ORACLE = "oracle";
    public static final String DB_TYPE_SQLITE = "sqlite";
    public static final String DB_TYPE_SQL_SERVER = "sqlserver";

    // Driver Class Names
    public static final String DRIVER_H2 = "org.h2.Driver";
    public static final String DRIVER_MYSQL = "com.mysql.cj.jdbc.Driver";
    public static final String DRIVER_POSTGRESQL = "org.postgresql.Driver";
    public static final String DRIVER_ORACLE = "oracle.jdbc.OracleDriver";
    public static final String DRIVER_SQLITE = "org.sqlite.JDBC";
    public static final String DRIVER_SQL_SERVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    // Default Connection
    public static final String DEFAULT_CONNECTION_NAME = "default";

    // SQL Keywords
    public static final String SQL_SELECT = "SELECT";
    public static final String SQL_CREATE_TABLE = "CREATE TABLE";
    public static final String SQL_NOT_NULL = "NOT NULL";
    public static final String SQL_PRIMARY_KEY = "PRIMARY KEY";
    public static final String SQL_DEFAULT = "DEFAULT";

    // Table Types
    public static final String[] TABLE_TYPES = {"TABLE"};

    // Formatting
    public static final String SEPARATOR_LONG = "═".repeat(80);
    public static final String SEPARATOR_SHORT = "─".repeat(40);
    public static final String SEPARATOR_MEDIUM = "─".repeat(60);

    private DatabaseConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
