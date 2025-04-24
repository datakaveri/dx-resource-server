package org.cdpg.dx.database.postgres.models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DataObject(generateConverter = true)
@JsonGen(inheritConverter = true, publicConverter = false)
public class DeleteQuery implements Query {
    private static final Logger LOG = LoggerFactory.getLogger(DeleteQuery.class);
    private  String table;
    private  Condition condition;
    private  List<OrderBy> orderBy;
    private  Integer limit; // Optional, so keep as Integer

    public DeleteQuery(){}
    // Constructor (OrderBy & Limit are optional)
    public DeleteQuery(String table, Condition condition, List<OrderBy> orderBy, Integer limit) {
        this.table = Objects.requireNonNull(table, "Table name cannot be null");
        this.condition = condition;
        this.orderBy = orderBy != null ? List.copyOf(orderBy) : List.of();
        this.limit = limit; // Can be null (optional)
    }

    public DeleteQuery(DeleteQuery other){
        this.condition = other.getCondition();
        this.table = other.getTable();
        this.orderBy = other.getOrderBy();
        this.limit = other.getLimit();

    }

    // JSON Constructor
    public DeleteQuery(JsonObject json) {
//        if(json.getInteger("limit")!= null){
//            this.limit = json.getInteger("limit");
//        }
//        if(json.getString("table") != null){
//            this.table = json.getString("table");
//        }
//        if(json.getValue("condition") != null)
//        {
//
//      this.condition = (Condition) json.getValue("condition");
//        }
//        if(json.getValue("orderBy") != null){
//            this.orderBy = (List<OrderBy>) json.getValue("orderBy");
//        }

        DeleteQueryConverter.fromJson(json, this);
    }

    // Convert to JSON
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        DeleteQueryConverter.toJson(this, json);
        return json;
    }

    public String getTable() { return table; }

    public void setTable(String table) {
        this.table = table;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public List<OrderBy> getOrderBy() { return orderBy; }

    public void setOrderBy(List<OrderBy> orderBy) {
        this.orderBy = orderBy;
    }

    public Integer getLimit() { return limit; }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    @Override
    public String toSQL() {
        StringBuilder query = new StringBuilder("DELETE FROM " + table + " WHERE " + getCondition().toSQL());

        if (!orderBy.isEmpty()) {
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
        return condition.getQueryParams();
    }
}
