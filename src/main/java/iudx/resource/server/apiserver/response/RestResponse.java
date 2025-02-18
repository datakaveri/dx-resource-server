package iudx.resource.server.apiserver.response;

import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.util.Constants;

/** create a rest response body for API. */
public class RestResponse {

  private String type;
  private String title;
  private String detail;

  private RestResponse(String type, String title, String message) {
    super();
    this.type = type;
    this.title = title;
    this.detail = message;
  }

  /**
   * convert object to json.
   *
   * @return JsonObject json representation for object
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_TYPE, this.type);
    json.put(Constants.JSON_TITLE, this.title);
    json.put(Constants.JSON_DETAIL, this.detail);
    return json;
  }

  public String toJsonString() {
    return toJson().toString();
  }

  public static class Builder {
    private String type;
    private String title;
    private String message;

    public Builder() {}

    public Builder withType(String type) {
      this.type = type;
      return this;
    }

    public Builder withTitle(String title) {
      this.title = title;
      return this;
    }

    public Builder withMessage(String message) {
      this.message = message;
      return this;
    }

    public RestResponse build() {
      return new RestResponse(this.type, this.title, this.message);
    }
  }
}