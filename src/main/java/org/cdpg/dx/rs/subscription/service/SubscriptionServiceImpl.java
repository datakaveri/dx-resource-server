package org.cdpg.dx.rs.subscription.service;

import static org.cdpg.dx.common.ErrorMessage.INTERNAL_SERVER_ERROR;
import static org.cdpg.dx.common.ErrorMessage.NOT_FOUND_ERROR;
import static org.cdpg.dx.rs.subscription.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.catalogue.service.CatalogueService;
import org.cdpg.dx.common.exception.DxNotFoundException;
import org.cdpg.dx.common.exception.DxSubscriptionException;
import org.cdpg.dx.common.exception.QueueAlreadyExistsException;
import org.cdpg.dx.databroker.model.RegisterQueueModel;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.databroker.util.PermissionOpType;
import org.cdpg.dx.databroker.util.Vhosts;
import org.cdpg.dx.rs.subscription.dao.SubscriptionServiceDAO;
import org.cdpg.dx.rs.subscription.model.*;
import org.cdpg.dx.rs.subscription.util.SubsType;

public class SubscriptionServiceImpl implements SubscriptionService {
  private static final Logger LOGGER = LogManager.getLogger(SubscriptionServiceImpl.class);
  private final DataBrokerService dataBrokerService;
  private final CatalogueService catalogueService;
  private final SubscriptionServiceDAO subscriptionServiceDAO;

  public SubscriptionServiceImpl(
      SubscriptionServiceDAO subscriptionServiceDAO,
      DataBrokerService dataBrokerService,
      CatalogueService catalogueService) {
    this.subscriptionServiceDAO = subscriptionServiceDAO;
    this.dataBrokerService = dataBrokerService;
    this.catalogueService = catalogueService;
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
      return "Error: " + ex.getMessage();
    }
  }

  private static String getRoutingKey(String entitiesId, JsonObject catalogueServiceResult) {
    String routingKey;
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
      return "Error: " + ex.getMessage();
    }
    return routingKey;
  }

  @Override
  public Future<GetSubscriptionModel> getSubscription(String subscriptionId) {
    LOGGER.info("getSubscription() method started");
    Promise<GetSubscriptionModel> promise = Promise.promise();
    LOGGER.info("sub id :: {}", subscriptionId);
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
    subscriptionServiceDAO
        .getEntityIdByQueueName(queueName)
        .onSuccess(
            postgresHandler -> {
              if (!postgresHandler.equalsIgnoreCase("Not Found")) {
                LOGGER.error("Queue already exists, conflict");
                // TODO: change to DXException
                promise.fail(new QueueAlreadyExistsException("Queue already exist"));
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
                          return queueBinding(subscriptionMetaDataHandler, entityId, queueName)
                              .map(queueBindHandler -> subscriptionMetaDataHandler);
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
                          String type = getType(updateHandler.catalogueInfo());
                          return subscriptionServiceDAO
                              .insertSubscription(
                                  new SubscriptionDTO(
                                      queueName,
                                      subType.type,
                                      queueName,
                                      entityId,
                                      postSubscriptionModel.getExpiry(),
                                      updateHandler.catalogueInfo().getString("name"),
                                      updateHandler.catalogueInfo(),
                                      postSubscriptionModel.getUserId(),
                                      updateHandler.catalogueInfo().getString(RESOURCE_GROUP),
                                      updateHandler.catalogueInfo().getString(PROVIDER),
                                      postSubscriptionModel.getDelegatorId(),
                                      type,
                                      null,
                                      null))
                              .map(postgres -> updateHandler.registerQueueModel());
                        })
                    .onSuccess(
                        successHandler -> {
                          LOGGER.debug("data inserted in postgres successful");
                          promise.complete(successHandler);
                        })
                    .onFailure(
                        failureHandler -> {
                          LOGGER.error("failed "+failureHandler);
                          promise.fail(failureHandler);
                        });
              }
            })
        .onFailure(
            failure -> {
              LOGGER.error("Failed to query postgres service "+failure);
              promise.fail(failure);
            });

    return promise.future();
  }

  private Future<Void> queueBinding(
      SubscriptionMetaData subscriptionMetaDataHandler, String entityId, String queueName) {
    Optional<String> resourceGroup =
        Optional.ofNullable(getResourceGroup(subscriptionMetaDataHandler.catalogueInfo()));
    Optional<String> routingKey =
        Optional.ofNullable(getRoutingKey(entityId, subscriptionMetaDataHandler.catalogueInfo()));
    if (resourceGroup.isEmpty() || routingKey.isEmpty()) {
      return Future.failedFuture(
          new DxSubscriptionException("Resource group or routing key is missing"));
    }
    return dataBrokerService.queueBinding(
        resourceGroup.get(), queueName, routingKey.get(), Vhosts.IUDX_PROD);
  }

  @Override
  public Future<String> updateSubscription(String entityId, String queueName, String expiry) {
    LOGGER.info("updateSubscription() method started");
    Promise<String> promise = Promise.promise();
    subscriptionServiceDAO
        .getSubscriptionByQueueNameAndEntityId(queueName, entityId)
        .compose(
            selectQueryHandler -> {
              LOGGER.debug("selectQueryHandler {}", selectQueryHandler);
              if (selectQueryHandler.isEmpty()) {
                return Future.failedFuture(
                    new DxSubscriptionException("Subscription not found for [queue,entity]"));
              }
              return subscriptionServiceDAO.updateSubscriptionExpiryByQueueNameAndEntityId(
                  queueName, entityId, expiry);
            })
        .onSuccess(
            pgHandler -> {
              LOGGER.debug("updated in subscription successful");
              promise.complete(entityId);
            })
        .onFailure(
            failure -> {
              LOGGER.error(failure);
              promise.fail(failure);
            });

    return promise.future();
  }

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
                    new DxSubscriptionException("Resource group or routing key is missing"));
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
              return subscriptionServiceDAO
                  .getSubscriptionByQueueNameAndEntityId(
                      subscriptionId, postSubscriptionModel.getEntityId())
                  .map(appendSelectQuery -> new AppendMetaData(appendSelectQuery, catalogueResult));
            })
        .compose(
            selectSuccess -> {
              LOGGER.trace("catalogueResult: {}", selectSuccess.catalogue().toString());
              if (selectSuccess.appendSelectQueryResult().isEmpty()) {
                SubsType subType = SubsType.valueOf(postSubscriptionModel.getSubscriptionType());
                String type = getType(selectSuccess.catalogue());
                return subscriptionServiceDAO.insertSubscription(
                    new SubscriptionDTO(
                        subscriptionId,
                        subType.type,
                        subscriptionId,
                        entityId,
                        postSubscriptionModel.getExpiry(),
                        selectSuccess.catalogue().getString("name"),
                        selectSuccess.catalogue(),
                        postSubscriptionModel.getUserId(),
                        selectSuccess.catalogue().getString(RESOURCE_GROUP),
                        selectSuccess.catalogue().getString(PROVIDER),
                        postSubscriptionModel.getDelegatorId(),
                        type,
                        null,
                        null));
              } else {
                return Future.succeededFuture();
              }
            })
        .onSuccess(
            subscriptionDataResultSuccess -> {
              LOGGER.info("successfully subs data inserted");
              promise.complete(postSubscriptionModel.getEntityId());
            })
        .onFailure(
            failed -> {
              LOGGER.error("Failed :: {}", failed.getMessage());
              promise.fail(failed);
            });
    return promise.future();
  }

  private String getType(JsonObject catalogue) {
    return catalogue.containsKey(RESOURCE_GROUP) ? "RESOURCE" : "RESOURCE_GROUP";
  }

  @Override
  public Future<String> deleteSubscription(String subscriptionId, String userid) {
    LOGGER.info("deleteSubscription() method started");
    LOGGER.info("queueName to delete :: {}", subscriptionId);
    Promise<String> promise = Promise.promise();
    getEntityName(subscriptionId)
        .compose(
            foundEntityId -> {
              LOGGER.debug("entityId found {}", foundEntityId);
              return subscriptionServiceDAO
                  .deleteSubscriptionBySubId(subscriptionId)
                  .map(result -> foundEntityId);
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

  @Override
  public Future<List<SubscriberDetails>> getAllSubscriptionQueueForUser(String userId) {
    LOGGER.info("getAllSubscriptionQueueForUser() method started{}", userId);
    Promise<List<SubscriberDetails>> promise = Promise.promise();
    subscriptionServiceDAO
        .getSubscriptionByUserId(userId)
        .onComplete(
            pgHandler -> {
              LOGGER.info(
                  "pg handler : {} and size {}",
                  pgHandler.result().isEmpty(),
                  pgHandler.result().size());
              if (!pgHandler.result().isEmpty()) {
                List<SubscriberDetails> subscriberDetails =
                    pgHandler.result().stream()
                        .map(obj -> new SubscriberDetails((JsonObject) obj))
                        .collect(Collectors.toList());
                promise.complete(subscriberDetails);
              } else {
                promise.fail(new DxSubscriptionException(INTERNAL_SERVER_ERROR));
              }
            });
    return promise.future();
  }

  private Future<String> getEntityName(String subscriptionId) {
    Promise<String> promise = Promise.promise();
    subscriptionServiceDAO
        .getEntityIdByQueueName(subscriptionId)
        .onComplete(
            entityResult -> {
              if (entityResult.succeeded()) {
                if (entityResult.result().equalsIgnoreCase("Not Found")) {
                  promise.fail(new DxNotFoundException(NOT_FOUND_ERROR));
                } else {
                  promise.complete(entityResult.result());
                }
              } else {
                LOGGER.error("error:: {}", entityResult.cause().getMessage());
                promise.fail(entityResult.cause());
              }
            });
    return promise.future();
  }
}
