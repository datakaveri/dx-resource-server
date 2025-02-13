package iudx.resource.server.cache.service.type;

import static iudx.resource.server.cache.util.Constants.SELECT_REVOKE_TOKEN_SQL;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.cache.util.CacheType;
import iudx.resource.server.cache.util.CacheValue;
import iudx.resource.server.cache.util.IudxCache;
import iudx.resource.server.database.postgres.service.PostgresService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RevokedClientCache implements IudxCache {

  private static final Logger LOGGER = LogManager.getLogger(RevokedClientCache.class);
  private static final CacheType cacheType = CacheType.REVOKED_CLIENT;

  private final Cache<String, CacheValue<JsonObject>> cache =
      CacheBuilder.newBuilder().maximumSize(5000).expireAfterWrite(1L, TimeUnit.DAYS).build();

  private PostgresService pgService;

  public RevokedClientCache(Vertx vertx, PostgresService postgresService) {
    this.pgService = postgresService;
    refreshCache();

    vertx.setPeriodic(
        TimeUnit.HOURS.toMillis(1),
        handler -> {
          refreshCache();
        });
  }

  @Override
  public Future<Void> put(String key, CacheValue<JsonObject> value) {
    cache.put(key, value);
    return Future.succeededFuture();
  }

  @Override
  public Future<CacheValue<JsonObject>> get(String key) {
    if (cache.getIfPresent(key) != null) {
      return Future.succeededFuture(cache.getIfPresent(key));
    } else {
      return Future.failedFuture("Value not found");
    }
  }

  @Override
  public Future<Void> refreshCache() {
    LOGGER.trace(cacheType + " refreshCache() called");
    Promise<Void> promise = Promise.promise();
    String query = SELECT_REVOKE_TOKEN_SQL;
    pgService
        .executeQuery(query)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                JsonArray clientIdArray = handler.result().getJsonArray("result");
                cache.invalidateAll();
                clientIdArray.forEach(
                    e -> {
                      JsonObject clientInfo = (JsonObject) e;
                      String key = clientInfo.getString("_id");
                      String expiry = clientInfo.getString("expiry");
                      CacheValue<JsonObject> cacheValue = createCacheValue(key, expiry);
                      this.cache.put(key, cacheValue);
                    });
                promise.complete();
              } else {
                promise.fail("failed to refresh");
              }
            });
    return promise.future();
  }

  @Override
  public CacheValue<JsonObject> createCacheValue(String key, String expiry) {
    return new CacheValue<JsonObject>() {
      @Override
      public JsonObject getValue() {
        JsonObject value = new JsonObject();
        value.put("id", key);
        value.put("expiry", expiry);
        value.put("value", expiry);
        return value;
      }
    };
  }
}
