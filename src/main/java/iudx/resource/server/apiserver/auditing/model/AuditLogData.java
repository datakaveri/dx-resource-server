package iudx.resource.server.apiserver.auditing.model;

import io.vertx.core.json.JsonObject;

public class AuditLogData {
  private String primaryKey;
  private String userid;
  private String id;
  private String api;
  private long responseSize;
  private long epochTime;
  private String isoTime;
  private String delegatorId;
  private String origin;

  public String getApi() {
    return api;
  }

  public void setApi(String api) {
    this.api = api;
  }

  // Getters and Setters
  public void setEpochTime(long epochTime) {
    this.epochTime = epochTime;
  }

  public void setIsoTime(String isoTime) {
    this.isoTime = isoTime;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setResponseSize(long responseSize) {
    this.responseSize = responseSize;
  }

  public void setPrimaryKey(String primaryKey) {
    this.primaryKey = primaryKey;
  }

  public void setOrigin(String origin) {
    this.origin = origin;
  }

  public void setDelegatorId(String delegatorId) {
    this.delegatorId = delegatorId;
  }

  public String getUserid() {
    return userid;
  }

  public void setUserid(String userid) {
    this.userid = userid;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();

    // Insert fields in the required order
    json.put("primaryKey", primaryKey);
    json.put("userid", userid);
    json.put("id", id);
    json.put("api", api);
    json.put("responseSize", responseSize);
    json.put("epochTime", epochTime);
    json.put("isoTime", isoTime);
    json.put("delegatorId", delegatorId);
    json.put("origin", origin);

    return json;
  }
}
