package org.cdpg.dx.auditing.model;

import io.vertx.core.json.JsonObject;

public class RsAuditLog implements AuditLog {
  private final String primaryKey;
  private final String userid;
  private final String id;
  private final String api;
  private final long responseSize;
  private final long epochTime;
  private final String isoTime;
  private final String delegatorId;
  private final String origin;

  public RsAuditLog(
      String primaryKey,
      String userid,
      String id,
      String api,
      long responseSize,
      long epochTime,
      String isoTime,
      String delegatorId,
      String origin) {
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
  // TODO: Other values are taken out from auditing server while refactoring like providerId,
  // resourcegroup, events etc...
}
