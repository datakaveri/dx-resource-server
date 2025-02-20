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
// ====================
/*package iudx.resource.server.apiserver.metering.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject
public class MeteringLog {

    private long epochTime;
    private String isoTime;
    private String userId;
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

    // Default Constructor (Required for Vert.x)
    public MeteringLog() {}

    // Parameterized Constructor
    public MeteringLog(long epochTime, String isoTime, String userId, String id, long responseSize,
                       String providerId, String primaryKey, String origin, String api,
                       String resourceGroup, String delegatorId, String type, String event) {
        this.epochTime = epochTime;
        this.isoTime = isoTime;
        this.userId = userId;
        this.id = id;
        this.responseSize = responseSize;
        this.providerId = providerId;
        this.primaryKey = primaryKey;
        this.origin = origin;
        this.api = api;
        this.resourceGroup = resourceGroup;
        this.delegatorId = delegatorId;
        this.type = type;
        this.event = event;
    }

    // Constructor from JSON (Required for Vert.x)
    public MeteringLog(JsonObject json) {
        this.epochTime = json.getLong("epochTime", 0L);
        this.isoTime = json.getString("isoTime");
        this.userId = json.getString("userId");
        this.id = json.getString("id");
        this.responseSize = json.getLong("responseSize", 0L);
        this.providerId = json.getString("providerId");
        this.primaryKey = json.getString("primaryKey");
        this.origin = json.getString("origin");
        this.api = json.getString("api");
        this.resourceGroup = json.getString("resourceGroup");
        this.delegatorId = json.getString("delegatorId");
        this.type = json.getString("type");
        this.event = json.getString("event");
    }

    // Convert to JSON (Required for Vert.x)
    public JsonObject toJson() {
        return new JsonObject()
                .put("epochTime", epochTime)
                .put("isoTime", isoTime)
                .put("userId", userId)
                .put("id", id)
                .put("responseSize", responseSize)
                .put("providerId", providerId)
                .put("primaryKey", primaryKey)
                .put("origin", origin)
                .put("api", api)
                .put("resourceGroup", resourceGroup)
                .put("delegatorId", delegatorId)
                .put("type", type)
                .put("event", event);
    }

    // Getters & Setters
    public long getEpochTime() { return epochTime; }
    public void setEpochTime(long epochTime) { this.epochTime = epochTime; }

    public String getIsoTime() { return isoTime; }
    public void setIsoTime(String isoTime) { this.isoTime = isoTime; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public long getResponseSize() { return responseSize; }
    public void setResponseSize(long responseSize) { this.responseSize = responseSize; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getPrimaryKey() { return primaryKey; }
    public void setPrimaryKey(String primaryKey) { this.primaryKey = primaryKey; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getApi() { return api; }
    public void setApi(String api) { this.api = api; }

    public String getResourceGroup() { return resourceGroup; }
    public void setResourceGroup(String resourceGroup) { this.resourceGroup = resourceGroup; }

    public String getDelegatorId() { return delegatorId; }
    public void setDelegatorId(String delegatorId) { this.delegatorId = delegatorId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }

    @Override
    public String toString() {
        return toJson().encodePrettily();
    }
    }*/
