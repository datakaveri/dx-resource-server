package iudx.resource.server.apiserver.management;

import static iudx.resource.server.apiserver.util.Constants.JSON_DETAIL;
import static iudx.resource.server.apiserver.util.Constants.JSON_ID;
import static iudx.resource.server.apiserver.util.Constants.JSON_TITLE;
import static iudx.resource.server.apiserver.util.Constants.JSON_TYPE;
import static iudx.resource.server.apiserver.util.Constants.SUCCCESS;
import static iudx.resource.server.apiserver.util.Constants.USER_ID;
import static iudx.resource.server.cache.cachelmpl.CacheType.CATALOGUE_CACHE;
import static iudx.resource.server.common.Constants.CREATE_INGESTION_SQL;
import static iudx.resource.server.common.Constants.DELETE_INGESTION_SQL;
import static iudx.resource.server.common.Constants.SELECT_INGESTION_SQL;
import static iudx.resource.server.database.archives.Constants.ITEM_TYPES;
import static iudx.resource.server.databroker.util.Constants.*;
import static iudx.resource.server.metering.util.Constants.PROVIDER_ID;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.response.ResponseType;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.common.Vhosts;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.databroker.DataBrokerService;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class handles all DataBrokerService related interactions of API server. TODO Need to add
 * documentation.
 */
public class ManagementApiImpl implements ManagementApi {

  private static final Logger LOGGER = LogManager.getLogger(ManagementApiImpl.class);

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> registerAdapter(
      JsonObject requestJson,
      DataBrokerService dataBroker,
      CacheService cacheService,
      PostgresService postgresService) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject cacheJson =
        new JsonObject()
            .put("key", requestJson.getJsonArray("entities").getString(0))
            .put("type", CATALOGUE_CACHE);

    cacheService
        .get(cacheJson)
        .onSuccess(
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
              postgresService.executeQuery(
                  query,
                  pgHandler -> {
                    if (pgHandler.succeeded()) {
                      LOGGER.debug("Inserted in postgres.");
                      dataBroker.registerAdaptor(
                          requestJson,
                          Vhosts.IUDX_PROD.name(),
                          brokerHandler -> {
                            if (brokerHandler.succeeded()) {
                              JsonObject brokerResponse = brokerHandler.result();
                              if (!brokerResponse.containsKey(JSON_TYPE)) {
                                JsonObject iudxResponse = new JsonObject();
                                iudxResponse.put(TYPE, ResponseUrn.SUCCESS_URN.getUrn());
                                iudxResponse.put(TITLE, "Success");
                                iudxResponse.put(RESULTS, new JsonArray().add(brokerResponse));
                                promise.complete(iudxResponse);
                              } else {
                                promise.fail(generateResponse(brokerResponse).toString());
                              }
                            } else {
                              String deleteQuery =
                                  DELETE_INGESTION_SQL.replace(
                                      "$0", requestJson.getJsonArray("entities").getString(0));
                              postgresService.executeQuery(
                                  deleteQuery,
                                  deletePgHandler -> {
                                    if (deletePgHandler.succeeded()) {
                                      LOGGER.debug("Deleted from postgres.");
                                      LOGGER.error(
                                          "broker fail " + brokerHandler.cause().getMessage());
                                      promise.fail(brokerHandler.cause().getMessage());
                                    }
                                  });
                            }
                          });
                    } else {
                      JsonObject pgFailResponseBuild = new JsonObject();
                      pgFailResponseBuild.put(TYPE, 409);
                      pgFailResponseBuild.put(TITLE, "urn:dx:rs:resourceAlreadyExist");
                      promise.fail(pgFailResponseBuild.toString());
                    }
                  });
            })
        .onFailure(
            cacheFailHandler -> {
              JsonObject cacheFailResponseBuild = new JsonObject();
              cacheFailResponseBuild.put(TYPE, 404);
              cacheFailResponseBuild.put(TITLE, "urn:dx:rs:resourceNotFound");
              promise.fail(cacheFailResponseBuild.toString());
            });
    return promise.future();
  }

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> deleteAdapter(
      String adapterId,
      String userId,
      DataBrokerService dataBrokerService,
      PostgresService postgresService) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject json = new JsonObject();
    json.put(JSON_ID, adapterId);
    json.put(USER_ID, userId);
    dataBrokerService.deleteAdaptor(
        json,
        Vhosts.IUDX_PROD.name(),
        dataBrokerHandler -> {
          if (dataBrokerHandler.succeeded()) {
            postgresService.executeQuery(
                DELETE_INGESTION_SQL.replace("$0", adapterId),
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

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> getAdapterDetails(String adapterId, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject json = new JsonObject();
    json.put(JSON_ID, adapterId);
    databroker.listAdaptor(
        json,
        Vhosts.IUDX_PROD.name(),
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

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> publishHeartbeat(JsonObject json, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    databroker.publishHeartbeat(
        json,
        Vhosts.IUDX_PROD.name(),
        handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result();
            LOGGER.debug("Result from databroker verticle :: " + result);
            if (result.containsKey(JSON_TYPE)
                && result.getString(JSON_TYPE).equalsIgnoreCase(SUCCCESS)) {
              promise.complete(result);
            } else {
              promise.fail(result.toString());
            }
          } else {
            promise.fail(handler.cause().getMessage());
          }
        });
    return promise.future();
  }

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> publishDataFromAdapter(JsonArray json, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    databroker.publishFromAdaptor(
        json,
        Vhosts.IUDX_PROD.name(),
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
  public Future<JsonObject> getAllAdapterDetailsForUser(
      JsonObject request, PostgresService postgresService) {
    LOGGER.debug("getAllAdapterDetailsForUser() started");
    Promise<JsonObject> promise = Promise.promise();

    postgresService.executeQuery(
        SELECT_INGESTION_SQL.replace("$0", request.getString(PROVIDER_ID)),
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
}
