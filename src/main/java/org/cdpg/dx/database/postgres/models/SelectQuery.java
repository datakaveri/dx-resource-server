package org.cdpg.dx.database.postgres.models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.stream.Collectors;

@DataObject(generateConverter = true)
public class SelectQuery implements Query {
    private String table;
    private List<String> columns;
    private Condition condition;
    private List<String> groupBy;
    private List<OrderBy> orderBy;
    private Integer limit;
    private Integer offset;

    public SelectQuery() {}

    public SelectQuery(String table, List<String> columns, Condition condition, List<String> groupBy,
                       List<OrderBy> orderBy, Integer limit, Integer offset) {
        this.table = table;
        this.columns = columns;
        this.condition = condition;
        this.groupBy = groupBy;
        this.orderBy = orderBy;
        this.limit = limit;
        this.offset = offset;
    }


    public SelectQuery(SelectQuery other){
        this.table = other.getTable();
        this.columns = other.getColumns();
        this.condition = other.getCondition();
        this.groupBy = other.getGroupBy();
        this.orderBy = other.getOrderBy();
        this.limit = other.getLimit();
        this.offset = other.getOffset();
    }

    public SelectQuery(JsonObject json) {
        SelectQueryConverter.fromJson(json, this); // Use the generated converter
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        SelectQueryConverter.toJson(this, json);
        return json;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
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

    @Override
    public String toSQL() {
        String columnNames = String.join(", ", columns);
        StringBuilder query = new StringBuilder("SELECT " + columnNames + " FROM " + table);

        if (condition != null) {
            query.append(" WHERE ").append(condition.toSQL());
        }
        else
        {
          System.out.println("Condition is null inside selectQuery");
        }

        if (groupBy != null && !groupBy.isEmpty()) {
            query.append(" GROUP BY ").append(String.join(", ", groupBy));
        }

        if (orderBy != null && !orderBy.isEmpty()) {
            query
                    .append(" ORDER BY ")
                    .append(orderBy.stream().map(OrderBy::toSQL).collect(Collectors.joining(", ")));
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
