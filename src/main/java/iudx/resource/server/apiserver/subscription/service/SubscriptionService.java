package iudx.resource.server.apiserver.subscription.service;

import io.vertx.core.Future;
import iudx.resource.server.apiserver.subscription.model.DeleteSubsResultModel;
import iudx.resource.server.apiserver.subscription.model.GetResultModel;
import iudx.resource.server.apiserver.subscription.model.PostModelSubscription;
import iudx.resource.server.apiserver.subscription.model.SubscriptionData;
import iudx.resource.server.database.postgres.model.PostgresResultModel;

/** interface to define all subscription related operation. */
public interface SubscriptionService {

  Future<GetResultModel> getSubscription(String subscriptionId, String subType);

  Future<SubscriptionData> createSubscription(PostModelSubscription postModelSubscription);

  Future<GetResultModel> updateSubscription(String entities, String queueName, String expiry);

  Future<GetResultModel> appendSubscription(
      PostModelSubscription postModelSubscription, String subId);

  Future<DeleteSubsResultModel> deleteSubscription(
      String subsId, String subscriptionType, String userid);

  Future<PostgresResultModel> getAllSubscriptionQueueForUser(String userId);
}
