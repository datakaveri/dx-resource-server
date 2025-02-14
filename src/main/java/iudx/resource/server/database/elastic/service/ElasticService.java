package iudx.resource.server.database.elastic.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@VertxGen
@ProxyGen
public interface ElasticService {

    @GenIgnore
    static ElasticService createProxy(Vertx vertx, String address) {
        return new ElasticServiceVertxEBProxy(vertx, address);
    }

    Future<JsonObject> search(JsonObject request);

    Future<JsonObject> count(JsonObject request);

}
