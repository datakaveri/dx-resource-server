package iudx.resource.server.apiserver.subscription.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject
public class PostSubscriptionModel {
  private String userId;
  private String subscriptionType;
  private String instanceId;
  private String entities;
  private String name;
  private String expiry;
  private String delegatorId;

  public PostSubscriptionModel() {}

  // Existing Constructor
  public PostSubscriptionModel(
      String userId,
      String subscriptionType,
      String instanceId,
      String entities,
      String name,
      String expiry,
      String delegatorId) {
    this.userId = userId;
    this.subscriptionType = subscriptionType;
    this.instanceId = instanceId;
    this.entities = entities;
    this.name = name;
    this.expiry = expiry;
    this.delegatorId = delegatorId;
  }

  // JSON Constructor
  public PostSubscriptionModel(JsonObject json) {
    this.userId = json.getString("userId");
    this.subscriptionType = json.getString("subscriptionType");
    this.instanceId = json.getString("instanceId");
    this.entities = json.getString("entities");
    this.name = json.getString("name");
    this.expiry = json.getString("expiry");
    this.delegatorId = json.getString("delegatorId");
  }

  public String getExpiry() {
    return expiry;
  }

  public void setExpiry(String expiry) {
    this.expiry = expiry;
  }

  public String getDelegatorId() {
    return delegatorId;
  }

  public void setDelegatorId(String delegatorId) {
    this.delegatorId = delegatorId;
  }

  // toJson() Method (Needed for Serialization)
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put("userId", userId);
    json.put("subscriptionType", subscriptionType);
    json.put("instanceId", instanceId);
    json.put("entities", entities);
    json.put("name", name);
    json.put("expiry", expiry);
    json.put("delegatorId", delegatorId);
    return json;
  }

  // Getters and Setters
  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getSubscriptionType() {
    return subscriptionType;
  }

  public void setSubscriptionType(String subscriptionType) {
    this.subscriptionType = subscriptionType;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }

  public String getEntities() {
    return entities;
  }

  public void setEntities(String entities) {
    this.entities = entities;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
