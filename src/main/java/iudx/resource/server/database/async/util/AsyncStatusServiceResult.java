package iudx.resource.server.database.async.util;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen
public class AsyncStatusServiceResult {
  private String title;
  private String message;
  private AsyncStatusQueryResult result;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
  private String type;

  public AsyncStatusServiceResult() {}

  public AsyncStatusServiceResult(JsonObject json) {
    AsyncStatusServiceResultConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    AsyncStatusServiceResultConverter.toJson(this, json);
    return json;
  }

  public String getTitle() {
    return title;
  }

  public AsyncStatusServiceResult setTitle(String title) {
    this.title = title;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public AsyncStatusServiceResult setMessage(String message) {
    this.message = message;
    return this;
  }

  public AsyncStatusQueryResult getResult() {
    return result;
  }

  public AsyncStatusServiceResult setResult(AsyncStatusQueryResult result) {
    this.result = result;
    return this;
  }
}