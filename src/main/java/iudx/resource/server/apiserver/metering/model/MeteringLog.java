package iudx.resource.server.apiserver.metering.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject
public class MeteringLog {
  private long epochTime;
  private String isoTime;
  private String userid;
  private String id;
  private long responseSize;
  private String providerId;
  private String primaryKey;
  private String origin;
  private String api;
  private String resourceGroup;
  private String delegatorId;
  private String type;
  private String event;

  private MeteringLog(Builder builder) {
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

  public MeteringLog() {}

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
        .put("origin", origin)
        .put("event", event);
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

    public Builder() {}

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

    public MeteringLog build() {
      return new MeteringLog(this);
    }
  }
}
