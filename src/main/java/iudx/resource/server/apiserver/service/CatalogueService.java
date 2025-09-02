package iudx.resource.server.apiserver.service;

import static iudx.resource.server.apiserver.util.Util.toList;
import static iudx.resource.server.database.archives.Constants.*;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.cache.cachelmpl.CacheType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** catalogue service to fetch calatogue items and groups for the purpose of cache */
public class CatalogueService {

  private static final Logger LOGGER = LogManager.getLogger(CatalogueService.class);
  // private final Cache<String, List<String>> applicableFilterCache =
  // CacheBuilder.newBuilder().maximumSize(1000)
  // .expireAfterAccess(Constants.CACHE_TIMEOUT_AMOUNT, TimeUnit.MINUTES).build();

  private CacheService cacheService;

  public CatalogueService(CacheService cacheService) {
    //    catSearchPath = Constants.CAT_RSG_PATH;
    this.cacheService = cacheService;
    // WebClientOptions options =
    // new WebClientOptions().setTrustAll(true).setVerifyHost(false).setSsl(true);
    // if(catWebClient == null)
    // {
    // catWebClient = WebClient.create(vertx, options);
    // }
    // populateCache();
    // cacheTimerid = vertx.setPeriodic(TimeUnit.DAYS.toMillis(1), handler -> {
    // populateCache();
    // });
  }

  // private Future<Boolean> populateCache() {
  // Promise<Boolean> promise = Promise.promise();
  // catWebClient.get(catPort, catHost, catSearchPath)
  // .addQueryParam("property", "[iudxResourceAPIs]")
  // .addQueryParam("value", "[[TEMPORAL,ATTR,SPATIAL]]")
  // .addQueryParam("filter", "[iudxResourceAPIs,id]").expect(ResponsePredicate.JSON)
  // .send(handler -> {
  // if (handler.succeeded()) {
  // JsonArray response = handler.result().bodyAsJsonObject().getJsonArray("results");
  // response.forEach(json -> {
  // JsonObject res = (JsonObject) json;
  // String id = res.getString("id");
  // String[] idArray = id.split("/");
  // if (idArray.length == 4) {
  // applicableFilterCache.put(id + "/*", toList(res.getJsonArray("iudxResourceAPIs")));
  // } else {
  // applicableFilterCache.put(id, toList(res.getJsonArray("iudxResourceAPIs")));
  // }
  // });
  // promise.complete(true);
  // } else if (handler.failed()) {
  // promise.fail(handler.cause());
  // }
  // });
  // return promise.future();
  // }

  // public Future<List<String>> getApplicableFilters(String id) {
  // Promise<List<String>> promise = Promise.promise();
  // String groupId = id.substring(0, id.lastIndexOf("/"));
  // List<String> filters = cache.getIfPresent(id);
  // if (filters == null) {
  // // check for group if not present by item key.
  // filters = applicableFilterCache.getIfPresent(groupId + "/*");
  // }
  // if (filters == null) {
  // // filters = fetchFilters4Item(id, groupId);
  // fetchFilters4Item(id, groupId).onComplete(handler -> {
  // if (handler.succeeded()) {
  // promise.complete(handler.result());
  // } else {
  // promise.fail("failed to fetch filters.");
  // }
  // });
  // } else {
  // promise.complete(filters);
  // }
  // return promise.future();
  // }

  public Future<List<String>> getApplicableFilters(String id) {
    Promise<List<String>> promise = Promise.promise();
    JsonObject cacheRequests = new JsonObject();
    cacheRequests.put("type", CacheType.CATALOGUE_CACHE);
    cacheRequests.put("key", id);
    Future<JsonObject> groupIdFuture = cacheService.get(cacheRequests);
    groupIdFuture.onComplete(
        grpId -> {
          if (grpId.succeeded()) {
            Set<String> type = new HashSet<String>(grpId.result().getJsonArray("type").getList());
            Set<String> itemTypeSet =
                type.stream().map(e -> e.split(":")[1]).collect(Collectors.toSet());
            itemTypeSet.retainAll(ITEM_TYPES);

            String groupId;
            if (!itemTypeSet.contains("Resource")) {
              groupId = id;
            } else {
              groupId = grpId.result().getString("resourceGroup");
            }

            LOGGER.debug("groupId = " + groupId);
            JsonObject cacheRequest = new JsonObject();
            cacheRequest.put("type", CacheType.CATALOGUE_CACHE);
            cacheRequest.put("key", groupId);
            Future<JsonObject> groupFilter = cacheService.get(cacheRequest);
            JsonObject itemCacheRequest = cacheRequest.copy();
            itemCacheRequest.put("key", id);
            Future<JsonObject> itemFilters = cacheService.get(itemCacheRequest);
            List<String> filters = new ArrayList<String>();
            CompositeFuture.any(List.of(groupFilter, itemFilters))
                .onComplete(
                    ar -> {
                      if (ar.failed()) {
                        promise.fail("no filters available for : " + id);
                        return;
                      }
                      if (groupFilter.result() != null
                          && groupFilter.result().containsKey("iudxResourceAPIs")) {
                        filters.addAll(
                            toList(groupFilter.result().getJsonArray("iudxResourceAPIs")));
                        promise.complete(filters);
                      }

                      if (itemFilters.result() != null
                          && itemFilters.result().containsKey("iudxResourceAPIs")) {
                        filters.addAll(
                            toList(itemFilters.result().getJsonArray("iudxResourceAPIs")));
                        promise.complete(filters);
                      }
                    });
          } else {
            LOGGER.debug("Failed : " + grpId.cause().getMessage());
          }
        });
    return promise.future();
  }

  // private Future<List<String>> fetchFilters4Item(String id, String groupId) {
  // Promise<List<String>> promise = Promise.promise();
  // Future<List<String>> getItemFilters = getFilterFromItemId(id);
  // Future<List<String>> getGroupFilters = getFilterFromGroupId(groupId);
  // getItemFilters.onComplete(itemHandler -> {
  // if (itemHandler.succeeded()) {
  // List<String> filters4Item = itemHandler.result();
  // if (filters4Item.isEmpty()) {
  // // Future<List<String>> getGroupFilters = getFilterFromGroupId(groupId);
  // getGroupFilters.onComplete(groupHandler -> {
  // if (groupHandler.succeeded()) {
  // List<String> filters4Group = groupHandler.result();
  // applicableFilterCache.put(groupId + "/*", filters4Group);
  // promise.complete(filters4Group);
  // } else {
  // LOGGER.error(
  // "Failed to fetch applicable filters for id: " + id + "or group id : " + groupId);
  // }
  // });
  // } else {
  // applicableFilterCache.put(id, filters4Item);
  // promise.complete(filters4Item);
  // }
  // } else {
  //
  // }
  // });
  // return promise.future();
  // }
  //
  //
  // private Future<List<String>> getFilterFromGroupId(String groupId) {
  // Promise<List<String>> promise = Promise.promise();
  // callCatalogueAPI(groupId, handler -> {
  // if (handler.succeeded()) {
  // promise.complete(handler.result());
  // } else {
  // promise.fail("failed to fetch filters for group");
  // }
  // });
  // return promise.future();
  // }
  //
  // private Future<List<String>> getFilterFromItemId(String itemId) {
  // Promise<List<String>> promise = Promise.promise();
  // callCatalogueAPI(itemId, handler -> {
  // if (handler.succeeded()) {
  // promise.complete(handler.result());
  // } else {
  // promise.fail("failed to fetch filters for group");
  // }
  // });
  // return promise.future();
  // }
  //
  // private void callCatalogueAPI(String id, Handler<AsyncResult<List<String>>> handler) {
  // List<String> filters = new ArrayList<String>();
  // catWebClient.get(catPort, catHost, catItemPath).addQueryParam("id", id).send(catHandler -> {
  // if (catHandler.succeeded()) {
  // JsonArray response = catHandler.result().bodyAsJsonObject().getJsonArray("results");
  // response.forEach(json -> {
  // JsonObject res = (JsonObject) json;
  // if (res.containsKey("iudxResourceAPIs")) {
  // filters.addAll(toList(res.getJsonArray("iudxResourceAPIs")));
  // }
  // });
  // handler.handle(Future.succeededFuture(filters));
  // } else if (catHandler.failed()) {
  // LOGGER.error("catalogue call(/iudx/cat/v1/item) failed for id" + id);
  // handler.handle(Future.failedFuture("catalogue call(/iudx/cat/v1/item) failed for id" + id));
  // }
  // });
  // }

  public Future<Boolean> isItemExist(String id) {
    LOGGER.trace("isItemExist() started");
    Promise<Boolean> promise = Promise.promise();
    JsonObject cacheRequest = new JsonObject();
    cacheRequest.put("type", CacheType.CATALOGUE_CACHE);
    cacheRequest.put("key", id);
    Future<JsonObject> cacheFuture = cacheService.get(cacheRequest);
    cacheFuture
        .onSuccess(
            successHandler -> {
              promise.complete(true);
            })
        .onFailure(
            failureHandler -> {
              promise.fail("Item not found");
            });
    return promise.future();
  }

  // public Future<Boolean> isItemExist(String id) {
  // LOGGER.trace("isItemExist() started");
  // Promise<Boolean> promise = Promise.promise();
  // LOGGER.info("id : " + id);
  // catWebClient.get(catPort, catHost, catItemPath).addQueryParam("id", id)
  // .expect(ResponsePredicate.JSON).send(responseHandler -> {
  // if (responseHandler.succeeded()) {
  // HttpResponse<Buffer> response = responseHandler.result();
  // JsonObject responseBody = response.bodyAsJsonObject();
  // if (responseBody.getString("type").equalsIgnoreCase("urn:dx:cat:Success")
  // && responseBody.getInteger("totalHits") > 0) {
  // promise.complete(true);
  // } else {
  // promise.fail(responseHandler.cause());
  // }
  // } else {
  // promise.fail(responseHandler.cause());
  // }
  // });
  // return promise.future();
  // }

  public Future<Boolean> isItemExist(List<String> ids) {
    Promise<Boolean> promise = Promise.promise();
    List<Future> futures = new ArrayList<Future>();
    for (String id : ids) {
      futures.add(isItemExist(id));
    }
    CompositeFuture.all(futures)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                promise.complete(true);
              } else {
                promise.fail(handler.cause());
              }
            });
    return promise.future();
  }
}
