package iudx.resource.server.apiserver.auditing.model;

import io.vertx.core.json.JsonObject;

public final class OverviewRequest {
  private final String startTime;
  private final String endTime;
  private final String role;
  private final String iid;
  private final String userId;
  private final String providerId; // Renamed from iid

  private OverviewRequest(
      String startTime, String endTime, String role, String iid, String userId, String providerId) {
    this.startTime = startTime;
    this.endTime = endTime;
    this.role = role;
    this.iid = iid;
    this.userId = userId;
    this.providerId = providerId;
  }

  public static OverviewRequest fromJson(JsonObject json) {
    return new OverviewRequest(
        json.getString("startTime"),
        json.getString("endTime"),
        json.getString("role"),
        json.getString("iid"),
        json.getString("userid"),
        json.getString("providerId"));
  }

  public String getStartTime() {
    return startTime;
  }

  public String getEndTime() {
    return endTime;
  }

  public String getRole() {
    return role;
  }

  public String getUserId() {
    return userId;
  }

  public String getIid() {
    return iid;
  }

  public JsonObject toJson() {
    return new JsonObject()
        .put("startTime", startTime)
        .put("endTime", endTime)
        .put("role", role)
        .put("iid", iid)
        .put("userId", userId)
        .put("providerId", providerId);
  }

  public String getProviderId() {
    return providerId;
  }

  public OverviewRequest withUpdatedProviderId(String newProviderId) {
    return new OverviewRequest(
        this.startTime, this.endTime, this.role, this.iid, this.userId, newProviderId);
  }
}
