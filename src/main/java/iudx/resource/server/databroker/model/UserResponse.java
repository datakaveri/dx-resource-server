package iudx.resource.server.databroker.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject
public class UserResponse {
    private String userId;
    private String password;
    private String status;
    private String detail;

    // Default Constructor
    public UserResponse() {
    }

    // Constructor with JsonObject (Manual Conversion)
    public UserResponse(JsonObject json) {
        this.userId = json.getString("userId");
        this.password = json.getString("password");
        this.status = json.getString("status");
        this.detail = json.getString("detail");
    }

    // Convert Object to JsonObject (Manual Conversion)
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (userId != null) json.put("userId", userId);
        if (password != null) json.put("password", password);
        if (status != null) json.put("status", status);
        if (detail != null) json.put("detail", detail);
        return json;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
