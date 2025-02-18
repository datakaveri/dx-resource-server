package iudx.resource.server.databroker.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject
public class UserRequest {
    private String userId;
    private String password;
    private String url;

    public String getVhost() {
        return vhost;
    }

    public void setVhost(String vhost) {
        this.vhost = vhost;
    }

    private String vhost;

    // Default Constructor
    public UserRequest() {}

    // Constructor with JsonObject (Manual Conversion)
    public UserRequest(JsonObject json) {
        this.userId = json.getString("userId");
        this.password = json.getString("password");
        this.url = json.getString("url");
        this.vhost = json.getString("vhost");
    }

    // Convert Object to JsonObject (Manual Conversion)
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (userId != null) json.put("userId", userId);
        if (password != null) json.put("password", password);
        if (url != null) json.put("url", url);
        if(vhost != null) json.put("vhost", vhost);
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
