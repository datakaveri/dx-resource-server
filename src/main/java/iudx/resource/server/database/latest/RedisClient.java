package iudx.resource.server.database.latest;

import static iudx.resource.server.database.archives.Constants.FAILED;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.RedisReplicas;
import iudx.resource.server.database.archives.ResponseBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RedisClient {
  private static final Logger LOGGER = LogManager.getLogger(RedisClient.class);
  // private Redis redisClient;
  private ResponseBuilder responseBuilder;
  private Redis clusteredClient;
  private RedisAPI redis;
  private Vertx vertx;
  private JsonObject config;

  /**
   * RedisClient - Redis vertx Client Low Level Wrapper
   *
   * @param vertx Vertx Instance
   * @param config configuration of redis
   */
  public RedisClient(Vertx vertx, JsonObject config) {
    this.vertx = vertx;
    this.config = config;
  }

  public Future<RedisClient> start() {
    Promise<RedisClient> promise = Promise.promise();
    StringBuilder redisuri = new StringBuilder();
    RedisOptions options = null;
    redisuri
        .append("redis://")
        .append(config.getString("redisUsername"))
        .append(":")
        .append(config.getString("redisPassword"))
        .append("@")
        .append(config.getString("redisHost"))
        .append(":")
        .append(config.getInteger("redisPort").toString());
    String mode = config.getString("redisMode");

    if (mode.equals("CLUSTER")) {
      options =
          new RedisOptions().setType(RedisClientType.CLUSTER).setUseReplicas(RedisReplicas.SHARE);
    } else if (mode.equals("STANDALONE")) {
      options = new RedisOptions().setType(RedisClientType.STANDALONE);
    } else {
      LOGGER.error("Invalid/Unsupported mode");
      promise.fail("Invalid/Unsupported mode");
      return promise.future();
    }
    options
        .setMaxWaitingHandlers(config.getInteger("redisMaxWaitingHandlers"))
        .setConnectionString(redisuri.toString());
    clusteredClient = Redis.createClient(vertx, options);
    clusteredClient.connect(
        conn -> {
          redis = RedisAPI.api(conn.result());
          promise.complete(this);
        });
    return promise.future();
  }

  /**
   * searchAsync - Wrapper around Redis async search requests.
   *
   * @param key Redis Key
   * @param pathParam Path Parameter for Redis Nested JSON object
   * @param searchHandler JsonObject result {@link AsyncResult}
   */
  public RedisClient searchAsync(
      String key, String pathParam, Handler<AsyncResult<JsonObject>> searchHandler) {
    // using get command
    get(key, pathParam)
        .onComplete(
            resultRedis -> {
              if (resultRedis.succeeded()) {
                LOGGER.debug("Key found!");
                JsonObject fromRedis = resultRedis.result();
                searchHandler.handle(Future.succeededFuture(fromRedis));
              } else {
                LOGGER.error("Redis Error: " + resultRedis.cause());
                responseBuilder =
                    new ResponseBuilder(FAILED)
                        .setTypeAndTitle(204)
                        .setMessage(resultRedis.cause().getLocalizedMessage());
                searchHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
              }
            });

    return this;
  }

  /**
   * get - Redis vertx JSON.GET wrapper
   *
   * @param key Redis Key returns Future Object with (JSON) result from Redis
   */
  public Future<JsonObject> get(String key) {
    return get(key, ".");
  }

  /**
   * get - Redis vertx JSON.GET wrapper, overridden method to include path parameter
   *
   * @param key Redis Key
   * @param path Redis Path parameter returns Future Object with (JSON) result from Redis
   */
  public Future<JsonObject> get(String key, String path) {
    Promise<JsonObject> promise = Promise.promise();
    redis
        .send(Command.JSON_GET, key, path)
        .onFailure(
            res -> {
              promise.fail(String.format("JSONGET did not work: %s", res.getMessage()));
            })
        .onSuccess(
            redisResponse -> {
              if (redisResponse == null) {
                promise.fail(String.format(" %s key not found", key));
              } else {
                promise.complete(new JsonObject(redisResponse.toString()));
              }
            });

    return promise.future();
  }

  public void close() {
    redis.close();
  }
}
