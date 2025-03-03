package iudx.resource.server.apiserver.ingestion.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject
public class IngestionModel {
  private String entities;
  private String userId;
  private String type;
  private String resourceIdForIngestion;

  // No-args constructor
  public IngestionModel() {}

  public IngestionModel(
      String entities, String userId, String type, String resourceIdForIngestion) {
    this.entities = entities;
    this.userId = userId;
    this.type = type;
    this.resourceIdForIngestion = resourceIdForIngestion;
  }

  public IngestionModel(JsonObject json) {
    this.entities = json.getString("entities");
    this.userId = json.getString("userId");
    this.type = json.getString("type");
    this.resourceIdForIngestion = json.getString("resourceIdForIngestion");
  }

  @Override
  public String toString() {
    return "IngestionModel{"
        + "entities='"
        + entities
        + '\''
        + ", userId='"
        + userId
        + '\''
        + ", type='"
        + type
        + '\''
        + ", resourceIdForIngestion='"
        + resourceIdForIngestion
        + '\''
        + '}';
  }

  public JsonObject toJson() {
    return new JsonObject()
        .put("entities", entities)
        .put("userId", userId)
        .put("type", type)
        .put("resourceIdForIngestion", resourceIdForIngestion);
  }

  public String getEntities() {
    return entities;
  }

  public void setEntities(String entities) {
    this.entities = entities;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getResourceIdForIngestion() {
    return resourceIdForIngestion;
  }

  public void setResourceIdForIngestion(String resourceIdForIngestion) {
    this.resourceIdForIngestion = resourceIdForIngestion;
  }
}
