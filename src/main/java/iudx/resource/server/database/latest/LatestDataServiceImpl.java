package iudx.resource.server.database.latest;

import static iudx.resource.server.database.archives.Constants.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.cache.cachelmpl.CacheType;
import iudx.resource.server.database.archives.ResponseBuilder;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The LatestData Service Implementation.
 *
 * <h1>LatestData Service Implementation</h1>
 *
 * <p>The LatestData Service implementation in the IUDX Resource Server implements the definitions
 * of the {@link iudx.resource.server.database.latest.LatestDataService}.
 *
 * @version 1.0
 * @since 2021-03-26
 */
public class LatestDataServiceImpl implements LatestDataService {

  private static final Logger LOGGER = LogManager.getLogger(LatestDataServiceImpl.class);
  private final CacheService cache;
  RedisClient redisClient;
  JsonObject attributeList;
  private ResponseBuilder responseBuilder;
  private String tenantPrefix;
  // private RedisAPI redisAPI;
  private RedisCommandArgsBuilder redisCmdBuilder = new RedisCommandArgsBuilder();

  public LatestDataServiceImpl(
      RedisClient client, final CacheService cacheService, String tenantPrefix) {
    this.redisClient = client;
    this.cache = cacheService;
    this.tenantPrefix = tenantPrefix;
  }

  /**
   * Performs a Latest search query using the Redis JReJSON client.
   *
   * @param request Json object received from the ApiServerVerticle
   * @param handler Handler to return redis response in case of success and appropriate error
   *     message in case of failure
   */
  @Override
  public LatestDataService getLatestData(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    request.put(ATTRIBUTE_LIST, attributeList);

    // Exceptions
    if (!request.containsKey(ID)) {
      LOGGER.debug("Info: " + ID_NOT_FOUND);
      responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(ID_NOT_FOUND);
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return null;
    }

    if (request.getJsonArray(ID).isEmpty()) {
      LOGGER.debug("Info: " + EMPTY_RESOURCE_ID);
      responseBuilder =
          new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(EMPTY_RESOURCE_ID);
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return null;
    }
    String id = request.getJsonArray(ID).getString(0);

    isUniqueAttrRecordExist(id)
        .onComplete(
            uaHandler -> {
              if (uaHandler.succeeded()) {
                LOGGER.debug("unique_attribute for id :" + id + " is :" + uaHandler.result());
                getLatestValue(id, true, handler);
              } else {
                getLatestValue(id, false, handler);
              }
            });
    return this;
  }

  private JsonArray extractValues(String key, JsonObject result, boolean groupSnapshot) {
    if (groupSnapshot) {
      result.remove(key);
      return new JsonArray(result.stream().map(e -> e.getValue()).collect(Collectors.toList()));
    }
    return new JsonArray().add(result);
  }

  private void getLatestValue(
      final String id,
      final boolean isUniqueAttrRecordExist,
      Handler<AsyncResult<JsonObject>> handler) {

    RedisArgs args =
        redisCmdBuilder.getRedisCommandArgs(id, isUniqueAttrRecordExist, this.tenantPrefix);

    LOGGER.debug("key : " + args.getKey() + " path : " + args.getPath());
    JsonArray response = new JsonArray();
    redisClient.searchAsync(
        args.getKey(),
        args.getPath(),
        searchRes -> {
          if (searchRes.succeeded()) {
            LOGGER.debug("Success: Successful Redis request");
            response.addAll(
                extractValues(args.getKey(), searchRes.result(), isUniqueAttrRecordExist));
            responseBuilder =
                new ResponseBuilder(SUCCESS).setTypeAndTitle(200).setMessage(response);
            handler.handle(Future.succeededFuture(responseBuilder.getResponse()));
          } else {
            LOGGER.error("Fail: Redis Cache Request;" + searchRes.cause().getMessage());
            handler.handle(Future.failedFuture(searchRes.cause().getMessage()));
          }
        });
  }

  public Future<JsonObject> isUniqueAttrRecordExist(String id) {
    Promise<JsonObject> promise = Promise.promise();

    JsonObject requestJson = new JsonObject();
    requestJson.put("type", CacheType.UNIQUE_ATTRIBUTE);
    requestJson.put("key", id);

    Future<JsonObject> cacheFuture = cache.get(requestJson);
    cacheFuture
        .onSuccess(
            successHandler -> {
              promise.complete(successHandler);
            })
        .onFailure(
            failureHandler -> {
              LOGGER.info("unique attribute doesn't exist for id : " + id);
              promise.fail("unique attribute doesn't exist for id : " + id);
            });
    return promise.future();
  }
}
