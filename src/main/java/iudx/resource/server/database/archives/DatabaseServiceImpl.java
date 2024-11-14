package iudx.resource.server.database.archives;

import static iudx.resource.server.database.archives.Constants.*;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.cache.cachelmpl.CacheType;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.database.elastic.ElasticClient;
import iudx.resource.server.database.elastic.QueryDecoder;
import iudx.resource.server.database.elastic.exception.EsQueryException;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Database Service Implementation.
 *
 * <h1>Database Service Implementation</h1>
 *
 * <p>The Database Service implementation in the IUDX Resource Server implements the definitions of
 * the {@link iudx.resource.server.database.archives.DatabaseService}.
 *
 * @version 1.0
 * @since 2020-05-31
 */
public class DatabaseServiceImpl implements DatabaseService {

  private static final Logger LOGGER = LogManager.getLogger(DatabaseServiceImpl.class);
  static ElasticClient client;
  static CacheService cacheService;
  private QueryDecoder queryDecoder = new QueryDecoder();
  private String timeLimit;
  private String tenantPrefix;

  public DatabaseServiceImpl(
      ElasticClient client, String timeLimit, String tenantPrefix, CacheService cacheService) {
    this.client = client;
    this.timeLimit = timeLimit;
    this.tenantPrefix = tenantPrefix;
    this.cacheService = cacheService;
  }

  public int getOrDefault(JsonObject json, String key, int def) {
    if (json.containsKey(key)) {
      int value = Integer.parseInt(json.getString(key));
      return value;
    }
    return def;
  }

  @Override
  public Future<JsonObject> search(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    request.put(TIME_LIMIT, timeLimit);
    try {
      Future<JsonObject> validationFuture = checkQuery(request);
      validationFuture
          .onFailure(
              handler -> {
                promise.fail(handler.toString());
              })
          .onSuccess(
              handler -> {
                String resourceGroup = handler.getString("resourceGroup");
                StringBuilder tenantBuilder = new StringBuilder(tenantPrefix);
                final String searchIndex;
                if (!this.tenantPrefix.equals("none")) {
                  searchIndex = String.valueOf(tenantBuilder.append("__").append(resourceGroup));
                } else {
                  searchIndex = resourceGroup;
                }
                // String searchIndex = tenantBuilder;
                final int sizeKeyValue = getOrDefault(request, PARAM_SIZE, DEFAULT_SIZE_VALUE);
                final int fromKeyValue = getOrDefault(request, PARAM_FROM, DEFAULT_FROM_VALUE);

                Query query = queryDecoder.getQuery(request);
                LOGGER.info("query : " + query.toString());
                CountResultPlaceholder countPlaceHolder = new CountResultPlaceholder();
                Future<JsonObject> countFuture = client.asyncCount(searchIndex, query);
                countFuture
                    .compose(
                        countQueryHandler -> {
                          long count =
                              countQueryHandler
                                  .getJsonArray("results")
                                  .getJsonObject(0)
                                  .getInteger("totalHits");
                          LOGGER.info("count : " + count);
                          if (count > 50000) {
                            JsonObject json = new JsonObject();
                            json.put("type", 413);
                            json.put("title", ResponseUrn.PAYLOAD_TOO_LARGE_URN.getUrn());
                            json.put("details", ResponseUrn.PAYLOAD_TOO_LARGE_URN.getMessage()+ " use filters to get value within limit or use async api");
                            return Future.failedFuture(json.toString());
                          }
                          countPlaceHolder.setCount(count);
                          SourceConfig sourceFilter = queryDecoder.getSourceConfigFilters(request);
                          return client.asyncSearch(
                              searchIndex, query, sizeKeyValue, fromKeyValue, sourceFilter);
                        })
                    .onSuccess(
                        successHandler -> {
                          LOGGER.debug("Success: Successful DB request");
                          JsonObject responseJson = successHandler;
                          responseJson
                              .put(PARAM_SIZE, sizeKeyValue)
                              .put(PARAM_FROM, fromKeyValue)
                              .put("totalHits", countPlaceHolder.getCount());
                          promise.complete(responseJson);
                        })
                    .onFailure(
                        failureHandler -> {
                          LOGGER.info("failed to query : " + failureHandler);
                          promise.fail(failureHandler.getMessage());
                        });
              });
      // TODO : we can use ServiceException here, check for feasibility
    } catch (EsQueryException ex) {
      ResponseUrn exceptionUrn = ResponseUrn.BAD_REQUEST_URN;
      promise.fail(new EsQueryException(exceptionUrn, ex.getMessage()).toString());
    } catch (Exception ex) {
      promise.fail(new EsQueryException("Exception occured executing query").toString());
    }
    return promise.future();
  }

  @Override
  public Future<JsonObject> count(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();

    request.put(TIME_LIMIT, timeLimit);
    try {
      Future<JsonObject> validationFuture = checkQuery(request);
      validationFuture
          .onFailure(
              handler -> {
                promise.fail(handler.toString());
              })
          .onSuccess(
              handler -> {
                String searchType = request.getString(SEARCH_TYPE);
                if (searchType.matches(RESPONSE_FILTER_REGEX)) {
                  promise.fail(
                      new EsQueryException(
                          ResponseUrn.BAD_REQUEST_URN, "Count is not supported with filtering"));
                  return;
                }
                final String searchIndex;
                String resourceGroup = handler.getString("resourceGroup");
                StringBuilder tenantBuilder = new StringBuilder(tenantPrefix);
                if (!this.tenantPrefix.equals("none")) {
                  searchIndex = String.valueOf(tenantBuilder.append("__").append(resourceGroup));
                } else {
                  searchIndex = resourceGroup;
                }
                Query query = queryDecoder.getQuery(request);
                LOGGER.info("query : " + query.toString());
                Future<JsonObject> countFuture = client.asyncCount(searchIndex, query);
                countFuture
                    .onSuccess(
                        success -> {
                          promise.complete(success);
                        })
                    .onFailure(
                        failure -> {
                          promise.fail(failure.getMessage());
                        });
              });
    } catch (EsQueryException ex) {
      ResponseUrn exceptionUrn = ResponseUrn.BAD_REQUEST_URN;
      promise.fail(new EsQueryException(exceptionUrn, ex.getMessage()).toString());
    } catch (Exception ex) {
      promise.fail(new EsQueryException("Exception occured executing query").toString());
    }
    return promise.future();
  }

  public Future<JsonObject> checkQuery(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject cacheRequest = new JsonObject();
    cacheRequest.put("type", CacheType.CATALOGUE_CACHE);
    cacheRequest.put("key", request.getJsonArray(ID).getString(0));
    Future<JsonObject> getItemType = cacheService.get(cacheRequest);
    getItemType.onSuccess(
        itemType -> {
          Set<String> type = new HashSet<String>(itemType.getJsonArray("type").getList());
          Set<String> itemTypeSet =
              type.stream().map(e -> e.split(":")[1]).collect(Collectors.toSet());
          itemTypeSet.retainAll(ITEM_TYPES);

          if (!itemTypeSet.contains("Resource")) {
            LOGGER.error("Malformed ID: " + request.getJsonArray(ID).getString(0));
            promise.fail(new EsQueryException(ResponseUrn.BAD_REQUEST_URN, MALFORMED_ID));
          } else {
            promise.complete(itemType);
          }
        });

    return promise.future();
  }

  private class CountResultPlaceholder {
    private long count;

    public CountResultPlaceholder() {
      this.count = 0L;
    }

    public long getCount() {
      return this.count;
    }

    public void setCount(long count) {
      this.count = count;
    }
  }
}
