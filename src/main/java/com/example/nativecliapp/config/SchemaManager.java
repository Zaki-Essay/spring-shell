package com.example.nativecliapp.config;

import com.example.nativecliapp.constant.DatabaseConstants;
import com.example.nativecliapp.dtos.ColumnDefinition;
import com.example.nativecliapp.dtos.ColumnInfo;
import com.example.nativecliapp.dtos.TableInfo;
import com.example.nativecliapp.exception.SchemaException;
import com.example.nativecliapp.exception.SqlExecutionException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class SchemaManager {

    private final DatabaseConnectionManager connectionManager;
    private final MeterRegistry meterRegistry;

    /**
     * Lists all schemas with enhanced error handling
     */
    public List<String> listSchemas() {
        Timer.Sample sample = Timer.start(meterRegistry);

        try (Connection conn = connectionManager.getCurrentDataSource().getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            List<String> schemas = new ArrayList<>();

            try (ResultSet rs = metaData.getSchemas()) {
                while (rs.next()) {
                    String schemaName = rs.getString("TABLE_SCHEM");
                    if (schemaName != null) {
                        schemas.add(schemaName);
                    }
                }
            }

            log.debug("Found {} schemas", schemas.size());
            return schemas;

        } catch (SQLException e) {
            throw new SchemaException("Failed to list schemas: " + e.getMessage(), e);
        } finally {
            sample.stop(Timer.builder("database.schema.list.duration").register(meterRegistry));
        }
    }

    /**
     * Lists tables in a schema with validation
     */
    public List<TableInfo> listTables(String schema) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try (Connection conn = connectionManager.getCurrentDataSource().getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            List<TableInfo> tables = new ArrayList<>();

            try (ResultSet rs = metaData.getTables(null, schema, null, DatabaseConstants.TABLE_TYPES)) {
                while (rs.next()) {
                    TableInfo table = TableInfo.builder()
                            .name(rs.getString("TABLE_NAME"))
                            .schema(rs.getString("TABLE_SCHEM"))
                            .type(rs.getString("TABLE_TYPE"))
                            .remarks(rs.getString("REMARKS"))
                            .build();
                    tables.add(table);
                }
            }

            log.debug("Found {} tables in schema '{}'", tables.size(), schema);
            return tables;

        } catch (SQLException e) {
            throw new SchemaException("Failed to list tables for schema '" + schema + "': " + e.getMessage(), e);
        } finally {
            sample.stop(Timer.builder("database.schema.tables.duration").register(meterRegistry));
        }
    }

    /**
     * Describes table structure with enhanced column information
     */
    public List<ColumnInfo> describeTable(String schema, String tableName) {
        Objects.requireNonNull(tableName, "Table name cannot be null");

        Timer.Sample sample = Timer.start(meterRegistry);

        try (Connection conn = connectionManager.getCurrentDataSource().getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            List<ColumnInfo> columns = new ArrayList<>();

            try (ResultSet rs = metaData.getColumns(null, schema, tableName, null)) {
                while (rs.next()) {
                    ColumnInfo column = ColumnInfo.builder()
                            .name(rs.getString("COLUMN_NAME"))
                            .type(rs.getString("TYPE_NAME"))
                            .size(rs.getInt("COLUMN_SIZE"))
                            .decimalDigits(rs.getInt("DECIMAL_DIGITS"))
                            .nullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable)
                            .defaultValue(rs.getString("COLUMN_DEF"))
                            .position(rs.getInt("ORDINAL_POSITION"))
                            .remarks(rs.getString("REMARKS"))
                            .build();
                    columns.add(column);
                }
            }

            if (columns.isEmpty()) {
                throw new SchemaException("Table '" + tableName + "' not found in schema '" + schema + "'");
            }

            log.debug("Found {} columns in table '{}.{}'", columns.size(), schema, tableName);
            return columns;

        } catch (SQLException e) {
            throw new SchemaException("Failed to describe table '" + tableName + "': " + e.getMessage(), e);
        } finally {
            sample.stop(Timer.builder("database.schema.describe.duration").register(meterRegistry));
        }
    }

    /**
     * Creates a table with enhanced validation
     */
    public void createTable(String schema, String tableName, List<ColumnDefinition> columns) {
        Objects.requireNonNull(tableName, "Table name cannot be null");
        Objects.requireNonNull(columns, "Columns cannot be null");

        if (columns.isEmpty()) {
            throw new IllegalArgumentException("At least one column must be specified");
        }

        // Validate column definitions
        validateColumnDefinitions(columns);

        String sql = buildCreateTableSql(schema, tableName, columns);
        executeUpdate(sql);

        log.info("✅ Table '{}.{}' created successfully with {} columns", schema, tableName, columns.size());
    }

    private void validateColumnDefinitions(List<ColumnDefinition> columns) {
        Set<String> columnNames = new HashSet<>();

        for (ColumnDefinition column : columns) {
            if (column.getName() == null || column.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Column name cannot be null or empty");
            }

            if (column.getType() == null || column.getType().trim().isEmpty()) {
                throw new IllegalArgumentException("Column type cannot be null or empty");
            }

            if (!columnNames.add(column.getName().toLowerCase())) {
                throw new IllegalArgumentException("Duplicate column name: " + column.getName());
            }
        }
    }

    private String buildCreateTableSql(String schema, String tableName, List<ColumnDefinition> columns) {
        StringBuilder sql = new StringBuilder(DatabaseConstants.SQL_CREATE_TABLE);
        sql.append(" ");

        if (schema != null && !schema.trim().isEmpty()) {
            sql.append(schema).append(".");
        }
        sql.append(tableName).append(" (");

        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");

            ColumnDefinition col = columns.get(i);
            sql.append(col.getName()).append(" ").append(col.getType());

            if (col.getSize() > 0) {
                sql.append("(").append(col.getSize()).append(")");
            }

            if (!col.isNullable()) {
                sql.append(" ").append(DatabaseConstants.SQL_NOT_NULL);
            }

            if (col.getDefaultValue() != null) {
                sql.append(" ").append(DatabaseConstants.SQL_DEFAULT).append(" ").append(col.getDefaultValue());
            }

            if (col.isPrimaryKey()) {
                sql.append(" ").append(DatabaseConstants.SQL_PRIMARY_KEY);
            }
        }

        sql.append(")");
        return sql.toString();
    }

    /**
     * Executes SQL update with enhanced logging and error handling
     */
    public void executeUpdate(String sql) {
        Objects.requireNonNull(sql, "SQL cannot be null");

        String trimmedSql = sql.trim();
        if (trimmedSql.isEmpty()) {
            throw new IllegalArgumentException("SQL cannot be empty");
        }

        Timer.Sample sample = Timer.start(meterRegistry);

        try (Connection conn = connectionManager.getCurrentDataSource().getConnection();
             Statement stmt = conn.createStatement()) {

            int rowsAffected = stmt.executeUpdate(trimmedSql);
            log.info("✅ SQL executed successfully. Rows affected: {} | SQL: {}", rowsAffected,
                    trimmedSql.length() > 100 ? trimmedSql.substring(0, 100) + "..." : trimmedSql);

        } catch (SQLException e) {
            log.error("❌ SQL execution failed: {} | SQL: {}", e.getMessage(), trimmedSql);
            throw new SqlExecutionException(trimmedSql, "SQL execution failed: " + e.getMessage(), e);
        } finally {
            sample.stop(Timer.builder("database.sql.update.duration").register(meterRegistry));
        }
    }

    /**
     * Executes SQL query with enhanced result handling
     */
    public List<Map<String, Object>> executeQuery(String sql) {
        Objects.requireNonNull(sql, "SQL cannot be null");

        String trimmedSql = sql.trim();
        if (trimmedSql.isEmpty()) {
            throw new IllegalArgumentException("SQL cannot be empty");
        }

        Timer.Sample sample = Timer.start(meterRegistry);

        try (Connection conn = connectionManager.getCurrentDataSource().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(trimmedSql)) {

            List<Map<String, Object>> results = new ArrayList<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                results.add(row);
            }

            log.info("✅ Query executed successfully. Rows returned: {} | SQL: {}", results.size(),
                    trimmedSql.length() > 100 ? trimmedSql.substring(0, 100) + "..." : trimmedSql);

            return results;

        } catch (SQLException e) {
            log.error("❌ SQL query failed: {} | SQL: {}", e.getMessage(), trimmedSql);
            throw new SqlExecutionException(trimmedSql, "SQL query failed: " + e.getMessage(), e);
        } finally {
            sample.stop(Timer.builder("database.sql.query.duration").register(meterRegistry));
        }
    }

    /**
     * Gets table count for a schema
     */
    public int getTableCount(String schema) {
        return listTables(schema).size();
    }

    /**
     * Checks if a table exists
     */
    public boolean tableExists(String schema, String tableName) {
        try {
            describeTable(schema, tableName);
            return true;
        } catch (SchemaException e) {
            return false;
        }
    }
}