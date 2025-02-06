package iudx.resource.server.apiserver.subscription.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.database.postgres.service.PostgresService;
import iudx.resource.server.databroker.service.DataBrokerService;

public class SubscriptionServiceImpl implements SubscriptionService {

    //SubsController -> SubscriptionServiceImpl -> StreamingSubscription
    @Override
    public Future<JsonObject> getSubscription(JsonObject json, DataBrokerService databroker, PostgresService pgService) {
        return null;
    }

    @Override
    public Future<JsonObject> createSubscription(JsonObject json, DataBrokerService databroker, PostgresService pgService, JsonObject authInfo, CacheService cacheService) {
        return null;
    }

    @Override
    public Future<JsonObject> updateSubscription(JsonObject json, DataBrokerService databroker, PostgresService pgService, JsonObject authInfo) {
        return null;
    }

    @Override
    public Future<JsonObject> appendSubscription(JsonObject json, DataBrokerService databroker, PostgresService pgService, JsonObject authInfo, CacheService cacheService) {
        return null;
    }

    @Override
    public Future<JsonObject> deleteSubscription(JsonObject json, DataBrokerService databroker, PostgresService pgService) {
        return null;
    }

    @Override
    public Future<JsonObject> getAllSubscriptionQueueForUser(JsonObject json, PostgresService pgService) {
        return null;
    }
}
