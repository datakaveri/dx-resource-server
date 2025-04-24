package org.cdpg.dx.database.postgres.models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.cdpg.dx.database.postgres.models.Join;

@DataObject(generateConverter = true)
public class SelectQuery implements Query {
    private String table;
    private String tableAlias;  // New field for table alias
    private List<String> columns;
    private Condition condition;
    private List<String> groupBy;
    private List<OrderBy> orderBy;
    private Integer limit;
    private Integer offset;
    private List<Join> joins; // List to store joins

    // Default constructor
    public SelectQuery() {}

    // Constructor including table alias and joins
    public SelectQuery(String table, String tableAlias, List<String> columns, List<Join> joins, Condition condition, List<String> groupBy,
                       List<OrderBy> orderBy, Integer limit, Integer offset) {
        this.table = table;
        this.tableAlias = tableAlias;
        this.columns = columns;
        this.condition = condition;
        this.groupBy = groupBy;
        this.orderBy = orderBy;
        this.limit = limit;
        this.offset = offset;
        this.joins = joins != null ? joins : new ArrayList<>();
    }

    public SelectQuery(String table, List<String> columns, Condition condition, List<String> groupBy,
                       List<OrderBy> orderBy, Integer limit, Integer offset) {
        this.table = table;
        this.tableAlias = null;
        this.columns = columns;
        this.condition = condition;
        this.groupBy = groupBy;
        this.orderBy = orderBy;
        this.limit = limit;
        this.offset = offset;
        this.joins = null;
    }

    // Copy constructor
    public SelectQuery(SelectQuery other){
        this.table = other.getTable();
        this.tableAlias = other.getTableAlias();
        this.columns = other.getColumns();
        this.condition = other.getCondition();
        this.groupBy = other.getGroupBy();
        this.orderBy = other.getOrderBy();
        this.limit = other.getLimit();
        this.offset = other.getOffset();
        this.joins = other.getJoins();
    }

    // JSON constructor
    public SelectQuery(JsonObject json) {
        SelectQueryConverter.fromJson(json, this); // Use the generated converter
    }

    // Convert to JSON
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        SelectQueryConverter.toJson(this, json);
        return json;
    }

    // Getters and setters
    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getTableAlias() {
        return tableAlias;
    }

    public void setTableAlias(String tableAlias) {
        this.tableAlias = tableAlias;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public List<String> getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(List<String> groupBy) {
        this.groupBy = groupBy;
    }

    public List<OrderBy> getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(List<OrderBy> orderBy) {
        this.orderBy = orderBy;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public List<Join> getJoins() {
        return joins;
    }

    public void setJoins(List<Join> joins) {
        this.joins = joins;
    }

    private List<Object> queryParams = new ArrayList<>();

    @Override
    public String toSQL() {
        queryParams.clear(); // clear old params
        String columnNames = String.join(", ", columns);
        StringBuilder query = new StringBuilder("SELECT " + columnNames + " FROM ");

        // Table with alias
        String baseTable = tableAlias != null && !tableAlias.isEmpty()
                ? table + " " + tableAlias
                : table;
        query.append(baseTable);

        // Adding joins
        if (joins != null && !joins.isEmpty()) {
            for (Join join : joins) {
                query.append(" ").append(join.toSQL());
            }
        }

        // Adding condition (WHERE clause)
        if (condition != null) {
            query.append(" WHERE ").append(condition.toSQL(queryParams));
        }

        // Adding GROUP BY clause
        if (groupBy != null && !groupBy.isEmpty()) {
            query.append(" GROUP BY ").append(String.join(", ", groupBy));
        }

        // Adding ORDER BY clause
        if (orderBy != null && !orderBy.isEmpty()) {
            query.append(" ORDER BY ")
                .append(orderBy.stream().map(OrderBy::toSQL).collect(Collectors.joining(", ")));
        }

        // Adding LIMIT
        if (limit != null) {
            query.append(" LIMIT ").append(limit);
        }

        // Adding OFFSET
        if (offset != null) {
            query.append(" OFFSET ").append(offset);
        }

        return query.toString();
    }

    @Override
    public List<Object> getQueryParams() {
        List<Object> params = new ArrayList<>();
        if (condition != null) {
            params.addAll(condition.getQueryParams());
        }
        return params;
    }
}
