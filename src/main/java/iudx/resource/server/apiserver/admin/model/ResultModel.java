package iudx.resource.server.apiserver.admin.model;

import io.vertx.core.json.JsonObject;
import iudx.resource.server.common.ResponseUrn;

public record ResultModel() {
  public JsonObject constructSuccessResponse() {
    return new JsonObject()
        .put("type", ResponseUrn.SUCCESS_URN.getUrn())
        .put("title", ResponseUrn.SUCCESS_URN.getMessage())
        .put("detail", ResponseUrn.SUCCESS_URN.getMessage());
  }
}
