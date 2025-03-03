package iudx.resource.server.apiserver.subscription.service;

import static iudx.resource.server.apiserver.subscription.util.Constants.*;
import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.apiserver.util.Constants.RESOURCE_GROUP;
import static iudx.resource.server.cache.util.CacheType.CATALOGUE_CACHE;
import static iudx.resource.server.common.HttpStatusCode.INTERNAL_SERVER_ERROR;
import static iudx.resource.server.common.HttpStatusCode.NOT_FOUND;
import static iudx.resource.server.databroker.util.Constants.SUCCESS;
import static iudx.resource.server.databroker.util.Util.getResponseJson;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceException;
import iudx.resource.server.apiserver.subscription.model.*;
import iudx.resource.server.apiserver.subscription.util.SubsType;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.database.postgres.model.PostgresResultModel;
import iudx.resource.server.database.postgres.service.PostgresService;
import iudx.resource.server.databroker.model.SubscriptionResponseModel;
import iudx.resource.server.databroker.service.DataBrokerService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SubscriptionServiceImpl implements SubscriptionService {
  private static final Logger LOGGER = LogManager.getLogger(SubscriptionServiceImpl.class);
  private final PostgresService postgresService;
  private final DataBrokerService dataBrokerService;
  private final CacheService cacheService;

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
      SubscriptionData subscriptionDataResult,
      SubsType subsType,
      String type) {
    StringBuilder query =
        new StringBuilder(
            CREATE_SUB_SQL
                .replace("$1", subscriptionDataResult.dataBrokerResult().getString("id"))
                .replace("$2", subsType.type)
                .replace("$3", subscriptionDataResult.dataBrokerResult().getString("id"))
                .replace("$4", postModelSubscription.getEntities())
                .replace("$5", postModelSubscription.getExpiry())
                .replace("$6", subscriptionDataResult.cacheResult().getString("name"))
                .replace("$7", subscriptionDataResult.cacheResult().toString())
                .replace("$8", postModelSubscription.getUserId())
                .replace("$9", subscriptionDataResult.cacheResult().getString(RESOURCE_GROUP))
                .replace("$a", subscriptionDataResult.cacheResult().getString("provider"))
                .replace("$b", postModelSubscription.getDelegatorId())
                .replace("$c", type));
    return query;
  }

  private static StringBuilder appendSubsQuery(
      String subId,
      String entities,
      SubscriptionData appendSubscriptionResult,
      SubsType subType,
      String type,
      PostModelSubscription postModelSubscription) {
    StringBuilder appendQuery =
        new StringBuilder(
            APPEND_SUB_SQL
                .replace("$1", subId)
                .replace("$2", subType.type)
                .replace("$3", subId)
                .replace("$4", entities)
                .replace("$5", postModelSubscription.getExpiry())
                .replace("$6", appendSubscriptionResult.cacheResult().getString("name"))
                .replace("$7", appendSubscriptionResult.cacheResult().toString())
                .replace("$8", postModelSubscription.getUserId())
                .replace("$9", appendSubscriptionResult.cacheResult().getString(RESOURCE_GROUP))
                .replace("$a", appendSubscriptionResult.cacheResult().getString("provider"))
                .replace("$b", postModelSubscription.getDelegatorId())
                .replace("$c", type));
    return appendQuery;
  }

  @Override
  public Future<GetResultModel> getSubscription(String subscriptionId, String subType) {
    LOGGER.info("getSubscription() method started");
    Promise<GetResultModel> promise = Promise.promise();
    LOGGER.info("sub id :: " + subscriptionId);
    getEntityName(subscriptionId)
        .compose(
            postgresSuccess -> {
              return dataBrokerService
                  .listStreamingSubscription(subscriptionId)
                  .map(GetResultModel::new);
            })
        .onComplete(
            getDataBroker -> {
              if (getDataBroker.succeeded()) {
                promise.complete(getDataBroker.result());
              } else {
                promise.fail(getDataBroker.cause());
              }
            });
    return promise.future();
  }

  @Override
  public Future<SubscriptionData> createSubscription(PostModelSubscription postModelSubscription) {
    LOGGER.info("createSubscription() method started");
    Promise<SubscriptionData> promise = Promise.promise();
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
              SubscriptionImplModel subscriptionImplModel =
                  new SubscriptionImplModel(
                      postModelSubscription, itemTypeSet.iterator().next(), resourceGroup);
              return dataBrokerService.registerStreamingSubscription(subscriptionImplModel);
            })
        .compose(
            registerStreaming -> {
              JsonObject brokerResponse = registerStreaming.toJson();
              LOGGER.trace("registerStreaming: " + registerStreaming);
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
              LOGGER.trace("cacheResult: " + subscriptionDataResult.cacheResult());

              String type =
                  subscriptionDataResult.cacheResult().containsKey(RESOURCE_GROUP)
                      ? "RESOURCE"
                      : "RESOURCE_GROUP";

              StringBuilder query =
                  createSubQuery(postModelSubscription, subscriptionDataResult, subType, type);
              LOGGER.debug("query: " + query);

              return postgresService
                  .executeQuery1(query.toString())
                  .map(postgresSuccess -> subscriptionDataResult);
            })
        .onSuccess(
            subscriptionDataResultSuccess -> {
              promise.complete(subscriptionDataResultSuccess);
            })
        .onFailure(
            failure -> {
              LOGGER.debug(failure);
              promise.fail(failure);
            });
    return promise.future();
  }

  @Override
  public Future<GetResultModel> updateSubscription(
      String entities, String queueName, String expiry) {
    LOGGER.info("updateSubscription() method started");
    Promise<GetResultModel> promise = Promise.promise();

    StringBuilder selectQuery =
        new StringBuilder(SELECT_SUB_SQL.replace("$1", queueName).replace("$2", entities));
    LOGGER.debug(selectQuery);

    postgresService
        .executeQuery1(selectQuery.toString())
        .compose(
            selectQueryHandler -> {
              JsonArray resultArray = selectQueryHandler.getResult();
              LOGGER.debug("selectQueryHandler   " + selectQueryHandler.getResult());
              if (resultArray.isEmpty()) {
                JsonObject failJson =
                    getResponseJson(
                        NOT_FOUND.getUrn(),
                        NOT_FOUND.getValue(),
                        NOT_FOUND.getDescription(),
                        "Subscription not found for [queue,entity]");
                return Future.failedFuture(failJson.toString());
              }
              StringBuilder updateQuery =
                  new StringBuilder(
                      UPDATE_SUB_SQL
                          .replace("$1", expiry)
                          .replace("$2", queueName)
                          .replace("$3", entities));
              LOGGER.trace("updateQuery : " + updateQuery);
              return postgresService.executeQuery1(updateQuery.toString());
            })
        .onSuccess(
            pgHandler -> {
              List<String> resultEntities = new ArrayList<String>();
              resultEntities.add(entities);
              promise.complete(new GetResultModel(resultEntities));
            })
        .onFailure(
            failure -> {
              LOGGER.error(failure);
              promise.fail(failure);
            });

    return promise.future();
  }

  @Override
  public Future<GetResultModel> appendSubscription(
      PostModelSubscription postModelSubscription, String subsId) {
    LOGGER.info("appendSubscription() method started");
    String entities = postModelSubscription.getEntities();
    JsonObject cacheJson = new JsonObject().put("key", entities).put("type", CATALOGUE_CACHE);
    SubsType subType = SubsType.valueOf(postModelSubscription.getSubscriptionType());
    Promise<GetResultModel> promise = Promise.promise();
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
              LOGGER.trace("appendStreaming: " + appendStreaming);

              JsonObject cacheJson1 =
                  new JsonObject().put("key", entities).put("type", CATALOGUE_CACHE);
              return cacheService
                  .get(cacheJson1)
                  .map(
                      cacheResult ->
                          new SubscriptionData(
                              new JsonObject(), cacheResult, new SubscriptionResponseModel()));
            })
        .compose(
            appendSubscriptionResult -> {
              LOGGER.trace("cacheResult: " + appendSubscriptionResult.cacheResult());
              String type =
                  appendSubscriptionResult.cacheResult().containsKey(RESOURCE_GROUP)
                      ? "RESOURCE"
                      : "RESOURCE_GROUP";

              StringBuilder appendQuery =
                  appendSubsQuery(
                      subsId,
                      entities,
                      appendSubscriptionResult,
                      subType,
                      type,
                      postModelSubscription);
              LOGGER.debug("appendQuery = " + appendQuery);

              return postgresService
                  .executeQuery1(appendQuery.toString())
                  .map(postgresSuccess -> appendSubscriptionResult);
            })
        .onSuccess(
            subscriptionDataResultSuccess -> {
              List<String> listAppend = new ArrayList<String>();
              listAppend.add(entities);
              promise.complete(new GetResultModel(listAppend));
            })
        .onFailure(
            failed -> {
              LOGGER.error("Failed :: " + failed);
              promise.fail(failed);
            });
    return promise.future();
  }

  @Override
  public Future<DeleteSubsResultModel> deleteSubscription(
      String subsId, String subscriptionType, String userid) {
    LOGGER.info("deleteSubscription() method started");
    LOGGER.info("queueName to delete :: " + subsId);
    Promise<DeleteSubsResultModel> promise = Promise.promise();
    getEntityName(subsId)
        .compose(
            foundId -> {
              return deleteSubscriptionFromPg(subsId);
            })
        .compose(
            postgresSuccess -> {
              return dataBrokerService.deleteStreamingSubscription(subsId, userid);
            })
        .onComplete(
            deleteDataBroker -> {
              if (deleteDataBroker.succeeded()) {
                DeleteSubsResultModel deleteSubsResultModel =
                    new DeleteSubsResultModel(
                        getResponseJson(
                            ResponseUrn.SUCCESS_URN.getUrn(),
                            HttpStatus.SC_OK,
                            SUCCESS,
                            "Subscription deleted Successfully"));
                promise.complete(deleteSubsResultModel);
              } else {
                promise.fail(deleteDataBroker.cause());
              }
            });
    return promise.future();
  }

  private Future<Void> deleteSubscriptionFromPg(String subscriptionId) {
    Promise<Void> promise = Promise.promise();
    String deleteQueueQuery = DELETE_SUB_SQL.replace("$1", subscriptionId);
    LOGGER.trace("delete query- " + deleteQueueQuery);
    postgresService
        .executeQuery1(deleteQueueQuery)
        .onComplete(
            pgHandler -> {
              if (pgHandler.succeeded()) {
                LOGGER.debug("deleted from postgres");
                promise.complete();
              } else {
                LOGGER.error("fail here");
                promise.fail(new ServiceException(0, INTERNAL_SERVER_ERROR.getDescription()));
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
              if (pgHandler.succeeded()) {
                promise.complete(pgHandler.result());
              } else {
                promise.fail(new ServiceException(0, INTERNAL_SERVER_ERROR.getDescription()));
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
                promise.complete(entities);
              } else {
                LOGGER.info("Empty response from database. Entities not found");
                promise.fail(new ServiceException(4, NOT_FOUND.getDescription()));
              }
            });
    return promise.future();
  }
}
