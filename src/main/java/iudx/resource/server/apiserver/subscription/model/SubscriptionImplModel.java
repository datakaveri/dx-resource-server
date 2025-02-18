package iudx.resource.server.apiserver.subscription.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.Getter;

@DataObject
public class SubscriptionImplModel {
    private PostModelSubscription postModelSubscription;
    private String type;
    private String resourcegroup;

    // Default Constructor (Needed for Vert.x Codegen)
    public SubscriptionImplModel() {}

    // Existing Constructor
    public SubscriptionImplModel(PostModelSubscription postModelSubscription, String type, String resourcegroup) {
        this.postModelSubscription = postModelSubscription;
        this.type = type;
        this.resourcegroup = resourcegroup;
    }

    // JSON Constructor (IMPORTANT)
    public SubscriptionImplModel(JsonObject json) {
        this.postModelSubscription = new PostModelSubscription(json.getJsonObject("controllerModel"));
        this.type = json.getString("type");
        this.resourcegroup = json.getString("resourcegroup");
    }

    // toJson() Method (Needed for Serialization)
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.put("controllerModel", postModelSubscription.toJson());
        json.put("type", type);
        json.put("resourcegroup", resourcegroup);
        return json;
    }

    // Getters and Setters
    public PostModelSubscription getControllerModel() {
        return postModelSubscription;
    }

    public void setControllerModel(PostModelSubscription postModelSubscription) {
        this.postModelSubscription = postModelSubscription;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getResourcegroup() {
        return resourcegroup;
    }

    public void setResourcegroup(String resourcegroup) {
        this.resourcegroup = resourcegroup;
    }
}
