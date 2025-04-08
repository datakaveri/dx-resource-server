package org.cdpg.dx.rs.search.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DataObject
public class RequestParamsDTO1 {
    private Map<String, List<String>> params;
    private Map<String, String> pathParams;
    private JsonObject requestBody;
    private int timeLimit;
    private String timeLimitConfig;
    private int timeLimitAsync;
    private String tenantPrefix;
    private MultiMap queryParams;
    private String host;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    private String groupId;
    public RequestParamsDTO1() {
        this.params = new HashMap<>();
        this.pathParams = new HashMap<>();
        this.requestBody = new JsonObject();
        this.queryParams = MultiMap.caseInsensitiveMultiMap();
    }

    public RequestParamsDTO1(JsonObject json) {
        this.timeLimit = json.getInteger("timeLimit", 0);
        this.timeLimitAsync = json.getInteger("timeLimitAsync", 0);
        this.tenantPrefix = json.getString("tenantPrefix", null);
        this.params = new HashMap<>();
        this.pathParams = new HashMap<>();
        this.requestBody = json.getJsonObject("requestBody", new JsonObject());
        this.host = json.getString("host");
        this.timeLimitConfig = json.getString("timeLimitConfig");

        JsonObject jsonParams = json.getJsonObject("params", new JsonObject());
        for (String key : jsonParams.fieldNames()) {
            JsonArray values = jsonParams.getJsonArray(key);
            this.params.put(key, values.getList());
        }

        JsonObject jsonPathParams = json.getJsonObject("pathParams", new JsonObject());
        for (String key : jsonPathParams.fieldNames()) {
            this.pathParams.put(key, jsonPathParams.getString(key));
        }

        setQueryParams(this.params);
    }

    public RequestParamsDTO1(Map<String, List<String>> params, String host, int timeLimit, String tenantPrefix, String timeLimitConfig) {
        this.params = params;
        this.pathParams = new HashMap<>();
        this.requestBody = new JsonObject();
        this.host = host;
        this.timeLimit = timeLimit;
        this.tenantPrefix = tenantPrefix;
        this.timeLimitConfig = timeLimitConfig;
        setQueryParams(this.params);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject()
                .put("timeLimit", timeLimit)
                .put("timeLimitAsync", timeLimitAsync)
                .put("tenantPrefix", tenantPrefix)
                .put("host", host)
                .put("timeLimitConfig", timeLimitConfig)
                .put("requestBody", requestBody);

        JsonObject jsonParams = new JsonObject();
        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            jsonParams.put(entry.getKey(), new JsonArray(entry.getValue()));
        }
        json.put("params", jsonParams);

        JsonObject jsonPathParams = new JsonObject();
        for (Map.Entry<String, String> entry : pathParams.entrySet()) {
            jsonPathParams.put(entry.getKey(), entry.getValue());
        }
        json.put("pathParams", jsonPathParams);

        return json;
    }

    private void setQueryParams(Map<String, List<String>> params) {
        queryParams = MultiMap.caseInsensitiveMultiMap();
        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            queryParams.add(entry.getKey(), entry.getValue());
        }
    }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getTimeLimit() { return timeLimit; }
    public void setTimeLimit(int timeLimit) { this.timeLimit = timeLimit; }

    public int getTimeLimitAsync() { return timeLimitAsync; }
    public void setTimeLimitAsync(int timeLimitAsync) { this.timeLimitAsync = timeLimitAsync; }

    public String getTenantPrefix() { return tenantPrefix; }
    public void setTenantPrefix(String tenantPrefix) { this.tenantPrefix = tenantPrefix; }

    public MultiMap getQueryParams() { return queryParams; }
    public Map<String, String> getPathParams() { return pathParams; }
    public void setPathParams(Map<String, String> pathParams) { this.pathParams = pathParams; }

    public JsonObject getRequestBody() { return requestBody; }
    public void setRequestBody(JsonObject requestBody) { this.requestBody = requestBody; }

    public String getTimeLimitConfig() { return timeLimitConfig; }
    public void setTimeLimitConfig(String timeLimitConfig) { this.timeLimitConfig = timeLimitConfig; }
}
