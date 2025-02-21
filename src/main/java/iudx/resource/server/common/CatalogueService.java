package iudx.resource.server.common;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import iudx.resource.server.cache.service.CacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CatalogueService {
  private static final Logger LOGGER = LogManager.getLogger(CatalogueService.class);
  static WebClient catWebClient;
  final int port;
  final String host;
  private final String catBasePath;
  private final CacheService cacheService;

  public CatalogueService(CacheService cacheService, JsonObject config, Vertx vertx) {
    this.host = config.getString("catServerHost");
    this.port = config.getInteger("catServerPort");
    this.cacheService = cacheService;
    this.catBasePath = config.getString("dxCatalogueBasePath");
    WebClientOptions options =
        new WebClientOptions().setTrustAll(true).setVerifyHost(false).setSsl(true);
    if (catWebClient == null) {
      catWebClient = WebClient.create(vertx, options);
    }
  }

  public Future<String> getProviderUserId(String id) {
    LOGGER.trace("getProviderUserId () started");
    String relationshipCatPath = catBasePath + "/relationship";
    Promise<String> promise = Promise.promise();
    LOGGER.debug("id: " + id);
    catWebClient
        .get(port, host, relationshipCatPath)
        .addQueryParam("id", id)
        .addQueryParam("rel", "provider")
        .expect(ResponsePredicate.JSON)
        .send(
            catHandler -> {
              if (catHandler.succeeded()) {
                JsonArray response = catHandler.result().bodyAsJsonObject().getJsonArray("results");
                response.forEach(
                    json -> {
                      JsonObject res = (JsonObject) json;
                      String providerUserId = null;
                      providerUserId = res.getString("providerUserId");
                      if (providerUserId == null) {
                        providerUserId = res.getString("ownerUserId");
                        LOGGER.info(" owneruserid : " + providerUserId);
                      }
                      promise.complete(providerUserId);
                    });

              } else {
                LOGGER.error(
                    "Failed to call catalogue  while getting provider user id {}",
                    catHandler.cause().getMessage());
              }
            });

    return promise.future();
  }
}
