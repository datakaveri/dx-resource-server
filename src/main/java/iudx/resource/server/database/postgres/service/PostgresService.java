package iudx.resource.server.database.postgres.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.database.postgres.model.PostgresResultModel;


@VertxGen
@ProxyGen
public interface PostgresService {
  @GenIgnore
  static PostgresService createProxy(Vertx vertx, String address) {
    return new PostgresServiceVertxEBProxy(vertx, address);
  }

  Future<JsonObject> executeQuery(final String query);

  Future<PostgresResultModel> executeQuery1(final String query);

  Future<JsonObject> executePreparedQuery(final String query, final JsonObject queryparams);
}
