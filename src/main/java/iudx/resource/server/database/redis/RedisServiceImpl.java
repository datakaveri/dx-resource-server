package iudx.resource.server.database.redis;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.Response;
import iudx.resource.server.database.redis.model.ResponseModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

public class RedisServiceImpl implements RedisService {
    private static final Logger LOGGER = LogManager.getLogger(RedisServiceImpl.class);
    private final RedisAPI redisAPI;

    public RedisServiceImpl(Vertx vertx, RedisOptions options) {
        Redis redisClient = Redis.createClient(vertx, options);
        this.redisAPI = RedisAPI.api(redisClient);
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


    private List<String> buildJsonSetArgs(String key, JsonObject jsonValue) {
        return Arrays.asList(key, ".", jsonValue.encode());
    }


    @Override
    public Future<ResponseModel> getFromRedis(List<String> listOfKeys) {
        // Extract the key   list from the input JSON as a List of Strings
        
        // Use a promise to handle the asynchronous result
        Promise<ResponseModel> promise = Promise.promise();

        // Call Redis MGET with the list of keys

        redisAPI.mget(listOfKeys).onSuccess(result -> {
            ResponseModel responseModel = new ResponseModel(result, listOfKeys);
            // Complete the promise with the populated resultObject
            promise.complete(responseModel);
        }).onFailure(failure -> {
            // Fail the promise with an error message in case of failure
            promise.fail("Failed to retrieve values from Redis: " + failure.getMessage());
        });

        // Return the future to handle the result asynchronously
        return promise.future();
    }

    @Override
    public Future<JsonObject> insertIntoRedis(String key, JsonObject jsonObject) {
        Promise<JsonObject> promise = Promise.promise();
        List<String> args = buildJsonSetArgs(key, jsonObject);

        redisAPI.jsonSet(args).onSuccess(res -> {
            LOGGER.info("Successfully inserted JSON value in Redis for key: {}", key);
            promise.complete(new JsonObject().put("status", "success"));
        }).onFailure(err -> {
            LOGGER.error("Failed to insert JSON value in Redis for key {}: {}", key, err.getMessage());
            promise.fail("Failed to set key in Redis: " + err);
        });

        return null;
    }
}
