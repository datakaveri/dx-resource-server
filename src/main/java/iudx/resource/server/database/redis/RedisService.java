package iudx.resource.server.database.redis;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.database.redis.model.ResponseModel;

import java.util.List;


@VertxGen
@ProxyGen
public interface RedisService {

    @GenIgnore
    static RedisService createProxy(Vertx vertx, String address) {
        return (RedisService) new RedisServiceVertxEBProxy(vertx, address);
    }

    Future<ResponseModel> getFromRedis(List<String> listOfKeys);

//    Future<JsonObject> getFromRedis(JsonObject jsonObject);

    Future<JsonObject> insertIntoRedis(String key, JsonObject jsonObject);


}
