package iudx.resource.server.apiserver.subscription.service;

import static iudx.resource.server.apiserver.subscription.util.Constants.*;
import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.apiserver.util.Constants.RESOURCE_GROUP;
import static iudx.resource.server.cache.util.CacheType.CATALOGUE_CACHE;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.subscription.model.PostModelSubscription;
import iudx.resource.server.apiserver.subscription.model.SubscriptionData;
import iudx.resource.server.apiserver.subscription.model.SubscriptionImplModel;
import iudx.resource.server.apiserver.subscription.util.SubsType;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.database.postgres.service.PostgresService;
import iudx.resource.server.databroker.service.DataBrokerService;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SubscriptionServiceImpl implements SubscriptionService {
  private static final Logger LOGGER = LogManager.getLogger(SubscriptionServiceImpl.class);
  private PostgresService postgresService;
  private DataBrokerService dataBrokerService;
  private CacheService cacheService;

  public SubscriptionServiceImpl(
      PostgresService postgresService,
      DataBrokerService dataBrokerService,
      CacheService cacheService) {
    this.postgresService = postgresService;
    this.dataBrokerService = dataBrokerService;
    this.cacheService = cacheService;
  }

  private static StringBuilder createSubQuery(
      PostModelSubscription postModelSubscription,
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
                .replace("$4", postModelSubscription.getEntities())
                .replace("$5", /*authInfo.getString("expiry")*/ "2025-02-13T03:15:02")
                .replace("$6", subscriptionDataResult.getCacheResult().getString("name"))
                .replace("$7", subscriptionDataResult.getCacheResult().toString())
                .replace(
                    "$8", /*authInfo.getString("userid")*/ "fd47486b-3497-4248-ac1e-082e4d37a66c")
                .replace("$9", subscriptionDataResult.getCacheResult().getString(RESOURCE_GROUP))
                .replace("$a", subscriptionDataResult.getCacheResult().getString("provider"))
                .replace("$b", delegatorId)
                .replace("$c", type));
    return query;
  }

  private static StringBuilder appendSubsQuery(
      JsonObject json,
      JsonObject authInfo,
      SubscriptionData appendSubscriptionResult,
      SubsType subType,
      String delegatorId,
      String type) {
    StringBuilder appendQuery =
        new StringBuilder(
            APPEND_SUB_SQL
                .replace("$1", json.getString(SUBSCRIPTION_ID))
                .replace("$2", subType.type)
                .replace("$3", json.getString(SUBSCRIPTION_ID))
                .replace("$4", json.getJsonArray("entities").getString(0))
                .replace("$5", authInfo.getString("expiry"))
                .replace("$6", appendSubscriptionResult.getCacheResult().getString("name"))
                .replace("$7", appendSubscriptionResult.getCacheResult().toString())
                .replace("$8", authInfo.getString("userid"))
                .replace("$9", appendSubscriptionResult.getCacheResult().getString(RESOURCE_GROUP))
                .replace("$a", appendSubscriptionResult.getCacheResult().getString("provider"))
                .replace("$b", delegatorId)
                .replace("$c", type));
    return appendQuery;
  }

  /*@Override
  public Future<JsonObject> getSubscription(JsonObject json) {
    LOGGER.info("getSubscription() method started");
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.info("sub id :: " + json.getString(Constants.SUBSCRIPTION_ID));
    if (json.containsKey(SUB_TYPE)) {
      Future<JsonObject> entityName =
          getEntityName(json)
              .compose(
                  postgresSuccess -> {
                    json.getJsonObject("authInfo").put("id", postgresSuccess.getValue("id"));
                    return dataBrokerService.listStreamingSubscription(json);
                  })
              .onComplete(
                  deleteDataBroker -> {
                    if (deleteDataBroker.succeeded()) {
                      promise.complete(deleteDataBroker.result());
                    } else {
                      promise.fail(deleteDataBroker.cause().getMessage());
                    }
                  });
    } else {
      // TODO: Can be removed
    }
    return promise.future();
  }*/

  @Override
  public Future<JsonObject> getSubscription(String subscriptionID, String subType) {
    LOGGER.info("getSubscription() method started");
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.info("sub id :: " + /*json.getString(Constants.SUBSCRIPTION_ID)*/ subscriptionID);
    if (
    /*json.containsKey(SUB_TYPE)*/ subType != null) {
      Future<JsonObject> entityName =
          getEntityName(subscriptionID)
              .compose(
                  postgresSuccess -> {
                    // TODO: think about this
                    /*json.getJsonObject("authInfo").put("id", postgresSuccess.getValue("id"));*/
                    return dataBrokerService.listStreamingSubscription(subscriptionID);
                  })
              .onComplete(
                  deleteDataBroker -> {
                    if (deleteDataBroker.succeeded()) {
                      promise.complete(deleteDataBroker.result());
                    } else {
                      promise.fail(deleteDataBroker.cause().getMessage());
                    }
                  });
    } else {
      // TODO: Can be removed
    }
    return promise.future();
  }

  @Override
  public Future<JsonObject> createSubscription(PostModelSubscription postModelSubscription) {
    LOGGER.info("createSubscription() method started");
    Promise<JsonObject> promise = Promise.promise();
    SubsType subType = SubsType.valueOf(postModelSubscription.getSubscriptionType());
    String entities = postModelSubscription.getEntities();
    JsonObject cacheJson = new JsonObject().put("key", entities).put("type", CATALOGUE_CACHE);

    cacheService
        .get(cacheJson)
        .compose(
            cacheResult -> {
              Set<String> type = new HashSet<String>(cacheResult.getJsonArray("type").getList());
              Set<String> itemTypeSet =
                  type.stream().map(e -> e.split(":")[1]).collect(Collectors.toSet());
              itemTypeSet.retainAll(ITEM_TYPES);

              String resourceGroup;
              if (!itemTypeSet.contains("Resource")) {
                resourceGroup = cacheResult.getString("id");
              } else {
                resourceGroup = cacheResult.getString("resourceGroup");
              }
              /*json.put("type", itemTypeSet.iterator().next());
              json.put("resourcegroup", resourceGroup);*/
              SubscriptionImplModel subscriptionImplModel =
                  new SubscriptionImplModel(
                      postModelSubscription, itemTypeSet.iterator().next(), resourceGroup);
              return dataBrokerService.registerStreamingSubscription(subscriptionImplModel);
            })
        .compose(
            registerStreaming -> {
              JsonObject brokerResponse =
                  registerStreaming.getJsonArray("results").getJsonObject(0);

              LOGGER.debug("brokerResponse: " + brokerResponse);
              JsonObject cacheJson1 =
                  new JsonObject().put("key", entities).put("type", CATALOGUE_CACHE);
              return cacheService
                  .get(cacheJson1)
                  .map(
                      cacheResult ->
                          new SubscriptionData(brokerResponse, cacheResult, registerStreaming));
            })
        .compose(
            subscriptionDataResult -> {
              LOGGER.debug("cacheResult: " + subscriptionDataResult.getCacheResult());
              String role = /*authInfo.getString("ROLE");*/ "consumer";
              String drl = /*authInfo.getString(DRL);*/ "";
              String delegatorId;
              if (role.equalsIgnoreCase("delegate") && drl != null) {
                delegatorId = /*authInfo.getString(DID);*/ "";
              } else {
                delegatorId = /*authInfo.getString("userid");*/
                    "fd47486b-3497-4248-ac1e-082e4d37a66c";
              }
              String type =
                  subscriptionDataResult.getCacheResult().containsKey(RESOURCE_GROUP)
                      ? "RESOURCE"
                      : "RESOURCE_GROUP";

              StringBuilder query =
                  createSubQuery(
                      postModelSubscription,
                      new JsonObject() /*authInfo*/,
                      subscriptionDataResult,
                      subType,
                      delegatorId,
                      type);
              LOGGER.debug("query: " + query);

              return postgresService
                  .executeQuery(query.toString())
                  .map(postgresSuccess -> subscriptionDataResult);
            })
        .onComplete(
            subscriptionDataResultSuccess -> {
              if (subscriptionDataResultSuccess.succeeded()) {
                LOGGER.debug(">><><<><><><><------" + subscriptionDataResultSuccess.toString());
                promise.complete(subscriptionDataResultSuccess.result().getStreamingResult());
              } else {
                JsonObject deleteJson = new JsonObject();
                deleteJson
                    .put("instanceID", postModelSubscription.getInstanceId())
                    .put("subscriptionType", postModelSubscription.getSubscriptionType());
                deleteJson.put("userid", postModelSubscription.getUserId());
                deleteJson.put(
                    "subscriptionID",
                    subscriptionDataResultSuccess.result().getDataBrokerResult().getString("id"));
                deleteSubscription(deleteJson)
                    .onComplete(
                        handlers -> {
                          if (handlers.succeeded()) {
                            LOGGER.info("subscription rolled back successfully");
                          } else {
                            LOGGER.error("subscription rolled back failed");
                          }
                          JsonObject res =
                              new JsonObject(subscriptionDataResultSuccess.cause().getMessage());
                          LOGGER.debug(
                              "pgHandler.cause().getMessage "
                                  + subscriptionDataResultSuccess.cause().getMessage());
                          /*promise.fail(generateResponse(res).toString());*/
                        });
              }
            })
        .onFailure(
            failure -> {
              JsonObject res = new JsonObject(failure.getMessage());
              promise.fail("generateResponse(res).toString()");
            });
    return promise.future();
  }

  public Future<JsonObject> createSubscription(JsonObject json, JsonObject authInfo) {
    return null;
  } /*{
    LOGGER.info("createSubscription() method started");
    Promise<JsonObject> promise = Promise.promise();
    SubsType subType = SubsType.valueOf(json.getString(SUB_TYPE));
    String entities = json.getJsonArray("entities").getString(0);
    JsonObject cacheJson = new JsonObject().put("key", entities).put("type", CATALOGUE_CACHE);
    cacheService
        .get(cacheJson)
        .compose(
            cacheResult -> {
              Set<String> type = new HashSet<String>(cacheResult.getJsonArray("type").getList());
              Set<String> itemTypeSet =
                  type.stream().map(e -> e.split(":")[1]).collect(Collectors.toSet());
              itemTypeSet.retainAll(ITEM_TYPES);

              String resourceGroup;
              if (!itemTypeSet.contains("Resource")) {
                resourceGroup = cacheResult.getString("id");
              } else {
                resourceGroup = cacheResult.getString("resourceGroup");
              }
              json.put("type", itemTypeSet.iterator().next());
              json.put("resourcegroup", resourceGroup);

              return dataBrokerService.registerStreamingSubscription(json);
            })
        .compose(
            registerStreaming -> {
              JsonObject brokerResponse =
                  registerStreaming.getJsonArray("results").getJsonObject(0);
              LOGGER.debug("brokerResponse: " + brokerResponse);

              JsonObject cacheJson1 =
                  new JsonObject().put("key", entities).put("type", CATALOGUE_CACHE);
              return cacheService
                  .get(cacheJson1)
                  .map(
                      cacheResult ->
                          new SubscriptionData(brokerResponse, cacheResult, registerStreaming));
            })
        .compose(
            subscriptionDataResult -> {
              LOGGER.debug("cacheResult: " + subscriptionDataResult.getCacheResult());
              String role = authInfo.getString("ROLE");
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
                      json, authInfo, subscriptionDataResult, subType, delegatorId, type);
              LOGGER.debug("query: " + query);

              return postgresService
                  .executeQuery(query.toString())
                  .map(postgresSuccess -> subscriptionDataResult);
            })
        .onComplete(
            subscriptionDataResultSuccess -> {
              if (subscriptionDataResultSuccess.succeeded()) {
                promise.complete(subscriptionDataResultSuccess.result().getStreamingResult());
              } else {
                JsonObject deleteJson = new JsonObject();
                deleteJson
                    .put("instanceID", json.getString("instanceID"))
                    .put("subscriptionType", json.getString("subscriptionType"));
                deleteJson.put("userid", authInfo.getString("userid"));
                deleteJson.put(
                    "subscriptionID",
                    subscriptionDataResultSuccess.result().getDataBrokerResult().getString("id"));
                deleteSubscription(deleteJson)
                    .onComplete(
                        handlers -> {
                          if (handlers.succeeded()) {
                            LOGGER.info("subscription rolled back successfully");
                          } else {
                            LOGGER.error("subscription rolled back failed");
                          }
                          JsonObject res =
                              new JsonObject(subscriptionDataResultSuccess.cause().getMessage());
                          LOGGER.debug(
                              "pgHandler.cause().getMessage "
                                  + subscriptionDataResultSuccess.cause().getMessage());
                          */

  /*promise.fail(generateResponse(res).toString());*/
  /*
                  });
        }
      })
  .onFailure(
      failure -> {
        JsonObject res = new JsonObject(failure.getMessage());
        */
  /*promise.fail(generateResponse(res).toString());*/
  /*
            });
    return promise.future();
  }*/

  @Override
  public Future<JsonObject> updateSubscription(JsonObject json, JsonObject authInfo) {
    LOGGER.info("updateSubscription() method started");
    Promise<JsonObject> promise = Promise.promise();

    String queueName = json.getString(SUBSCRIPTION_ID);
    String entity = json.getJsonArray("entities").getString(0);

    StringBuilder selectQuery =
        new StringBuilder(SELECT_SUB_SQL.replace("$1", queueName).replace("$2", entity));

    LOGGER.debug(selectQuery);
    postgresService
        .executeQuery(selectQuery.toString())
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                JsonArray resultArray = handler.result().getJsonArray("result");
                if (resultArray.isEmpty()) {
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
                  postgresService
                      .executeQuery(updateQuery.toString())
                      .onComplete(
                          pgHandler -> {
                            if (pgHandler.succeeded()) {
                              JsonObject response = new JsonObject();
                              JsonArray entities = new JsonArray();

                              entities.add(json.getJsonArray("entities").getString(0));

                              JsonObject results = new JsonObject();
                              results.put("entities", entities);

                              /*response.put(TYPE, ResponseUrn.SUCCESS_URN.getUrn());
                              response.put(TITLE, "success");
                              response.put(RESULTS, new JsonArray().add(results));*/

                              promise.complete(response);
                            } else {
                              LOGGER.error(pgHandler.cause());
                              JsonObject res = new JsonObject(pgHandler.cause().getMessage());
                              /*promise.fail(generateResponse(res).toString());*/
                            }
                          });
                }
              } else {
                LOGGER.error(handler.cause());
                JsonObject res = new JsonObject(handler.cause().getMessage());
                /*promise.fail(generateResponse(res).toString());*/
              }
            });

    return promise.future();
  }

  @Override
  public Future<JsonObject> appendSubscription(JsonObject json, JsonObject authInfo) {
    LOGGER.info("appendSubscription() method started");
    String entities = json.getJsonArray("entities").getString(0);
    JsonObject cacheJson = new JsonObject().put("key", entities).put("type", CATALOGUE_CACHE);
    SubsType subType = SubsType.valueOf(json.getString(SUB_TYPE));
    Promise<JsonObject> promise = Promise.promise();
    cacheService
        .get(cacheJson)
        .compose(
            cacheResult -> {
              Set<String> type = new HashSet<String>(cacheResult.getJsonArray("type").getList());
              Set<String> itemTypeSet =
                  type.stream().map(e -> e.split(":")[1]).collect(Collectors.toSet());
              itemTypeSet.retainAll(ITEM_TYPES);

              String resourceGroup;
              if (!itemTypeSet.contains("Resource")) {
                resourceGroup = cacheResult.getString("id");
              } else {
                resourceGroup = cacheResult.getString("resourceGroup");
              }
              json.put("type", itemTypeSet.iterator().next());
              json.put("resourcegroup", resourceGroup);

              return dataBrokerService.appendStreamingSubscription(json);
            })
        .compose(
            appendStreaming -> {
              JsonObject brokerResponse = appendStreaming.getJsonArray("results").getJsonObject(0);
              LOGGER.debug("brokerResponse: " + brokerResponse);

              JsonObject cacheJson1 =
                  new JsonObject().put("key", entities).put("type", CATALOGUE_CACHE);

              return cacheService
                  .get(cacheJson1)
                  .map(
                      cacheResult ->
                          new SubscriptionData(brokerResponse, cacheResult, appendStreaming));
            })
        .compose(
            appendSubscriptionResult -> {
              LOGGER.debug("cacheResult: " + appendSubscriptionResult.getCacheResult());
              String role = authInfo.getString("ROLE");
              String drl = authInfo.getString(DRL);
              String delegatorId;
              if (role.equalsIgnoreCase("delegate") && drl != null) {
                delegatorId = authInfo.getString(DID);
              } else {
                delegatorId = authInfo.getString("userid");
              }
              String type =
                  appendSubscriptionResult.getCacheResult().containsKey(RESOURCE_GROUP)
                      ? "RESOURCE"
                      : "RESOURCE_GROUP";

              StringBuilder appendQuery =
                  appendSubsQuery(
                      json, authInfo, appendSubscriptionResult, subType, delegatorId, type);
              LOGGER.debug("appendQuery = " + appendQuery);

              return postgresService
                  .executeQuery(appendQuery.toString())
                  .map(postgresSuccess -> appendSubscriptionResult);
            })
        .onComplete(
            subscriptionDataResultSuccess -> {
              if (subscriptionDataResultSuccess.succeeded()) {
                promise.complete(subscriptionDataResultSuccess.result().getDataBrokerResult());
              } else {
                JsonObject deleteJson = new JsonObject();
                deleteJson
                    .put("instanceID", json.getString("instanceID"))
                    .put("subscriptionType", subType);
                deleteJson.put("userid", authInfo.getString("userid"));
                deleteJson.put("subscriptionID", json.getString(SUBSCRIPTION_ID));
                deleteSubscription(deleteJson)
                    .onComplete(
                        handlers -> {
                          if (handlers.succeeded()) {
                            LOGGER.info("subscription rolled back successfully");
                          } else {
                            LOGGER.error("subscription rolled back failed");
                          }
                          JsonObject res =
                              new JsonObject(subscriptionDataResultSuccess.cause().getMessage());
                          LOGGER.debug(
                              "pgHandler.cause().getMessage "
                                  + subscriptionDataResultSuccess.cause().getMessage());
                          /*promise.fail(generateResponse(res).toString());*/
                        });
              }
            })
        .onFailure(failed -> LOGGER.error(failed.getCause()));
    return promise.future();
  }

  @Override
  public Future<JsonObject> deleteSubscription(JsonObject json) {
    LOGGER.info("deleteSubscription() method started");
    Promise<JsonObject> promise = Promise.promise();
    if (json.containsKey(SUB_TYPE)) {
      Future<JsonObject> entityName =
          getEntityName(json)
              .compose(
                  postgresSuccess -> {
                    json.getJsonObject("authInfo").put("id", postgresSuccess.getValue("id"));
                    return dataBrokerService.deleteStreamingSubscription(json);
                  })
              .onComplete(
                  deleteDataBroker -> {
                    if (deleteDataBroker.succeeded()) {
                      promise.complete(deleteDataBroker.result());
                    } else {
                      promise.fail(deleteDataBroker.cause().getMessage());
                    }
                  });

    } else {
      // TODO: Can be removed
    }
    return promise.future();
  }

  @Override
  public Future<JsonObject> getAllSubscriptionQueueForUser(String userId) {
    LOGGER.info("getAllSubscriptionQueueForUser() method started");
    Promise<JsonObject> promise = Promise.promise();
    StringBuilder query = new StringBuilder(GET_ALL_QUEUE.replace("$1", userId));

    LOGGER.debug("query: " + query);
    postgresService
        .executeQuery(query.toString())
        .onComplete(
            pgHandler -> {
              LOGGER.debug(pgHandler);
              if (pgHandler.succeeded()) {
                promise.complete(pgHandler.result());
              } else {
                /*JsonObject res = new JsonObject(pgHandler.cause().getMessage());*/
                promise.fail(/*generateResponse(res).toString()*/ "fail");
              }
            });
    return promise.future();
  }

  private Future<JsonObject> getEntityName(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    String getEntityNameQuery = ENTITY_QUERY.replace("$0", request.getString(SUBSCRIPTION_ID));

    postgresService
        .executeQuery(getEntityNameQuery)
        .onComplete(
            pgHandler -> {
              if (pgHandler.succeeded() && !pgHandler.result().getJsonArray("result").isEmpty()) {
                request.put(
                    "id",
                    pgHandler.result().getJsonArray("result").getJsonObject(0).getString("entity"));
                promise.complete(request);
              } else {
                if (pgHandler.result().getJsonArray("result").isEmpty()) {
                  LOGGER.error("Empty response from database.");
                  promise.fail("Resource Not Found");
                } else {
                  LOGGER.error("fail here");
                  promise.fail(pgHandler.cause().getMessage());
                }
              }
            });
    return promise.future();
  }

  private Future<String> getEntityName(String subscriptionID) {
    Promise<String> promise = Promise.promise();
    String getEntityNameQuery = ENTITY_QUERY.replace("$0", subscriptionID);

    postgresService
        .executeQuery(getEntityNameQuery)
        .onComplete(
            pgHandler -> {
              if (pgHandler.succeeded() && !pgHandler.result().getJsonArray("result").isEmpty()) {

                String entities =
                    pgHandler.result().getJsonArray("result").getJsonObject(0).getString("entity");
                promise.complete(entities);
              } else {
                if (pgHandler.result().getJsonArray("result").isEmpty()) {
                  LOGGER.error("Empty response from database.");
                  promise.fail("Resource Not Found");
                } else {
                  LOGGER.error("fail here");
                  promise.fail(pgHandler.cause().getMessage());
                }
              }
            });
    return promise.future();
  }
}
