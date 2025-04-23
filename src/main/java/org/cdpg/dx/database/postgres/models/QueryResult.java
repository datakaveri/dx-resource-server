package org.cdpg.dx.database.postgres.models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class QueryResult {
  private JsonArray rows;
  private int totalCount;
  private boolean hasMore;
  private boolean rowsAffected;

  public QueryResult(QueryResult other){
    this.rows = other.getRows();
    this.totalCount = other.getTotalCount();
    this.hasMore = other.isHasMore();
    this.rowsAffected = other.isRowsAffected();
  }

  public QueryResult(){}

  // JSON Constructor
  public QueryResult(JsonObject json) {
    QueryResultConverter.fromJson(json, this); // Use the generated converter
  }

  // Convert to JSON
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    QueryResultConverter.toJson(this, json);
    return json;
  }

  // Getters & Setters
  public JsonArray getRows() {
    return rows;
  }

  public void setRows(JsonArray rows) {
    this.rows = rows;
  }

  public int getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(int totalCount) {
    this.totalCount = totalCount;
  }

  public boolean isHasMore() {
    return hasMore;
  }

  public void setHasMore(boolean hasMore) {
    this.hasMore = hasMore;
  }

  public boolean isRowsAffected() {
    return rowsAffected;
  }

  public void setRowsAffected(boolean rowsAffected) {
    this.rowsAffected = rowsAffected;
  }
}
