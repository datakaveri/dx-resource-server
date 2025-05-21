package org.cdpg.dx.database.redis.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.redis.client.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.common.exception.*;
import org.cdpg.dx.database.redis.model.RedisResponseModel;

import java.util.List;
import java.util.concurrent.TimeoutException;
import static org.cdpg.dx.common.exception.DxErrorCodes.*;

public class RedisServiceImpl implements RedisService {
    private static final Logger LOGGER = LogManager.getLogger(RedisServiceImpl.class);
    private final RedisAPI redisAPI;

    public RedisServiceImpl(Vertx vertx, RedisOptions options) {
        try {
            Redis redisClient = Redis.createClient(vertx, options);
            this.redisAPI = RedisAPI.api(redisClient);

            // Verify connection is operational with PING
            this.redisAPI.ping(List.of())
                    .onSuccess(response -> {
                        LOGGER.info("CONNECTION SUCCESSFUL - Redis server responded");
                    })
                    .onFailure(error -> {
                        LOGGER.error("Connection established but Redis server not responding: {}", error.getMessage());
                        throw new RedisConnectionException(
                                CONNECTION_ERROR,
                                "Redis server not responding: " + error.getMessage()
                        );
                    });
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Redis client: {}", e.getMessage());
            throw new RedisConnectionException(
                    CONNECTION_ERROR,
                    "Failed to initialize Redis connection: " + e.getMessage()
            );
        }

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
        LOGGER.info("Searching for key {} in Redis", key);
        LOGGER.info("Path {}",path);
        // Validate inputs
        if (key == null || key.trim().isEmpty()) {
            return Future.failedFuture(new RedisOperationException(
                    OPERATION_FAILED,
                    "Redis key cannot be null or empty"
            ));
        }

        if (path == null) {
            return Future.failedFuture(new InvalidJsonPathException(
                    INVALID_JSON_PATH,
                    "Invalid JSON path provided: " + path
            ));
        }

        redisAPI.send(Command.JSON_GET, key, "path")
                .onSuccess(redisResponse -> {
                    if (redisResponse == null) {
                        promise.fail(new RedisKeyNotFoundException(
                                KEY_NOT_FOUND,
                                "No data found for key: " + key
                        ));
                    } else {
                        try {
                            LOGGER.debug("Retrieved key {} from Redis", key);
                            RedisResponseModel responseModel = new RedisResponseModel(redisResponse);
                            promise.complete(responseModel);
                        } catch (Exception e) {
                            promise.fail(new RedisOperationException(
                                    INVALID_RESPONSE,
                                    "Failed to process Redis response: " + e.getMessage()
                            ));
                        }
                    }
                })
                .onFailure(err -> {

                    LOGGER.error("Failed to get key {} from Redis: {}", key, err);
                    if (err instanceof RedisConnectionException) {
                        promise.fail(err);
                    } else if (err instanceof TimeoutException) {
                        promise.fail(new RedisOperationException(
                                REDIS_TIMEOUT,
                                "Operation timed out for key: " + key
                        ));
                    } else {
                        promise.fail(new RedisOperationException(
                                OPERATION_FAILED,
                                "Failed to get key from Redis: " + err.getMessage()
                        ));
                    }
                });

        return promise.future();
    }
}
