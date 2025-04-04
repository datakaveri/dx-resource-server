package org.cdpg.dx.auditing.model;
import io.vertx.core.json.JsonObject;

public class RsAuditLog implements AuditLog {
  private String primaryKey;
  private String userid;
  private String id;
  private String api;
  private long responseSize;
  private long epochTime;
  private String isoTime;
  private String delegatorId;
  private String origin;

  public RsAuditLog(String primaryKey, String userid, String id, String api, long responseSize, long epochTime, String isoTime, String delegatorId, String origin) {
    this.primaryKey = primaryKey;
    this.userid = userid;
    this.id = id;
    this.api = api;
    this.responseSize = responseSize;
    this.epochTime = epochTime;
    this.isoTime = isoTime;
    this.delegatorId = delegatorId;
    this.origin = origin;
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
