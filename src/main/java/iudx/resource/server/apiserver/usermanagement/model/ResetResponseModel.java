package iudx.resource.server.apiserver.usermanagement.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.common.ResponseUrn;

@DataObject
public class ResetResponseModel {
  private ResetPasswordModel result;
  private String title = "successful";
  private String type = ResponseUrn.SUCCESS_URN.getUrn();
  private String detail = "Successfully changed the password";

  // No-args constructor
  public ResetResponseModel() {}

  // Parameterized constructor
  public ResetResponseModel(ResetPasswordModel result) {
    this.result = result;
  }

  // Constructor to create from JSON
  public ResetResponseModel(JsonObject json) {
    this.result = new ResetPasswordModel(json.getJsonArray("results").getJsonObject(0));
  }

  public JsonObject toJson() {
    return new JsonObject()
        .put("type", type)
        .put("title", title)
        .put("detail", detail)
        .put("results", new JsonArray().add(result.toJson()));
  }

  // Getter and Setter
  public ResetPasswordModel getResult() {
    return result;
  }

  public void setResult(ResetPasswordModel result) {
    this.result = result;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getDetail() {
    return detail;
  }

  public void setDetail(String detail) {
    this.detail = detail;
  }

  @Override
  public String toString() {
    return "ResetResponseModel{"
        + "type='"
        + type
        + '\''
        + ", title='"
        + title
        + '\''
        + ", detail='"
        + detail
        + '\''
        + ", results=["
        + result
        + "]"
        + '}';
  }
}
