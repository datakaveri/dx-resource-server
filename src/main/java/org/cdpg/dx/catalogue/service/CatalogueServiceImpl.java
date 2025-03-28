package org.cdpg.dx.catalogue.service;

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
import org.cdpg.dx.catalogue.client.CatalogueClient;

public class CatalogueServiceImpl implements CatalogueService {
  private static final Logger LOGGER = LogManager.getLogger(CatalogueServiceImpl.class);
  private final Cache<String, JsonObject> catalogueCache =
      CacheBuilder.newBuilder().maximumSize(10000).expireAfterWrite(1L, TimeUnit.DAYS).build();
  private final CatalogueClient catalogueClient;

  public CatalogueServiceImpl(Vertx vertx, CatalogueClient catalogueClient) {
    this.catalogueClient = catalogueClient;
    refreshCatalogue();
    vertx.setPeriodic(
        TimeUnit.HOURS.toMillis(1),
        handler -> {
          refreshCatalogue();
        });
  }

  @Override
  public Future<JsonObject> fetchCatalogueInfo(String id) {
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.trace("request for id : {}", id);
    if (catalogueCache.getIfPresent(id) != null) {
      return Future.succeededFuture(catalogueCache.getIfPresent(id));
    } else {
      idCatalogueInfo(id)
          .onSuccess(
              successHandler -> {
                if (catalogueCache.getIfPresent(id) != null) {
                  promise.complete(catalogueCache.getIfPresent(id));
                } else {
                  LOGGER.info("id :{} not found in catalogue server", id);
                  promise.fail(new ServiceException(ERROR_NOT_FOUND, BAD_REQUEST_ERROR));
                }
              })
          .onFailure(promise::fail);
    }
    return promise.future();
  }

  public Future<Void> refreshCatalogue() {
    LOGGER.trace("refresh catalogue() called");
    Promise<Void> promise = Promise.promise();
    catalogueClient
        .fetchCatalogueData()
        .onSuccess(
            successHandler -> {
              catalogueCache.invalidateAll();
              successHandler.forEach(
                  result -> {
                    JsonObject res = (JsonObject) result;
                    String rsId = res.getString("id");
                    catalogueCache.put(rsId, res);
                  });
              promise.complete();
            })
        .onFailure(
            failure -> {
              LOGGER.error("Failed to refresh catalogue",failure);
              promise.fail(new ServiceException(ERROR_BAD_REQUEST, "Failed to refresh catalogue"));
            });
    return promise.future();
  }

  public Future<Void> idCatalogueInfo(String id) {
    LOGGER.trace("id ::{}", id);
    Promise<Void> promise = Promise.promise();
    catalogueClient
        .getCatalogueInfoForId(id)
        .onSuccess(
            successHandler -> {
              successHandler.forEach(
                  result -> {
                    JsonObject res = (JsonObject) result;
                    catalogueCache.put(id, res);
                  });
              promise.complete();
            })
        .onFailure(
            failure -> {
              LOGGER.error("Failed to found id catalogue");
              promise.fail(
                  new ServiceException(ERROR_BAD_REQUEST, "Failed to search in catalogue"));
            });
    return promise.future();
  }
}
