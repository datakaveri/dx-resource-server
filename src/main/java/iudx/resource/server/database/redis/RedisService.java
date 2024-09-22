package iudx.resource.server.database.redis;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@VertxGen
@ProxyGen
public interface  RedisService {


    Future<JsonObject> getJson(String key);


    Future<JsonObject> insertJson(String key, JsonObject jsonObject);

    @GenIgnore
    static RedisService createProxy(Vertx vertx, String address) {
        return new RedisServiceVertxEBProxy(vertx, address);
    }


}
