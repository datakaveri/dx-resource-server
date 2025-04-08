package org.cdpg.dx.database.elastic.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class ElasticsearchResponse {
  private static JsonObject aggregations;
  private String docId;
  private JsonObject source;

  public ElasticsearchResponse() {
    // Default constructor
  }

  public ElasticsearchResponse(JsonObject json) {
    ElasticsearchResponseConverter.fromJson(json, this);
  }

  public ElasticsearchResponse(String docId, JsonObject source) {
    this.docId = docId;
    this.source = source;
  }

  public static JsonObject getAggregations() {
    return aggregations;
  }

  public static void setAggregations(JsonObject aggregations) {
    ElasticsearchResponse.aggregations = aggregations;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ElasticsearchResponseConverter.toJson(this, json);
    return json;
  }

  public String getDocId() {
    return docId;
  }

  public void setDocId(String docId) {
    this.docId = docId;
  }

  public JsonObject getSource() {
    return source;
  }

  public void setSource(JsonObject source) {
    this.source = source;
  }

  @Override
  public String toString() {
    return "ElasticsearchResponse{" + "docId='" + docId + '\'' + ", source=" + source + '}';
  }
}
