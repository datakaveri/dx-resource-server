package iudx.resource.server.apiserver.ingestion.service;

import static iudx.resource.server.apiserver.ingestion.util.Constants.*;
import static iudx.resource.server.apiserver.ingestion.util.Constants.ID;
import static iudx.resource.server.apiserver.subscription.util.Constants.ITEM_TYPES;
import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.cache.util.CacheType.CATALOGUE_CACHE;
import static iudx.resource.server.common.HttpStatusCode.INTERNAL_SERVER_ERROR;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceException;
import iudx.resource.server.apiserver.ingestion.model.GetResultModel;
import iudx.resource.server.apiserver.ingestion.model.IngestionData;
import iudx.resource.server.apiserver.ingestion.model.IngestionEntitiesResponseModel;
import iudx.resource.server.apiserver.ingestion.model.IngestionModel;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.database.postgres.model.PostgresResultModel;
import iudx.resource.server.database.postgres.service.PostgresService;
import iudx.resource.server.databroker.service.DataBrokerService;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

  @Override
  public Future<IngestionData> registerAdapter(String entities, String instanceId, String userId) {
    Promise<IngestionData> promise = Promise.promise();
    JsonObject cacheJson = new JsonObject().put("key", entities).put("type", CATALOGUE_CACHE);
    AtomicBoolean isAdaptorCreated = new AtomicBoolean(false);
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
              } else {
                resourceIdForIngestion = cacheServiceResult.getString("resourceGroup");
              }
              String query =
                  CREATE_INGESTION_SQL
                      .replace("$1", resourceIdForIngestion) /* exchange name */
                      .replace("$2", cacheServiceResult.getString("id")) /* resource */
                      .replace("$3", cacheServiceResult.getString("name")) /* dataset name */
                      .replace("$4", cacheServiceResult.toString()) /* dataset json */
                      .replace("$5", userId) /* user id */
                      .replace("$6", cacheServiceResult.getString("provider")); /*provider*/

              return postgresService
                  .executeQuery(query)
                  .map(
                      pgHandler ->
                          new IngestionModel(
                              entities,
                              userId,
                              itemTypeSet.iterator().next(),
                              resourceIdForIngestion));
            })
        .compose(
            ingestionHandler -> {
              LOGGER.debug("Inserted in postgres.");
              isAdaptorCreated.set(true);
              return dataBroker.registerAdaptor(ingestionHandler).map(IngestionData::new);
            })
        .onSuccess(
            ingestionData -> {
              promise.complete(ingestionData);
            })
        .onFailure(
            failure -> {
              if (isAdaptorCreated.get()) {
                String deleteQuery = DELETE_INGESTION_SQL.replace("$0", entities);
                postgresService
                    .executeQuery(deleteQuery)
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

  @Override
  public Future<Void> deleteAdapter(String adapterId, String userId) {
    Promise<Void> promise = Promise.promise();
    dataBroker
        .deleteAdaptor(adapterId, userId)
        .compose(
            deleteAdaptorHandler -> {
              LOGGER.info("Adapter deleted");
              return postgresService.executeQuery(DELETE_INGESTION_SQL.replace("$0", adapterId));
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
  public Future<GetResultModel> getAdapterDetails(String adapterId) {
    Promise<GetResultModel> promise = Promise.promise();
    dataBroker
        .listAdaptor(adapterId)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                GetResultModel getResultModel = new GetResultModel(handler.result());
                promise.complete(getResultModel);
              } else {
                promise.fail(handler.cause());
              }
            });
    return promise.future();
  }

  @Override
  public Future<IngestionEntitiesResponseModel> publishDataFromAdapter(JsonArray request) {
    Promise<IngestionEntitiesResponseModel> promise = Promise.promise();
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
                promise.complete(new IngestionEntitiesResponseModel("Item Published"));
              }
            })
        .onFailure(
            failure -> {
              LOGGER.error("Error occurred while publishing data from adapter", failure);
              promise.fail(new ServiceException(0, INTERNAL_SERVER_ERROR.getDescription()));
            });

    return promise.future();
  }

  @Override
  public Future<PostgresResultModel> getAllAdapterDetailsForUser(String iid) {
    LOGGER.debug("getAllAdapterDetailsForUser() started");
    Promise<PostgresResultModel> promise = Promise.promise();

    JsonObject cacheRequest = new JsonObject();
    cacheRequest.put("type", CATALOGUE_CACHE);
    cacheRequest.put("key", iid);
    cacheService
        .get(cacheRequest)
        .compose(
            cacheResult -> {
              String providerId = cacheResult.getString("provider");
              return postgresService.executeQuery1(SELECT_INGESTION_SQL.replace("$0", providerId));
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
