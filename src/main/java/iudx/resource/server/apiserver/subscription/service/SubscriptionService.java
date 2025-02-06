package iudx.resource.server.apiserver.subscription.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.databroker.DataBrokerService;

public interface SubscriptionService {

  /**
   * get a subscription by id.
   *
   * @param json json containing subscription id.
   * @return Future object
   */
  Future<JsonObject> getSubscription(
      JsonObject json, DataBrokerService databroker, PostgresService1 pgService);

  /**
   * create a subscription.
   *
   * @param json subscription json.
   * @return Future object
   */
  Future<JsonObject> createSubscription(
      JsonObject json,
      DataBrokerService databroker,
      PostgresService1 pgService,
      JsonObject authInfo,
      CacheService cacheService);

  /**
   * update a subscription.
   *
   * @param json subscription body
   * @return Future
   */
  Future<JsonObject> updateSubscription(
      JsonObject json,
      DataBrokerService databroker,
      PostgresService1 pgService,
      JsonObject authInfo);

  /**
   * append a subscription with new values.
   *
   * @param json subscription vlaues to be updated
   * @return Future object
   */
  Future<JsonObject> appendSubscription(
      JsonObject json,
      DataBrokerService databroker,
      PostgresService1 pgService,
      JsonObject authInfo,
      CacheService cacheService);

  /**
   * delete a subscription request.
   *
   * @param json json containing id for sub to delete
   * @return Future object
   */
  Future<JsonObject> deleteSubscription(
      JsonObject json, DataBrokerService databroker, PostgresService1 pgService);
}
