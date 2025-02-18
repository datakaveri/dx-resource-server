package iudx.resource.server.apiserver.subscription.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.subscription.model.DeleteSubsResultModel;
import iudx.resource.server.apiserver.subscription.model.GetResultModel;
import iudx.resource.server.apiserver.subscription.model.PostModelSubscription;
import iudx.resource.server.apiserver.subscription.model.SubscriptionData;
import iudx.resource.server.database.postgres.model.PostgresResultModel;

/** interface to define all subscription related operation. */
public interface SubscriptionService {

  Future<GetResultModel> getSubscription(String subscriptionID, String subType);

  Future<SubscriptionData> createSubscription(PostModelSubscription postModelSubscription);

  Future<JsonObject> updateSubscription(String entities, String subId, JsonObject authInfo);

  Future<SubscriptionData> appendSubscription(
      PostModelSubscription postModelSubscription, String subId);

  Future<DeleteSubsResultModel> deleteSubscription(
      String subsId, String subscriptionType, String userid);

  Future<PostgresResultModel> getAllSubscriptionQueueForUser(String userId);
}
