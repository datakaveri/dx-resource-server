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
import org.cdpg.dx.common.exception.DxBadRequestException;
import org.cdpg.dx.common.exception.DxInternalServerErrorException;
import org.cdpg.dx.common.exception.ExchangeRegistrationException;
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
      promise.fail(new DxBadRequestException("Invalid input or blank value"));
      return promise.future();
    }
      ingestionDAO
              .getAdapterDetailsByExchangeName(entitiesId)
              .onSuccess(postgresHandler -> {
                  if (!postgresHandler.isEmpty()) {
                      LOGGER.error("Adapter already exists for ID: {}", entitiesId);
                      promise.fail(new ExchangeRegistrationException(EXCHANGE_EXISTS));  // Proper DXException used
                      return;
                  }

                  LOGGER.debug("Adapter does not exist. Proceeding to create a new adapter...");

                  catalogueService
                          .fetchCatalogueInfo(entitiesId)
                          .compose(catalogueResult -> {
                              Optional<String> resourceGroup = Optional.ofNullable(getResourceGroup(catalogueResult));
                              Optional<String> routingKey = Optional.ofNullable(getRoutingKey(entitiesId, catalogueResult));

                              if (resourceGroup.isEmpty() || routingKey.isEmpty()) {
                                  return Future.failedFuture(new DxBadRequestException("Missing resource group or routing key"));
                              }

                              return dataBroker
                                      .registerExchange(userId, resourceGroup.get(), Vhosts.IUDX_PROD)
                                      .map(exchange -> new AdapterMetaDataModel(exchange, catalogueResult, routingKey.get()));
                          })
                          .compose(meta -> {
                              LOGGER.debug("Exchange created in DataBroker");
                              return dataBroker
                                      .updatePermission(
                                              userId,
                                              meta.registerExchange().getExchangeName(),
                                              PermissionOpType.ADD_WRITE,
                                              Vhosts.IUDX_PROD)
                                      .map(v -> meta);
                          })
                          .compose(meta -> {
                              LOGGER.debug("Permission granted. Binding to Database queue...");
                              return dataBroker
                                      .queueBinding(
                                              meta.registerExchange().getExchangeName(),
                                              DATABASE_QUEUE,
                                              meta.routingKey(),
                                              Vhosts.IUDX_PROD)
                                      .map(v -> meta);
                          })
                          .compose(meta -> {
                              LOGGER.debug("Database queue bound. Binding to Redis queue...");
                              return dataBroker
                                      .queueBinding(
                                              meta.registerExchange().getExchangeName(),
                                              REDIS_LATEST_QUEUE,
                                              meta.routingKey(),
                                              Vhosts.IUDX_PROD)
                                      .map(v -> meta);
                          })
                          .compose(meta -> {
                              LOGGER.debug("Redis queue bound. Binding to Subscription queue...");
                              return dataBroker
                                      .queueBinding(
                                              meta.registerExchange().getExchangeName(),
                                              QUEUE_SUBS,
                                              meta.routingKey(),
                                              Vhosts.IUDX_PROD)
                                      .map(v -> meta);
                          })
                          .compose(registerExchangeHandler -> {
                              LOGGER.debug("All queues bound. Inserting adapter metadata in Postgres...");
                              IngestionDTO ingestionDTO = new IngestionDTO(
                                      null,
                                      registerExchangeHandler.registerExchange().getExchangeName(),
                                      registerExchangeHandler.catalogueResult().getString("id"),
                                      registerExchangeHandler.catalogueResult().getString("name"),
                                      registerExchangeHandler.catalogueResult(),
                                      userId,
                                      null,
                                      null,
                                      UUID.fromString(registerExchangeHandler.catalogueResult().getString("provider"))
                              );
                              return ingestionDAO.insertAdapterDetails(ingestionDTO).map(pg -> registerExchangeHandler);
                          })
                          .onSuccess(meta -> {
                              LOGGER.debug("Adapter metadata inserted successfully.");
                              promise.complete(meta.registerExchange());
                          })
                          .onFailure(error -> {
                              LOGGER.error("Failed to register adapter: {}", error.getMessage(), error);
                              promise.fail(error);
                          });
              })
              .onFailure(error -> {
                  LOGGER.error("Postgres error while checking adapter existence: {}", error.getMessage(), error);
                  promise.fail(error);
              });


    return promise.future();
  }

  @Override
  public Future<Void> deleteAdapter(String exchangeName, String userId) {
    Promise<Void> promise = Promise.promise();
      dataBroker
              .deleteExchange(exchangeName, userId, Vhosts.IUDX_PROD)
              .compose(deletionResult -> {
                  LOGGER.info("Exchange '{}' deleted successfully for user '{}'.", exchangeName, userId);
                  return dataBroker.updatePermission(
                          userId, exchangeName, PermissionOpType.DELETE_WRITE, Vhosts.IUDX_PROD);
              })
              .compose(permissionResult -> {
                  LOGGER.info("Permissions deleted for exchange '{}'.", exchangeName);
                  return ingestionDAO.deleteAdapterByExchangeName(exchangeName);
              })
              .onSuccess(pgResult -> {
                  LOGGER.info("Adapter '{}' deleted from PostgreSQL.", exchangeName);
                  promise.complete();
              })
              .onFailure(error -> {
                  LOGGER.error("Failed to delete adapter '{}': {}", exchangeName, error.getMessage(), error);
                  promise.fail(error);
              });

    return promise.future();
  }

  @Override
  public Future<ExchangeSubscribersResponse> getAdapterDetails(String exchangeName) {
    Promise<ExchangeSubscribersResponse> promise = Promise.promise();
      dataBroker
              .listExchange(exchangeName, Vhosts.IUDX_PROD)
              .onSuccess(promise::complete)
              .onFailure(promise::fail);
    return promise.future();
  }

  @Override
  public Future<Void> publishDataFromAdapter(JsonArray request) {
    Promise<Void> promise = Promise.promise();
    String entities = request.getJsonObject(0).getJsonArray("entities").getValue(0).toString();
      catalogueService
              .fetchCatalogueInfo(entities)
              .compose(catalogueResult -> {
                  String resourceGroupId = getResourceGroup(catalogueResult);
                  if (resourceGroupId == null) {
                      return Future.failedFuture(new DxBadRequestException("resourceGroupId not found"));
                  }

                  LOGGER.debug("Resource Group ID: {}", resourceGroupId);
                  String routingKey = resourceGroupId + "/." + entities;

                  // Update each JSON object in the request array
                  for (int i = 0; i < request.size(); i++) {
                      JsonObject jsonObject = request.getJsonObject(i);
                      jsonObject.remove("entities");
                      jsonObject.put(ID, entities);
                  }

                  LOGGER.trace("Final request payload: {}", request.encodePrettily());
                  LOGGER.debug("Routing Key: {}", routingKey);

                  return dataBroker.publishMessageExternal(resourceGroupId, routingKey, request);
              })
              .onSuccess(result -> {
                  LOGGER.info("Publish result: {}", result);
                  if ("success".equalsIgnoreCase(result)) {
                      promise.complete();
                  } else {
                      LOGGER.warn("Unexpected publish result: {}", result);
                      promise.fail(new DxInternalServerErrorException("Unexpected response from message broker"));
                  }
              })
              .onFailure(error -> {
                  LOGGER.error("Error while publishing data from adapter: {}", error.getMessage(), error);
                  promise.fail(new DxInternalServerErrorException(INTERNAL_SERVER_ERROR));
              });


      return promise.future();
  }

  @Override
  public Future<List<JsonObject>> getAllAdapterDetailsForUser(String iid) {
    LOGGER.debug("getAllAdapterDetailsForUser() started");
    Promise<List<JsonObject>> promise = Promise.promise();

      catalogueService
              .fetchCatalogueInfo(iid)
              .compose(catalogueResult -> {
                  if (catalogueResult == null || !catalogueResult.containsKey("provider")) {
                      return Future.failedFuture(new DxBadRequestException("Missing 'provider' in catalogue result for iid: " + iid));
                  }

                  String providerId = catalogueResult.getString("provider");
                  return ingestionDAO.getAllAdaptersDetailsByProviderId(providerId);
              })
              .onSuccess(promise::complete)
              .onFailure(failure -> {
                  LOGGER.error("Failed to fetch adapter details for iid {}: {}", iid, failure.getMessage(), failure);
                  promise.fail(failure);
              });

    return promise.future();
  }
}
