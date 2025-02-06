package iudx.resource.server.apiserver.subscription.service;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.apiserver.util.Constants.SUBSCRIPTION_ID;
import static iudx.resource.server.authenticator.Constants.ROLE;
import static iudx.resource.server.cache.cachelmpl.CacheType.CATALOGUE_CACHE;
import static iudx.resource.server.databroker.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.response.ResponseType;
import iudx.resource.server.apiserver.subscription.model.SubscriptionData;
import iudx.resource.server.apiserver.subscription.util.SubsType;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.databroker.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SubscriptionServiceImpl implements SubscriptionService {
  private static final Logger LOGGER = LogManager.getLogger(SubscriptionService.class);
  JsonObject json;
  JsonObject authInfo;
  private DataBrokerService1 databrokerService1;
  private CacheService cacheService;
  private PostgresService1 postgresService1;

  public SubscriptionServiceImpl(
      JsonObject json,
      DataBrokerService1 databrokerService1,
      CacheService cacheService,
      PostgresService1 postgresService1,
      JsonObject authInfo) {
    this.databrokerService1 = databrokerService1;
    this.cacheService = cacheService;
    this.postgresService1 = postgresService1;
    this.json = json;
    this.authInfo = authInfo;
  }

  private static StringBuilder createSubQuery(
      JsonObject json,
      JsonObject authInfo,
      SubscriptionData subscriptionDataResult,
      SubsType subsType,
      String delegatorId,
      String type) {
    StringBuilder query =
        new StringBuilder(
            CREATE_SUB_SQL
                .replace("$1", subscriptionDataResult.getDataBrokerResult().getString("id"))
                .replace("$2", subsType.type)
                .replace("$3", subscriptionDataResult.getDataBrokerResult().getString("id"))
                .replace("$4", json.getJsonArray("entities").getString(0))
                .replace("$5", authInfo.getString("expiry"))
                .replace("$6", subscriptionDataResult.getCacheResult().getString("name"))
                .replace("$7", subscriptionDataResult.getCacheResult().toString())
                .replace("$8", authInfo.getString("userid"))
                .replace("$9", subscriptionDataResult.getCacheResult().getString(RESOURCE_GROUP))
                .replace("$a", subscriptionDataResult.getCacheResult().getString("provider"))
                .replace("$b", delegatorId)
                .replace("$c", type));
    return query;
  }

  private static StringBuilder appendSubscriptionQuery(
      JsonObject json,
      JsonObject authInfo,
      SubscriptionData subscriptionDataResult,
      SubsType subType,
      String delegatorId,
      String type) {
    StringBuilder query =
        new StringBuilder(
            APPEND_SUB_SQL
                .replace("$1", json.getString(SUBSCRIPTION_ID))
                .replace("$2", subType.type)
                .replace("$3", json.getString(SUBSCRIPTION_ID))
                .replace("$4", json.getJsonArray("entities").getString(0))
                .replace("$5", authInfo.getString("expiry"))
                .replace("$6", subscriptionDataResult.getCacheResult().getString("name"))
                .replace("$7", subscriptionDataResult.getCacheResult().toString())
                .replace("$8", authInfo.getString("userid"))
                .replace("$9", subscriptionDataResult.getCacheResult().getString(RESOURCE_GROUP))
                .replace("$a", subscriptionDataResult.getCacheResult().getString("provider"))
                .replace("$b", delegatorId)
                .replace("$c", type));
    return query;
  }

  @Override
  public Future<JsonObject> getSubscription(
      JsonObject json, DataBrokerService databroker, PostgresService1 pgService) {
    // TODO: Change old flow which follows ->
    // SubscriptionService->StreamingSubscription->DataBrokerService->DB/SubscriptionService
    // TODO: Now new flow -> SubscriptionServiceImpl->DatabrokerService->Subscription

    LOGGER.info("getSubscription() method started");

    Promise<JsonObject> promise = Promise.promise();
    databrokerService1
        .listStreamingSubscription(json)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                promise.complete(handler.result());
              } else {
                JsonObject res = new JsonObject(handler.cause().getMessage());
                promise.fail(generateResponse(res).toString());
              }
            });
    return promise.future();
  }

  @Override
  public Future<JsonObject> createSubscription(
      JsonObject json,
      DataBrokerService databroker,
      PostgresService1 pgService,
      JsonObject authInfo,
      CacheService cacheService) {
    // TODO: Change old flow which follows ->
    // SubscriptionService->StreamingSubscription->DataBrokerService->DB/SubscriptionService
    // TODO: Now new flow -> SubscriptionServiceImpl->DatabrokerService->DB/Subscription

    LOGGER.info("createSubscription() method started");
    Promise<JsonObject> promise = Promise.promise();

    SubsType subsType = SubsType.valueOf(json.getString(SUB_TYPE));

    databrokerService1
        .registerStreamingSubscription(json)
        .compose(
            dataBrokerSuccess -> {
              JsonObject brokerResponse =
                  dataBrokerSuccess.getJsonArray("results").getJsonObject(0);
              LOGGER.debug("brokerResponse: " + brokerResponse);

              JsonObject cacheJson =
                  new JsonObject()
                      .put("key", json.getJsonArray("entities").getString(0))
                      .put("type", CATALOGUE_CACHE);

              return cacheService
                  .get(cacheJson)
                  .map(cacheResult -> new SubscriptionData(brokerResponse, cacheResult));
            })
        .onSuccess(
            subscriptionDataResult -> {
              LOGGER.debug("cacheResult: " + subscriptionDataResult.getCacheResult());

              String role = authInfo.getString(ROLE);
              String drl = authInfo.getString(DRL);
              String delegatorId;
              if (role.equalsIgnoreCase("delegate") && drl != null) {
                delegatorId = authInfo.getString(DID);
              } else {
                delegatorId = authInfo.getString("userid");
              }
              String type =
                  subscriptionDataResult.getCacheResult().containsKey(RESOURCE_GROUP)
                      ? "RESOURCE"
                      : "RESOURCE_GROUP";

              StringBuilder query =
                  createSubQuery(
                      json, authInfo, subscriptionDataResult, subsType, delegatorId, type);

              LOGGER.debug("query: " + query);

              postgresService1
                  .executeQuery(query.toString())
                  .onComplete(
                      pgHandler -> {
                        if (pgHandler.succeeded()) {
                          promise.complete(subscriptionDataResult.getDataBrokerResult());
                        } else {
                          JsonObject deleteJson = new JsonObject();
                          deleteJson
                              .put("instanceID", json.getString("instanceID"))
                              .put("subscriptionType", json.getString("subscriptionType"));
                          deleteJson.put("userid", authInfo.getString("userid"));
                          deleteJson.put(
                              "subscriptionID",
                              subscriptionDataResult.getDataBrokerResult().getString("id"));
                          deleteSubscription(deleteJson, databroker, pgService)
                              .onComplete(
                                  handlers -> {
                                    if (handlers.succeeded()) {
                                      LOGGER.info("subscription rolled back successfully");
                                    } else {
                                      LOGGER.error("subscription rolled back failed");
                                    }
                                    JsonObject res = new JsonObject(pgHandler.cause().getMessage());
                                    LOGGER.debug(
                                        "pgHandler.cause().getMessage "
                                            + pgHandler.cause().getMessage());
                                    promise.fail(generateResponse(res).toString());
                                  });
                        }
                      });
            })
        .onFailure(
            failure -> {
              JsonObject res = new JsonObject(failure.getMessage());
              promise.fail(generateResponse(res).toString());
            });

    // ------------------------------------------------------------------------------------

    /*databrokerService1
    .registerStreamingSubscription(json)
    .onSuccess(
        successResult -> {
          JsonObject brokerResponse = successResult.getJsonArray("results").getJsonObject(0);
          LOGGER.debug("brokerResponse: " + brokerResponse);

          JsonObject cacheJson =
              new JsonObject()
                  .put("key", json.getJsonArray("entities").getString(0))
                  .put("type", CATALOGUE_CACHE);

          cacheService
              .get(cacheJson)
              .onSuccess(
                  cacheResult -> {
                    LOGGER.debug("cacheResult: " + cacheResult);

                    String role = authInfo.getString(ROLE);
                    String drl = authInfo.getString(DRL);
                    String delegatorId;
                    if (role.equalsIgnoreCase("delegate") && drl != null) {
                      delegatorId = authInfo.getString(DID);
                    } else {
                      delegatorId = authInfo.getString("userid");
                    }
                    String type =
                        cacheResult.containsKey(RESOURCE_GROUP) ? "RESOURCE" : "RESOURCE_GROUP";
                    StringBuilder query =
                        new StringBuilder(
                            CREATE_SUB_SQL
                                .replace("$1", brokerResponse.getString("id"))
                                .replace("$2", subsType.type)
                                .replace("$3", brokerResponse.getString("id"))
                                .replace("$4", json.getJsonArray("entities").getString(0))
                                .replace("$5", authInfo.getString("expiry"))
                                .replace("$6", cacheResult.getString("name"))
                                .replace("$7", cacheResult.toString())
                                .replace("$8", authInfo.getString("userid"))
                                .replace("$9", cacheResult.getString(RESOURCE_GROUP))
                                .replace("$a", cacheResult.getString("provider"))
                                .replace("$b", delegatorId)
                                .replace("$c", type));

                    LOGGER.debug("query: " + query);
                    postgresService1
                        .executeQuery(query.toString())
                        .onComplete(
                            pgHandler -> {
                              if (pgHandler.succeeded()) {
                                promise.complete(successResult);
                              } else {
                                JsonObject deleteJson = new JsonObject();
                                deleteJson
                                    .put("instanceID", json.getString("instanceID"))
                                    .put(
                                        "subscriptionType", json.getString("subscriptionType"));
                                deleteJson.put("userid", authInfo.getString("userid"));
                                deleteJson.put(
                                    "subscriptionID", brokerResponse.getString("id"));
                                deleteSubscription(deleteJson, databroker, pgService)
                                    .onComplete(
                                        handlers -> {
                                          if (handlers.succeeded()) {
                                            LOGGER.info(
                                                "subscription rolled back successfully");
                                          } else {
                                            LOGGER.error("subscription rolled back failed");
                                          }
                                          JsonObject res =
                                              new JsonObject(pgHandler.cause().getMessage());
                                          LOGGER.debug(
                                              "pgHandler.cause().getMessage "
                                                  + pgHandler.cause().getMessage());
                                          promise.fail(generateResponse(res).toString());
                                        });
                              }
                            });
                  })
              .onFailure(failedCache -> LOGGER.error(failedCache.getCause()));
        })
    .onFailure(
        failure -> {
          JsonObject res = new JsonObject(failure.getMessage());
          promise.fail(generateResponse(res).toString());
        });*/
    return promise.future();
  }

  @Override
  public Future<JsonObject> updateSubscription(
      JsonObject json,
      DataBrokerService databroker,
      PostgresService1 pgService,
      JsonObject authInfo) {
    // TODO: Change old flow which follows ->
    // SubscriptionService->StreamingSubscription->DataBrokerService->DB/SubscriptionService
    // TODO: Now new flow -> SubscriptionServiceImpl->DatabrokerService->Subscription

    LOGGER.info("updateSubscription() method started");

    Promise<JsonObject> promise = Promise.promise();

    String queueName = json.getString(SUBSCRIPTION_ID);
    String entity = json.getJsonArray("entities").getString(0);

    StringBuilder selectQuery =
        new StringBuilder(SELECT_SUB_SQL.replace("$1", queueName).replace("$2", entity));

    LOGGER.debug(selectQuery);
    pgService
        .executeQuery(selectQuery.toString())
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                JsonArray resultArray = handler.result().getJsonArray("result");
                if (resultArray.size() == 0) {
                  JsonObject res = new JsonObject();
                  res.put(JSON_TYPE, 404)
                      .put(JSON_TITLE, ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                      .put(JSON_DETAIL, "Subscription not found for [queue,entity]");
                  promise.fail(res.toString());
                } else {
                  StringBuilder updateQuery =
                      new StringBuilder(
                          UPDATE_SUB_SQL
                              .replace("$1", authInfo.getString("expiry"))
                              .replace("$2", queueName)
                              .replace("$3", entity));
                  LOGGER.debug(updateQuery);
                  pgService
                      .executeQuery(updateQuery.toString())
                      .onComplete(
                          pgHandler -> {
                            if (pgHandler.succeeded()) {
                              JsonObject response = new JsonObject();
                              JsonArray entities = new JsonArray();

                              entities.add(json.getJsonArray("entities").getString(0));

                              JsonObject results = new JsonObject();
                              results.put("entities", entities);

                              response.put(TYPE, ResponseUrn.SUCCESS_URN.getUrn());
                              response.put(TITLE, "success");
                              response.put(RESULTS, new JsonArray().add(results));

                              promise.complete(response);
                            } else {
                              LOGGER.error(pgHandler.cause());
                              JsonObject res = new JsonObject(pgHandler.cause().getMessage());
                              promise.fail(generateResponse(res).toString());
                            }
                          });
                }
              } else {
                LOGGER.error(handler.cause());
                JsonObject res = new JsonObject(handler.cause().getMessage());
                promise.fail(generateResponse(res).toString());
              }
            });

    return promise.future();
  }

  @Override
  public Future<JsonObject> appendSubscription(
      JsonObject json,
      DataBrokerService databroker,
      PostgresService1 pgService,
      JsonObject authInfo,
      CacheService cacheService) {
    // TODO: Change old flow which follows ->
    // SubscriptionService->StreamingSubscription->DataBrokerService->DB/SubscriptionService
    // TODO: Now new flow -> SubscriptionServiceImpl->DatabrokerService->Subscription

    LOGGER.info("appendSubscription() method started");
    Promise<JsonObject> promise = Promise.promise();
    SubsType subType = SubsType.valueOf(json.getString(SUB_TYPE));

    databrokerService1
        .appendStreamingSubscription(json)
        .compose(
            dataBrokerResult -> {
              JsonObject brokerSubResult =
                  dataBrokerResult.getJsonArray("results").getJsonObject(0);
              JsonObject cacheJson =
                  new JsonObject()
                      .put("key", json.getJsonArray("entities").getString(0))
                      .put("type", CATALOGUE_CACHE);

              return cacheService
                  .get(cacheJson)
                  .map(cacheResult -> new SubscriptionData(brokerSubResult, cacheResult));
            })
        .onSuccess(
            subscriptionDataResult -> {
              String role = authInfo.getString(ROLE);
              String drl = authInfo.getString(DRL);
              String delegatorId;
              if (role.equalsIgnoreCase("delegate") && drl != null) {
                delegatorId = authInfo.getString(DID);
              } else {
                delegatorId = authInfo.getString("userid");
              }
              String type =
                  subscriptionDataResult.getCacheResult().containsKey(RESOURCE_GROUP)
                      ? "RESOURCE"
                      : "RESOURCE_GROUP";

              StringBuilder query =
                  appendSubscriptionQuery(
                      json, authInfo, subscriptionDataResult, subType, delegatorId, type);
              LOGGER.debug(query);

              pgService
                  .executeQuery(query.toString())
                  .onComplete(
                      pgHandler -> {
                        if (pgHandler.succeeded()) {
                          JsonObject responses = new JsonObject();
                          responses.put(TYPE, ResponseUrn.SUCCESS_URN.getUrn());
                          responses.put(TITLE, "success");
                          responses.put("results", subscriptionDataResult.getDataBrokerResult());
                          promise.complete(responses);
                        } else {
                          JsonObject deleteJson = new JsonObject();
                          deleteJson
                              .put("instanceID", json.getString("instanceID"))
                              .put("subscriptionType", subType);
                          deleteJson.put("userid", authInfo.getString("userid"));
                          deleteJson.put("subscriptionID", json.getString(SUBSCRIPTION_ID));
                          deleteSubscription(deleteJson, databroker, pgService)
                              .onComplete(
                                  handlers -> {
                                    if (handlers.succeeded()) {
                                      LOGGER.info("subscription rolled back successfully");
                                    } else {
                                      LOGGER.error("subscription rolled back failed");
                                    }
                                    JsonObject res = new JsonObject(pgHandler.cause().getMessage());
                                    LOGGER.debug(
                                        "pgHandler.cause().getMessage "
                                            + pgHandler.cause().getMessage());
                                    promise.fail(generateResponse(res).toString());
                                  });
                        }
                      });
            })
        .onFailure(
            failure -> {
              JsonObject res = new JsonObject(failure.getMessage());
              promise.fail(generateResponse(res).toString());
            });
    // --------------------------------------------------------------------------
    /*databrokerService1
    .appendStreamingSubscription(json)
    .onComplete(
        handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result();
            JsonObject brokerSubResult = response.getJsonArray("results").getJsonObject(0);
            JsonObject cacheJson =
                new JsonObject()
                    .put("key", json.getJsonArray("entities").getString(0))
                    .put("type", CATALOGUE_CACHE);
            cacheService
                .get(cacheJson)
                .onSuccess(
                    cacheResult -> {
                      String role = authInfo.getString(ROLE);
                      String drl = authInfo.getString(DRL);
                      String delegatorId;
                      if (role.equalsIgnoreCase("delegate") && drl != null) {
                        delegatorId = authInfo.getString(DID);
                      } else {
                        delegatorId = authInfo.getString("userid");
                      }
                      String type =
                          cacheResult.containsKey(RESOURCE_GROUP)
                              ? "RESOURCE"
                              : "RESOURCE_GROUP";

                      StringBuilder query =
                          new StringBuilder(
                              APPEND_SUB_SQL
                                  .replace("$1", json.getString(SUBSCRIPTION_ID))
                                  .replace("$2", subType.type)
                                  .replace("$3", json.getString(SUBSCRIPTION_ID))
                                  .replace("$4", json.getJsonArray("entities").getString(0))
                                  .replace("$5", authInfo.getString("expiry"))
                                  .replace("$6", cacheResult.getString("name"))
                                  .replace("$7", cacheResult.toString())
                                  .replace("$8", authInfo.getString("userid"))
                                  .replace("$9", cacheResult.getString(RESOURCE_GROUP))
                                  .replace("$a", cacheResult.getString("provider"))
                                  .replace("$b", delegatorId)
                                  .replace("$c", type));
                      LOGGER.debug(query);
                      pgService
                          .executeQuery(query.toString())
                          .onComplete(
                              pgHandler -> {
                                if (pgHandler.succeeded()) {
                                  JsonObject responses = new JsonObject();
                                  responses.put(TYPE, ResponseUrn.SUCCESS_URN.getUrn());
                                  responses.put(TITLE, "success");
                                  responses.put("results", brokerSubResult);
                                  promise.complete(responses);
                                } else {
                                  JsonObject deleteJson = new JsonObject();
                                  deleteJson
                                      .put("instanceID", json.getString("instanceID"))
                                      .put("subscriptionType", subType);
                                  deleteJson.put("userid", authInfo.getString("userid"));
                                  deleteJson.put(
                                      "subscriptionID", json.getString(SUBSCRIPTION_ID));
                                  deleteSubscription(deleteJson, databroker, pgService)
                                      .onComplete(
                                          handlers -> {
                                            if (handlers.succeeded()) {
                                              LOGGER.info(
                                                  "subscription rolled back successfully");
                                            } else {
                                              LOGGER.error("subscription rolled back failed");
                                            }
                                            JsonObject res =
                                                new JsonObject(pgHandler.cause().getMessage());
                                            LOGGER.debug(
                                                "pgHandler.cause().getMessage "
                                                    + pgHandler.cause().getMessage());
                                            promise.fail(generateResponse(res).toString());
                                          });
                                }
                              });
                    })
                .onFailure(failed -> LOGGER.error(failed.getCause()));
          } else {
            JsonObject res = new JsonObject(handler.cause().getMessage());
            promise.fail(generateResponse(res).toString());
          }
        });*/

    return promise.future();
  }

  @Override
  public Future<JsonObject> deleteSubscription(
      JsonObject json, DataBrokerService databroker, PostgresService1 pgService) {
    // TODO: Change old flow which follows ->
    // SubscriptionService->StreamingSubscription->DataBrokerService->DB/SubscriptionService
    // TODO: Now new flow -> SubscriptionServiceImpl->DatabrokerService->Subscription
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.info("deleteSubscription() method started");

    databrokerService1
        .deleteStreamingSubscription(json)
        .onSuccess(
            deleteHandler -> {
              LOGGER.debug("deleteHandler: " + deleteHandler);
              StringBuilder deleteQuery =
                  new StringBuilder(DELETE_SUB_SQL.replace("$1", json.getString(SUBSCRIPTION_ID)));
              LOGGER.debug(deleteQuery);
              pgService
                  .executeQuery(deleteQuery.toString())
                  .onComplete(
                      pgHandler -> {
                        if (pgHandler.succeeded()) {
                          deleteHandler.remove("status");
                          promise.complete(deleteHandler);
                        } else {
                          JsonObject res = new JsonObject(pgHandler.cause().getMessage());
                          promise.fail(generateResponse(res).toString());
                        }
                      });
            })
        .onFailure(
            failure -> {
              LOGGER.error("Failed to delete subscription", failure);
              JsonObject res = new JsonObject(failure.getMessage());
              promise.fail(generateResponse(res).toString());
            });
    return promise.future();
  }

  /*public Future<JsonObject> getCatalogueCache(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    cacheService
        .get(request)
        .onSuccess(
            cacheResult -> {
              LOGGER.debug("cacheResult: " + cacheResult);
              promise.complete(cacheResult);
            })
        .onFailure(
            failure -> {
              LOGGER.error("Failed to get cache", failure);
              promise.fail(failure.getMessage());
            });
    return promise.future();
  }*/

  /*private Future<JsonObject> executeQueryDatabaseOperation(String query) {
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
  }*/

  public Future<JsonObject> getAllSubscriptionQueueForUser(
      JsonObject json, PostgresService pgService) {
    LOGGER.info("getAllSubscriptionQueueForUser() method started");
    Promise<JsonObject> promise = Promise.promise();
    StringBuilder query = new StringBuilder(GET_ALL_QUEUE.replace("$1", json.getString("userid")));

    LOGGER.debug("query: " + query);
    pgService.executeQuery(
        query.toString(),
        pgHandler -> {
          if (pgHandler.succeeded()) {
            promise.complete(pgHandler.result());
          } else {
            JsonObject res = new JsonObject(pgHandler.cause().getMessage());
            promise.fail(generateResponse(res).toString());
          }
        });
    return promise.future();
  }

  private JsonObject generateResponse(JsonObject response) {
    JsonObject finalResponse = new JsonObject();
    int type;
    try {
      type = response.getInteger(JSON_TYPE);
    } catch (Exception e) {
      type = response.getInteger("status");
    }
    switch (type) {
      case 400:
        finalResponse
            .put(JSON_TYPE, type)
            .put(JSON_TITLE, ResponseType.fromCode(type).getMessage())
            .put(JSON_DETAIL, response.getString(JSON_DETAIL));
        break;
      case 404:
        finalResponse
            .put(JSON_TYPE, type)
            .put(JSON_TITLE, ResponseType.fromCode(type).getMessage())
            .put(JSON_DETAIL, ResponseType.ResourceNotFound.getMessage());
        break;
      case 409:
        finalResponse
            .put(JSON_TYPE, type)
            .put(JSON_TITLE, ResponseType.fromCode(type).getMessage())
            .put(JSON_DETAIL, "Subscription " + ResponseType.AlreadyExists.getMessage());
        break;
      default:
        finalResponse
            .put(JSON_TYPE, type)
            .put(JSON_TITLE, ResponseType.BadRequestData.getMessage())
            .put(JSON_DETAIL, response.getString(JSON_DETAIL));
        break;
    }
    return finalResponse;
  }
}
