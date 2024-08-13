package iudx.resource.server.metering;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

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
  MeteringService monthlyOverview(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  MeteringService summaryOverview(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  MeteringService getConsumedData(JsonObject request, Handler<AsyncResult<JsonObject>> handler);
}
