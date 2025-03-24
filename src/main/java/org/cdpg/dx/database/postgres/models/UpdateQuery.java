package org.cdpg.dx.database.postgres.models;

import java.util.List;
import java.util.stream.Collectors;

public record UpdateQuery(String table, List<String> columns, List<Object> values,
                          ConditionComponent condition, List<OrderBy> orderBy, Integer limit) implements Query {

    @Override
    public String toSQL() {
        String setClause = columns.stream()
                .map(column -> column + " = ?")
                .collect(Collectors.joining(", "));

        StringBuilder query = new StringBuilder("UPDATE " + table + " SET " + setClause);

        if (condition != null) {
            query.append(" WHERE ").append(condition.toSQL());
        }

        if (orderBy != null && !orderBy.isEmpty()) {
            query.append(" ORDER BY ").append(orderBy.stream()
                    .map(OrderBy::toSQL)
                    .collect(Collectors.joining(", ")));
        }

        if (limit != null) {
            query.append(" LIMIT ").append(limit);
        }

        return query.toString();
    }

    @Override
    public List<Object> getQueryParams() {
        List<Object> params = values.stream().toList();
        if (condition != null) {
            params.addAll(condition.getQueryParams());
        }
        return params;
    }
}
