package iudx.resource.server.databroker.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.databroker.model.*;
import iudx.resource.server.databroker.util.PermissionOpType;

import java.util.List;

@VertxGen
@ProxyGen
public interface DataBrokerService {
  @GenIgnore
  static DataBrokerService createProxy(Vertx vertx, String address) {
    return new DataBrokerServiceVertxEBProxy(vertx, address);
  }
  Future<RegisterQueueModel> registerQueue(String userId, String queueName);

  Future<Void> queueBinding(String exchangeName, String queueName, String routingKey);

  Future<RegisterExchangeModel> registerExchange(String userId,String exchangeName);

  Future<ExchangeSubscribersResponse> listExchange(String exchangeName);

  Future<Void> updatePermission(String userId, String queueOrExchangeName, PermissionOpType permissionType);

  Future<Void> deleteQueue(String queueName, String userid);

  Future<List<String>> listQueue(String queueName);

  Future<Void> deleteExchange(String adapterId, String userId);

  Future<String> resetPassword(String userId);

  Future<Void> publishMessage(JsonObject body, String exchangeName, String routingKey);

  Future<String> publishFromAdaptor(String exchangeName, String routingKey, JsonArray request);
}
