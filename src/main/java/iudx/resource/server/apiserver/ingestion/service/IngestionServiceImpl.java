package iudx.resource.server.apiserver.ingestion.service;

import static iudx.resource.server.apiserver.ingestion.util.Constants.*;
import static iudx.resource.server.apiserver.subscription.util.Constants.ITEM_TYPES;
import static iudx.resource.server.apiserver.subscription.util.Constants.RESULTS;
import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.apiserver.util.Constants.JSON_DETAIL;
import static iudx.resource.server.cache.util.CacheType.CATALOGUE_CACHE;
import static iudx.resource.server.databroker.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.common.ResponseType;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.common.Vhosts;
import iudx.resource.server.database.postgres.service.PostgresService;
import iudx.resource.server.databroker.service.DataBrokerService;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IngestionServiceImpl implements IngestionService {
  private static final Logger LOGGER = LogManager.getLogger(IngestionServiceImpl.class);
  CreateResultContainer createResultContainer = new CreateResultContainer();
  private CacheService cacheService;
  private DataBrokerService dataBroker;
  private PostgresService postgresService;

  public IngestionServiceImpl(
      CacheService cacheService, DataBrokerService dataBroker, PostgresService postgresService) {
    this.cacheService = cacheService;
    this.dataBroker = dataBroker;
    this.postgresService = postgresService;
  }

  @Override
  public Future<JsonObject> registerAdapter(JsonObject requestJson) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject cacheJson =
        new JsonObject()
            .put("key", requestJson.getJsonArray("entities").getString(0))
            .put("type", CATALOGUE_CACHE);

    cacheService
        .get(cacheJson)
        .compose(
            cacheServiceResult -> {
              Set<String> type =
                  new HashSet<String>(cacheServiceResult.getJsonArray("type").getList());
              Set<String> itemTypeSet =
                  type.stream().map(e -> e.split(":")[1]).collect(Collectors.toSet());
              itemTypeSet.retainAll(ITEM_TYPES);

              String resourceIdForIngestions;
              if (!itemTypeSet.contains("Resource")) {
                resourceIdForIngestions = cacheServiceResult.getString("id");
              } else {
                resourceIdForIngestions = cacheServiceResult.getString("resourceGroup");
              }
              requestJson
                  .put("resourceGroup", resourceIdForIngestions)
                  .put("types", itemTypeSet.iterator().next());
              String query =
                  CREATE_INGESTION_SQL
                      .replace(
                          "$1",
                          requestJson.getJsonArray("entities").getString(0)) /* exchange name */
                      .replace("$2", cacheServiceResult.getString("id")) /* resource id */
                      .replace("$3", cacheServiceResult.getString("name")) /* dataset name */
                      .replace("$4", cacheServiceResult.toString()) /* dataset json */
                      .replace("$5", requestJson.getString("userid")) /* user id */
                      .replace("$6", cacheServiceResult.getString("provider")); /*provider*/

              return postgresService.executeQuery(query);
            })
        .compose(
            pgHandler -> {
              LOGGER.debug("Inserted in postgres.");
              return dataBroker.registerAdaptor(requestJson, Vhosts.IUDX_PROD.name());
            })
        .onSuccess(
            brokerHandler -> {
              createResultContainer.isAdaptorCreated = true;
              if (!brokerHandler.containsKey(JSON_TYPE)) {
                JsonObject iudxResponse = new JsonObject();
                iudxResponse.put(TYPE, ResponseUrn.SUCCESS_URN.getUrn());
                iudxResponse.put(TITLE, "Success");
                iudxResponse.put(RESULTS, new JsonArray().add(brokerHandler));
                promise.complete(iudxResponse);
              } else {
                promise.fail(generateResponse(brokerHandler).toString());
              }
            })
        .onFailure(
            failure -> {
              if (!createResultContainer.isAdaptorCreated) {
                String deleteQuery =
                    DELETE_INGESTION_SQL.replace(
                        "$0", requestJson.getJsonArray("entities").getString(0));
                postgresService
                    .executeQuery(deleteQuery)
                    .onComplete(
                        deletePgHandler -> {
                          if (deletePgHandler.succeeded()) {
                            LOGGER.debug("Deleted from postgres.");
                            LOGGER.error("broker fail " + failure.getMessage());
                            promise.fail(failure.getMessage());
                          }
                        });
              } else {
                JsonObject pgFailResponseBuild = new JsonObject();
                pgFailResponseBuild.put(TYPE, 409);
                pgFailResponseBuild.put(TITLE, "urn:dx:rs:resourceAlreadyExist");
                promise.fail(pgFailResponseBuild.toString());
                /*promise.fail(failure.getMessage());*/
              }
            });
    return promise.future();
  }

  @Override
  public Future<JsonObject> deleteAdapter(String adapterId, String userId) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject json = new JsonObject();
    json.put(JSON_ID, adapterId);
    json.put(USER_ID, userId);
    dataBroker
        .deleteAdaptor(json, Vhosts.IUDX_PROD.name())
        .onComplete(
            dataBrokerHandler -> {
              if (dataBrokerHandler.succeeded()) {
                postgresService
                    .executeQuery(DELETE_INGESTION_SQL.replace("$0", adapterId))
                    .onComplete(
                        pgHandler -> {
                          if (pgHandler.succeeded()) {
                            JsonObject result = dataBrokerHandler.result();
                            LOGGER.debug("Result from dataBroker verticle :: " + result);
                            JsonObject iudxResponse = new JsonObject();
                            iudxResponse.put(TYPE, ResponseUrn.SUCCESS_URN.getUrn());
                            iudxResponse.put(TITLE, "Success");
                            iudxResponse.put(RESULTS, "Adapter deleted");
                            promise.complete(iudxResponse);
                          } else {
                            // TODO need to figure out the rollback if postgres delete fails
                            LOGGER.debug("fail to delete");
                            promise.fail("unable to delete");
                          }
                        });
              } else if (dataBrokerHandler.failed()) {
                String result = dataBrokerHandler.cause().getMessage();
                promise.fail(generateResponse(new JsonObject(result)).toString());
              }
            });
    return promise.future();
  }

  @Override
  public Future<JsonObject> getAdapterDetails(String adapterId) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject json = new JsonObject();
    json.put(JSON_ID, adapterId);
    dataBroker
        .listAdaptor(json, Vhosts.IUDX_PROD.name())
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                JsonObject result = handler.result();
                LOGGER.debug("Result from databroker verticle :: " + result);
                if (!result.containsKey(JSON_TYPE)) {

                  JsonObject iudxResponse = new JsonObject();
                  iudxResponse.put(TYPE, ResponseUrn.SUCCESS_URN.getUrn());
                  iudxResponse.put(TITLE, "Success");
                  iudxResponse.put(RESULTS, new JsonArray().add(result));

                  promise.complete(iudxResponse);
                } else {
                  promise.fail(generateResponse(result).toString());
                }
              } else {
                promise.fail(handler.cause().getMessage());
              }
            });
    return promise.future();
  }

  @Override
  public Future<JsonObject> publishDataFromAdapter(JsonArray json) {
    Promise<JsonObject> promise = Promise.promise();
    dataBroker
        .publishFromAdaptor(json, Vhosts.IUDX_PROD.name())
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                JsonObject result = handler.result();
                JsonObject finalResponse = new JsonObject();
                LOGGER.debug("Result from databroker verticle :: " + result);
                if (!result.containsKey(JSON_TYPE)) {
                  finalResponse.put(TYPE, ResponseUrn.SUCCESS_URN.getUrn());
                  finalResponse.put(TITLE, ResponseUrn.SUCCESS_URN.getMessage());
                  finalResponse.put(DETAIL, "Item Published");
                  promise.complete(finalResponse);
                } else {
                  finalResponse.put(TYPE, ResponseUrn.BAD_REQUEST_URN.getUrn());
                  finalResponse.put(TITLE, ResponseUrn.BAD_REQUEST_URN.getMessage());
                  finalResponse.put(DETAIL, "Failed to published");
                  promise.fail(finalResponse.toString());
                }
              } else {
                promise.fail(handler.cause().getMessage());
              }
            });
    return promise.future();
  }

  @Override
  public Future<JsonObject> getAllAdapterDetailsForUser(String iid) {
    LOGGER.debug("getAllAdapterDetailsForUser() started");
    Promise<JsonObject> promise = Promise.promise();

    JsonObject cacheRequest = new JsonObject();
    cacheRequest.put("type", CATALOGUE_CACHE);
    cacheRequest.put("key", iid);
    cacheService
        .get(cacheRequest)
        .compose(
            cacheResult -> {
              String providerId = cacheResult.getString("provider");
              return postgresService.executeQuery(SELECT_INGESTION_SQL.replace("$0", providerId));
            })
        .onComplete(
            postgresServiceHandler -> {
              if (postgresServiceHandler.succeeded()) {
                JsonObject result = postgresServiceHandler.result();
                promise.complete(result);
              } else {
                JsonObject pgFailResponseBuild = new JsonObject();
                pgFailResponseBuild.put(TYPE, 400);
                LOGGER.debug("cause " + postgresServiceHandler.cause());
                promise.fail(pgFailResponseBuild.toString());
              }
            });
    return promise.future();
  }

  private JsonObject generateResponse(JsonObject response) {
    JsonObject finalResponse = new JsonObject();
    int type = response.getInteger(JSON_TYPE);
    switch (type) {
      case 200:
        finalResponse
            .put(JSON_TYPE, type)
            .put(JSON_TITLE, ResponseType.fromCode(type).getMessage())
            .put(JSON_DETAIL, response.getString(JSON_DETAIL));
        break;
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
            .put(JSON_DETAIL, ResponseType.AlreadyExists.getMessage());
        break;
      default:
        finalResponse
            .put(JSON_TYPE, ResponseType.BadRequestData.getCode())
            .put(JSON_TITLE, ResponseType.BadRequestData.getMessage())
            .put(JSON_DETAIL, response.getString(JSON_DETAIL));
        break;
    }
    return finalResponse;
  }

  public class CreateResultContainer {
    public String adaptor;
    public boolean isAdaptorCreated = false;
  }
}
