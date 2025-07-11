package com.example.nativecliapp.config;

import com.example.nativecliapp.constant.DatabaseConstants;
import com.example.nativecliapp.dtos.DatabaseInfo;
import com.example.nativecliapp.exception.ConnectionException;
import com.example.nativecliapp.exception.DatabaseException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
@RequiredArgsConstructor
public class DatabaseConnectionManager {

    private final DatabaseConfig databaseConfig;
    private final MeterRegistry meterRegistry;

    private final Map<String, HikariDataSource> connections = new ConcurrentHashMap<>();
    private final AtomicReference<String> currentConnection = new AtomicReference<>(DatabaseConstants.DEFAULT_CONNECTION_NAME);
    private final Map<String, Timer> connectionTimers = new ConcurrentHashMap<>();

    @PostConstruct
    public void initializeDefaultConnection() {
        log.info("Initializing default database connection");

        DatabaseConfig.DefaultConnection defaultConn = databaseConfig.getDefaultConnection();
        if (defaultConn != null) {
            createConnection(
                    DatabaseConstants.DEFAULT_CONNECTION_NAME,
                    defaultConn.getType(),
                    defaultConn.getUrl(),
                    defaultConn.getUsername(),
                    defaultConn.getPassword()
            );
        }
    }

    /**
     * Creates a new database connection with validation and monitoring
     */
    public void createConnection(String name, String dbType, String url, String username, String password) {
        Objects.requireNonNull(name, "Connection name cannot be null");
        Objects.requireNonNull(dbType, "Database type cannot be null");
        Objects.requireNonNull(url, "Database URL cannot be null");

        if (connections.containsKey(name)) {
            log.warn("Connection '{}' already exists, closing existing connection", name);
            closeConnection(name);
        }

        try {
            HikariDataSource dataSource = createDataSource(name, dbType, url, username, password);

            // Test connection
            validateConnection(dataSource);

            connections.put(name, dataSource);
            setupConnectionMetrics(name, dataSource);

            log.info("✅ Database connection '{}' created successfully for {} database", name, dbType);

        } catch (Exception e) {
            log.error("❌ Failed to create connection '{}' for database type '{}': {}", name, dbType, e.getMessage());
            throw new ConnectionException(name, "Failed to create database connection: " + e.getMessage(), e);
        }
    }

    private HikariDataSource createDataSource(String name, String dbType, String url, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(getDriverClassName(dbType));

        // Pool configuration
        config.setMaximumPoolSize(databaseConfig.getMaxPoolSize());
        config.setMinimumIdle(databaseConfig.getMinIdle());
        config.setConnectionTimeout(databaseConfig.getConnectionTimeout());
        config.setIdleTimeout(databaseConfig.getIdleTimeout());
        config.setMaxLifetime(databaseConfig.getMaxLifetime());

        // Connection pool name for monitoring
        config.setPoolName(name + "-pool");

        // Health check
        config.setConnectionTestQuery(databaseConfig.getHealthCheckQuery());

        // Additional optimizations
        config.setLeakDetectionThreshold(60000);
        config.setRegisterMbeans(true);

        return new HikariDataSource(config);
    }

    private void validateConnection(HikariDataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(5)) {
                throw new ConnectionException("validation", "Connection validation failed");
            }
        } catch (SQLException e) {
            throw new ConnectionException("validation", "Failed to validate connection", e);
        }
    }

    private void setupConnectionMetrics(String name, HikariDataSource dataSource) {
        if (databaseConfig.isEnableMetrics()) {
            Timer timer = Timer.builder("database.connection.duration")
                    .tag("connection", name)
                    .register(meterRegistry);
            connectionTimers.put(name, timer);

            // Register HikariCP metrics
            dataSource.getHikariPoolMXBean();
        }
    }

    /**
     * Switches to a different database connection with validation
     */
    public void switchConnection(String name) {
        Objects.requireNonNull(name, "Connection name cannot be null");

        if (!connections.containsKey(name)) {
            throw new ConnectionException(name, "Connection '" + name + "' not found. Available connections: " +
                    String.join(", ", connections.keySet()));
        }

        String previousConnection = currentConnection.getAndSet(name);
        log.info("🔄 Switched from connection '{}' to '{}'", previousConnection, name);
    }

    /**
     * Gets the current data source with connection validation
     */
    public DataSource getCurrentDataSource() {
        String connectionName = currentConnection.get();
        HikariDataSource dataSource = connections.get(connectionName);

        if (dataSource == null) {
            throw new ConnectionException(connectionName, "Current connection '" + connectionName + "' is not available");
        }

        if (dataSource.isClosed()) {
            throw new ConnectionException(connectionName, "Current connection '" + connectionName + "' is closed");
        }

        return dataSource;
    }

    public String getCurrentConnectionName() {
        return currentConnection.get();
    }

    public Set<String> getConnectionNames() {
        return new HashSet<>(connections.keySet());
    }

    /**
     * Closes a connection with proper cleanup
     */
    public void closeConnection(String name) {
        Objects.requireNonNull(name, "Connection name cannot be null");

        HikariDataSource dataSource = connections.remove(name);
        if (dataSource != null) {
            try {
                dataSource.close();
                connectionTimers.remove(name);
                log.info("🔒 Connection '{}' closed successfully", name);
            } catch (Exception e) {
                log.error("Failed to close connection '{}': {}", name, e.getMessage());
            }
        }
    }

    /**
     * Gets driver class name for database type with validation
     */
    private String getDriverClassName(String dbType) {
        Objects.requireNonNull(dbType, "Database type cannot be null");

        return switch (dbType.toLowerCase()) {
            case DatabaseConstants.DB_TYPE_H2 -> DatabaseConstants.DRIVER_H2;
            case DatabaseConstants.DB_TYPE_MYSQL -> DatabaseConstants.DRIVER_MYSQL;
            case DatabaseConstants.DB_TYPE_POSTGRESQL -> DatabaseConstants.DRIVER_POSTGRESQL;
            case DatabaseConstants.DB_TYPE_ORACLE -> DatabaseConstants.DRIVER_ORACLE;
            case DatabaseConstants.DB_TYPE_SQLITE -> DatabaseConstants.DRIVER_SQLITE;
            case DatabaseConstants.DB_TYPE_SQL_SERVER -> DatabaseConstants.DRIVER_SQL_SERVER;
            default -> throw new IllegalArgumentException("Unsupported database type: " + dbType +
                    ". Supported types: " + String.join(", ",
                    DatabaseConstants.DB_TYPE_H2, DatabaseConstants.DB_TYPE_MYSQL,
                    DatabaseConstants.DB_TYPE_POSTGRESQL, DatabaseConstants.DB_TYPE_ORACLE,
                    DatabaseConstants.DB_TYPE_SQLITE, DatabaseConstants.DB_TYPE_SQL_SERVER));
        };
    }

    /**
     * Gets database information with enhanced error handling
     */
    public DatabaseInfo getDatabaseInfo() {
        try (Connection conn = getCurrentDataSource().getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            return DatabaseInfo.builder()
                    .productName(metaData.getDatabaseProductName())
                    .productVersion(metaData.getDatabaseProductVersion())
                    .driverName(metaData.getDriverName())
                    .driverVersion(metaData.getDriverVersion())
                    .url(metaData.getURL())
                    .username(metaData.getUserName())
                    .supportsTransactions(metaData.supportsTransactions())
                    .maxConnections(metaData.getMaxConnections())
                    .catalogSeparator(metaData.getCatalogSeparator())
                    .build();

        } catch (SQLException e) {
            throw new DatabaseException("GET_INFO", "Failed to retrieve database information", e);
        }
    }

    /**
     * Health check for all connections
     */
    public Map<String, Boolean> getConnectionHealthStatus() {
        Map<String, Boolean> healthStatus = new HashMap<>();

        connections.forEach((name, dataSource) -> {
            try (Connection conn = dataSource.getConnection()) {
                healthStatus.put(name, conn.isValid(5));
            } catch (SQLException e) {
                healthStatus.put(name, false);
                log.warn("Health check failed for connection '{}': {}", name, e.getMessage());
            }
        });

        return healthStatus;
    }

    @PreDestroy
    public void cleanup() {
        log.info("Shutting down database connections...");
        connections.keySet().forEach(this::closeConnection);
    }
}