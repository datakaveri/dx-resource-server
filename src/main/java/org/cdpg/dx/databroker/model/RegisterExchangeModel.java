package org.cdpg.dx.databroker.model;

import static iudx.resource.server.databroker.util.Constants.*;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject
public class RegisterExchangeModel {
  private String userId;
  private String apiKey;
  private String exchangeName;
  private String url;
  private int port;
  private String vHost;

  public RegisterExchangeModel() {}

  public RegisterExchangeModel(
      String userId, String apiKey, String exchangeName, String url, int port, String vHost) {
    this.userId = userId;
    this.apiKey = apiKey;
    this.exchangeName = exchangeName;
    this.url = url;
    this.port = port;
    this.vHost = vHost;
  }

  public RegisterExchangeModel(JsonObject json) {
    this.userId = json.getString(USER_NAME);
    this.apiKey = json.getString(APIKEY);
    this.exchangeName = json.getString("id");
    this.url = json.getString(URL);
    this.port = json.getInteger(PORT);
    this.vHost = json.getString(VHOST);
  }

  public JsonObject toJson() {
    return new JsonObject()
        .put(USER_NAME, userId)
        .put(APIKEY, apiKey)
        .put(ID, exchangeName)
        .put(URL, url)
        .put(PORT, port)
        .put(VHOST, vHost);
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getExchangeName() {
    return exchangeName;
  }

  public void setExchangeName(String exchangeName) {
    this.exchangeName = exchangeName;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getvHost() {
    return vHost;
  }

  public void setvHost(String vHost) {
    this.vHost = vHost;
  }

  @Override
  public String toString() {
    return "RegisterExchangeModel{"
        + "userId='"
        + userId
        + '\''
        + ", apiKey='"
        + apiKey
        + '\''
        + ", exchangeName='"
        + exchangeName
        + '\''
        + ", url='"
        + url
        + '\''
        + ", port="
        + port
        + ", vHost='"
        + vHost
        + '\''
        + '}';
  }
}
