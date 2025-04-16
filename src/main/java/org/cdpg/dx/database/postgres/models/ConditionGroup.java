package org.cdpg.dx.database.postgres.models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@DataObject(generateConverter = true)
public class ConditionGroup implements ConditionComponent {
    private  List<ConditionComponent> conditions;
    private  LogicalOperator operator;

    public ConditionGroup(ConditionGroup other) {
        this.conditions = other.getConditions();
        this.operator = other.getOperator();
    }

    public ConditionGroup(List<ConditionComponent> conditions, LogicalOperator operator) {
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
        this.operator = Objects.requireNonNull(operator, "Operator cannot be null");
    }


    public ConditionGroup(JsonObject json) {
        ConditionGroupConverter.fromJson(json, this);
        this.conditions = json.getJsonArray("conditions").stream()
                .map(obj -> (ConditionComponent) new Condition((JsonObject) obj))
                .collect(Collectors.toList());
        this.operator = LogicalOperator.valueOf(json.getString("operator"));
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        ConditionGroupConverter.toJson(this, json);
        return json;
    }

    @Override
    public String toSQL() {
        return conditions.stream()
                .map(ConditionComponent::toSQL)
                .map(sql -> "(" + sql + ")")  // Ensure correct precedence
                .collect(Collectors.joining(" " + operator.getSymbol() + " "));
    }

    public List<ConditionComponent> getConditions() {
        return conditions;
    }

    public void setConditions(List<ConditionComponent> conditions) {
        this.conditions = conditions;
    }

    public LogicalOperator getOperator() {
        return operator;
    }

    public void setOperator(LogicalOperator operator) {
        this.operator = operator;
    }

    @Override
    public List<Object> getQueryParams() {
        return conditions.stream()
                .flatMap(condition -> condition.getQueryParams().stream())
                .toList();
    }

    public enum LogicalOperator {
        AND("AND"), OR("OR");

        private final String symbol;
        LogicalOperator(String symbol) { this.symbol = symbol; }
        public String getSymbol() { return symbol; }
    }
}