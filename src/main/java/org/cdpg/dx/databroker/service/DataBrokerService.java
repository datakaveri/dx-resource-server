package org.cdpg.dx.databroker.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import org.cdpg.dx.databroker.model.ExchangeSubscribersResponse;
import org.cdpg.dx.databroker.model.RegisterExchangeModel;
import org.cdpg.dx.databroker.model.RegisterQueueModel;
import org.cdpg.dx.databroker.util.PermissionOpType;
import org.cdpg.dx.databroker.util.Vhosts;

@VertxGen
@ProxyGen
public interface DataBrokerService {
  @GenIgnore
  static DataBrokerService createProxy(Vertx vertx, String address) {
    return new DataBrokerServiceVertxEBProxy(vertx, address);
  }

  Future<RegisterQueueModel> registerQueue(String userId, String queueName, Vhosts vhosts);

  Future<Void> queueBinding(
      String exchangeName, String queueName, String routingKey, Vhosts vhosts);

  Future<RegisterExchangeModel> registerExchange(String userId, String exchangeName, Vhosts vhosts);

  Future<ExchangeSubscribersResponse> listExchange(String exchangeName, Vhosts vhosts);

  Future<Void> updatePermission(
      String userId, String queueOrExchangeName, PermissionOpType permissionType, Vhosts vhosts);

  Future<Void> deleteQueue(String queueName, String userid, Vhosts vhosts);

  Future<List<String>> listQueue(String queueName, Vhosts vhosts);

  Future<Void> deleteExchange(String exchangeId, String userId, Vhosts vhosts);

  Future<String> resetPassword(String userId);

  Future<Void> publishMessageInternal(JsonObject body, String exchangeName, String routingKey);

  Future<String> publishMessageExternal(String exchangeName, String routingKey, JsonArray request);
}
