package com.example.nativecliapp.config;

import com.example.nativecliapp.dtos.ColumnDefinition;
import com.example.nativecliapp.dtos.ColumnInfo;
import com.example.nativecliapp.dtos.TableInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class SchemaManager {

    private final DatabaseConnectionManager connectionManager;

    public SchemaManager(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public List<String> listSchemas() {
        try (Connection conn = connectionManager.getCurrentDataSource().getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            List<String> schemas = new ArrayList<>();

            try (ResultSet rs = metaData.getSchemas()) {
                while (rs.next()) {
                    schemas.add(rs.getString("TABLE_SCHEM"));
                }
            }
            return schemas;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list schemas", e);
        }
    }

    public List<TableInfo> listTables(String schema) {
        try (Connection conn = connectionManager.getCurrentDataSource().getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            List<TableInfo> tables = new ArrayList<>();

            try (ResultSet rs = metaData.getTables(null, schema, null, new String[]{"TABLE"})) {
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
            return tables;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list tables", e);
        }
    }

    public List<ColumnInfo> describeTable(String schema, String tableName) {
        try (Connection conn = connectionManager.getCurrentDataSource().getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            List<ColumnInfo> columns = new ArrayList<>();

            try (ResultSet rs = metaData.getColumns(null, schema, tableName, null)) {
                while (rs.next()) {
                    ColumnInfo column = ColumnInfo.builder()
                            .name(rs.getString("COLUMN_NAME"))
                            .type(rs.getString("TYPE_NAME"))
                            .size(rs.getInt("COLUMN_SIZE"))
                            .nullable(rs.getBoolean("NULLABLE"))
                            .defaultValue(rs.getString("COLUMN_DEF"))
                            .position(rs.getInt("ORDINAL_POSITION"))
                            .build();
                    columns.add(column);
                }
            }
            return columns;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to describe table", e);
        }
    }

    public void createTable(String schema, String tableName, List<ColumnDefinition> columns) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ");
        if (schema != null && !schema.isEmpty()) {
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
                sql.append(" NOT NULL");
            }
            if (col.getDefaultValue() != null) {
                sql.append(" DEFAULT ").append(col.getDefaultValue());
            }
            if (col.isPrimaryKey()) {
                sql.append(" PRIMARY KEY");
            }
        }
        sql.append(")");

        executeUpdate(sql.toString());
    }

    public void executeUpdate(String sql) {
        try (Connection conn = connectionManager.getCurrentDataSource().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            log.info("✅ SQL executed successfully: {}", sql);
        } catch (SQLException e) {
            log.error("❌ SQL execution failed: {}", e.getMessage());
            throw new RuntimeException("SQL execution failed", e);
        }
    }

    public List<Map<String, Object>> executeQuery(String sql) {
        try (Connection conn = connectionManager.getCurrentDataSource().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

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

            return results;
        } catch (SQLException e) {
            log.error("❌ SQL query failed: {}", e.getMessage());
            throw new RuntimeException("SQL query failed", e);
        }
    }
}
