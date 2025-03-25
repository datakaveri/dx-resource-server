package iudx.resource.server.apiserver.ingestion.model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.common.ResponseUrn;

public record IngestionData(IngestionResponseModel ingestionResponseModel) {
  public JsonObject constructSuccessResponse() {
    return new JsonObject()
        .put("type", ResponseUrn.SUCCESS_URN.getUrn())
        .put("title", ResponseUrn.SUCCESS_URN.getMessage().toLowerCase())
        .put("results", new JsonArray().add(ingestionResponseModel.toJson()));
  }
}
