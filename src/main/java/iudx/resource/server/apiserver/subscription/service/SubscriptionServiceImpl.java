package iudx.resource.server.apiserver.subscription.service;

import static iudx.resource.server.apiserver.subscription.util.Constants.*;
import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.apiserver.util.Constants.RESOURCE_GROUP;
import static iudx.resource.server.cache.util.CacheType.CATALOGUE_CACHE;
import static iudx.resource.server.databroker.util.Constants.*;
import static iudx.resource.server.databroker.util.Util.getResponseJson;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.subscription.model.*;
import iudx.resource.server.apiserver.subscription.util.SubsType;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.database.postgres.model.PostgresResultModel;
import iudx.resource.server.database.postgres.service.PostgresService;
import iudx.resource.server.databroker.model.SubscriptionResponseModel;
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
                .replace("$1", subscriptionDataResult.dataBrokerResult().getString("id"))
                .replace("$2", subsType.type)
                .replace("$3", subscriptionDataResult.dataBrokerResult().getString("id"))
                .replace("$4", postModelSubscription.getEntities())
                .replace("$5", /*authInfo.getString("expiry")*/ "2025-02-13T03:15:02")
                .replace("$6", subscriptionDataResult.cacheResult().getString("name"))
                .replace("$7", subscriptionDataResult.cacheResult().toString())
                .replace(
                    "$8", /*authInfo.getString("userid")*/ "fd47486b-3497-4248-ac1e-082e4d37a66c")
                .replace("$9", subscriptionDataResult.cacheResult().getString(RESOURCE_GROUP))
                .replace("$a", subscriptionDataResult.cacheResult().getString("provider"))
                .replace("$b", delegatorId)
                .replace("$c", type));
    return query;
  }

  private static StringBuilder appendSubsQuery(
      String subId,
      String entities,
      JsonObject authInfo,
      SubscriptionData appendSubscriptionResult,
      SubsType subType,
      String delegatorId,
      String type) {
    StringBuilder appendQuery =
        new StringBuilder(
            APPEND_SUB_SQL
                .replace("$1", subId)
                .replace("$2", subType.type)
                .replace("$3", subId)
                .replace("$4", entities)
                .replace("$5", /*authInfo.getString("expiry")*/ "2025-02-13T03:15:02")
                .replace("$6", appendSubscriptionResult.cacheResult().getString("name"))
                .replace("$7", appendSubscriptionResult.cacheResult().toString())
                .replace(
                    "$8", /*authInfo.getString("userid")*/ "fd47486b-3497-4248-ac1e-082e4d37a66c")
                .replace("$9", appendSubscriptionResult.cacheResult().getString(RESOURCE_GROUP))
                .replace("$a", appendSubscriptionResult.cacheResult().getString("provider"))
                .replace("$b", delegatorId)
                .replace("$c", type));
    return appendQuery;
  }

  @Override
  public Future<GetResultModel> getSubscription(String subscriptionID, String subType) {
    LOGGER.info("getSubscription() method started");
    Promise<GetResultModel> promise = Promise.promise();
    LOGGER.info("sub id :: " + subscriptionID);
    if (subType != null) {
      getEntityName(subscriptionID)
          .compose(
              postgresSuccess -> {
                // TODO: think about this
                /*json.getJsonObject("authInfo").put("id", postgresSuccess.getValue("id"));*/
                return dataBrokerService
                    .listStreamingSubscription(subscriptionID)
                    .map(GetResultModel::new);
              })
          .onComplete(
              getDataBroker -> {
                if (getDataBroker.succeeded()) {
                  promise.complete(getDataBroker.result());
                } else {
                  promise.fail(getDataBroker.cause().getMessage());
                }
              });
    } else {
      // TODO: Can be removed
    }
    return promise.future();
  }

  @Override
  public Future<SubscriptionData> createSubscription(PostModelSubscription postModelSubscription) {
    LOGGER.info("createSubscription() method started");
    Promise<SubscriptionData> promise = Promise.promise();
    SubsType subType = SubsType.valueOf(postModelSubscription.getSubscriptionType());
    String entities = postModelSubscription.getEntities();
    JsonObject cacheJson = new JsonObject().put("key", entities).put("type", CATALOGUE_CACHE);
    CreateResultContainer createResultContainer = new CreateResultContainer();
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
              createResultContainer.queueName =
                  postModelSubscription.getUserId() + "/" + postModelSubscription.getName();
              SubscriptionImplModel subscriptionImplModel =
                  new SubscriptionImplModel(
                      postModelSubscription, itemTypeSet.iterator().next(), resourceGroup);
              return dataBrokerService.registerStreamingSubscription(subscriptionImplModel);
            })
        .compose(
            registerStreaming -> {
              LOGGER.debug("--->>>" + registerStreaming.toString());
              createResultContainer.isQueueCreated = true;
              JsonObject brokerResponse = registerStreaming.toJson();

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
              LOGGER.debug("cacheResult: " + subscriptionDataResult.cacheResult());
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
                  subscriptionDataResult.cacheResult().containsKey(RESOURCE_GROUP)
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
        .onSuccess(
            subscriptionDataResultSuccess -> {
              LOGGER.debug(">><><<><><><><------" + subscriptionDataResultSuccess.toString());
              promise.complete(subscriptionDataResultSuccess);
            })
        .onFailure(
            failure -> {
              if (createResultContainer.isQueueCreated) {
                LOGGER.trace("subsId: to delete " + postModelSubscription.getEntities());
                LOGGER.trace("UserId: " + postModelSubscription.getUserId());

                dataBrokerService
                    .deleteStreamingSubscription(
                        createResultContainer.queueName, postModelSubscription.getUserId())
                    .onComplete(
                        handlers -> {
                          if (handlers.succeeded()) {
                            LOGGER.info("subscription rolled back successfully");
                          } else {
                            LOGGER.error("subscription rolled back failed");
                          }

                          promise.fail(failure.toString());
                        });
              } else {
                // throw new DxRuntimeException(failure);

                LOGGER.debug("res -->" + failure.getMessage());
                promise.fail(failure.getMessage());
                /*promise.fail(new DxRuntimeException(failure.getMessage()));*/
                /*throw new DxRuntimeException(409, ResponseUrn.YET_NOT_IMPLEMENTED_URN,failure.getMessage());*/
              }
            });
    return promise.future();
  }

  @Override
  public Future<JsonObject> updateSubscription(String entities, String subId, JsonObject authInfo) {
    LOGGER.info("updateSubscription() method started");
    Promise<JsonObject> promise = Promise.promise();

    String queueName = subId;
    String entity = entities;

    StringBuilder selectQuery =
        new StringBuilder(SELECT_SUB_SQL.replace("$1", queueName).replace("$2", entity));

    LOGGER.debug(selectQuery);

    postgresService
        .executeQuery1(selectQuery.toString())
        .compose(
            selectQueryHandler -> {
              JsonArray resultArray = /*selectQueryHandler.getJsonArray("result")*/
                  selectQueryHandler.getResult();
              if (resultArray.isEmpty()) {
                JsonObject res = new JsonObject();
                res.put(JSON_TYPE, 404)
                    .put(JSON_TITLE, ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                    .put(JSON_DETAIL, "Subscription not found for [queue,entity]");

                promise.fail(res.toString());
                // TODO: Throw Error and return from here
              }
              StringBuilder updateQuery =
                  new StringBuilder(
                      UPDATE_SUB_SQL
                          .replace("$1", "2025-02-19T03:15:02" /*authInfo.getString("expiry")*/)
                          .replace("$2", queueName)
                          .replace("$3", entity));
              LOGGER.debug(updateQuery);
              return postgresService.executeQuery1(updateQuery.toString());
            })
        .onComplete(
            pgHandler -> {
              if (pgHandler.succeeded()) {

                JsonObject response = new JsonObject();
                JsonArray jsonEntities = new JsonArray();

                jsonEntities.add(entities);

                JsonObject results = new JsonObject();
                results.put("entities", jsonEntities);

                response.put(TYPE, ResponseUrn.SUCCESS_URN.getUrn());
                response.put(TITLE, "success");
                response.put(RESULTS, new JsonArray().add(results));

                promise.complete(response);
              } else {
                LOGGER.error(pgHandler.cause());
                JsonObject res = new JsonObject(pgHandler.cause().getMessage());
                /*promise.fail(generateResponse(res).toString());*/
              }
            });

    return promise.future();
  }

  @Override
  public Future<SubscriptionData> appendSubscription(
      PostModelSubscription postModelSubscription, String subsId) {
    LOGGER.info("appendSubscription() method started");
    String entities = postModelSubscription.getEntities();
    JsonObject cacheJson = new JsonObject().put("key", entities).put("type", CATALOGUE_CACHE);
    SubsType subType = SubsType.valueOf(postModelSubscription.getSubscriptionType());
    Promise<SubscriptionData> promise = Promise.promise();
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

              SubscriptionImplModel subscriptionImplModel =
                  new SubscriptionImplModel(
                      postModelSubscription, itemTypeSet.iterator().next(), resourceGroup);

              return dataBrokerService.appendStreamingSubscription(subscriptionImplModel, subsId);
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
                          new SubscriptionData(
                              brokerResponse, cacheResult, new SubscriptionResponseModel()));
            })
        .compose(
            appendSubscriptionResult -> {
              LOGGER.debug("cacheResult: " + appendSubscriptionResult.cacheResult());
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
                  appendSubscriptionResult.cacheResult().containsKey(RESOURCE_GROUP)
                      ? "RESOURCE"
                      : "RESOURCE_GROUP";

              StringBuilder appendQuery =
                  appendSubsQuery(
                      subsId,
                      entities,
                      new JsonObject() /*authInfo*/,
                      appendSubscriptionResult,
                      subType,
                      delegatorId,
                      type);
              LOGGER.debug("appendQuery = " + appendQuery);

              return postgresService
                  .executeQuery(appendQuery.toString())
                  .map(postgresSuccess -> appendSubscriptionResult);
            })
        .onComplete(
            subscriptionDataResultSuccess -> {
              if (subscriptionDataResultSuccess.succeeded()) {
                promise.complete(subscriptionDataResultSuccess.result());
              } else {
                deleteSubscription(
                        subsId,
                        subType.type,
                        postModelSubscription.getUserId() /*authInfo.getString("userid")*/)
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
  public Future<DeleteSubsResultModel> deleteSubscription(
      String subsId, String subscriptionType, String userid) {
    LOGGER.info("deleteSubscription() method started");
    LOGGER.info("queueName to delete :: " + subsId);
    Promise<DeleteSubsResultModel> promise = Promise.promise();
    if (subscriptionType != null) {
      getEntityName(subsId)
          .compose(
              foundId -> {
                return deleteSubscriptionFromPg(subsId);
              })
          .compose(
              postgresSuccess -> {
                // TODO: Think about this line
                /*json.getJsonObject("authInfo").put("id", postgresSuccess.getValue("id"));*/
                return dataBrokerService.deleteStreamingSubscription(subsId, userid);
              })
          .onComplete(
              deleteDataBroker -> {
                if (deleteDataBroker.succeeded()) {
                  DeleteSubsResultModel deleteSubsResultModel =
                      new DeleteSubsResultModel(deleteDataBroker.result());
                  promise.complete(deleteSubsResultModel);
                } else {
                  promise.fail(deleteDataBroker.cause().getMessage());
                }
              });

    } else {
      // TODO: Can be removed
    }
    return promise.future();
  }

  private Future<JsonObject> deleteSubscriptionFromPg(String subscriptionID) {
    Promise<JsonObject> promise = Promise.promise();
    String deleteQueueQuery = DELETE_SUB_SQL.replace("$1", subscriptionID);
    LOGGER.trace("delete query- " + deleteQueueQuery);
    postgresService
        .executeQuery(deleteQueueQuery)
        .onComplete(
            pgHandler -> {
              if (pgHandler.succeeded()) {
                promise.complete(pgHandler.result());
              } else {
                LOGGER.error("fail here");
                JsonObject failJson =
                    getResponseJson(
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getUrn(),
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getValue(),
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getDescription(),
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getDescription());
                promise.fail(failJson.toString());
              }
            });
    return promise.future();
  }

  @Override
  public Future<PostgresResultModel> getAllSubscriptionQueueForUser(String userId) {
    LOGGER.info("getAllSubscriptionQueueForUser() method started");
    Promise<PostgresResultModel> promise = Promise.promise();
    StringBuilder query = new StringBuilder(GET_ALL_QUEUE.replace("$1", userId));

    LOGGER.debug("query: " + query);
    postgresService
        .executeQuery1(query.toString())
        .onComplete(
            pgHandler -> {
              LOGGER.debug(pgHandler);
              if (pgHandler.succeeded()) {
                promise.complete(pgHandler.result());
              } else {
                JsonObject failJson =
                    getResponseJson(
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getUrn(),
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getValue(),
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getDescription(),
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getDescription());
                promise.fail(failJson.toString());
              }
            });
    return promise.future();
  }

  private Future<String> getEntityName(String subscriptionID) {
    Promise<String> promise = Promise.promise();
    String getEntityNameQuery = ENTITY_QUERY.replace("$0", subscriptionID);
    LOGGER.trace("query- " + getEntityNameQuery);
    postgresService
        .executeQuery1(getEntityNameQuery)
        .onComplete(
            pgHandler -> {
              if (pgHandler.succeeded() && !pgHandler.result().getResult().isEmpty()) {
                String entities =
                    pgHandler.result().getResult().getJsonObject(0).getString("entity");

                LOGGER.debug("Entities: " + entities);

                if (entities == null) {
                  LOGGER.error("Entity not found.");
                  JsonObject failJson =
                      getResponseJson(
                          HttpStatusCode.NOT_FOUND.getUrn(),
                          HttpStatusCode.NOT_FOUND.getValue(),
                          HttpStatusCode.NOT_FOUND.getDescription(),
                          HttpStatusCode.NOT_FOUND.getDescription());
                  promise.fail(failJson.toString());
                } else {
                  promise.complete(entities);
                }
              } else {
                if (pgHandler.result().getResult().isEmpty()) {
                  LOGGER.error("Empty response from database.");
                  JsonObject failJson =
                      getResponseJson(
                          HttpStatusCode.NOT_FOUND.getUrn(),
                          HttpStatusCode.NOT_FOUND.getValue(),
                          HttpStatusCode.NOT_FOUND.getDescription(),
                          HttpStatusCode.NOT_FOUND.getDescription());
                  promise.fail(failJson.toString());
                } else {
                  LOGGER.error("fail here");
                  JsonObject failJson =
                      getResponseJson(
                          HttpStatusCode.INTERNAL_SERVER_ERROR.getUrn(),
                          HttpStatusCode.INTERNAL_SERVER_ERROR.getValue(),
                          HttpStatusCode.INTERNAL_SERVER_ERROR.getDescription(),
                          HttpStatusCode.INTERNAL_SERVER_ERROR.getDescription());
                  promise.fail(failJson.toString());
                }
              }
            });
    return promise.future();
  }

  private JsonObject constructSuccessResponse(JsonObject request) {
    return new JsonObject()
        .put("type", ResponseUrn.SUCCESS_URN.getUrn())
        .put("title", ResponseUrn.SUCCESS_URN.getMessage().toLowerCase())
        .put("results", new JsonArray().add(request));
  }

  public class CreateResultContainer {
    public String queueName;
    public boolean isQueueCreated = false;
  }
}
