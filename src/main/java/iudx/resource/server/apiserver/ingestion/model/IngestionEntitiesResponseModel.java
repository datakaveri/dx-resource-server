package iudx.resource.server.apiserver.ingestion.model;

import io.vertx.core.json.JsonObject;
import iudx.resource.server.common.ResponseUrn;

public record IngestionEntitiesResponseModel(String details) {
  public JsonObject constructSuccessResponse() {
    return new JsonObject()
        .put("type", ResponseUrn.SUCCESS_URN.getUrn())
        .put("title", ResponseUrn.SUCCESS_URN.getMessage().toLowerCase())
        .put("detail", details);
  }
}
