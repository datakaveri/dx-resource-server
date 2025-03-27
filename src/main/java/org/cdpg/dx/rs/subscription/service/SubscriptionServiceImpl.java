package org.cdpg.dx.rs.subscription.service;

import static org.cdpg.dx.common.ErrorCode.*;
import static org.cdpg.dx.common.ErrorMessage.INTERNAL_SERVER_ERROR;
import static org.cdpg.dx.common.ErrorMessage.NOT_FOUND_ERROR;
import static org.cdpg.dx.rs.subscription.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.catalogue.service.CatalogueService;
import org.cdpg.dx.database.postgres.models.*;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.databroker.model.RegisterQueueModel;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.databroker.util.PermissionOpType;
import org.cdpg.dx.databroker.util.Vhosts;
import org.cdpg.dx.rs.subscription.model.*;
import org.cdpg.dx.rs.subscription.util.SubsType;

public class SubscriptionServiceImpl implements SubscriptionService {
  private static final Logger LOGGER = LogManager.getLogger(SubscriptionServiceImpl.class);
  private final PostgresService postgresService;
  private final DataBrokerService dataBrokerService;
  private final CatalogueService catalogueService;

  public SubscriptionServiceImpl(
      PostgresService postgresService,
      DataBrokerService dataBrokerService,
      CatalogueService catalogueService) {
    this.postgresService = postgresService;
    this.dataBrokerService = dataBrokerService;
    this.catalogueService = catalogueService;
  }

  // TODO: Need to correct postgresmodel
  private static InsertQuery createSubQuery(
      PostSubscriptionModel postSubscriptionModel,
      SubscriptionMetaData subscriptionMetaDataResult,
      SubsType subsType,
      String queueName) {
    String type =
        subscriptionMetaDataResult.catalogueInfo().containsKey(RESOURCE_GROUP)
            ? "RESOURCE"
            : "RESOURCE_GROUP";
    List<String> columns =
        List.of(
            "_id",
            "_type",
            "queue_name",
            "entity",
            "expiry",
            "dataset_name",
            "dataset_json",
            "user_id",
            "resource_group",
            "provider_id",
            "delegator_id",
            "item_type");

    List<Object> values =
        List.of(
            queueName,
            subsType.type,
            queueName,
            postSubscriptionModel.getEntityId(),
            postSubscriptionModel.getExpiry(),
            subscriptionMetaDataResult.catalogueInfo().getString("name"),
            subscriptionMetaDataResult.catalogueInfo().toString(),
            postSubscriptionModel.getUserId(),
            subscriptionMetaDataResult.catalogueInfo().getString(RESOURCE_GROUP),
            subscriptionMetaDataResult.catalogueInfo().getString("provider"),
            postSubscriptionModel.getDelegatorId(),
            type);
    return new InsertQuery("subscriptions", columns, values);
  }

  // TODO: Need to correct postgresmodel
  private static InsertQuery appendSubsQuery(
      String subId,
      String entities,
      JsonObject catalogueResult,
      SubsType subType,
      PostSubscriptionModel postSubscriptionModel) {
    String type = catalogueResult.containsKey(RESOURCE_GROUP) ? "RESOURCE" : "RESOURCE_GROUP";
    List<String> columns =
        List.of(
            "_id",
            "_type",
            "queue_name",
            "entity",
            "expiry",
            "dataset_name",
            "dataset_json",
            "user_id",
            "resource_group",
            "provider_id",
            "delegator_id",
            "item_type");

    List<Object> values =
        List.of(
            subId,
            subType.type,
            subId,
            entities,
            postSubscriptionModel.getExpiry(),
            catalogueResult.getString("name"),
            catalogueResult.toString(),
            postSubscriptionModel.getUserId(),
            catalogueResult.getString(RESOURCE_GROUP),
            catalogueResult.getString("provider"),
            postSubscriptionModel.getDelegatorId(),
            type);
    return new InsertQuery("subscriptions", columns, values);
  }

  private static String getResourceGroup(JsonObject catalogueResult) {
    try {
      if (catalogueResult.containsKey(RESOURCE_GROUP)) {
        return catalogueResult.getString(RESOURCE_GROUP);
      } else {
        return catalogueResult.getString(ID);
      }
    } catch (Exception ex) {
      LOGGER.error("Error while getting resourceGroup {}", ex.getMessage());
      return null;
    }
  }

  private static String getRoutingKey(String entitiesId, JsonObject catalogueServiceResult) {
    String routingKey = null;
    try {
      Set<String> type = new HashSet<String>(catalogueServiceResult.getJsonArray("type").getList());
      Set<String> itemTypeSet = type.stream().map(e -> e.split(":")[1]).collect(Collectors.toSet());
      itemTypeSet.retainAll(ITEM_TYPES);

      String resourceGroup;
      if (!itemTypeSet.contains("Resource")) {
        resourceGroup = catalogueServiceResult.getString("id");
        routingKey = resourceGroup + DATA_WILDCARD_ROUTINGKEY;
      } else {
        resourceGroup = catalogueServiceResult.getString("resourceGroup");
        routingKey = resourceGroup + "/." + entitiesId;
      }
    } catch (Exception ex) {
      LOGGER.error("Error while getting resourceGroup {}", ex.getMessage());
      return null;
    }
    return routingKey;
  }

  @Override
  public Future<GetSubscriptionModel> getSubscription(String subscriptionId) {
    LOGGER.info("getSubscription() method started");
    Promise<GetSubscriptionModel> promise = Promise.promise();
    LOGGER.info("sub id :: " + subscriptionId);
    getEntityName(subscriptionId)
        .compose(
            postgresSuccess -> {
              LOGGER.debug("entities found {}", postgresSuccess);
              return dataBrokerService
                  .listQueue(subscriptionId, Vhosts.IUDX_PROD)
                  .map(listStream -> new GetSubscriptionModel(listStream, postgresSuccess));
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
  public Future<RegisterQueueModel> createSubscription(
      PostSubscriptionModel postSubscriptionModel) {
    LOGGER.info("createSubscription() method started");
    Promise<RegisterQueueModel> promise = Promise.promise();
    SubsType subType = SubsType.valueOf(postSubscriptionModel.getSubscriptionType());
    String entityId = postSubscriptionModel.getEntityId();
    String queueName = postSubscriptionModel.getUserId() + "/" + postSubscriptionModel.getName();
    ConditionComponent conditionComponent =
        new Condition("queue_name", Condition.Operator.EQUALS, List.of(queueName));
    SelectQuery selectQuery =
        new SelectQuery(
            "subscriptions", List.of("queue_name"), conditionComponent, null, null, null, null);
    postgresService
        .select(selectQuery)
        .onSuccess(
            postgresHandler -> {
              // TODO:: Check this once catalogueInfo model and postgresmodel is available, here we
              // need to return if data present in database
              var result = postgresHandler.rows();
              if (!result.isEmpty()) {
                LOGGER.error("Adapter already exists, conflict");
                // TODO: change to DXException
                promise.fail(new ServiceException(ERROR_CONFLICT, QUEUE_ALREADY_EXISTS));
              } else {
                dataBrokerService
                    .registerQueue(postSubscriptionModel.getUserId(), queueName, Vhosts.IUDX_PROD)
                    .compose(
                        registerQueueHandler -> {
                          LOGGER.debug("registerQueueHandler success");
                          return catalogueService
                              .fetchCatalogueInfo(entityId)
                              .map(
                                  catalogueResult ->
                                      new SubscriptionMetaData(
                                          catalogueResult, registerQueueHandler));
                        })
                    .compose(
                        subscriptionMetaDataHandler -> {
                          LOGGER.debug("subscriptionMetaDataHandler success");
                          Optional<String> resourceGroup =
                              Optional.ofNullable(
                                  getResourceGroup(subscriptionMetaDataHandler.catalogueInfo()));
                          Optional<String> routingKey =
                              Optional.ofNullable(
                                  getRoutingKey(
                                      entityId, subscriptionMetaDataHandler.catalogueInfo()));
                          if (resourceGroup.isEmpty() || routingKey.isEmpty()) {
                            return Future.failedFuture(
                                new ServiceException(
                                    ERROR_BAD_REQUEST, "Resource group or routing key is missing"));
                          }
                          return dataBrokerService
                              .queueBinding(
                                  resourceGroup.get(),
                                  queueName,
                                  routingKey.get(),
                                  Vhosts.IUDX_PROD)
                              .map(queueBinding -> subscriptionMetaDataHandler);
                        })
                    .compose(
                        subscriptionMetaDataHandler -> {
                          LOGGER.debug("binding Queue successful");
                          return dataBrokerService
                              .updatePermission(
                                  postSubscriptionModel.getUserId(),
                                  queueName,
                                  PermissionOpType.ADD_READ,
                                  Vhosts.IUDX_PROD)
                              .map(updatePermission -> subscriptionMetaDataHandler);
                        })
                    .compose(
                        updateHandler -> {
                          LOGGER.debug("update permission successful");
                          InsertQuery insertQuery =
                              createSubQuery(
                                  postSubscriptionModel, updateHandler, subType, queueName);
                          return postgresService
                              .insert(insertQuery)
                              .map(postgres -> updateHandler.registerQueueModel());
                        })
                    .onSuccess(
                        successHandler -> {
                          LOGGER.debug("data inserted in postgres successful");
                          promise.complete(successHandler);
                        })
                    .onFailure(
                        failureHandler -> {
                          LOGGER.error("failed");
                          promise.fail(failureHandler);
                        });
              }
            })
        .onFailure(
            failure -> {
              LOGGER.error("Failed to query postgres service");
              promise.fail(failure);
            });

    return promise.future();
  }

  // TODO: Need to correct postgresmodel
  @Override
  public Future<String> updateSubscription(String entities, String queueName, String expiry) {
    LOGGER.info("updateSubscription() method started");
    Promise<String> promise = Promise.promise();
    List<String> columns = List.of("*");
    List<String> conditionColumns = List.of("queue_name", "entity");
    ConditionComponent conditionComponent = new Condition(null, null, null);
    // ConditionComponent conditionComponent1 = new ConditionGroup(null,null);
    SelectQuery selectQuery =
        new SelectQuery("subscriptions", columns, null, null, null, null, null);

    postgresService
        .select(selectQuery)
        .compose(
            selectQueryHandler -> {
              List<JsonObject> resultRow = selectQueryHandler.rows();
              LOGGER.debug("selectQueryHandler   " + selectQueryHandler.rows());
              if (resultRow.isEmpty()) {
                return Future.failedFuture(
                    new ServiceException(
                        ERROR_BAD_REQUEST, "Subscription not found for [queue,entity]"));
              }
              UpdateQuery updateQuery =
                  new UpdateQuery("subscriptions", null, null, null, null, null);
              return postgresService.update(updateQuery);
            })
        .onSuccess(
            pgHandler -> {
              LOGGER.debug("updated in subscription successful");
              promise.complete(entities);
            })
        .onFailure(
            failure -> {
              LOGGER.error(failure);
              promise.fail(failure);
            });

    return promise.future();
  }

  // TODO: Need to correct postgresmodel
  @Override
  public Future<String> appendSubscription(
      PostSubscriptionModel postSubscriptionModel, String subscriptionId) {
    LOGGER.info("appendSubscription() method started");
    String entityId = postSubscriptionModel.getEntityId();
    Promise<String> promise = Promise.promise();

    catalogueService
        .fetchCatalogueInfo(entityId)
        .compose(
            catalogueResult -> {
              Optional<String> resourceGroup =
                  Optional.ofNullable(getResourceGroup(catalogueResult));
              Optional<String> routingKey =
                  Optional.ofNullable(getRoutingKey(entityId, catalogueResult));
              if (resourceGroup.isEmpty() || routingKey.isEmpty()) {
                return Future.failedFuture(
                    new ServiceException(
                        ERROR_BAD_REQUEST, "Resource group or routing key is missing"));
              }
              return dataBrokerService
                  .queueBinding(
                      resourceGroup.get(), subscriptionId, routingKey.get(), Vhosts.IUDX_PROD)
                  .map(queueBind -> catalogueResult);
            })
        .compose(
            queueBindingHandler -> {
              LOGGER.debug("binding Queue successful");
              return dataBrokerService
                  .updatePermission(
                      postSubscriptionModel.getUserId(),
                      subscriptionId,
                      PermissionOpType.ADD_READ,
                      Vhosts.IUDX_PROD)
                  .map(updatePermission -> queueBindingHandler);
            })
        .compose(
            catalogueResult -> {
              LOGGER.trace("appendStreaming successful ");
              LOGGER.trace("catalogueResult: " + catalogueResult.toString());
              SubsType subType = SubsType.valueOf(postSubscriptionModel.getSubscriptionType());
              InsertQuery insertQuery =
                  appendSubsQuery(
                      subscriptionId, entityId, catalogueResult, subType, postSubscriptionModel);
              return postgresService.insert(insertQuery);
            })
        .onSuccess(
            subscriptionDataResultSuccess -> {
              promise.complete(postSubscriptionModel.getEntityId());
            })
        .onFailure(
            failed -> {
              LOGGER.error("Failed :: " + failed);
              promise.fail(failed);
            });
    return promise.future();
  }

  @Override
  public Future<String> deleteSubscription(String subscriptionId, String userid) {
    LOGGER.info("deleteSubscription() method started");
    LOGGER.info("queueName to delete :: " + subscriptionId);
    Promise<String> promise = Promise.promise();
    getEntityName(subscriptionId)
        .compose(
            foundEntityId -> {
              LOGGER.debug("entityId found {}", foundEntityId);
              return deleteSubscriptionFromPg(subscriptionId).map(result -> foundEntityId);
            })
        .compose(
            postgresSuccess -> {
              LOGGER.debug("deleted from postgres successful");
              return dataBrokerService
                  .deleteQueue(subscriptionId, userid, Vhosts.IUDX_PROD)
                  .map(result -> postgresSuccess);
            })
        .onComplete(
            deleteDataBroker -> {
              if (deleteDataBroker.succeeded()) {
                promise.complete(deleteDataBroker.result());
              } else {
                promise.fail(deleteDataBroker.cause());
              }
            });
    return promise.future();
  }

  // TODO: Need to correct postgresmodel
  private Future<Void> deleteSubscriptionFromPg(String subscriptionId) {
    Promise<Void> promise = Promise.promise();
    ConditionComponent conditionComponent =
        new Condition("queue_name", Condition.Operator.EQUALS, List.of(subscriptionId));
    DeleteQuery deleteQuery = new DeleteQuery("subscriptions", conditionComponent, null, null);

    postgresService
        .delete(deleteQuery)
        .onComplete(
            pgHandler -> {
              if (pgHandler.succeeded()) {
                LOGGER.debug("deleted from postgres");
                promise.complete();
              } else {
                LOGGER.error("fail here");
                promise.fail(new ServiceException(ERROR_INTERNAL_SERVER, INTERNAL_SERVER_ERROR));
              }
            });
    return promise.future();
  }

  // TODO: Need to correct postgresmodel
  @Override
  public Future<List<SubscriberDetails>> getAllSubscriptionQueueForUser(String userId) {
    LOGGER.info("getAllSubscriptionQueueForUser() method started");
    Promise<List<SubscriberDetails>> promise = Promise.promise();
    /*String query = GET_ALL_QUEUE.replace("$1", userId);*/
    List<String> columns = List.of();
    ConditionComponent conditionComponent =
        new Condition("user_id", Condition.Operator.EQUALS, List.of(userId));
    SelectQuery selectQuery =
        new SelectQuery("subscriptions", null, conditionComponent, null, null, null, null);
    postgresService
        .select(selectQuery)
        .onComplete(
            pgHandler -> {
              if (pgHandler.succeeded()) {
                List<JsonObject> result = pgHandler.result().rows();
                List<SubscriberDetails> subscriberDetails =
                    result.stream()
                        .map(obj -> new SubscriberDetails(obj))
                        .collect(Collectors.toList());
                promise.complete(subscriberDetails);
              } else {
                promise.fail(new ServiceException(ERROR_INTERNAL_SERVER, INTERNAL_SERVER_ERROR));
              }
            });
    return promise.future();
  }

  // TODO: Need to correct postgresmodel
  private Future<String> getEntityName(String subscriptionId) {
    Promise<String> promise = Promise.promise();
    ConditionComponent conditionComponent =
        new Condition("queue_name", Condition.Operator.EQUALS, List.of(subscriptionId));
    SelectQuery selectQuery =
        new SelectQuery(
            "subscriptions", List.of("entity"), conditionComponent, null, null, null, null);

    postgresService
        .select(selectQuery)
        .onComplete(
            pgHandler -> {
              if (pgHandler.succeeded() && !pgHandler.result().rows().isEmpty()) {
                String entityId = pgHandler.result().rows().get(0).getString("entity");
                LOGGER.debug("EntityId " + entityId);
                promise.complete(entityId);
              } else {
                LOGGER.info("Empty response from database. EntityId not found");
                promise.fail(new ServiceException(ERROR_NOT_FOUND, NOT_FOUND_ERROR));
              }
            });
    return promise.future();
  }
}
