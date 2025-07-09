package com.example.nativecliapp.config;

import com.example.nativecliapp.dtos.DatabaseInfo;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class DatabaseConnectionManager {

    private final Map<String, DataSource> connections = new ConcurrentHashMap<>();
    private String currentConnection = "default";

    @Value("${db.default.url:jdbc:h2:mem:testdb}")
    private String defaultUrl;

    @Value("${db.default.username:sa}")
    private String defaultUsername;

    @Value("${db.default.password:}")
    private String defaultPassword;

    @PostConstruct
    public void initializeDefault() {
        createConnection("default", "h2", defaultUrl, defaultUsername, defaultPassword);
    }

    public void createConnection(String name, String dbType, String url, String username, String password) {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName(getDriverClassName(dbType));
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);

            HikariDataSource dataSource = new HikariDataSource(config);
            connections.put(name, dataSource);

            log.info("✅ Database connection '{}' created successfully", name);
        } catch (Exception e) {
            log.error("❌ Failed to create connection '{}': {}", name, e.getMessage());
            throw new RuntimeException("Failed to create database connection", e);
        }
    }

    public void switchConnection(String name) {
        if (!connections.containsKey(name)) {
            throw new IllegalArgumentException("Connection '" + name + "' not found");
        }
        this.currentConnection = name;
        log.info("🔄 Switched to connection: {}", name);
    }

    public DataSource getCurrentDataSource() {
        return connections.get(currentConnection);
    }

    public String getCurrentConnectionName() {
        return currentConnection;
    }

    public Set<String> getConnectionNames() {
        return new HashSet<>(connections.keySet());
    }

    public void closeConnection(String name) {
        DataSource dataSource = connections.remove(name);
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
            log.info("🔒 Connection '{}' closed", name);
        }
    }

    private String getDriverClassName(String dbType) {
        switch (dbType.toLowerCase()) {
            case "h2": return "org.h2.Driver";
            case "mysql": return "com.mysql.cj.jdbc.Driver";
            case "postgresql": return "org.postgresql.Driver";
            case "oracle": return "oracle.jdbc.OracleDriver";
            case "sqlite": return "org.sqlite.JDBC";
            case "sqlserver": return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            default: throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
    }

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
                    .build();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get database info", e);
        }
    }
}
