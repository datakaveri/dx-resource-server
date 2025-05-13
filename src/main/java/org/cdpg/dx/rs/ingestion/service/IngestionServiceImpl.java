package org.cdpg.dx.rs.ingestion.service;

import static org.cdpg.dx.common.ErrorCode.*;
import static org.cdpg.dx.common.ErrorMessage.INTERNAL_SERVER_ERROR;
import static org.cdpg.dx.databroker.util.Constants.*;
import static org.cdpg.dx.rs.ingestion.util.Constants.ITEM_TYPES;
import static org.cdpg.dx.rs.ingestion.util.Constants.RESOURCE_GROUP;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceException;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.catalogue.service.CatalogueService;
import org.cdpg.dx.database.postgres.models.*;
import org.cdpg.dx.databroker.model.ExchangeSubscribersResponse;
import org.cdpg.dx.databroker.model.RegisterExchangeModel;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.databroker.util.PermissionOpType;
import org.cdpg.dx.databroker.util.Vhosts;
import org.cdpg.dx.rs.ingestion.dao.IngestionDAO;
import org.cdpg.dx.rs.ingestion.model.AdapterMetaDataModel;
import org.cdpg.dx.rs.ingestion.model.IngestionDTO;

public class IngestionServiceImpl implements IngestionService {
  private static final Logger LOGGER = LogManager.getLogger(IngestionServiceImpl.class);
  private final DataBrokerService dataBroker;
  private final IngestionDAO ingestionDAO;
  private final CatalogueService catalogueService;

  public IngestionServiceImpl(
      CatalogueService catalogueService, DataBrokerService dataBroker, IngestionDAO ingestionDAO) {
    this.catalogueService = catalogueService;
    this.dataBroker = dataBroker;
    this.ingestionDAO = ingestionDAO;
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
      return ex.getMessage();
    }
  }

  private static String getRoutingKey(String entitiesId, JsonObject cacheServiceResult) {
    String routingKey;
    try {
      Set<String> type = new HashSet<String>(cacheServiceResult.getJsonArray("type").getList());
      Set<String> itemTypeSet = type.stream().map(e -> e.split(":")[1]).collect(Collectors.toSet());
      itemTypeSet.retainAll(ITEM_TYPES);

      String resourceGroup;
      if (!itemTypeSet.contains("Resource")) {
        resourceGroup = cacheServiceResult.getString("id");
        routingKey = resourceGroup + DATA_WILDCARD_ROUTINGKEY;
      } else {
        resourceGroup = cacheServiceResult.getString("resourceGroup");
        routingKey = resourceGroup + "/." + entitiesId;
      }
    } catch (Exception ex) {
      LOGGER.error("Error while getting resourceGroup {}", ex.getMessage());
      return ex.getMessage();
    }
    return routingKey;
  }

  @Override
  public Future<RegisterExchangeModel> registerAdapter(String entitiesId, String userId) {
    Promise<RegisterExchangeModel> promise = Promise.promise();
    if (entitiesId == null || entitiesId.isEmpty() || userId == null || userId.isEmpty()) {
      promise.fail(new ServiceException(ERROR_BAD_REQUEST, "Invalid input or blank value"));
      return promise.future();
    }
    ingestionDAO
        .getAdapterDetailsByExchangeName(entitiesId)
        .onSuccess(
            postgresHandler -> {
              if (!postgresHandler.isEmpty()) {
                LOGGER.error("Adapter already exists, conflict");
                // TODO: change to DXException
                promise.fail(new ServiceException(ERROR_CONFLICT, EXCHANGE_EXISTS));
              } else {
                LOGGER.debug("Adapter does not exist, creating new adapter");

                catalogueService
                    .fetchCatalogueInfo(entitiesId)
                    .compose(
                        catalogueResult -> {
                          Optional<String> resourceGroup =
                              Optional.ofNullable(getResourceGroup(catalogueResult));
                          Optional<String> routingKey =
                              Optional.ofNullable(getRoutingKey(entitiesId, catalogueResult));
                          if (resourceGroup.isEmpty() || routingKey.isEmpty()) {
                            return Future.failedFuture(
                                new ServiceException(
                                    ERROR_BAD_REQUEST, "Resource group or routing key is missing"));
                          }
                          return dataBroker
                              .registerExchange(userId, resourceGroup.get(), Vhosts.IUDX_PROD)
                              .map(
                                  registerExchange ->
                                      new AdapterMetaDataModel(
                                          registerExchange, catalogueResult, routingKey.get()));
                        })
                    .compose(
                        registerExchangeHandler -> {
                          LOGGER.debug("Exchanges created in databroker");
                          return dataBroker
                              .updatePermission(
                                  userId,
                                  registerExchangeHandler.registerExchange().getExchangeName(),
                                  PermissionOpType.ADD_WRITE,
                                  Vhosts.IUDX_PROD)
                              .map(updatePermission -> registerExchangeHandler);
                        })
                    .compose(
                        registerExchangeHandler -> {
                          LOGGER.debug("Permission updated.");
                          return dataBroker
                              .queueBinding(
                                  registerExchangeHandler.registerExchange().getExchangeName(),
                                  DATABASE_QUEUE,
                                  registerExchangeHandler.routingKey(),
                                  Vhosts.IUDX_PROD)
                              .map(bindQueueHandler -> registerExchangeHandler);
                        })
                    .compose(
                        registerExchangeHandler -> {
                          LOGGER.debug("Success : Data Base Queue Binding successful.");
                          return dataBroker
                              .queueBinding(
                                  registerExchangeHandler.registerExchange().getExchangeName(),
                                  REDIS_LATEST_QUEUE,
                                  registerExchangeHandler.routingKey(),
                                  Vhosts.IUDX_PROD)
                              .map(bindQueueHandler -> registerExchangeHandler);
                        })
                    .compose(
                        registerExchangeHandler -> {
                          LOGGER.debug("Success : Redis Queue Binding successful.");
                          return dataBroker
                              .queueBinding(
                                  registerExchangeHandler.registerExchange().getExchangeName(),
                                  QUEUE_SUBS,
                                  registerExchangeHandler.routingKey(),
                                  Vhosts.IUDX_PROD)
                              .map(bindQueueHandler -> registerExchangeHandler);
                        })
                    .compose(
                        registerExchangeHandler -> {
                          LOGGER.debug("Success : Subscription-Monitoring Queue Binding");
                          return ingestionDAO
                              .insertAdapterDetails(
                                  new IngestionDTO(
                                      null,
                                      registerExchangeHandler.registerExchange().getExchangeName(),
                                      registerExchangeHandler.catalogueResult().getString("id"),
                                      registerExchangeHandler.catalogueResult().getString("name"),
                                      registerExchangeHandler.catalogueResult(),
                                      userId,
                                      null,
                                      null,
                                      UUID.fromString(
                                          registerExchangeHandler
                                              .catalogueResult()
                                              .getString("provider"))))
                              .map(pgHandler -> registerExchangeHandler);
                        })
                    .onSuccess(
                        registerExchangeHandler -> {
                          LOGGER.debug("Success: Data inserted in postgres");
                          promise.complete(registerExchangeHandler.registerExchange());
                        })
                    .onFailure(
                        failure -> {
                          LOGGER.error(failure.getMessage());
                          promise.fail(failure);
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

  @Override
  public Future<Void> deleteAdapter(String exchangeName, String userId) {
    Promise<Void> promise = Promise.promise();
    dataBroker
        .deleteExchange(exchangeName, userId, Vhosts.IUDX_PROD)
        .compose(
            deleteAdaptorHandler -> {
              LOGGER.info("Adapter deleted");
              return dataBroker.updatePermission(
                  userId, exchangeName, PermissionOpType.DELETE_WRITE, Vhosts.IUDX_PROD);
            })
        .compose(
            updatePermissionHandler -> {
              LOGGER.info("Permission deleted for exchange successfully");
              return ingestionDAO.deleteAdapterByExchangeName(exchangeName);
            })
        .onSuccess(
            pgHandler -> {
              LOGGER.info("Deleted from postgres.");
              promise.complete();
            })
        .onFailure(
            failure -> {
              LOGGER.error("fail to delete adapter");
              promise.fail(failure);
            });
    return promise.future();
  }

  @Override
  public Future<ExchangeSubscribersResponse> getAdapterDetails(String exchangeName) {
    Promise<ExchangeSubscribersResponse> promise = Promise.promise();
    dataBroker
        .listExchange(exchangeName, Vhosts.IUDX_PROD)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                promise.complete(handler.result());
              } else {
                promise.fail(handler.cause());
              }
            });
    return promise.future();
  }

  @Override
  public Future<Void> publishDataFromAdapter(JsonArray request) {
    Promise<Void> promise = Promise.promise();
    String entities = request.getJsonObject(0).getJsonArray("entities").getValue(0).toString();
    catalogueService
        .fetchCatalogueInfo(entities)
        .compose(
            catalogueResult -> {
              String resourceGroupId = getResourceGroup(catalogueResult);
              if (resourceGroupId == null) {
                return Future.failedFuture(
                    new ServiceException(ERROR_BAD_REQUEST, "resourceGroupId not found"));
              }

              LOGGER.debug("Info : resourceGroupId  " + resourceGroupId);
              String routingKey = resourceGroupId + "/." + entities;
              request.remove("entities");

              for (int i = 0; i < request.size(); i++) {
                JsonObject jsonObject = request.getJsonObject(i);
                jsonObject.remove("entities");
                jsonObject.put(ID, entities);
              }
              LOGGER.trace(request);
              LOGGER.debug("Info : routingKey {}", routingKey);
              return dataBroker.publishMessageExternal(resourceGroupId, routingKey, request);
            })
        .onSuccess(
            resultHandler -> {
              LOGGER.info("result handler ::{}", resultHandler);
              if (resultHandler.equalsIgnoreCase("success")) {
                promise.complete();
              }
            })
        .onFailure(
            failure -> {
              LOGGER.error("Error occurred while publishing data from adapter", failure);
              promise.fail(new ServiceException(ERROR_INTERNAL_SERVER, INTERNAL_SERVER_ERROR));
            });

    return promise.future();
  }

  @Override
  public Future<QueryResult> getAllAdapterDetailsForUser(String iid) {
    LOGGER.debug("getAllAdapterDetailsForUser() started");
    Promise<QueryResult> promise = Promise.promise();

    catalogueService
        .fetchCatalogueInfo(iid)
        .compose(
            catalogueResult -> {
              String providerId = catalogueResult.getString("provider");
              return ingestionDAO.getAllAdaptersDetailsByProviderId(providerId);
            })
        .onSuccess(
            postgresServiceHandler -> {
              promise.complete(postgresServiceHandler);
            })
        .onFailure(
            failure -> {
              LOGGER.error("failed");
              promise.fail(new ServiceException(ERROR_INTERNAL_SERVER, INTERNAL_SERVER_ERROR));
            });
    return promise.future();
  }
}
