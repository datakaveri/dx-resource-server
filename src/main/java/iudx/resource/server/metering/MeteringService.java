package iudx.resource.server.metering;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.authenticator.model.JwtData;

@ProxyGen
@VertxGen
public interface MeteringService {

  @GenIgnore
  static MeteringService createProxy(Vertx vertx, String address) {
    return new MeteringServiceVertxEBProxy(vertx, address);
  }

  @Fluent
  MeteringService executeReadQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  MeteringService insertMeteringValuesInRmq(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  MeteringService monthlyOverview(
      JwtData jwtData, String start, String end, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  MeteringService summaryOverview(
      JwtData jwtData, String startTime, String endTime, Handler<AsyncResult<JsonObject>> handler);
}
