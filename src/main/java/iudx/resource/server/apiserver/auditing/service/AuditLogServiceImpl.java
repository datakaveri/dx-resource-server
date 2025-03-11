package iudx.resource.server.apiserver.auditing.service;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import iudx.resource.server.apiserver.auditing.model.AuditLogSearchRequest;
import iudx.resource.server.apiserver.auditing.model.MonthlyOverviewResponse;
import iudx.resource.server.apiserver.auditing.model.OverviewRequest;
import iudx.resource.server.apiserver.auditing.util.ParamsValidation;
import iudx.resource.server.apiserver.auditing.util.QueryBuilder;
import iudx.resource.server.apiserver.auditing.util.ResponseBuilder;
import iudx.resource.server.apiserver.exception.DxRuntimeException;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.cache.util.CacheType;
import iudx.resource.server.common.Api;
import iudx.resource.server.database.postgres.service.PostgresService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.resource.server.apiserver.auditing.util.Constants.ERROR;
import static iudx.resource.server.apiserver.auditing.util.Constants.QUERY_KEY;
import static iudx.resource.server.apiserver.util.Constants.LIMITPARAM;
import static iudx.resource.server.apiserver.util.Constants.OFFSETPARAM;
import static iudx.resource.server.common.ResponseUrn.BAD_REQUEST_URN;
import static iudx.resource.server.database.elastic.Constants.FAILED;

public class AuditLogServiceImpl implements AuditLogService {
  private static final Logger LOGGER = LogManager.getLogger(AuditLogServiceImpl.class);

  private final PostgresService pgService;
  private final CacheService cache;
  private final QueryBuilder queryBuilder = new QueryBuilder();
  String summaryOverview;
  private ParamsValidation validation;
  private Api api;

  public AuditLogServiceImpl(PostgresService pgService, CacheService cache, Api api) {
    this.pgService = pgService;
    this.cache = cache;
    this.api = api;
    validation = new ParamsValidation(api);
  }

  @Override
  public Future<JsonObject> executeAuditingSearchQuery(AuditLogSearchRequest searchRequest) {
    LOGGER.trace("Info: Read Query {}", searchRequest.toJson());
    JsonObject request = searchRequest.toJson();

    JsonObject validationCheck = validation.paramsCheck(searchRequest.toJson());
    if (validationCheck != null && validationCheck.containsKey(ERROR)) {
      return Future.failedFuture(
          new DxRuntimeException(
              400, BAD_REQUEST_URN, validationCheck.getString(ERROR)));
    }

    String options = searchRequest.getOptions();

    return executeCountQuery(request)
        .compose(
            countResponse -> {
              int total = countResponse.getInteger("total", 0);
              request.put("totalHits", total);

              // If options = "count", return only count as response
              if ("count".equalsIgnoreCase(options)) {
                return Future.succeededFuture(
                    new ResponseBuilder("Success")
                        .setTypeAndTitle(200)
                        .setCount(total)
                        .getResponse());
              }
              // If total count is zero, return empty response
              if (total == 0) {
                return Future.succeededFuture(
                    new ResponseBuilder(FAILED).setTypeAndTitle(204).setCount(0).getResponse());
              }
              // Otherwise, fetch the full data
              return executeReadQueryForData(request);
            });
  }

  private Future<JsonObject> executeCountQuery(JsonObject request) {
    JsonObject query = queryBuilder.buildCountReadQueryFromPg(request);
    return executeQueryDatabaseOperation(query)
        .map(
            countHandler -> {
              JsonArray countResult = countHandler.getJsonArray("result");
              int total = 0;

              if (countResult != null && !countResult.isEmpty()) {
                JsonObject countObj = countResult.getJsonObject(0);
                total = countObj.getInteger("count", 0);
              }

              return new JsonObject().put("total", total);
            });
  }

  private Future<JsonObject> executeReadQueryForData(JsonObject request) {
    int limit;
    int offset;
    if (request.getString(LIMITPARAM) == null) {
      limit = 2000;
      request.put(LIMITPARAM, limit);
    } else {
      limit = Integer.parseInt(request.getString(LIMITPARAM));
    }
    if (request.getString(OFFSETPARAM) == null) {
      offset = 0;
      request.put(OFFSETPARAM, offset);
    } else {
      offset = Integer.parseInt(request.getString(OFFSETPARAM));
    }

    JsonObject query = queryBuilder.buildReadQueryFromPg(request);
    LOGGER.debug("Read query = {}", query);

    return executeQueryDatabaseOperation(query)
        .map(
            result -> {
              result.put(LIMITPARAM, limit);
              result.put(OFFSETPARAM, offset);
              result.put("totalHits", request.getInteger("totalHits"));
              return result;
            });
  }

  @Override
  public Future<List<MonthlyOverviewResponse>> monthlyOverview(OverviewRequest overviewRequest) {
    Promise<List<MonthlyOverviewResponse>> promise = Promise.promise();
    LOGGER.info("fetching monthlyOverview for role: {}", overviewRequest.getRole());

    if ("admin".equalsIgnoreCase(overviewRequest.getRole())
        || "consumer".equalsIgnoreCase(overviewRequest.getRole())) {

      String query = queryBuilder.buildMonthlyOverviewQueryNew(overviewRequest);

      executeQueryDatabaseOperation(new JsonObject().put(QUERY_KEY, query))
          .onSuccess(
              result -> {
                JsonArray jsonArray = result.getJsonArray("result");

                List<MonthlyOverviewResponse> monthlyOverviewResponses =
                    jsonArray.stream()
                        .map(obj -> new MonthlyOverviewResponse((JsonObject) obj))
                        .collect(Collectors.toList());
                promise.complete(monthlyOverviewResponses);
              });
    }
    if ("provider".equalsIgnoreCase(overviewRequest.getRole())
        || "delegate".equalsIgnoreCase(overviewRequest.getRole())) {
      fetchProviderId(overviewRequest.getIid())
          .compose(
              providerId -> {
                OverviewRequest request = overviewRequest.withUpdatedProviderId(providerId);
                return executeQueryDatabaseOperation(
                    new JsonObject()
                        .put(QUERY_KEY, queryBuilder.buildMonthlyOverviewQueryNew(request)));
              })
          .onSuccess(
              result -> {
                JsonArray jsonArray = result.getJsonArray("result");
                List<MonthlyOverviewResponse> monthlyOverviewResponses =
                    jsonArray.stream()
                        .map(obj -> new MonthlyOverviewResponse((JsonObject) obj))
                        .collect(Collectors.toList());
                promise.complete(monthlyOverviewResponses);
              });
    }
    return promise.future();
  }

  @Override
  public Future<JsonObject> summaryOverview(OverviewRequest overviewRequest) {

    String role = overviewRequest.getRole();
    if ("admin".equalsIgnoreCase(role) || "consumer".equalsIgnoreCase(role)) {
      return executeSummaryQuery(overviewRequest);
    } else if ("provider".equalsIgnoreCase(role) || "delegate".equalsIgnoreCase(role)) {
      return fetchProviderId(overviewRequest.getIid())
          .compose(
              providerId -> {
                OverviewRequest request = overviewRequest.withUpdatedProviderId(providerId);
                return executeSummaryQuery(request);
              });
    }
    return Future.failedFuture("Invalid Role");
  }

  private Future<JsonObject> executeSummaryQuery(OverviewRequest request) {
    summaryOverview = queryBuilder.buildSummaryOverview(request);
    LOGGER.debug("summary query = {}", summaryOverview);
    JsonObject query = new JsonObject().put(QUERY_KEY, summaryOverview);
    return executeQueryDatabaseOperation(query).compose(this::processSummaryResult);
  }

  private Future<String> fetchProviderId(String id) {
    JsonObject jsonObject = new JsonObject().put("type", CacheType.CATALOGUE_CACHE).put("key", id);

    return cache.get(jsonObject).map(providerHandler -> providerHandler.getString("provider"));
  }

  private Future<JsonObject> processSummaryResult(JsonObject result) {
    JsonArray jsonArray = result.getJsonArray("result");
    if (jsonArray == null || jsonArray.isEmpty()) {
      ResponseBuilder responseBuilder =
          new ResponseBuilder("not found").setTypeAndTitle(204).setMessage("NO ID Present");
      return Future.succeededFuture(responseBuilder.getResponse());
    }

    return cacheCall(jsonArray)
        .map(
            resultHandler -> {
              return new JsonObject()
                  .put("type", "urn:dx:rs:Success")
                  .put("title", "Success")
                  .put("results", resultHandler);
            });
  }

  public Future<JsonArray> cacheCall(JsonArray jsonArray) {
    HashMap<String, Integer> resourceCount = new HashMap<>();
    JsonArray resultJsonArray = new JsonArray();
    List<Future<JsonObject>> futureList = new ArrayList<>();

    for (int i = 0; i < jsonArray.size(); i++) {
      JsonObject jsonObject =
          new JsonObject()
              .put("type", CacheType.CATALOGUE_CACHE)
              .put("key", jsonArray.getJsonObject(i).getString("resourceid"));

      resourceCount.put(
          jsonArray.getJsonObject(i).getString("resourceid"),
          Integer.parseInt(jsonArray.getJsonObject(i).getString("count")));

      futureList.add(cache.get(jsonObject).recover(f -> Future.succeededFuture(null)));
    }

    return CompositeFuture.all(new ArrayList<>(futureList))
        .map(
            composite -> {
              List<Object> results = composite.list();
              JsonArray jsonArrayResult = new JsonArray();

              results.stream()
                  .filter(Objects::nonNull) // Remove null results (failed cache lookups)
                  .map(obj -> (JsonObject) obj)
                  .forEach(
                      res -> {
                        jsonArrayResult.add(
                            new JsonObject()
                                .put("resourceid", res.getString("id"))
                                .put("resource_label", res.getString("description"))
                                .put("publisher", res.getString("name"))
                                .put("publisher_id", res.getString("provider"))
                                .put("city", res.getString("instance"))
                                .put("count", resourceCount.get(res.getString("id"))));
                      });

              return jsonArrayResult;
            });
  }

  private Future<JsonObject> executeQueryDatabaseOperation(JsonObject jsonObject) {
    return pgService.executeQuery(jsonObject.toString());
  }
}
