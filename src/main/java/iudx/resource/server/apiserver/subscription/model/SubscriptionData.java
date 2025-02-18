package iudx.resource.server.apiserver.subscription.model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.databroker.model.SubscriptionResponseModel;

public record SubscriptionData(
    JsonObject dataBrokerResult,
    JsonObject cacheResult,
    SubscriptionResponseModel streamingResult) {
  public JsonObject constructSuccessResponse() {
    return new JsonObject()
        .put("type", ResponseUrn.SUCCESS_URN.getUrn())
        .put("title", ResponseUrn.SUCCESS_URN.getMessage().toLowerCase())
        .put("results", new JsonArray().add(streamingResult.toJson()));
  }
}
