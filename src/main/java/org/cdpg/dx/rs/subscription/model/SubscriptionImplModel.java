package org.cdpg.dx.rs.subscription.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject
public class SubscriptionImplModel {
  private PostSubscriptionModel postSubscriptionModel;
  private String type;
  private String resourceGroup;

  // Default Constructor (Needed for Vert.x Codegen)
  public SubscriptionImplModel() {}

  // Existing Constructor
  public SubscriptionImplModel(
          PostSubscriptionModel postSubscriptionModel, String type, String resourceGroup) {
    this.postSubscriptionModel = postSubscriptionModel;
    this.type = type;
    this.resourceGroup = resourceGroup;
  }

  // JSON Constructor (IMPORTANT)
  public SubscriptionImplModel(JsonObject json) {
    this.postSubscriptionModel = new PostSubscriptionModel(json.getJsonObject("controllerModel"));
    this.type = json.getString("type");
    this.resourceGroup = json.getString("resourceGroup");
  }

  // toJson() Method (Needed for Serialization)
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put("controllerModel", postSubscriptionModel.toJson());
    json.put("type", type);
    json.put("resourceGroup", resourceGroup);
    return json;
  }

  // Getters and Setters
  public PostSubscriptionModel getControllerModel() {
    return postSubscriptionModel;
  }

  public void setControllerModel(PostSubscriptionModel postSubscriptionModel) {
    this.postSubscriptionModel = postSubscriptionModel;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getResourceGroup() {
    return resourceGroup;
  }

  public void setResourceGroup(String resourceGroup) {
    this.resourceGroup = resourceGroup;
  }
}
