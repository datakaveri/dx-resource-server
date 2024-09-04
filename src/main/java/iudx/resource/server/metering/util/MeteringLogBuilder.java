package iudx.resource.server.metering.util;

import io.vertx.core.json.JsonObject;

public class MeteringLogBuilder {

  private final long epochTime;
  private final String isoTime;
  private final String userid;
  private final String id;
  private final long responseSize;
  private final String providerId;
  private final String primaryKey;
  private final String origin;
  private final String api;
  private final String resourceGroup;
  private final String delegatorId;
  private final String type;
  private final String event;

  private MeteringLogBuilder(Builder builder) {
    this.epochTime = builder.epoch;
    this.isoTime = builder.isoTime;
    this.userid = builder.userid;
    this.id = builder.id;
    this.responseSize = builder.responseSize;
    this.providerId = builder.providerId;
    this.primaryKey = builder.primaryKey;
    this.origin = builder.origin;
    this.api = builder.api;
    this.resourceGroup = builder.resourceGroup;
    this.delegatorId = builder.delegatorId;
    this.type = builder.type;
    this.event = builder.event;
  }

  public JsonObject toJson() {
    return new JsonObject()
        .put("primaryKey", primaryKey)
        .put("userid", userid)
        .put("id", id)
        .put("resourceGroup", resourceGroup)
        .put("providerID", providerId)
        .put("delegatorId", delegatorId)
        .put("type", type)
        .put("api", api)
        .put("epochTime", epochTime)
        .put("isoTime", isoTime)
        .put("response_size", responseSize)
        .put("origin", origin);
  }

  public String toString() {
    return toJson().toString();
  }

  public static class Builder {
    private long epoch;
    private String isoTime;
    private String userid;
    private String id;
    private long responseSize;
    private String providerId;
    private String primaryKey;
    private String origin;
    private String resourceGroup;
    private String delegatorId;
    private String type;
    private String api;
    private String event;

    public Builder atEpoch(long epoch) {
      this.epoch = epoch;
      return this;
    }

    public Builder atIsoTime(String isoTime) {
      this.isoTime = isoTime;
      return this;
    }

    public Builder forUserId(String userId) {
      this.userid = userId;
      return this;
    }

    public Builder forResourceId(String id) {
      this.id = id;
      return this;
    }

    public Builder withResponseSize(long responseSize) {
      this.responseSize = responseSize;
      return this;
    }

    public Builder withProviderId(String providerId) {
      this.providerId = providerId;
      return this;
    }

    public Builder withPrimaryKey(String primaryKey) {
      this.primaryKey = primaryKey;
      return this;
    }

    public Builder forOrigin(String origin) {
      this.origin = origin;
      return this;
    }

    public Builder forResourceGroup(String resourceGroup) {
      this.resourceGroup = resourceGroup;
      return this;
    }

    public Builder withDelegatorId(String delegatorId) {
      this.delegatorId = delegatorId;
      return this;
    }

    public Builder forType(String type) {
      this.type = type;
      return this;
    }

    public Builder forApi(String api) {
      this.api = api;
      return this;
    }

    public Builder forEvent(String event) {
      this.event = event;
      return this;
    }

    public MeteringLogBuilder build() {
      return new MeteringLogBuilder(this);
    }
  }
}
