package com.example.nativecliapp;

import com.example.nativecliapp.config.DatabaseConnectionManager;
import com.example.nativecliapp.config.SchemaManager;
import com.example.nativecliapp.constant.DatabaseConstants;
import com.example.nativecliapp.dtos.ColumnInfo;
import com.example.nativecliapp.dtos.DatabaseInfo;
import com.example.nativecliapp.dtos.TableInfo;
import com.example.nativecliapp.exception.ConnectionException;
import com.example.nativecliapp.exception.SchemaException;
import com.example.nativecliapp.exception.SqlExecutionException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ShellComponent
@Slf4j
@RequiredArgsConstructor
public class GenericDatabaseCLI {
    private final DatabaseConnectionManager connectionManager;
    private final SchemaManager schemaManager;
    private final MeterRegistry meterRegistry;

    // ═══════════════════════════════════════════════════════════════════════════════════════
    // 🔗 CONNECTION MANAGEMENT COMMANDS
    // ═══════════════════════════════════════════════════════════════════════════════════════

    @ShellMethod(key = {"connect", "conn"}, value = "🔗 Connect to a database")
    public String connect(
            @ShellOption(value = {"-n", "--name"}, help = "Connection name") String name,
            @ShellOption(value = {"-t", "--type"}, help = "Database type (h2, mysql, postgresql, oracle, sqlite, sqlserver)") String dbType,
            @ShellOption(value = {"-u", "--url"}, help = "Database URL") String url,
            @ShellOption(value = {"--username"}, help = "Username", defaultValue = "sa") String username,
            @ShellOption(value = {"--password"}, help = "Password", defaultValue = "") String password) {

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            // Validate inputs
            validateConnectionParameters(name, dbType, url);

            connectionManager.createConnection(name, dbType, url, username, password);
            connectionManager.switchConnection(name);

            DatabaseInfo info = connectionManager.getDatabaseInfo();

            sample.stop(Timer.builder("database.cli.connect.success").register(meterRegistry));

            return formatConnectionSuccess(name, info);

        } catch (ConnectionException e) {
            sample.stop( Timer.builder("database.cli.connect.error").register(meterRegistry));
            return formatError("Connection Failed",
                    "Failed to connect to '" + e.getConnectionName() + "': " + e.getMessage());
        } catch (IllegalArgumentException e) {
            sample.stop( Timer.builder("database.cli.connect.validation_error").register(meterRegistry));
            return formatError("Invalid Parameters", e.getMessage());
        } catch (Exception e) {
            sample.stop( Timer.builder("database.cli.connect.unexpected_error").register(meterRegistry));
            return formatError("Unexpected Error", e.getMessage());
        }
    }

    private void validateConnectionParameters(String name, String dbType, String url) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Connection name cannot be empty");
        }

        if (dbType == null || dbType.trim().isEmpty()) {
            throw new IllegalArgumentException("Database type cannot be empty");
        }

        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Database URL cannot be empty");
        }

        // Validate URL format
        if (!url.startsWith("jdbc:")) {
            throw new IllegalArgumentException("Database URL must start with 'jdbc:'");
        }
    }

    private String formatConnectionSuccess(String name, DatabaseInfo info) {
        return formatResponse("DATABASE CONNECTED SUCCESSFULLY",
                String.format("""
                🔗 Connection: %s
                🗄️  Database: %s %s
                🔧 Driver: %s %s
                🌐 URL: %s
                👤 User: %s
                🔢 Max Connections: %d
                ✅ Supports Transactions: %s
                📅 Connected: %s
                """,
                        name,
                        info.getProductName(), info.getProductVersion(),
                        info.getDriverName(), info.getDriverVersion(),
                        info.getUrl(),
                        info.getUsername(),
                        info.getMaxConnections(),
                        info.isSupportsTransactions() ? "Yes" : "No",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                ));
    }

    @ShellMethod(key = {"connections", "conns"}, value = "📋 List all database connections")
    public String listConnections() {
        try {
            Set<String> connections = connectionManager.getConnectionNames();
            String current = connectionManager.getCurrentConnectionName();
            Map<String, Boolean> healthStatus = connectionManager.getConnectionHealthStatus();

            if (connections.isEmpty()) {
                return formatWarning("No Connections", "No database connections configured");
            }

            StringBuilder result = new StringBuilder();
            result.append("🔗 DATABASE CONNECTIONS\n");
            result.append(DatabaseConstants.SEPARATOR_MEDIUM).append("\n");
            result.append(String.format("%-20s | %-12s | %-10s\n", "NAME", "STATUS", "HEALTH"));
            result.append(DatabaseConstants.SEPARATOR_MEDIUM).append("\n");

            connections.forEach(name -> {
                String status = name.equals(current) ? "🟢 ACTIVE" : "⚪ INACTIVE";
                String health = Boolean.TRUE.equals(healthStatus.get(name)) ? "✅ HEALTHY" : "❌ UNHEALTHY";
                result.append(String.format("%-20s | %-12s | %-10s\n", name, status, health));
            });

            result.append("\n").append(DatabaseConstants.SEPARATOR_MEDIUM);
            result.append(String.format("\nTotal: %d connections", connections.size()));
            return result.toString();

        } catch (Exception e) {
            return formatError("List Connections Failed", e.getMessage());
        }
    }

    @ShellMethod(key = {"use", "switch"}, value = "🔄 Switch to a different database connection")
    public String switchConnection(@ShellOption(value = {"-n", "--name"}, help = "Connection name") String name) {
        try {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Connection name cannot be empty");
            }

            connectionManager.switchConnection(name);
            DatabaseInfo info = connectionManager.getDatabaseInfo();

            return formatResponse("CONNECTION SWITCHED",
                    String.format("🔄 Now using connection: %s (%s)", name, info.getProductName()));

        } catch (ConnectionException e) {
            return formatError("Switch Failed", e.getMessage());
        } catch (IllegalArgumentException e) {
            return formatError("Invalid Parameter", e.getMessage());
        } catch (Exception e) {
            return formatError("Unexpected Error", e.getMessage());
        }
    }

    @ShellMethod(key = {"disconnect", "close"}, value = "🔒 Close a database connection")
    public String closeConnection(@ShellOption(value = {"-n", "--name"}, help = "Connection name") String name) {
        try {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Connection name cannot be empty");
            }

            if (name.equals(connectionManager.getCurrentConnectionName())) {
                return formatWarning("Cannot Close Active Connection",
                        "Cannot close the currently active connection. Switch to another connection first.");
            }

            connectionManager.closeConnection(name);
            return formatResponse("CONNECTION CLOSED", "🔒 Connection '" + name + "' closed successfully");

        } catch (IllegalArgumentException e) {
            return formatError("Invalid Parameter", e.getMessage());
        } catch (Exception e) {
            return formatError("Close Failed", e.getMessage());
        }
    }

    @ShellMethod(key = {"info", "db-info"}, value = "ℹ️ Show current database information")
    public String showDatabaseInfo() {
        try {
            DatabaseInfo info = connectionManager.getDatabaseInfo();
            String connectionName = connectionManager.getCurrentConnectionName();

            return formatResponse("DATABASE INFORMATION",
                    String.format("""
                    🔗 Connection: %s
                    🗄️  Database: %s %s
                    🔧 Driver: %s %s
                    🌐 URL: %s
                    👤 User: %s
                    🔢 Max Connections: %d
                    📊 Catalog Separator: %s
                    ✅ Supports Transactions: %s
                    """,
                            connectionName,
                            info.getProductName(), info.getProductVersion(),
                            info.getDriverName(), info.getDriverVersion(),
                            info.getUrl(),
                            info.getUsername(),
                            info.getMaxConnections(),
                            info.getCatalogSeparator(),
                            info.isSupportsTransactions() ? "Yes" : "No"
                    ));

        } catch (Exception e) {
            return formatError("Database Info Failed", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════
    // 🗄️ SCHEMA MANAGEMENT COMMANDS
    // ═══════════════════════════════════════════════════════════════════════════════════════

    @ShellMethod(key = {"schemas", "show-schemas"}, value = "📋 List all schemas")
    public String listSchemas() {
        try {
            List<String> schemas = schemaManager.listSchemas();

            if (schemas.isEmpty()) {
                return formatWarning("No Schemas", "No schemas found in current database");
            }

            StringBuilder result = new StringBuilder();
            result.append("📋 DATABASE SCHEMAS\n");
            result.append(DatabaseConstants.SEPARATOR_SHORT).append("\n");
            result.append(String.format("Database: %s\n\n", connectionManager.getCurrentConnectionName()));

            schemas.forEach(schema -> result.append("📁 ").append(schema).append("\n"));

            result.append(String.format("\nTotal: %d schemas", schemas.size()));
            return result.toString();

        } catch (SchemaException e) {
            return formatError("List Schemas Failed", e.getMessage());
        } catch (Exception e) {
            return formatError("Unexpected Error", e.getMessage());
        }
    }

    @ShellMethod(key = {"tables", "show-tables"}, value = "📋 List all tables in a schema")
    public String listTables(@ShellOption(value = {"-s", "--schema"}, help = "Schema name", defaultValue = ShellOption.NULL) String schema) {
        try {
            List<TableInfo> tables = schemaManager.listTables(schema);

            if (tables.isEmpty()) {
                return formatWarning("No Tables", "No tables found in schema: " + (schema != null ? schema : "default"));
            }

            StringBuilder result = new StringBuilder();
            result.append("📋 DATABASE TABLES\n");
            result.append(DatabaseConstants.SEPARATOR_LONG).append("\n");
            result.append(String.format("📁 Schema: %s\n", schema != null ? schema : "default"));
            result.append(String.format("🔗 Connection: %s\n\n", connectionManager.getCurrentConnectionName()));

            result.append(String.format("%-30s | %-15s | %-30s\n", "TABLE NAME", "TYPE", "REMARKS"));
            result.append(DatabaseConstants.SEPARATOR_LONG).append("\n");

            tables.forEach(table -> {
                result.append(String.format("%-30s | %-15s | %-30s\n",
                        truncate(table.getName(), 30),
                        truncate(table.getType(), 15),
                        truncate(table.getRemarks() != null ? table.getRemarks() : "-", 30)
                ));
            });

            result.append(String.format("\nTotal: %d tables", tables.size()));
            return result.toString();

        } catch (SchemaException e) {
            return formatError("List Tables Failed", e.getMessage());
        } catch (Exception e) {
            return formatError("Unexpected Error", e.getMessage());
        }
    }

    @ShellMethod(key = {"describe", "desc"}, value = "📝 Describe table structure")
    public String describeTable(
            @ShellOption(value = {"-t", "--table"}, help = "Table name") String tableName,
            @ShellOption(value = {"-s", "--schema"}, help = "Schema name", defaultValue = ShellOption.NULL) String schema) {

        try {
            if (tableName == null || tableName.trim().isEmpty()) {
                throw new IllegalArgumentException("Table name cannot be empty");
            }

            List<ColumnInfo> columns = schemaManager.describeTable(schema, tableName);

            StringBuilder result = new StringBuilder();
            result.append("📝 TABLE STRUCTURE\n");
            result.append(DatabaseConstants.SEPARATOR_LONG).append("\n");
            result.append(String.format("📄 Table: %s.%s\n", schema != null ? schema : "default", tableName));
            result.append(String.format("🔗 Connection: %s\n\n", connectionManager.getCurrentConnectionName()));

            result.append(String.format("%-20s | %-15s | %-8s | %-8s | %-15s | %-20s\n",
                    "COLUMN", "TYPE", "SIZE", "NULLABLE", "DEFAULT", "REMARKS"));
            result.append(DatabaseConstants.SEPARATOR_LONG).append("\n");

            columns.forEach(column -> {
                result.append(String.format("%-20s | %-15s | %-8s | %-8s | %-15s | %-20s\n",
                        truncate(column.getName(), 20),
                        truncate(column.getType(), 15),
                        column.getSize() > 0 ? String.valueOf(column.getSize()) : "-",
                        column.isNullable() ? "YES" : "NO",
                        truncate(column.getDefaultValue() != null ? column.getDefaultValue() : "-", 15),
                        truncate(column.getRemarks() != null ? column.getRemarks() : "-", 20)
                ));
            });

            result.append(String.format("\nTotal: %d columns", columns.size()));
            return result.toString();

        } catch (SchemaException e) {
            return formatError("Describe Failed", e.getMessage());
        } catch (IllegalArgumentException e) {
            return formatError("Invalid Parameter", e.getMessage());
        } catch (Exception e) {
            return formatError("Unexpected Error", e.getMessage());
        }
    }

    @ShellMethod(key = {"table-exists"}, value = "❓ Check if a table exists")
    public String checkTableExists(
            @ShellOption(value = {"-t", "--table"}, help = "Table name") String tableName,
            @ShellOption(value = {"-s", "--schema"}, help = "Schema name", defaultValue = ShellOption.NULL) String schema) {

        try {
            if (tableName == null || tableName.trim().isEmpty()) {
                throw new IllegalArgumentException("Table name cannot be empty");
            }

            boolean exists = schemaManager.tableExists(schema, tableName);
            String schemaName = schema != null ? schema : "default";

            if (exists) {
                return formatResponse("TABLE EXISTS",
                        String.format("✅ Table '%s.%s' exists", schemaName, tableName));
            } else {
                return formatWarning("TABLE NOT FOUND",
                        String.format("❌ Table '%s.%s' does not exist", schemaName, tableName));
            }

        } catch (IllegalArgumentException e) {
            return formatError("Invalid Parameter", e.getMessage());
        } catch (Exception e) {
            return formatError("Check Failed", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════
    // 🔧 SQL EXECUTION COMMANDS
    // ═══════════════════════════════════════════════════════════════════════════════════════

    @ShellMethod(key = {"sql", "execute"}, value = "⚡ Execute SQL query or statement")
    public String executeSQL(@ShellOption(value = {"-q", "--query"}, help = "SQL query") String sql) {
        try {
            if (sql == null || sql.trim().isEmpty()) {
                throw new IllegalArgumentException("SQL query cannot be empty");
            }

            String trimmedSql = sql.trim();

            if (trimmedSql.toUpperCase().startsWith(DatabaseConstants.SQL_SELECT)) {
                return executeQueryCommand(trimmedSql);
            } else {
                return executeUpdateCommand(trimmedSql);
            }

        } catch (SqlExecutionException e) {
            return formatError("SQL Execution Failed",
                    String.format("Query: %s\nError: %s",
                            truncate(e.getSql(), 100), e.getMessage()));
        } catch (IllegalArgumentException e) {
            return formatError("Invalid Parameter", e.getMessage());
        } catch (Exception e) {
            return formatError("Unexpected Error", e.getMessage());
        }
    }

    private String executeQueryCommand(String sql) {
        List<Map<String, Object>> results = schemaManager.executeQuery(sql);

        if (results.isEmpty()) {
            return formatWarning("No Results", "Query returned no results");
        }

        StringBuilder result = new StringBuilder();
        result.append("📊 QUERY RESULTS\n");
        result.append(DatabaseConstants.SEPARATOR_LONG).append("\n");
        result.append(String.format("🔗 Connection: %s\n", connectionManager.getCurrentConnectionName()));
        result.append(String.format("📝 Query: %s\n\n", truncate(sql, 100)));

        // Get column names
        Set<String> columnNames = new LinkedHashSet<>(results.get(0).keySet());

        // Header
        columnNames.forEach(col -> result.append(String.format("%-20s | ", truncate(col, 20))));
        result.append("\n");
        result.append(DatabaseConstants.SEPARATOR_LONG).append("\n");

        // Rows (limit to first 100 for readability)
        int rowCount = Math.min(results.size(), 100);
        for (int i = 0; i < rowCount; i++) {
            Map<String, Object> row = results.get(i);
            columnNames.forEach(col -> {
                Object value = row.get(col);
                String displayValue = value != null ? value.toString() : "NULL";
                result.append(String.format("%-20s | ", truncate(displayValue, 20)));
            });
            result.append("\n");
        }

        result.append(String.format("\nTotal: %d rows", results.size()));
        if (results.size() > 100) {
            result.append(" (showing first 100)");
        }

        return result.toString();
    }

    private String executeUpdateCommand(String sql) {
        schemaManager.executeUpdate(sql);
        return formatResponse("SQL EXECUTED",
                String.format("✅ SQL statement executed successfully\n📝 Query: %s", truncate(sql, 100)));
    }

    @ShellMethod(key = {"query-file", "exec-file"}, value = "📄 Execute SQL from file")
    public String executeFile(@ShellOption(value = {"-f", "--file"}, help = "SQL file path") String filePath) {
        try {
            if (filePath == null || filePath.trim().isEmpty()) {
                throw new IllegalArgumentException("File path cannot be empty");
            }

            // This would typically read from a file - placeholder for now
            return formatWarning("Feature Not Implemented",
                    "File execution feature is not yet implemented. Use 'sql' command for direct SQL execution.");

        } catch (IllegalArgumentException e) {
            return formatError("Invalid Parameter", e.getMessage());
        } catch (Exception e) {
            return formatError("File Execution Failed", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════
    // 🎨 FORMATTING UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════════════════

    private String formatResponse(String title, String content) {
        return String.format("✅ %s\n%s\n%s",
                title,
                DatabaseConstants.SEPARATOR_SHORT,
                content);
    }

    private String formatError(String title, String message) {
        return String.format("❌ %s\n%s\n🔥 %s",
                title,
                DatabaseConstants.SEPARATOR_SHORT,
                message);
    }

    private String formatWarning(String title, String message) {
        return String.format("⚠️ %s\n%s\n💡 %s",
                title,
                DatabaseConstants.SEPARATOR_SHORT,
                message);
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════
    // 🔍 HEALTH CHECK AND MONITORING COMMANDS
    // ═══════════════════════════════════════════════════════════════════════════════════════

    @ShellMethod(key = {"health", "status"}, value = "🏥 Check database connection health")
    public String checkHealth() {
        try {
            Map<String, Boolean> healthStatus = connectionManager.getConnectionHealthStatus();
            String currentConnection = connectionManager.getCurrentConnectionName();

            StringBuilder result = new StringBuilder();
            result.append("🏥 DATABASE HEALTH STATUS\n");
            result.append(DatabaseConstants.SEPARATOR_MEDIUM).append("\n");

            healthStatus.forEach((name, isHealthy) -> {
                String indicator = isHealthy ? "✅ HEALTHY" : "❌ UNHEALTHY";
                String current = name.equals(currentConnection) ? " (CURRENT)" : "";
                result.append(String.format("%-20s | %s%s\n", name, indicator, current));
            });

            long healthyCount = healthStatus.values().stream().mapToLong(h -> h ? 1 : 0).sum();
            result.append(String.format("\nSummary: %d/%d connections healthy", healthyCount, healthStatus.size()));

            return result.toString();

        } catch (Exception e) {
            return formatError("Health Check Failed", e.getMessage());
        }
    }

    @ShellMethod(key = {"stats", "statistics"}, value = "📊 Show database statistics")
    public String showStatistics() {
        try {
            Set<String> connections = connectionManager.getConnectionNames();
            String currentConnection = connectionManager.getCurrentConnectionName();

            StringBuilder result = new StringBuilder();
            result.append("📊 DATABASE STATISTICS\n");
            result.append(DatabaseConstants.SEPARATOR_MEDIUM).append("\n");
            result.append(String.format("🔗 Total Connections: %d\n", connections.size()));
            result.append(String.format("🟢 Current Connection: %s\n", currentConnection));

            // Schema statistics
            try {
                List<String> schemas = schemaManager.listSchemas();
                result.append(String.format("📁 Schemas: %d\n", schemas.size()));

                if (!schemas.isEmpty()) {
                    // Get table count for first schema as example
                    String firstSchema = schemas.get(0);
                    int tableCount = schemaManager.getTableCount(firstSchema);
                    result.append(String.format("📄 Tables (in %s): %d\n", firstSchema, tableCount));
                }
            } catch (Exception e) {
                result.append("📁 Schemas: Unable to retrieve\n");
            }

            DatabaseInfo info = connectionManager.getDatabaseInfo();
            result.append(String.format("🗄️ Database: %s %s\n", info.getProductName(), info.getProductVersion()));

            return result.toString();

        } catch (Exception e) {
            return formatError("Statistics Failed", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════
    // 🆘 HELP AND UTILITY COMMANDS
    // ═══════════════════════════════════════════════════════════════════════════════════════

    @ShellMethod(key = {"db-help", "help-db"}, value = "🆘 Show database CLI help")
    public String showHelp() {
        return formatResponse("DATABASE CLI HELP",
                """
                🔗 CONNECTION COMMANDS:
                • connect -n <name> -t <type> -u <url> [--username] [--password]
                • connections / conns - List all connections
                • use -n <name> - Switch connection
                • disconnect -n <name> - Close connection
                • info - Show current database info
                
                🗄️ SCHEMA COMMANDS:
                • schemas - List all schemas
                • tables [-s <schema>] - List tables
                • describe -t <table> [-s <schema>] - Describe table
                • table-exists -t <table> [-s <schema>] - Check if table exists
                
                🔧 SQL COMMANDS:
                • sql -q "<query>" - Execute SQL
                • query-file -f <file> - Execute SQL from file
                
                🔍 MONITORING COMMANDS:
                • health - Check connection health
                • stats - Show database statistics
                
                📝 SUPPORTED DATABASE TYPES:
                • h2, mysql, postgresql, oracle, sqlite, sqlserver
                
                💡 TIP: Use 'help' to see all available commands
                """);
    }
}