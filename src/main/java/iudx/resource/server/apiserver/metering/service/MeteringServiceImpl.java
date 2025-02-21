package iudx.resource.server.apiserver.metering.service;

import static iudx.resource.server.apiserver.metering.util.Constant.*;
import static iudx.resource.server.databroker.util.Util.getResponseJson;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.metering.util.DateValidation;
import iudx.resource.server.apiserver.metering.util.ParamsValidation;
import iudx.resource.server.apiserver.metering.util.QueryBuilder;
import iudx.resource.server.apiserver.metering.util.ResponseBuilder;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.database.postgres.service.PostgresService;
import iudx.resource.server.databroker.service.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MeteringServiceImpl implements MeteringService {
  private static final Logger LOGGER = LogManager.getLogger(MeteringServiceImpl.class);
  private final QueryBuilder queryBuilder = new QueryBuilder();
  private final ParamsValidation validation = new ParamsValidation();
  private final DateValidation dateValidation = new DateValidation();
  JsonObject validationCheck = new JsonObject();
  String queryPg;
  String queryCount;
  String queryOverview;
  String summaryOverview;
  long total;
  JsonArray jsonArray;
  JsonArray resultJsonArray;
  int loopi;
  private PostgresService postgresService;
  private CacheService cacheService;
  private DataBrokerService dataBrokerService;
  private ResponseBuilder responseBuilder;

  public MeteringServiceImpl(
      PostgresService postgresService,
      CacheService cacheService,
      DataBrokerService dataBrokerService) {
    this.postgresService = postgresService;
    this.cacheService = cacheService;
    this.dataBrokerService = dataBrokerService;
  }

  @Override
  public Future<JsonObject> executeReadQuery(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
    LOGGER.trace("Info: " + request.toString());
    validationCheck = validation.paramsCheck(request);

    if (validationCheck != null && validationCheck.containsKey(ERROR)) {
      responseBuilder =
          new ResponseBuilder().setTypeAndTitle(400).setMessage(validationCheck.getString(ERROR));
      finalResponse.mergeIn(
          getResponseJson(
              HttpStatusCode.BAD_REQUEST.getUrn(),
              HttpStatusCode.BAD_REQUEST.getValue(),
              HttpStatusCode.BAD_REQUEST.getUrn(),
              validationCheck.getString(ERROR)));
      promise.fail(finalResponse.toString());
      /* handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));*/
      return promise.future();
    }
    request.put(TABLE_NAME, RS_DATABASE_TABLE_NAME);

    String count = request.getString("options");
    if (count == null) {
      /*countQueryForRead(request, handler);*/
    } else {
      LOGGER.trace("--------------------------------");
      countQuery(request, promise);
    }
    return promise.future();
  }

  private void countQuery(JsonObject request, Promise<JsonObject> promise) {
    queryCount = queryBuilder.buildCountReadQueryFromPg(request);
    LOGGER.trace("queryCount " + queryCount);
    JsonObject finalResponse = new JsonObject();
    Future<JsonObject> resultCountPg = executeQueryDatabaseOperation(queryCount);
    resultCountPg.onComplete(
        countHandler -> {
          if (countHandler.succeeded()) {
            try {
              var countHandle = countHandler.result().getJsonArray("result");
              total = countHandle.getJsonObject(0).getInteger("count");
              if (total == 0) {
                finalResponse.mergeIn(
                    getResponseJson(
                        HttpStatusCode.NO_CONTENT.getUrn(),
                        HttpStatusCode.NO_CONTENT.getValue(),
                        HttpStatusCode.NO_CONTENT.getUrn(),
                        HttpStatusCode.NO_CONTENT.getDescription()));
                promise.fail(finalResponse.toString());
                /*responseBuilder = new ResponseBuilder().setTypeAndTitle(204).setCount(0);
                handler.handle(Future.succeededFuture(responseBuilder.getResponse()));*/

              } else {

                responseBuilder = new ResponseBuilder().setTypeAndTitle(200).setCount((int) total);
                promise.complete(responseBuilder.getResponse());
                /*handler.handle(Future.succeededFuture(responseBuilder.getResponse()));*/
              }
            } catch (NullPointerException nullPointerException) {
              LOGGER.debug(nullPointerException.toString());
            }
          }
        });
  }

  @Override
  public Future<JsonObject> insertMeteringValuesInRmq(JsonObject request) {
    return null;
  }

  @Override
  public Future<JsonObject> monthlyOverview(JsonObject request) {
    return null;
  }

  @Override
  public Future<JsonObject> summaryOverview(JsonObject request) {
    return null;
  }

  private Future<JsonObject> executeQueryDatabaseOperation(String query) {
    Promise<JsonObject> promise = Promise.promise();
    postgresService
        .executeQuery(query)
        .onComplete(
            dbHandler -> {
              if (dbHandler.succeeded()) {
                promise.complete(dbHandler.result());
              } else {

                promise.fail(dbHandler.cause().getMessage());
              }
            });

    return promise.future();
  }
}
