package iudx.resource.server.metering;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.authenticator.Constants.ROLE;
import static iudx.resource.server.common.Constants.*;
import static iudx.resource.server.metering.util.Constants.*;
import static iudx.resource.server.metering.util.Constants.IID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.cache.cachelmpl.CacheType;
import iudx.resource.server.common.Response;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.databroker.DataBrokerService;
import iudx.resource.server.metering.readpg.ReadQueryBuilder;
import iudx.resource.server.metering.util.DateValidation;
import iudx.resource.server.metering.util.ParamsValidation;
import iudx.resource.server.metering.util.QueryBuilder;
import iudx.resource.server.metering.util.ResponseBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MeteringServiceImpl implements MeteringService {

  private static final Logger LOGGER = LogManager.getLogger(MeteringServiceImpl.class);
  public static DataBrokerService rmqService;
  private final Vertx vertx;
  private final QueryBuilder queryBuilder = new QueryBuilder();
  private final ParamsValidation validation = new ParamsValidation();
  private final DateValidation dateValidation = new DateValidation();
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final PostgresService postgresService;
  private final CacheService cacheService;
  String queryPg;
  String queryCount;
  String queryOverview;
  String summaryOverview;
  long total;
  JsonObject validationCheck = new JsonObject();
  JsonArray jsonArray;
  JsonArray resultJsonArray;
  int loopi;
  DateValidation.Time time;
  private ResponseBuilder responseBuilder;

  public MeteringServiceImpl(
      Vertx vertxInstance, PostgresService postgresService, CacheService cacheService) {
    this.vertx = vertxInstance;
    this.postgresService = postgresService;
    this.rmqService = DataBrokerService.createProxy(vertx, BROKER_SERVICE_ADDRESS);
    this.cacheService = cacheService;
  }

  @Override
  public MeteringService executeReadQuery(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    LOGGER.trace("Info: Read Query" + request.toString());
    validationCheck = validation.paramsCheck(request);

    if (validationCheck != null && validationCheck.containsKey(ERROR)) {
      responseBuilder =
          new ResponseBuilder().setTypeAndTitle(400).setMessage(validationCheck.getString(ERROR));
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return this;
    }
    request.put(TABLE_NAME, RS_DATABASE_TABLE_NAME);

    String count = request.getString("options");
    if (count == null) {
      countQueryForRead(request, handler);
    } else {
      countQuery(request, handler);
    }

    return this;
  }

  private void countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    queryCount = queryBuilder.buildCountReadQueryFromPg(request);
    Future<JsonObject> resultCountPg = executeQueryDatabaseOperation(queryCount);
    resultCountPg.onComplete(
        countHandler -> {
          if (countHandler.succeeded()) {
            try {
              var countHandle = countHandler.result().getJsonArray("result");
              total = countHandle.getJsonObject(0).getInteger("count");
              if (total == 0) {
                responseBuilder = new ResponseBuilder().setTypeAndTitle(204).setCount(0);
                handler.handle(Future.succeededFuture(responseBuilder.getResponse()));

              } else {
                responseBuilder = new ResponseBuilder().setTypeAndTitle(200).setCount((int) total);
                handler.handle(Future.succeededFuture(responseBuilder.getResponse()));
              }
            } catch (NullPointerException nullPointerException) {
              LOGGER.debug(nullPointerException.toString());
            }
          }
        });
  }

  private void countQueryForRead(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    queryCount = queryBuilder.buildCountReadQueryFromPg(request);

    Future<JsonObject> resultCountPg = executeQueryDatabaseOperation(queryCount);
    resultCountPg.onComplete(
        countHandler -> {
          if (countHandler.succeeded()) {
            try {
              var countHandle = countHandler.result().getJsonArray("result");
              total = countHandle.getJsonObject(0).getInteger("count");
              request.put(TOTALHITS, total);
              if (total == 0) {
                responseBuilder = new ResponseBuilder().setTypeAndTitle(204).setCount(0);
                handler.handle(Future.succeededFuture(responseBuilder.getResponse()));

              } else {
                readMethod(request, handler);
              }
            } catch (NullPointerException nullPointerException) {
              LOGGER.debug(nullPointerException.toString());
            }
          }
        });
  }

  private void readMethod(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    ReadQueryBuilder readQueryBuilder = new ReadQueryBuilder();
    String offset = request.getString(OFFSETPARAM);
    String limit = request.getString(LIMITPARAM);
    int limitInt;
    int offsetInt;
    if (limit == null) {
      limitInt = 2000;
      request.put(LIMITPARAM, limitInt);
    } else {
      limit = request.getString(LIMITPARAM);
      try {
        limitInt = Integer.parseInt(limit);
        if (limitInt < 0) {
          LOGGER.error("Fail msg " + "Negative Limit Value");
          handler.handle(Future.failedFuture("Negative Limit Value"));
          return;
        }
      } catch (Exception e) {
        handler.handle(Future.failedFuture(e.getMessage()));
        return;
      }
    }
    if (offset == null) {
      offsetInt = 0;
      request.put(OFFSETPARAM, offsetInt);
    } else {
      offset = request.getString(OFFSETPARAM);
      try {
        offsetInt = Integer.parseInt(offset);
        if (offsetInt < 0) {
          LOGGER.error("Fail msg " + "Negative Offset Value");
          handler.handle(Future.failedFuture("Negative offset Value"));
          return;
        }
      } catch (Exception e) {
        handler.handle(Future.failedFuture(e.getMessage()));
        return;
      }
    }
    queryPg = readQueryBuilder.getQuery(request);
    LOGGER.debug("read query = " + queryPg);
    Future<JsonObject> resultsPg = executeQueryDatabaseOperation(queryPg);
    resultsPg.onComplete(
        readHandler -> {
          if (readHandler.succeeded()) {
            LOGGER.info("Read Completed successfully");
            JsonObject resultJsonObject = readHandler.result();
            resultJsonObject.put(LIMITPARAM, limitInt);
            resultJsonObject.put(OFFSETPARAM, offsetInt);
            resultJsonObject.put(TOTALHITS, request.getLong(TOTALHITS));
            handler.handle(Future.succeededFuture(resultJsonObject));
          } else {
            LOGGER.debug("Could not read from DB : " + readHandler.cause());
            handler.handle(Future.failedFuture(readHandler.cause().getMessage()));
          }
        });
  }

  @Override
  public MeteringService monthlyOverview(
      JwtData jwtData, String startTime, String endTime, Handler<AsyncResult<JsonObject>> handler) {

    if (startTime != null && endTime == null || startTime == null && endTime != null) {
      handler.handle(
          Future.failedFuture(
              new JsonObject().put(JSON_TITLE, "Bad Request").put(JSON_TYPE, 400).toString()));
      return this;
    }
    if (startTime != null && endTime != null) {
      this.time = dateValidation.dateParamCheck(startTime, endTime);
    }

    String role = jwtData.getRole();
    if (role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("consumer")) {
      queryOverview = queryBuilder.buildMonthlyOverview(jwtData, getTime(), null);
      LOGGER.debug("query Overview =" + queryOverview);

      Future<JsonObject> result = executeQueryDatabaseOperation(queryOverview);
      result.onComplete(
          handlers -> {
            if (handlers.succeeded()) {
              LOGGER.debug("Count return Successfully");
              handler.handle(Future.succeededFuture(handlers.result()));
            } else {
              LOGGER.debug("Could not read from DB : " + handlers.cause());
              handler.handle(Future.failedFuture(handlers.cause().getMessage()));
            }
          });
    } else if (role.equalsIgnoreCase("provider") || role.equalsIgnoreCase("delegate")) {
      String resourceId = jwtData.getIid().split(":")[1];
      JsonObject jsonObject =
          new JsonObject().put("type", CacheType.CATALOGUE_CACHE).put("key", resourceId);

      cacheService
          .get(jsonObject)
          .onSuccess(
              providerHandler -> {
                String providerId = providerHandler.getString("provider");
                queryOverview = queryBuilder.buildMonthlyOverview(jwtData, getTime(), providerId);
                LOGGER.debug("query Overview =" + queryOverview);

                Future<JsonObject> result = executeQueryDatabaseOperation(queryOverview);
                result.onComplete(
                    handlers -> {
                      if (handlers.succeeded()) {
                        LOGGER.debug("Count return Successfully");
                        handler.handle(Future.succeededFuture(handlers.result()));
                      } else {
                        LOGGER.debug("Could not read from DB : " + handlers.cause());
                        handler.handle(Future.failedFuture(handlers.cause().getMessage()));
                      }
                    });
              })
          .onFailure(fail -> LOGGER.debug(fail.getMessage()));
    }
    return this;
  }

  @Override
  public MeteringService summaryOverview(
      JwtData jwtData, String startTime, String endTime, Handler<AsyncResult<JsonObject>> handler) {

    if (startTime != null && endTime == null || startTime == null && endTime != null) {
      handler.handle(
          Future.failedFuture(
              new JsonObject().put(JSON_TITLE, "Bad Request").put(JSON_TYPE, 400).toString()));
      return this;
    }
    if (startTime != null && endTime != null) {
      this.time = dateValidation.dateParamCheck(startTime, endTime);
    }

    String role = jwtData.getRole();
    if (role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("consumer")) {
      summaryOverview = queryBuilder.buildSummaryOverview(jwtData, getTime(), null);
      LOGGER.debug("summary query =" + summaryOverview);
      Future<JsonObject> result = executeQueryDatabaseOperation(summaryOverview);
      result.onComplete(
          handlers -> {
            if (handlers.succeeded()) {
              jsonArray = handlers.result().getJsonArray("result");
              if (jsonArray.size() == 0) {
                responseBuilder =
                    new ResponseBuilder().setTypeAndTitle(204).setMessage("NO ID Present");
                handler.handle(Future.succeededFuture(responseBuilder.getResponse()));
              }
              cacheCall(jsonArray)
                  .onSuccess(
                      resultHandler -> {
                        JsonObject resultJson =
                            new JsonObject()
                                .put("type", "urn:dx:dm:Success")
                                .put("title", "Success")
                                .put("results", resultHandler);
                        handler.handle(Future.succeededFuture(resultJson));
                      });
            } else {
              LOGGER.debug("Could not read from DB : " + handlers.cause());
              handler.handle(Future.failedFuture(handlers.cause().getMessage()));
            }
          });
    } else if (role.equalsIgnoreCase("provider") || role.equalsIgnoreCase("delegate")) {
      String resourceId = jwtData.getIid().split(":")[1];
      JsonObject jsonObject =
          new JsonObject().put("type", CacheType.CATALOGUE_CACHE).put("key", resourceId);
      cacheService
          .get(jsonObject)
          .onSuccess(
              providerHandler -> {
                String providerId = providerHandler.getString("provider");
                summaryOverview = queryBuilder.buildSummaryOverview(jwtData, getTime(), providerId);
                LOGGER.debug("summary query =" + summaryOverview);
                Future<JsonObject> result = executeQueryDatabaseOperation(summaryOverview);
                result.onComplete(
                    handlers -> {
                      if (handlers.succeeded()) {
                        jsonArray = handlers.result().getJsonArray("result");
                        if (jsonArray.size() == 0) {
                          responseBuilder =
                              new ResponseBuilder()
                                  .setTypeAndTitle(204)
                                  .setMessage("NO ID Present");
                          handler.handle(Future.succeededFuture(responseBuilder.getResponse()));
                        }
                        cacheCall(jsonArray)
                            .onSuccess(
                                resultHandler -> {
                                  JsonObject resultJson =
                                      new JsonObject()
                                          .put("type", "urn:dx:dm:Success")
                                          .put("title", "Success")
                                          .put("results", resultHandler);
                                  handler.handle(Future.succeededFuture(resultJson));
                                });
                      } else {
                        LOGGER.debug("Could not read from DB : " + handlers.cause());
                        handler.handle(Future.failedFuture(handlers.cause().getMessage()));
                      }
                    });
              })
          .onFailure(
              fail -> {
                LOGGER.debug(fail.getMessage());
                handler.handle(Future.failedFuture(fail.getMessage()));
              });
    }
    return this;
  }

  public Future<JsonArray> cacheCall(JsonArray jsonArray) {
    Promise<JsonArray> promise = Promise.promise();
    HashMap<String, Integer> resourceCount = new HashMap<>();
    resultJsonArray = new JsonArray();
    List<Future> list = new ArrayList<>();

    for (loopi = 0; loopi < jsonArray.size(); loopi++) {
      JsonObject jsonObject =
          new JsonObject()
              .put("type", CacheType.CATALOGUE_CACHE)
              .put("key", jsonArray.getJsonObject(loopi).getString("resourceid"));
      resourceCount.put(
          jsonArray.getJsonObject(loopi).getString("resourceid"),
          Integer.valueOf(jsonArray.getJsonObject(loopi).getString("count")));

      list.add(cacheService.get(jsonObject).recover(f -> Future.succeededFuture(null)));
    }

    CompositeFuture.join(list)
        .map(CompositeFuture::list)
        .map(result -> result.stream().filter(Objects::nonNull).collect(Collectors.toList()))
        .onSuccess(
            l -> {
              for (int i = 0; i < l.size(); i++) {
                JsonObject result = (JsonObject) l.get(i);
                JsonObject outputFormat =
                    new JsonObject()
                        .put("resourceid", result.getString("id"))
                        .put("resource_label", result.getString("description"))
                        .put("publisher", result.getString("name"))
                        .put("publisher_id", result.getString("provider"))
                        .put("city", result.getString("instance"))
                        .put("count", resourceCount.get(result.getString("id")));
                resultJsonArray.add(outputFormat);
              }
              promise.complete(resultJsonArray);
            });

    return promise.future();
  }

  @Override
  public MeteringService insertMeteringValuesInRmq(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    JsonObject writeMessage = queryBuilder.buildMessageForRmq(request);
    LOGGER.debug("write message =  {}", writeMessage);

    rmqService.publishMessage(
        writeMessage,
        EXCHANGE_NAME,
        ROUTING_KEY,
        rmqHandler -> {
          if (rmqHandler.succeeded()) {
            handler.handle(Future.succeededFuture());
            LOGGER.info("inserted into rmq");
          } else {
            LOGGER.error(rmqHandler.cause());
            try {
              Response resp =
                  objectMapper.readValue(rmqHandler.cause().getMessage(), Response.class);
              LOGGER.debug("response from rmq " + resp);
              handler.handle(Future.failedFuture(resp.toString()));
            } catch (JsonProcessingException e) {
              LOGGER.error("Failure message not in format [type,title,detail]");
              handler.handle(Future.failedFuture(e.getMessage()));
            }
          }
        });
    return this;
  }

  private Future<JsonObject> executeQueryDatabaseOperation(String query) {
    Promise<JsonObject> promise = Promise.promise();
    postgresService.executeQuery(
        query,
        dbHandler -> {
          if (dbHandler.succeeded()) {
            promise.complete(dbHandler.result());
          } else {

            promise.fail(dbHandler.cause().getMessage());
          }
        });

    return promise.future();
  }

  public DateValidation.Time getTime() {
    return time;
  }
}
