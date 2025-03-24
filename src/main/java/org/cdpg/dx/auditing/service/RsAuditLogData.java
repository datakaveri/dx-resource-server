package org.cdpg.dx.auditing.service;

import io.vertx.core.json.JsonObject;

public class RsAuditLogData implements AuditLog {
  private String primaryKey;
  private String userid;
  private String id;
  private String api;
  private long responseSize;
  private long epochTime;
  private String isoTime;
  private String delegatorId;
  private String origin;

  public String getOrigin() {
    return origin;
  }

  public void setOrigin(String origin) {
    this.origin = origin;
  }

  public String getDelegatorId() {
    return delegatorId;
  }

  public void setDelegatorId(String delegatorId) {
    this.delegatorId = delegatorId;
  }

  public String getIsoTime() {
    return isoTime;
  }

  public void setIsoTime(String isoTime) {
    this.isoTime = isoTime;
  }

  public long getEpochTime() {
    return epochTime;
  }

  public void setEpochTime(long epochTime) {
    this.epochTime = epochTime;
  }

  public long getResponseSize() {
    return responseSize;
  }

  public void setResponseSize(long responseSize) {
    this.responseSize = responseSize;
  }

  public String getApi() {
    return api;
  }

  public void setApi(String api) {
    this.api = api;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUserid() {
    return userid;
  }

  public void setUserid(String userid) {
    this.userid = userid;
  }

  public String getPrimaryKey() {
    return primaryKey;
  }

  public void setPrimaryKey(String primaryKey) {
    this.primaryKey = primaryKey;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
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
