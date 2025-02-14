package iudx.resource.server.apiserver.subscription.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.subscription.model.PostModelSubscription;

/** interface to define all subscription related operation. */
public interface SubscriptionService {

  /*Future<JsonObject> getSubscription(
      JsonObject json);*/

  Future<JsonObject> getSubscription(String subscriptionID, String subType);

  /*Future<JsonObject> createSubscription(
      JsonObject json,
      JsonObject authInfo);*/

  Future<JsonObject> createSubscription(
          PostModelSubscription postModelSubscription);

  Future<JsonObject> updateSubscription(
      JsonObject json,
      JsonObject authInfo);

  Future<JsonObject> appendSubscription(
      JsonObject json,
      JsonObject authInfo);

  Future<JsonObject> deleteSubscription(
      JsonObject json);

  Future<JsonObject> getAllSubscriptionQueueForUser(String userId);
}
