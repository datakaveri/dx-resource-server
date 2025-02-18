package iudx.resource.server.database.postgres.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@DataObject
public class PostgresResultModel {
  private String type;
  private String title;
  private JsonArray result;

  public PostgresResultModel() {}

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public JsonArray getResult() {
    return result;
  }

  public void setResult(JsonArray result) {
    this.result = result;
  }

  public PostgresResultModel(String type, String title, JsonArray result) {
    this.type = type;
    this.title = title;
    this.result = result;
  }

  public PostgresResultModel(JsonObject json) {
    this.type = json.getString("type");
    this.title = json.getString("title");
    this.result = json.getJsonArray("result");
  }

  public JsonObject toJson() {
    return new JsonObject()
            .put("type", type)
            .put("title", title)
            .put("result", result);
  }
}
