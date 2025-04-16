package org.cdpg.dx.databroker.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject
public class UserResponseModel {
  private String userId;
  private String password;

  // Default Constructor
  public UserResponseModel() {}

  // Constructor with JsonObject (Manual Conversion)
  public UserResponseModel(JsonObject json) {
    this.userId = json.getString("userId");
    this.password = json.getString("password");
  }

  // Convert Object to JsonObject (Manual Conversion)
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    if (userId != null) json.put("userId", userId);
    if (password != null) json.put("password", password);
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
}
