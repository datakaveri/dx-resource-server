package iudx.resource.server.apiserver.ingestion.service;

import static iudx.resource.server.apiserver.ingestion.util.Constants.*;
import static iudx.resource.server.apiserver.ingestion.util.Constants.ID;
import static iudx.resource.server.apiserver.subscription.util.Constants.ITEM_TYPES;
import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.cache.util.CacheType.CATALOGUE_CACHE;
import static iudx.resource.server.common.HttpStatusCode.INTERNAL_SERVER_ERROR;
import static org.cdpg.dx.databroker.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceException;
import iudx.resource.server.apiserver.ingestion.model.*;
import iudx.resource.server.cache.service.CacheService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.database.postgres.models.*;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.databroker.model.ExchangeSubscribersResponse;
import org.cdpg.dx.databroker.model.RegisterExchangeModel;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.databroker.util.PermissionOpType;
import org.cdpg.dx.databroker.util.Vhosts;

public class IngestionServiceImpl implements IngestionService {
  private static final Logger LOGGER = LogManager.getLogger(IngestionServiceImpl.class);
  private final CacheService cacheService;
  private final DataBrokerService dataBroker;
  private final PostgresService postgresService;

  public IngestionServiceImpl(
      CacheService cacheService, DataBrokerService dataBroker, PostgresService postgresService) {
    this.cacheService = cacheService;
    this.dataBroker = dataBroker;
    this.postgresService = postgresService;
  }

  // TODO: Need to revisit once postgresmodel
  @Override
  public Future<RegisterExchangeModel> registerAdapter(
      String entities, String instanceId, String userId) {
    Promise<RegisterExchangeModel> promise = Promise.promise();
    JsonObject cacheJson = new JsonObject().put("key", entities).put("type", CATALOGUE_CACHE);
    AtomicBoolean isPostgresUpdated = new AtomicBoolean(false);
    AtomicReference<String> routingKey = new AtomicReference<>(null);
    cacheService
        .get(cacheJson)
        .compose(
            cacheServiceResult -> {
              Set<String> type =
                  new HashSet<String>(cacheServiceResult.getJsonArray("type").getList());
              Set<String> itemTypeSet =
                  type.stream().map(e -> e.split(":")[1]).collect(Collectors.toSet());
              itemTypeSet.retainAll(ITEM_TYPES);

              String resourceIdForIngestion;
              if (!itemTypeSet.contains("Resource")) {
                resourceIdForIngestion = cacheServiceResult.getString("id");
                routingKey.set(resourceIdForIngestion + DATA_WILDCARD_ROUTINGKEY);
              } else {
                resourceIdForIngestion = cacheServiceResult.getString("resourceGroup");
                routingKey.set(resourceIdForIngestion + "/." + entities);
              }
              String query =
                  CREATE_INGESTION_SQL
                      .replace("$1", resourceIdForIngestion)
                      .replace("$2", cacheServiceResult.getString("id"))
                      .replace("$3", cacheServiceResult.getString("name"))
                      .replace("$4", cacheServiceResult.toString())
                      .replace("$5", userId)
                      .replace("$6", cacheServiceResult.getString("provider"));

              List<String> columns =
                  List.of(
                      "exchange_name",
                      "resource_id",
                      "dataset_name",
                      "dataset_details_json",
                      "user_id",
                      "providerid");
              List<Object> values =
                  List.of(
                      resourceIdForIngestion,
                      cacheServiceResult.getString("id"),
                      cacheServiceResult.getString("name"),
                      cacheServiceResult.toString(),
                      userId,
                      cacheServiceResult.getString("provider"));
              InsertQuery insertQuery = new InsertQuery("adaptors_details", columns, values);

              return postgresService
                  .insert(insertQuery)
                  .map(
                      pgHandler ->
                          new RequestRegisterExchangeModel(userId, resourceIdForIngestion));
            })
        .compose(
            ingestionHandler -> {
              LOGGER.debug("Inserted in postgres.");
              isPostgresUpdated.set(true);
              return dataBroker.registerExchange(
                  ingestionHandler.userId(), ingestionHandler.exchangeName(), Vhosts.IUDX_PROD);
            })
        .compose(
            registerExchangeHandler -> {
              LOGGER.debug("Exchanges created in databroker.");
              return dataBroker
                  .updatePermission(
                      userId,
                      registerExchangeHandler.getExchangeName(),
                      PermissionOpType.ADD_WRITE,
                      Vhosts.IUDX_PROD)
                  .map(updatePermission -> registerExchangeHandler);
            })
        .compose(
            updatePermissionHandler -> {
              LOGGER.debug("Permission updated.");
              return dataBroker
                  .queueBinding(
                      updatePermissionHandler.getExchangeName(),
                      QUEUE_DATA,
                      routingKey.get(),
                      Vhosts.IUDX_PROD)
                  .map(bindQueueHandler -> updatePermissionHandler);
            })
        .compose(
            queueDatabaseHandler -> {
              LOGGER.debug("Success : Data Base Queue Binding successful.");
              return dataBroker
                  .queueBinding(
                      queueDatabaseHandler.getExchangeName(),
                      REDIS_LATEST,
                      routingKey.get(),
                      Vhosts.IUDX_PROD)
                  .map(bindQueueHandler -> queueDatabaseHandler);
            })
        .compose(
            redisLatestHandler -> {
              LOGGER.debug("Success : Redis Queue Binding successful.");
              return dataBroker
                  .queueBinding(
                      redisLatestHandler.getExchangeName(),
                      QUEUE_AUDITING,
                      routingKey.get(),
                      Vhosts.IUDX_PROD)
                  .map(bindQueueHandler -> redisLatestHandler);
            })
        .onSuccess(
            ingestionData -> {
              LOGGER.debug("Success : Subscription-Monitoring Queue Binding");
              promise.complete(ingestionData);
            })
        .onFailure(
            failure -> {
              if (isPostgresUpdated.get()) {
                /*String deleteQuery = DELETE_INGESTION_SQL.replace("$0", entities);*/
                ConditionComponent conditionComponent =
                    new Condition("exchange_name", Condition.Operator.EQUALS, List.of(entities));
                DeleteQuery deleteQuery =
                    new DeleteQuery("adaptors_details", conditionComponent, null, null);
                postgresService
                    .delete(deleteQuery)
                    .onComplete(
                        deletePgHandler -> {
                          if (deletePgHandler.succeeded()) {
                            LOGGER.debug("Deleted from postgres.");
                            LOGGER.error("broker fail " + failure.getMessage());
                            promise.fail(failure);
                          }
                        });
              } else {
                LOGGER.error(failure.getMessage());
                promise.fail(failure);
              }
            });
    return promise.future();
  }

  // TODO: Need to revisit once postgresmodel
  @Override
  public Future<Void> deleteAdapter(String adapterId, String userId) {
    Promise<Void> promise = Promise.promise();
    dataBroker
        .deleteExchange(adapterId, userId, Vhosts.IUDX_PROD)
        .compose(
            deleteAdaptorHandler -> {
              LOGGER.info("Adapter deleted");
              return dataBroker.updatePermission(
                  userId, adapterId, PermissionOpType.DELETE_WRITE, Vhosts.IUDX_PROD);
            })
        .compose(
            updatePermissionHandler -> {
              LOGGER.info("Permission deleted for exchange successfully");
              ConditionComponent conditionComponent =
                  new Condition("exchange_name", Condition.Operator.EQUALS, List.of(adapterId));
              DeleteQuery deleteQuery =
                  new DeleteQuery("adaptors_details", conditionComponent, null, null);
              return postgresService.delete(deleteQuery);
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

  // TODO: Need to revisit once postgresmodel
  @Override
  public Future<ExchangeSubscribersResponse> getAdapterDetails(String adapterId) {
    Promise<ExchangeSubscribersResponse> promise = Promise.promise();
    dataBroker
        .listExchange(adapterId, Vhosts.IUDX_PROD)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                /* GetResultModel getResultModel = new GetResultModel(handler.result());*/
                promise.complete(handler.result());
              } else {
                promise.fail(handler.cause());
              }
            });
    return promise.future();
  }

  // TODO: Need to revisit once postgresmodel
  @Override
  public Future<Void> publishDataFromAdapter(JsonArray request) {
    Promise<Void> promise = Promise.promise();
    String entities = request.getJsonObject(0).getJsonArray("entities").getValue(0).toString();
    JsonObject cacheRequestJson = new JsonObject();
    cacheRequestJson.put("type", CATALOGUE_CACHE);
    cacheRequestJson.put("key", entities);
    cacheService
        .get(cacheRequestJson)
        .compose(
            cacheResult -> {
              String resourceGroupId =
                  cacheResult.containsKey(RESOURCE_GROUP)
                      ? cacheResult.getString(RESOURCE_GROUP)
                      : cacheResult.getString(ID);
              LOGGER.debug("Info : resourceGroupId  " + resourceGroupId);
              String routingKey = resourceGroupId + "/." + entities;
              request.remove("entities");

              for (int i = 0; i < request.size(); i++) {
                JsonObject jsonObject = request.getJsonObject(i);
                jsonObject.remove("entities");
                jsonObject.put(ID, entities);
              }
              LOGGER.trace(request);
              LOGGER.debug("Info : routingKey  " + routingKey);
              return dataBroker.publishFromAdaptor(resourceGroupId, routingKey, request);
            })
        .onSuccess(
            resultHandler -> {
              LOGGER.info("result handler ::" + resultHandler);
              if (resultHandler.equalsIgnoreCase("success")) {
                promise.complete();
              }
            })
        .onFailure(
            failure -> {
              LOGGER.error("Error occurred while publishing data from adapter", failure);
              promise.fail(new ServiceException(0, INTERNAL_SERVER_ERROR.getDescription()));
            });

    return promise.future();
  }

  // TODO: Need to revisit once postgresmodel
  @Override
  public Future<QueryResult> getAllAdapterDetailsForUser(String iid) {
    LOGGER.debug("getAllAdapterDetailsForUser() started");
    Promise<QueryResult> promise = Promise.promise();

    JsonObject cacheRequest = new JsonObject();
    cacheRequest.put("type", CATALOGUE_CACHE);
    cacheRequest.put("key", iid);
    cacheService
        .get(cacheRequest)
        .compose(
            cacheResult -> {
              String providerId = cacheResult.getString("provider");
              ConditionComponent conditionComponent =
                  new Condition("providerid", Condition.Operator.EQUALS, List.of(providerId));
              SelectQuery selectQuery =
                  new SelectQuery(
                      "adaptors_details", List.of("*"), conditionComponent, null, null, null, null);
              return postgresService.select(selectQuery);
            })
        .onSuccess(
            postgresServiceHandler -> {
              promise.complete(postgresServiceHandler);
            })
        .onFailure(
            failure -> {
              LOGGER.error("failed");
              promise.fail(new ServiceException(0, INTERNAL_SERVER_ERROR.getDescription()));
            });
    return promise.future();
  }
}
