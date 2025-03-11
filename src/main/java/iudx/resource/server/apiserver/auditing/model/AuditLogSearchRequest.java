package iudx.resource.server.apiserver.auditing.model;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public final class AuditLogSearchRequest {
    private final String endPoint;
    private final String userId;
    private final String iid;
    private final String startTime;
    private final String endTime;
    private final String timeRelation;
    private final String providerId;
    private final String consumerId;
    private final String resourceId;
    private final String api;
    private final String options;
    private final String offset;
    private final String limit;

    private AuditLogSearchRequest(Builder builder) {
        this.endPoint = builder.endPoint;
        this.userId = builder.userId;
        this.iid = builder.iid;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.timeRelation = builder.timeRelation;
        this.providerId = builder.providerId;
        this.consumerId = builder.consumerId;
        this.resourceId = builder.resourceId;
        this.api = builder.api;
        this.options = builder.options;
        this.offset = builder.offset;
        this.limit = builder.limit;
    }

    public static AuditLogSearchRequest fromJson(JsonObject json) {
        return new Builder()
                .setEndPoint(json.getString("endPoint"))
                .setUserId(json.getString("userid"))
                .setIid(json.getString("iid"))
                .setStartTime(json.getString("startTime"))
                .setEndTime(json.getString("endTime"))
                .setTimeRelation(json.getString("timeRelation"))
                .setProviderId(json.getString("providerID"))
                .setConsumerId(json.getString("consumerID"))
                .setResourceId(json.getString("resourceId"))
                .setApi(json.getString("api"))
                .setOptions(json.getString("options"))
                .setOffset(json.getString("offset"))
                .setLimit(json.getString("limit"))
                .build();
    }

    public static AuditLogSearchRequest fromHttpRequest(HttpServerRequest request, JsonObject provider) {
        return new Builder()
                .setEndPoint(provider.getString("apiEndpoint"))
                .setUserId(provider.getString("userid"))
                .setIid(provider.getString("iid"))
                .setStartTime(request.getParam("time"))
                .setEndTime(request.getParam("endTime"))
                .setTimeRelation(request.getParam("timerel"))
                .setProviderId(request.getParam("providerID"))
                .setConsumerId(request.getParam("consumer"))
                .setResourceId(request.getParam("id"))
                .setApi(request.getParam("api"))
                .setOptions(request.headers().get("options"))
                .setOffset(request.getParam("offset"))
                .setLimit(request.getParam("limit"))
                .build();
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("endPoint", endPoint)
                .put("userid", userId)
                .put("iid", iid)
                .put("startTime", startTime)
                .put("endTime", endTime)
                .put("timeRelation", timeRelation)
                .put("providerID", providerId)
                .put("consumerID", consumerId)
                .put("resourceId", resourceId)
                .put("api", api)
                .put("options", options)
                .put("offset", offset)
                .put("limit", limit);
    }

    public String getEndPoint() { return endPoint; }

    public String getUserId() { return userId; }

    public String getIid() { return iid; }

    public String getStartTime() { return startTime; }

    public String getEndTime() { return endTime; }

    public String getTimeRelation() { return timeRelation; }

    public String getProviderId() { return providerId; }

    public String getConsumerId() { return consumerId; }

    public String getResourceId() { return resourceId; }

    public String getApi() { return api; }

    public String getOptions() { return options; }

    public String getOffset() { return offset; }

    public String getLimit() { return limit; }

    public static class Builder {
        private String endPoint;
        private String userId;
        private String iid;
        private String startTime;
        private String endTime;
        private String timeRelation;
        private String providerId;
        private String consumerId;
        private String resourceId;
        private String api;
        private String options;
        private String offset;
        private String limit;

        public Builder setEndPoint(String endPoint) { this.endPoint = endPoint; return this; }
        public Builder setUserId(String userId) { this.userId = userId; return this; }
        public Builder setIid(String iid) { this.iid = iid; return this; }
        public Builder setStartTime(String startTime) { this.startTime = startTime; return this; }
        public Builder setEndTime(String endTime) { this.endTime = endTime; return this; }
        public Builder setTimeRelation(String timeRelation) { this.timeRelation = timeRelation; return this; }
        public Builder setProviderId(String providerId) { this.providerId = providerId; return this; }
        public Builder setConsumerId(String consumerId) { this.consumerId = consumerId; return this; }
        public Builder setResourceId(String resourceId) { this.resourceId = resourceId; return this; }
        public Builder setApi(String api) { this.api = api; return this; }
        public Builder setOptions(String options) { this.options = options; return this; }
        public Builder setOffset(String offset) { this.offset = offset; return this; }
        public Builder setLimit(String limit) { this.limit = limit; return this; }

        public AuditLogSearchRequest build() {
            return new AuditLogSearchRequest(this);
        }
    }
}
