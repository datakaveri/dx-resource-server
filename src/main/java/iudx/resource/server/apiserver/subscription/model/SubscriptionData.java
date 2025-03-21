package iudx.resource.server.apiserver.subscription.model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.common.ResponseUrn;
import org.cdpg.dx.databroker.model.RegisterQueueModel;

public record SubscriptionData(
    JsonObject cacheResult,
    RegisterQueueModel registerQueueModel) {
 /* public JsonObject constructSuccessResponse() {
    return new JsonObject()
        .put("type", ResponseUrn.SUCCESS_URN.getUrn())
        .put("title", ResponseUrn.SUCCESS_URN.getMessage().toLowerCase())
        .put("results", new JsonArray().add(streamingResult.toJson()));
  }*/
}
