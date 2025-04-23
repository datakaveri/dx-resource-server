package org.cdpg.dx.database.postgres.models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@DataObject(generateConverter = true)
public class Condition {
  private String column; // Used for single conditions
  private Operator operator; // Used for single conditions
  private List<Object> values; // Used for single conditions
  private List<Condition> conditions; // Used for condition groups
  private LogicalOperator logicalOperator; // Used for condition groups
  private boolean isGroup; // Flag to distinguish between single condition and group

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

  // Default constructor
  public Condition() {
  }

  // Constructor for single condition
  public Condition(String column, Operator operator, List<Object> values) {
    this.column = Objects.requireNonNull(column, "Column cannot be null");
    this.operator = Objects.requireNonNull(operator, "Operator cannot be null");
    this.values = values; // Can be null for IS_NULL, IS_NOT_NULL
    this.isGroup = false;
  }

  // Constructor for condition group
  public Condition(List<Condition> conditions, LogicalOperator logicalOperator) {
    this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
    this.logicalOperator = Objects.requireNonNull(logicalOperator, "Logical operator cannot be null");
    this.isGroup = true;
  }

  // Copy constructor
  public Condition(Condition other) {
    this.isGroup = other.isGroup;
    if (other.isGroup) {
      this.conditions = other.conditions.stream()
        .map(Condition::new)
        .collect(Collectors.toList());
      this.logicalOperator = other.logicalOperator;
    } else {
      this.column = other.column;
      this.operator = other.operator;
      this.values = other.values != null ? new ArrayList<>(other.values) : null;
    }
  }

  // JSON constructor
  public Condition(JsonObject json) {
    this.isGroup = json.getBoolean("isGroup", false);
    if (isGroup) {
      this.logicalOperator = LogicalOperator.valueOf(json.getString("logicalOperator"));
      this.conditions = json.getJsonArray("conditions").stream()
        .map(obj -> new Condition((JsonObject) obj))
        .collect(Collectors.toList());
    } else {
      this.column = json.getString("column");
      this.operator = Operator.valueOf(json.getString("operator"));
      this.values = json.containsKey("values") ? json.getJsonArray("values").getList() : null;
    }
  }

  // Convert to JSON
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put("isGroup", isGroup);
    if (isGroup) {
      json.put("logicalOperator", logicalOperator.name());
      JsonArray conditionsArray = new JsonArray(conditions.stream()
        .map(Condition::toJson)
        .collect(Collectors.toList()));
      json.put("conditions", conditionsArray);
    } else {
      json.put("column", column);
      json.put("operator", operator.name());
      if (values != null) {
        json.put("values", new JsonArray(values));
      }
    }
    return json;
  }

  // Generate SQL string
  public String toSQL() {
    if (isGroup) {
      return conditions.stream()
        .map(Condition::toSQL)
        .map(sql -> "(" + sql + ")") // Ensure correct precedence
        .collect(Collectors.joining(" " + logicalOperator.getSymbol() + " "));
    } else {
      return switch (operator) {
        case EQUALS, NOT_EQUALS, GREATER, LESS, GREATER_EQUALS, LESS_EQUALS, LIKE ->
          column + " " + operator.getSymbol() + " $1";
        case IN, NOT_IN ->
          column + " " + operator.getSymbol() + " (" +
            values.stream().map(v -> "?").collect(Collectors.joining(", ")) + ")";
        case BETWEEN ->
          column + " BETWEEN ? AND ?";
        case IS_NULL, IS_NOT_NULL ->
          column + " " + operator.getSymbol();
      };
    }
  }

  // Get query parameters
  public List<Object> getQueryParams() {
    if (isGroup) {
      return conditions.stream()
        .flatMap(condition -> condition.getQueryParams().stream())
        .collect(Collectors.toList());
    } else {
      return values == null ? List.of() : values;
    }
  }

  // Getters and setters
  public String getColumn() {
    return column;
  }

  public void setColumn(String column) {
    this.column = column;
  }

  public Operator getOperator() {
    return operator;
  }

  public void setOperator(Operator operator) {
    this.operator = operator;
  }

  public List<Object> getValues() {
    return values;
  }

  public void setValues(List<Object> values) {
    this.values = values;
  }

  public List<Condition> getConditions() {
    return conditions;
  }

  public void setConditions(List<Condition> conditions) {
    this.conditions = conditions;
  }

  public LogicalOperator getLogicalOperator() {
    return logicalOperator;
  }

  public void setLogicalOperator(LogicalOperator logicalOperator) {
    this.logicalOperator = logicalOperator;
  }

  public boolean isGroup() {
    return isGroup;
  }

  public void setGroup(boolean group) {
    isGroup = group;
  }
}
