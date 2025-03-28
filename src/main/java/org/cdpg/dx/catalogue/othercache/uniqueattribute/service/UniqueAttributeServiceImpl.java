package org.cdpg.dx.catalogue.othercache.uniqueattribute.service;

import static org.cdpg.dx.common.ErrorCode.ERROR_BAD_REQUEST;
import static org.cdpg.dx.common.ErrorCode.ERROR_NOT_FOUND;
import static org.cdpg.dx.common.ErrorMessage.BAD_REQUEST_ERROR;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceException;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.catalogue.othercache.uniqueattribute.client.UniqueAttributeClient;

public class UniqueAttributeServiceImpl implements UniqueAttributeService {
  private static final Logger LOGGER = LogManager.getLogger(UniqueAttributeServiceImpl.class);
  private final Cache<String, JsonObject> uniqueAttributeCache =
      CacheBuilder.newBuilder().maximumSize(10000).expireAfterWrite(1L, TimeUnit.DAYS).build();
  private final UniqueAttributeClient uniqueAttributeClient;

  public UniqueAttributeServiceImpl(Vertx vertx, UniqueAttributeClient uniqueAttributeClient) {
    this.uniqueAttributeClient = uniqueAttributeClient;
    refreshUniqueAttribute();
    vertx.setPeriodic(
        TimeUnit.HOURS.toMillis(1),
        handler -> {
          refreshUniqueAttribute();
        });
  }

  @Override
  public Future<JsonObject> fetchUniqueAttributeInfo(String id) {
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.trace("request for id : {}", id);
    if (uniqueAttributeCache.getIfPresent(id) != null) {
      return Future.succeededFuture(uniqueAttributeCache.getIfPresent(id));
    } else {
      refreshUniqueAttribute()
          .onSuccess(
              successHandler -> {
                if (uniqueAttributeCache.getIfPresent(id) != null) {
                  promise.complete(uniqueAttributeCache.getIfPresent(id));
                } else {
                  LOGGER.info("id :{} not found in catalogue server", id);
                  promise.fail(new ServiceException(ERROR_NOT_FOUND, BAD_REQUEST_ERROR));
                }
              })
          .onFailure(promise::fail);
    }
    return promise.future();
  }

  // TODO :: need to revisit code once postgres model will be available
  public Future<Void> refreshUniqueAttribute() {
    LOGGER.trace("refresh refreshUniqueAttribute() called");
    Promise<Void> promise = Promise.promise();
    uniqueAttributeClient
        .fetchUniqueAttribute()
        .onSuccess(
            successHandler -> {
              uniqueAttributeCache.invalidateAll();
              successHandler.forEach(
                  result -> {
                    String rsId = result.getString("resource_id");
                    String uniqueAttribute = result.getString("unique_attribute");
                    JsonObject res = new JsonObject();
                    res.put("resource_id", rsId);
                    res.put("key", rsId);
                    res.put("unique_attribute", uniqueAttribute);
                    res.put("value", uniqueAttribute);
                    uniqueAttributeCache.put(rsId, res);
                  });
              promise.complete();
            })
        .onFailure(
            failure -> {
              LOGGER.error("Failed to refresh", failure);
              promise.fail(new ServiceException(ERROR_BAD_REQUEST, "Failed to refresh"));
            });
    return promise.future();
  }
}
