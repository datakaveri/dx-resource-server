package org.cdpg.dx.database.postgres.models;

import java.util.List;
import java.util.stream.Collectors;

public record Condition(String column, Operator operator, List<Object> values) implements ConditionComponent {

    public enum Operator {
        EQUALS("="), NOT_EQUALS("!="), GREATER(">"), LESS("<"), GREATER_EQUALS(">="), LESS_EQUALS("<="),
        LIKE("LIKE"), IN("IN"), NOT_IN("NOT IN"), BETWEEN("BETWEEN"),
        IS_NULL("IS NULL"), IS_NOT_NULL("IS NOT NULL");

        private final String symbol;
        Operator(String symbol) {
            this.symbol = symbol;
        }
        public String getSymbol() {
            return symbol;
        }
    }

    @Override
    public String toSQL() {
        return switch (operator) {
            case EQUALS, NOT_EQUALS, GREATER, LESS, GREATER_EQUALS, LESS_EQUALS, LIKE ->
                    column + " " + operator.getSymbol() + " ?";
            case IN, NOT_IN ->
                    column + " " + operator.getSymbol() + " (" + values.stream().map(v -> "?").collect(Collectors.joining(", ")) + ")";
            case BETWEEN ->
                    column + " BETWEEN ? AND ?";
            case IS_NULL, IS_NOT_NULL ->
                    column + " " + operator.getSymbol();
        };
    }

    @Override
    public List<Object> getQueryParams() {
        return values;
    }
}
