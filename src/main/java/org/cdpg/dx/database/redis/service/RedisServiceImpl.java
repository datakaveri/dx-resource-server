package org.cdpg.dx.database.redis.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.database.redis.model.RedisResponseModel;

public class RedisServiceImpl implements RedisService {
    private static final Logger LOGGER = LogManager.getLogger(RedisServiceImpl.class);
    private final RedisAPI redisAPI;

    public RedisServiceImpl(Vertx vertx, RedisOptions options) {
        Redis redisClient = Redis.createClient(vertx, options);
        this.redisAPI = RedisAPI.api(redisClient);
    }

    /**
     * Retrieve JSON data from Redis and return it as a ResponseModel.
     *
     * @param key  Redis key
     * @param path JSON Path
     * @return Future containing ResponseModel with JSON data
     */
    public Future<RedisResponseModel> searchAsync(String key, String path) {
        Promise<RedisResponseModel> promise = Promise.promise();



        redisAPI.send(Command.JSON_GET, key, path)
                .onSuccess(redisResponse -> {
                    LOGGER.info("Retrieved key {} from Redis", key);
                    promise.complete(new RedisResponseModel(redisResponse));
                })
                .onFailure(err -> {
                    LOGGER.error("Failed to get key {} from Redis: {}", key, err.getMessage());
                    promise.fail(err);
                });

        return promise.future();
    }

    /**
     * Insert JSON data into Redis using JSON.SET.
     *
     * @param key        Redis key
     * @param jsonObject JSON value to store
     * @return Future indicating success or failure
     */
    @Override
    public Future<RedisResponseModel> insertIntoRedis(String key, JsonObject jsonObject) {
        return null;
    }
}
