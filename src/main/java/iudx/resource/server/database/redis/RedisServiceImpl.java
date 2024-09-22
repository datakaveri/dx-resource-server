package iudx.resource.server.database.redis;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

public class RedisServiceImpl implements RedisService {
    private final RedisAPI redisAPI;
    private static final Logger LOGGER = LogManager.getLogger(RedisServiceImpl.class);

    public RedisServiceImpl(Vertx vertx, RedisOptions options) {
        Redis redisClient = Redis.createClient(vertx, options);
        this.redisAPI = RedisAPI.api(redisClient);
    }

    @Override
    public Future<JsonObject> getJson(String key) {
        Promise<JsonObject> promise = Promise.promise();
        redisAPI.send(Command.JSON_GET, key).onSuccess(result -> {
            handleGetResult(result, promise, key);
        }).onFailure(err -> {
            LOGGER.error("Failed to get from Redis for key {}: {}", key, err.getMessage());
            promise.fail("Failed to get key from Redis: " + err);
        });

        return promise.future();
    }

    private void handleGetResult(Response result, Promise<JsonObject> promise, String key) {
        if (result == null || result.toBuffer().length() == 0) {
            LOGGER.warn("Key does not exist in Redis: {}", key);
            promise.complete(new JsonObject()); // Return an empty JSON object
        } else {
            LOGGER.info("Result from Redis for key {}: {}", key, result.getClass());
            promise.complete(new JsonObject().put("array", result.toBuffer().toJsonArray()));
        }
    }

    @Override
    public Future<JsonObject> insertJson(String key, JsonObject jsonValue) {
        Promise<JsonObject> promise = Promise.promise();
        List<String> args = buildJsonSetArgs(key, jsonValue);

        redisAPI.jsonSet(args).onSuccess(res -> {
            LOGGER.info("Successfully inserted JSON value in Redis for key: {}", key);
            promise.complete(new JsonObject().put("status", "success"));
        }).onFailure(err -> {
            LOGGER.error("Failed to insert JSON value in Redis for key {}: {}", key, err.getMessage());
            promise.fail("Failed to set key in Redis: " + err);
        });

        return promise.future();
    }

    private List<String> buildJsonSetArgs(String key, JsonObject jsonValue) {
        return Arrays.asList(key, ".", jsonValue.encode());
    }
}
