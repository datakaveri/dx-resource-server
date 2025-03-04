package iudx.resource.server.apiserver.usermanagement.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject
public class ResetPasswordModel {
  private String username;
  private String apiKey;

  // No-args constructor
  public ResetPasswordModel() {}

  // Parameterized constructor
  public ResetPasswordModel(String username, String apiKey) {
    this.username = username;
    this.apiKey = apiKey;
  }

  // Constructor to create from JSON
  public ResetPasswordModel(JsonObject json) {
    this.username = json.getString("username");
    this.apiKey = json.getString("apiKey");
  }

  public JsonObject toJson() {
    return new JsonObject().put("username", username).put("apiKey", apiKey);
  }

  // Getters and Setters
  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  @Override
  public String toString() {
    return "ResetPasswordModel{"
        + "username='"
        + username
        + '\''
        + ", apiKey='"
        + apiKey
        + '\''
        + '}';
  }
}
