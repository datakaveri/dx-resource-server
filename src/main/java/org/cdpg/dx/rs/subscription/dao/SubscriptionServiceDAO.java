package org.cdpg.dx.rs.subscription.dao;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import org.cdpg.dx.rs.subscription.model.SubscriptionDTO;

public interface SubscriptionServiceDAO {
    Future<String> getEntityIdByQueueName(String subscriptionId);
    Future<JsonArray> getSubscriptionByUserId(String userId);
    Future<Void> deleteSubscriptionBySubId(String subscriptionId);
    Future<JsonArray> getSubscriptionByQueueNameAndEntityId(String subscriptionId, String entityId);
    Future<Void> updateSubscriptionExpiryByQueueNameAndEntityId(String queueName, String entityId, String expiry);
    Future<Void> insertSubscription(SubscriptionDTO subscriptionDTO);
}
