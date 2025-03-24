package org.cdpg.dx.database.postgres.models;

import java.util.List;
import java.util.stream.Collectors;

public record SelectQuery(String table, List<String> columns, ConditionComponent condition,
                          List<String> groupBy, List<OrderBy> orderBy, Integer limit, Integer offset) implements Query {

    @Override
    public String toSQL() {
        String columnNames = String.join(", ", columns);
        StringBuilder query = new StringBuilder("SELECT " + columnNames + " FROM " + table);

        if (condition != null) {
            query.append(" WHERE ").append(condition.toSQL());
        }

        if (groupBy != null && !groupBy.isEmpty()) {
            query.append(" GROUP BY ").append(String.join(", ", groupBy));
        }

        if (orderBy != null && !orderBy.isEmpty()) {
            query.append(" ORDER BY ").append(orderBy.stream()
                    .map(OrderBy::toSQL)
                    .collect(Collectors.joining(", ")));
        }

        if (limit != null) {
            query.append(" LIMIT ").append(limit);
        }

        if (offset != null) {
            query.append(" OFFSET ").append(offset);
        }

        return query.toString();
    }

    @Override
    public List<Object> getQueryParams() {
        return condition != null ? condition.getQueryParams() : List.of();
    }
}
