package org.cdpg.dx.database.redis.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.Response;
import org.cdpg.dx.database.redis.model.RedisResponseModel;


@VertxGen
@ProxyGen
public interface RedisService {

    @GenIgnore
    static RedisService createProxy(Vertx vertx, String address) {
        return (RedisService) new RedisServiceVertxEBProxy(vertx, address);
    }

    Future<RedisResponseModel> searchAsync(String key, String pathParam);



}
