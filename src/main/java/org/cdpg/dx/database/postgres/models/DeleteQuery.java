package org.cdpg.dx.database.postgres.models;

import java.util.List;
import java.util.stream.Collectors;

public record DeleteQuery(String table, ConditionComponent condition, List<OrderBy> orderBy, Integer limit) implements Query {

    @Override
    public String toSQL() {
        StringBuilder query = new StringBuilder("DELETE FROM " + table);

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
        return condition != null ? condition.getQueryParams() : List.of();
    }
}
