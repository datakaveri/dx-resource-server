package iudx.resource.server.database.async.util;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen
public class AsyncStatusServiceResult {
  private String status;
  private int statusCode;
  private String title;
  private String message;
  private AsyncStatusQueryResult result;

  public AsyncStatusServiceResult() {}

  public AsyncStatusServiceResult(JsonObject json) {
    AsyncStatusServiceResultConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    AsyncStatusServiceResultConverter.toJson(this, json);
    return json;
  }

  public String getStatus() {
    return status;
  }

  public AsyncStatusServiceResult setStatus(String status) {
    this.status = status;
    return this;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public AsyncStatusServiceResult setStatusCode(int statusCode) {
    this.statusCode = statusCode;
    return this;
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