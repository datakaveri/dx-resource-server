package iudx.resource.server.apiserver.metering.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@ProxyGen
@VertxGen
public interface MeteringService {

  @GenIgnore
  static MeteringService createProxy(Vertx vertx, String address) {
    return new MeteringServiceVertxEBProxy(vertx, address);
  }
  /*Future<Void> publishMeteringLogMessage(JwtData jwtData, long responseSize, String endPoint);*/
  Future<JsonObject> executeReadQuery(JsonObject request);

  Future<JsonObject> insertMeteringValuesInRmq(JsonObject request);

  Future<JsonObject> monthlyOverview(JsonObject request);

  Future<JsonObject> summaryOverview(JsonObject request);
}
