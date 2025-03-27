package org.cdpg.dx.rs.subscription.model;

import io.vertx.core.json.JsonObject;

public class DeleteSubsResultModel {
  private String type;
  private String title;
  private String detail;
  private int status;

  public DeleteSubsResultModel() {}

  public DeleteSubsResultModel(JsonObject jsonObject) {

    this.type = jsonObject.getString("type");
    this.title = jsonObject.getString("title");
    this.detail = jsonObject.getString("detail");
    this.status = jsonObject.getInteger("status");
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDetail() {
    return detail;
  }

  public void setDetail(String detail) {
    this.detail = detail;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put("type", type);
    json.put("title", title);
    json.put("detail", detail);
    return json;
  }
}
