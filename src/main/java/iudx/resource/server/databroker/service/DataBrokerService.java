package iudx.resource.server.databroker.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@VertxGen
@ProxyGen
public interface DataBrokerService {
  @GenIgnore
  static DataBrokerService createProxy(Vertx vertx, String address) {
    return new DataBrokerServiceVertxEBProxy(vertx, address);
  }

  Future<JsonObject> registerStreamingSubscription(JsonObject request);

  Future<JsonObject> registerAdaptor(JsonObject request, String vhost);

  Future<JsonObject> deleteAdaptor(JsonObject request, String vhost);

  Future<JsonObject> listAdaptor(JsonObject request, String vhost);

  Future<JsonObject> updateStreamingSubscription(JsonObject request);

  Future<JsonObject> appendStreamingSubscription(JsonObject request);

  Future<JsonObject> deleteStreamingSubscription(JsonObject request);

  Future<JsonObject> listStreamingSubscription(JsonObject request);

  Future<JsonObject> createExchange(JsonObject request, String vhost);

  Future<JsonObject> deleteExchange(JsonObject request, String vhost);

  Future<JsonObject> listExchangeSubscribers(JsonObject request, String vhost);

  Future<JsonObject> createQueue(JsonObject request, String vhost);

  Future<JsonObject> deleteQueue(JsonObject request, String vhost);

  Future<JsonObject> bindQueue(JsonObject request, String vhost);

  Future<JsonObject> unbindQueue(JsonObject request, String vhost);

  Future<JsonObject> createvHost(JsonObject request);

  Future<JsonObject> deletevHost(JsonObject request);

  Future<JsonObject> listvHost(JsonObject request);

  Future<JsonObject> listQueueSubscribers(JsonObject request, String vhost);

  Future<JsonObject> publishFromAdaptor(JsonArray request, String vhost);

  Future<JsonObject> resetPassword(JsonObject request);

  Future<JsonObject> getExchange(JsonObject request, String vhost);

  Future<JsonObject> publishHeartbeat(JsonObject request, String vhost);

  Future<JsonObject> publishMessage(JsonObject body, String toExchange, String routingKey);
}
