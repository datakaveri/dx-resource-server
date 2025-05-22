package org.cdpg.dx.revoked.service;

import static org.cdpg.dx.common.ErrorMessage.BAD_REQUEST_ERROR;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.common.exception.DxNotFoundException;
import org.cdpg.dx.revoked.client.RevokedClient;

public class RevokedServiceImpl implements RevokedService {
  private static final Logger LOGGER = LogManager.getLogger(RevokedServiceImpl.class);
  private final Cache<String, JsonObject> revokedCache =
      CacheBuilder.newBuilder().maximumSize(10000).expireAfterWrite(1L, TimeUnit.DAYS).build();
  private final RevokedClient revokedClient;

  public RevokedServiceImpl(Vertx vertx, RevokedClient revokedClient) {
    this.revokedClient = revokedClient;
    refreshRevoked();
    vertx.setPeriodic(
        TimeUnit.HOURS.toMillis(1),
        handler -> {
          refreshRevoked();
        });
  }

  @Override
  public Future<JsonObject> fetchRevokedInfo(String id) {
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.trace("request for id : {}", id);
    if (revokedCache.getIfPresent(id) != null) {
      return Future.succeededFuture(revokedCache.getIfPresent(id));
    } else {
      refreshRevoked()
          .onSuccess(
              successHandler -> {
                if (revokedCache.getIfPresent(id) != null) {
                  promise.complete(revokedCache.getIfPresent(id));
                } else {
                  LOGGER.info("id :{} not found", id);
                  promise.fail(new DxNotFoundException(BAD_REQUEST_ERROR));
                }
              })
          .onFailure(promise::fail);
    }
    return promise.future();
  }

    @Override
    public Future<Void> putRevokedInCache(String id, String value) {
        Promise<Void> promise = Promise.promise();
        if (id != null && value != null) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.put("id", id);
            jsonObject.put("expiry", value);
            jsonObject.put("value", value);
            revokedCache.put(id, jsonObject);
            promise.complete();
        } else {
            promise.fail(new DxNotFoundException(BAD_REQUEST_ERROR));
        }
        return promise.future();
    }

    // TODO :: need to revisit code once postgres model will be available
  public Future<Void> refreshRevoked() {
    LOGGER.trace("refresh catalogue() called");
    Promise<Void> promise = Promise.promise();
    revokedClient
        .fetchRevokedData()
        .onSuccess(
            successHandler -> {
              revokedCache.invalidateAll();
              successHandler.forEach(
                  result -> {
                    String rsId = result.getString("_id");
                    String expiry = result.getString("expiry");
                    JsonObject res = new JsonObject();
                    res.put("id", rsId);
                    res.put("expiry", expiry);
                    res.put("value", expiry);
                    revokedCache.put(rsId, res);
                  });
              promise.complete();
            })
        .onFailure(
            failure -> {
              LOGGER.error("Failed to refresh", failure);
              promise.fail(failure);
            });
    return promise.future();
  }
}
