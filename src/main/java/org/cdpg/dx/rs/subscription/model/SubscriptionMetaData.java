package org.cdpg.dx.rs.subscription.model;

import io.vertx.core.json.JsonObject;
import org.cdpg.dx.databroker.model.RegisterQueueModel;

public record SubscriptionMetaData(
    JsonObject catalogueInfo,
    RegisterQueueModel registerQueueModel) {
}
