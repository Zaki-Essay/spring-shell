package com.example.nativecliapp;

import com.example.nativecliapp.config.DatabaseConnectionManager;
import com.example.nativecliapp.config.SchemaManager;
import com.example.nativecliapp.dtos.ColumnInfo;
import com.example.nativecliapp.dtos.DatabaseInfo;
import com.example.nativecliapp.dtos.TableInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ShellComponent
@Slf4j
public class GenericDatabaseCLI {

    private final DatabaseConnectionManager connectionManager;
    private final SchemaManager schemaManager;

    public GenericDatabaseCLI(DatabaseConnectionManager connectionManager, SchemaManager schemaManager) {
        this.connectionManager = connectionManager;
        this.schemaManager = schemaManager;
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════
    // 🔗 CONNECTION MANAGEMENT COMMANDS
    // ═══════════════════════════════════════════════════════════════════════════════════════

    @ShellMethod(key = {"connect", "conn"}, value = "🔗 Connect to a database")
    public String connect(
            @ShellOption(value = {"-n", "--name"}, help = "Connection name") String name,
            @ShellOption(value = {"-t", "--type"}, help = "Database type (h2, mysql, postgresql, etc.)") String dbType,
            @ShellOption(value = {"-u", "--url"}, help = "Database URL") String url,
            @ShellOption(value = {"--username"}, help = "Username", defaultValue = "sa") String username,
            @ShellOption(value = {"--password"}, help = "Password", defaultValue = "") String password) {

        try {
            connectionManager.createConnection(name, dbType, url, username, password);
            connectionManager.switchConnection(name);

            DatabaseInfo info = connectionManager.getDatabaseInfo();

            return formatResponse("DATABASE CONNECTED",
                    "🔗 Connection: " + name + "\n" +
                            "🗄️  Database: " + info.getProductName() + " " + info.getProductVersion() + "\n" +
                            "🔧 Driver: " + info.getDriverName() + " " + info.getDriverVersion() + "\n" +
                            "🌐 URL: " + info.getUrl() + "\n" +
                            "👤 User: " + info.getUsername() + "\n" +
                            "📅 Connected: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
        } catch (Exception e) {
            return formatError("Connection Failed", e.getMessage());
        }
    }

    @ShellMethod(key = {"connections", "conns"}, value = "📋 List all database connections")
    public String listConnections() {
        Set<String> connections = connectionManager.getConnectionNames();
        String current = connectionManager.getCurrentConnectionName();

        if (connections.isEmpty()) {
            return formatWarning("No Connections", "No database connections configured");
        }

        StringBuilder result = new StringBuilder();
        result.append("🔗 DATABASE CONNECTIONS\n");
        result.append("═".repeat(50)).append("\n");

        connections.forEach(name -> {
            String indicator = name.equals(current) ? "🟢 ACTIVE" : "⚪ INACTIVE";
            result.append(String.format("%-20s | %s\n", name, indicator));
        });

        result.append("\n").append("═".repeat(50));
        return result.toString();
    }

    @ShellMethod(key = {"use", "switch"}, value = "🔄 Switch to a different database connection")
    public String switchConnection(@ShellOption(value = {"-n", "--name"}, help = "Connection name") String name) {
        try {
            connectionManager.switchConnection(name);
            return formatResponse("CONNECTION SWITCHED", "🔄 Now using connection: " + name);
        } catch (Exception e) {
            return formatError("Switch Failed", e.getMessage());
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
            result.append("═".repeat(40)).append("\n");

            schemas.forEach(schema -> result.append("📁 ").append(schema).append("\n"));

            result.append("\n").append("Total: ").append(schemas.size()).append(" schemas");
            return result.toString();
        } catch (Exception e) {
            return formatError("List Schemas Failed", e.getMessage());
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
            result.append("═".repeat(60)).append("\n");
            result.append(String.format("📁 Schema: %s\n\n", schema != null ? schema : "default"));

            tables.forEach(table -> {
                result.append(String.format("📄 %-25s | Type: %-10s\n",
                        table.getName(), table.getType()));
                if (table.getRemarks() != null && !table.getRemarks().isEmpty()) {
                    result.append(String.format("   💬 %s\n", table.getRemarks()));
                }
            });

            result.append("\n").append("Total: ").append(tables.size()).append(" tables");
            return result.toString();
        } catch (Exception e) {
            return formatError("List Tables Failed", e.getMessage());
        }
    }

    @ShellMethod(key = {"describe", "desc"}, value = "📝 Describe table structure")
    public String describeTable(
            @ShellOption(value = {"-t", "--table"}, help = "Table name") String tableName,
            @ShellOption(value = {"-s", "--schema"}, help = "Schema name", defaultValue = ShellOption.NULL) String schema) {

        try {
            List<ColumnInfo> columns = schemaManager.describeTable(schema, tableName);

            if (columns.isEmpty()) {
                return formatWarning("Table Not Found", "Table '" + tableName + "' not found");
            }

            StringBuilder result = new StringBuilder();
            result.append("📝 TABLE STRUCTURE\n");
            result.append("═".repeat(80)).append("\n");
            result.append(String.format("📄 Table: %s.%s\n\n", schema != null ? schema : "default", tableName));

            result.append(String.format("%-20s | %-15s | %-10s | %-8s | %-15s\n",
                    "COLUMN", "TYPE", "SIZE", "NULLABLE", "DEFAULT"));
            result.append("─".repeat(80)).append("\n");

            columns.forEach(column -> {
                result.append(String.format("%-20s | %-15s | %-10s | %-8s | %-15s\n",
                        column.getName(),
                        column.getType(),
                        column.getSize() > 0 ? String.valueOf(column.getSize()) : "-",
                        column.isNullable() ? "YES" : "NO",
                        column.getDefaultValue() != null ? column.getDefaultValue() : "-"
                ));
            });

            result.append("\n").append("Total: ").append(columns.size()).append(" columns");
            return result.toString();
        } catch (Exception e) {
            return formatError("Describe Failed", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════
    // 🔧 SQL EXECUTION COMMANDS
    // ═══════════════════════════════════════════════════════════════════════════════════════

    @ShellMethod(key = {"sql", "execute"}, value = "⚡ Execute SQL query")
    public String executeSQL(@ShellOption(value = {"-q", "--query"}, help = "SQL query") String sql) {
        try {
            sql = sql.trim();

            if (sql.toUpperCase().startsWith("SELECT")) {
                return executeQuery(sql);
            } else {
                schemaManager.executeUpdate(sql);
                return formatResponse("SQL EXECUTED", "✅ SQL command executed successfully");
            }
        } catch (Exception e) {
            return formatError("SQL Execution Failed", e.getMessage());
        }
    }

    private String executeQuery(String sql) {
        try {
            List<Map<String, Object>> results = schemaManager.executeQuery(sql);

            if (results.isEmpty()) {
                return formatWarning("No Results", "Query returned no results");
            }

            StringBuilder result = new StringBuilder();
            result.append("📊 QUERY RESULTS\n");
            result.append("═".repeat(80)).append("\n");

            // Get column names
            Set<String> columnNames = results.get(0).keySet();

            // Header
            columnNames.forEach(col -> result.append(String.format("%-20s | ", col)));
            result.append("\n");
            result.append("─".repeat(80)).append("\n");

            // Rows
            results.forEach(row -> {
                columnNames.forEach(col -> {
                    Object value = row.get(col);
                    result.append(String.format("%-20s | ", value != null ? value.toString() : "NULL"));
                });
                result.append("\n");
            });

            result.append("\n").append("Total: ").append(results.size()).append(" rows");
            return result.toString();
        } catch (Exception e) {
            return formatError("Query Failed", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════
    // 🎨 FORMATTING UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════════════════

    private String formatResponse(String title, String content) {
        return String.format("✅ %s\n%s\n%s",
                title,
                "─".repeat(Math.max(title.length(), 40)),
                content);
    }

    private String formatError(String title, String message) {
        return String.format("❌ %s\n%s\n🔥 %s",
                title,
                "─".repeat(Math.max(title.length(), 40)),
                message);
    }

    private String formatWarning(String title, String message) {
        return String.format("⚠️  %s\n%s\n💡 %s",
                title,
                "─".repeat(Math.max(title.length(), 40)),
                message);
    }
}
