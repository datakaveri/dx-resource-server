package org.cdpg.dx.database.postgres.models;

import java.util.List;
import java.util.stream.Collectors;

public record ConditionGroup(List<ConditionComponent> conditions, LogicalOperator operator) implements ConditionComponent {

    public enum LogicalOperator {
        AND("AND"), OR("OR");

        private final String symbol;
        LogicalOperator(String symbol) {
            this.symbol = symbol;
        }
        public String getSymbol() {
            return symbol;
        }
    }

    @Override
    public String toSQL() {
        return conditions.stream()
                .map(ConditionComponent::toSQL)
                .map(sql -> "(" + sql + ")")  // Ensure correct precedence
                .collect(Collectors.joining(" " + operator.getSymbol() + " "));
    }

    @Override
    public List<Object> getQueryParams() {
        return conditions.stream()
                .flatMap(condition -> condition.getQueryParams().stream())
                .toList();
    }
}
