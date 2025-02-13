package iudx.resource.server.cache.service.type;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import iudx.resource.server.cache.util.CacheType;
import iudx.resource.server.cache.util.CacheValue;
import iudx.resource.server.cache.util.IudxCache;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CatalogueCacheImpl implements IudxCache {

  private static final Logger LOGGER = LogManager.getLogger(CatalogueCacheImpl.class);
  private static final CacheType cacheType = CacheType.CATALOGUE_CACHE;
  private final Cache<String, CacheValue<JsonObject>> cache =
      CacheBuilder.newBuilder().maximumSize(5000).expireAfterWrite(1L, TimeUnit.DAYS).build();
  private WebClient catWebClient;
  private String catHost;
  private int catPort;
  private String catBasePath;

  public CatalogueCacheImpl(
      Vertx vertx, String catHost, int catPort, String catBasePath, WebClient catWebClient) {
    this.catHost = catHost;
    this.catPort = catPort;
    this.catBasePath = catBasePath;
    this.catWebClient = catWebClient;

    refreshCache();
    vertx.setPeriodic(
        TimeUnit.HOURS.toMillis(1),
        handler -> {
          refreshCache();
        });
  }

  @Override
  public Future<Void> put(String key, CacheValue<JsonObject> value) {
    throw new RuntimeException(
        "Adding elements in cache are not allowed, only refresh can be used");
  }

  @Override
  public Future<CacheValue<JsonObject>> get(String key) {
    LOGGER.trace("request for id : {}", key);
    Promise<CacheValue<JsonObject>> promise = Promise.promise();
    if (cache.getIfPresent(key) != null) {
      return Future.succeededFuture(cache.getIfPresent(key));
    } else {
      populateCache()
          .onSuccess(
              successHandler -> {
                if (cache.getIfPresent(key) != null) {
                  promise.complete(cache.getIfPresent(key));
                } else {
                  LOGGER.info("key :{} not found in cache/catatlgue server", key);
                  promise.fail("key not found");
                }
              })
          .onFailure(
              failureHandler -> {
                promise.fail("Value not found");
              });
    }
    return promise.future();
  }

  @Override
  public Future<Void> refreshCache() {
    populateCache();
    LOGGER.trace(cacheType + " refreshCache() called");
    return Future.succeededFuture();
  }

  private Future<Void> populateCache() {
    LOGGER.debug("refresh() cache started");
    Promise<Void> promise = Promise.promise();
    String url = catBasePath + "/search";
    catWebClient
        .get(catPort, catHost, url)
        .addQueryParam("property", "[itemStatus]")
        .addQueryParam("value", "[[ACTIVE]]")
        .addQueryParam(
            "filter",
            "[id,provider,name,description,label,accessPolicy,type,"
                + "iudxResourceAPIs,instance,resourceGroup]")
        .expect(ResponsePredicate.JSON)
        .send(
            catHandler -> {
              if (catHandler.succeeded()) {
                JsonArray response = catHandler.result().bodyAsJsonObject().getJsonArray("results");
                cache.invalidateAll();
                response.forEach(
                    json -> {
                      JsonObject res = (JsonObject) json;
                      String id = res.getString("id");
                      CacheValue<JsonObject> cacheValue = createCacheValue(id, res.toString());
                      cache.put(id, cacheValue);
                    });
                LOGGER.debug("refresh() cache completed");
                promise.complete();
              } else if (catHandler.failed()) {
                LOGGER.error("Failed to populate catalogue cache");
                promise.fail("Failed to populate catalogue cache");
              }
            });

    return promise.future();
  }

  @Override
  public CacheValue<JsonObject> createCacheValue(String key, String value) {
    return new CacheValue<JsonObject>() {
      @Override
      public JsonObject getValue() {
        return new JsonObject(value);
      }
    };
  }
}
