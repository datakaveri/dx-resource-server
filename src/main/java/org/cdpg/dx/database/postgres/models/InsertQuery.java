package org.cdpg.dx.database.postgres.models;

import java.util.List;

public record InsertQuery(String table, List<String> columns, List<Object> values) implements Query {
    @Override
    public String toSQL() {
        String placeholders = "?,".repeat(columns.size()).replaceAll(",$", "");
        return "INSERT INTO " + table + " (" + String.join(", ", columns) + ") VALUES (" + placeholders + ")";
    }

    @Override
    public List<Object> getQueryParams() {
        return values;
    }
}
