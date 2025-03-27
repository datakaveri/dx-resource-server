package org.cdpg.dx.rs.subscription.model;

import io.vertx.core.json.JsonObject;
import org.cdpg.dx.databroker.model.RegisterQueueModel;

public record SubscriptionMetaData(
    JsonObject catalogueInfo,
    RegisterQueueModel registerQueueModel) {
 /* public JsonObject constructSuccessResponse() {
    return new JsonObject()
        .put("type", ResponseUrn.SUCCESS_URN.getUrn())
        .put("title", ResponseUrn.SUCCESS_URN.getMessage().toLowerCase())
        .put("results", new JsonArray().add(streamingResult.toJson()));
  }*/
}
