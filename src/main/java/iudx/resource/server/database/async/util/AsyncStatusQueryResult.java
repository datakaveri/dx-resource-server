package iudx.resource.server.database.async.util;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen
public class AsyncStatusQueryResult {
  private String userId;
  private String status;
  private String fileDownloadUrl;
  private String searchId;

  public AsyncStatusQueryResult() {}

  public AsyncStatusQueryResult(JsonObject json) {
    AsyncStatusQueryResultConverter.fromJson(json, this);
    setSearchId(json.getString("search_id"));
    setFileDownloadUrl(json.getString("s3_url"));
    setUserId(json.getString("user_id"));
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    AsyncStatusQueryResultConverter.toJson(this, json);
    return json;
  }

  public String getUserId() {
    return userId;
  }

  public AsyncStatusQueryResult setUserId(String userId) {
    this.userId = userId;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public AsyncStatusQueryResult setStatus(String status) {
    this.status = status;
    return this;
  }

  public String getFileDownloadUrl() {
    return fileDownloadUrl;
  }

  public AsyncStatusQueryResult setFileDownloadUrl(String fileDownloadUrl) {
    this.fileDownloadUrl = fileDownloadUrl;
    return this;
  }

  public String getSearchId() {
    return searchId;
  }

  public AsyncStatusQueryResult setSearchId(String searchId) {
    this.searchId = searchId;
    return this;
  }
}
