package iudx.resource.server.databroker.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.ingestion.model.IngestionModel;
import iudx.resource.server.apiserver.subscription.model.SubscriptionImplModel;
import iudx.resource.server.databroker.model.IngestionResponseModel;
import iudx.resource.server.databroker.model.SubscriptionResponseModel;
import iudx.resource.server.databroker.model.ExchangeSubscribersResponse;

import java.util.List;

@VertxGen
@ProxyGen
public interface DataBrokerService {
  @GenIgnore
  static DataBrokerService createProxy(Vertx vertx, String address) {
    return new DataBrokerServiceVertxEBProxy(vertx, address);
  }

  Future<SubscriptionResponseModel> registerStreamingSubscription(
      SubscriptionImplModel subscriptionImplModel);

  Future<IngestionResponseModel> registerAdaptor(IngestionModel ingestionModel);

  Future<ExchangeSubscribersResponse> listAdaptor(String adaptorId);

  Future<List<String>> appendStreamingSubscription(
      SubscriptionImplModel subscriptionImplModel, String subId);

  Future<Void> deleteStreamingSubscription(String queueName, String userid);

  Future<List<String>> listStreamingSubscription(String subscriptionID);

  Future<JsonObject> resetPassword(String userId);

  Future<Void> publishMessage(JsonObject body, String toExchange, String routingKey);

  Future<Void> deleteAdaptor(String adapterId, String userId);

  Future<String> publishFromAdaptor(String resourceGroupId, String routingKey, JsonArray request);
}
