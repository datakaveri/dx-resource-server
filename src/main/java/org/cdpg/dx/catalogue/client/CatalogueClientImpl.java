package org.cdpg.dx.catalogue.client;

import static org.cdpg.dx.common.ErrorCode.ERROR_NOT_FOUND;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.serviceproxy.ServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CatalogueClientImpl implements CatalogueClient {
  private static final Logger LOGGER = LogManager.getLogger(CatalogueClientImpl.class);
  private final WebClient webClient;
  private final String catHost;
  private final int catPort;
  private final String catBasePath;

  public CatalogueClientImpl(
      String catServerHost,
      Integer catServerPort,
      String dxCatalogueBasePath,
      WebClient webClient) {
    this.webClient = webClient;
    this.catHost = catServerHost;
    this.catPort = catServerPort;
    this.catBasePath = dxCatalogueBasePath;
  }

  @Override
  public Future<JsonArray> fetchCatalogueData() {
    Promise<JsonArray> promise = Promise.promise();
    populateCatalogue().onSuccess(promise::complete).onFailure(promise::fail);
    return promise.future();
  }

  private Future<JsonArray> populateCatalogue() {
    LOGGER.debug("refresh() cache started");
    Promise<JsonArray> promise = Promise.promise();
    String url = catBasePath + "/search";
    webClient
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
                LOGGER.debug("refresh() catalogue completed");
                promise.complete(response);
              } else if (catHandler.failed()) {
                LOGGER.error("Failed to populate catalogue cache");
                promise.fail(
                    new ServiceException(ERROR_NOT_FOUND, "Failed to populate catalogue cache"));
              }
            });
    return promise.future();
  }

  @Override
  public Future<JsonArray> getCatalogueInfoForId(String id) {
    LOGGER.debug("get cat info for id: {} ", id);
    Promise<JsonArray> promise = Promise.promise();
    String url = catBasePath + "/search";
    webClient
        .get(catPort, catHost, url)
        .addQueryParam("property", "[id]")
        .addQueryParam("value", "[[" + id + "]]")
        .addQueryParam(
            "filter",
            "[id,provider,name,description,label,accessPolicy,type,"
                + "iudxResourceAPIs,instance,resourceGroup]")
        .expect(ResponsePredicate.JSON)
        .send(
            catHandler -> {
              if (catHandler.succeeded()) {
                JsonArray response = catHandler.result().bodyAsJsonObject().getJsonArray("results");
                promise.complete(response);
              } else {
                LOGGER.error("catalogue call search api failed: " + catHandler.cause());
                promise.fail(new ServiceException(ERROR_NOT_FOUND, "Failed to call catalogue"));
              }
            });

    return promise.future();
  }
}
