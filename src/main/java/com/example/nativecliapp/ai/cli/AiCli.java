package com.example.nativecliapp.ai.cli;

import com.example.nativecliapp.config.DatabaseConnectionManager;
import com.example.nativecliapp.config.SchemaManager;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ShellComponent
public class AiCli {

    private final ChatClient chatClient;
    private final SchemaManager schemaManager;
    private final DatabaseConnectionManager connectionManager;

    public AiCli(ChatClient.Builder chatClientBuilder, SchemaManager schemaManager, DatabaseConnectionManager connectionManager) {
        this.chatClient = chatClientBuilder.build();
        this.schemaManager = schemaManager;
        this.connectionManager = connectionManager;
    }

    @ShellMethod(key = "ask", value = "Ask a question in natural language to query the database")
    public String ask(@ShellOption(value = {"--question"}, help = "The natural language question to ask the database") String question) {
        try {
            String schema = connectionManager.getCurrentSchema();
            List<Map<String, Object>> tables = schemaManager.listTables(schema).stream()
                    .map(tableInfo -> {
                        Map<String, Object> tableMap = new HashMap<>();
                        tableMap.put("TABLE_NAME", tableInfo.getName());
                        return tableMap;
                    })
                    .collect(Collectors.toList());

            String tableNames = tables.stream()
                    .map(table -> table.get("TABLE_NAME").toString())
                    .collect(Collectors.joining(", "));

            String prompt = "Given the following database schema with tables: " + tableNames + ". " +
                    "Generate a SQL query to answer the following question: " + question;

            String sqlQuery = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            List<Map<String, Object>> result = schemaManager.executeQuery(sqlQuery);
            return formatResults(result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String formatResults(List<Map<String, Object>> results) {
        if (results == null || results.isEmpty()) {
            return "No results found.";
        }

        StringBuilder sb = new StringBuilder();
        // Header
        results.get(0).keySet().forEach(key -> sb.append(String.format("% -20s", key)));
        sb.append("\n");

        // Data
        for (Map<String, Object> row : results) {
            row.values().forEach(value -> sb.append(String.format("% -20s", value)));
            sb.append("\n");
        }

        return sb.toString();
    }
}
