package iudx.resource.server.apiserver.subscription.service;

import io.vertx.core.Future;
import iudx.resource.server.apiserver.subscription.model.GetSubscriptionResult;
import iudx.resource.server.apiserver.subscription.model.PostSubscriptionModel;
import iudx.resource.server.apiserver.subscription.model.SubscriberDetails;
import iudx.resource.server.database.postgres.model.PostgresResultModel;
import org.cdpg.dx.databroker.model.RegisterQueueModel;

import java.util.List;

/** interface to define all subscription related operation. */
public interface SubscriptionService {

  Future<GetSubscriptionResult> getSubscription(String subscriptionId, String subType);

  Future<RegisterQueueModel> createSubscription(PostSubscriptionModel postSubscriptionModel);

  Future<String> updateSubscription(String entities, String queueName, String expiry);

  Future<String> appendSubscription(
      PostSubscriptionModel postSubscriptionModel, String subId);

  Future<String> deleteSubscription(String subsId, String subscriptionType, String userid);

  Future<List<SubscriberDetails>> getAllSubscriptionQueueForUser(String userId);
}
