package iudx.resource.server.database.elastic.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.database.archives.DatabaseService;
import iudx.resource.server.database.archives.DatabaseServiceVertxEBProxy;

@VertxGen
@ProxyGen
public interface ElasticService {

    @GenIgnore
    static DatabaseService createProxy(Vertx vertx, String address) {
        return new DatabaseServiceVertxEBProxy(vertx, address);
    }

    Future<JsonObject> search(JsonObject request);

    Future<JsonObject> count(JsonObject request);

}
