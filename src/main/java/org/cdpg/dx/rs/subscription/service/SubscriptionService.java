package org.cdpg.dx.rs.subscription.service;

import io.vertx.core.Future;
import java.util.List;
import org.cdpg.dx.databroker.model.*;
import org.cdpg.dx.rs.subscription.model.*;

/** interface to define all subscription related operation. */
public interface SubscriptionService {

  Future<GetSubscriptionModel> getSubscription(String subscriptionId);

  Future<RegisterQueueModel> createSubscription(PostSubscriptionModel postSubscriptionModel);

  Future<String> updateSubscription(String entities, String queueName, String expiry);

  Future<String> appendSubscription(
      PostSubscriptionModel postSubscriptionModel, String subId);

  Future<String> deleteSubscription(String subsId, String userid);

  Future<List<SubscriberDetails>> getAllSubscriptionQueueForUser(String userId);
}
