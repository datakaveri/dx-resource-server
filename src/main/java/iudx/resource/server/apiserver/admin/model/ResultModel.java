package iudx.resource.server.apiserver.admin.model;

import io.vertx.core.json.JsonObject;
import iudx.resource.server.common.ResponseUrn;

// TODO: Need to remove once we will fix all responses
public record ResultModel() {
  public JsonObject constructSuccessResponse() {
    return new JsonObject()
        .put("type", ResponseUrn.SUCCESS_URN.getUrn())
        .put("title", ResponseUrn.SUCCESS_URN.getMessage())
        .put("detail", ResponseUrn.SUCCESS_URN.getMessage());
  }
}
