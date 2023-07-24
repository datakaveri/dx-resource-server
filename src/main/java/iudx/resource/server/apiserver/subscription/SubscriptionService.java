package iudx.resource.server.apiserver.subscription;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.apiserver.util.Constants.SUBSCRIPTION_ID;
import static iudx.resource.server.cache.cachelmpl.CacheType.CATALOGUE_CACHE;
import static iudx.resource.server.databroker.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.response.ResponseType;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.databroker.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** class contains all method for operations on subscriptions. */
public class SubscriptionService {

  private static final Logger LOGGER = LogManager.getLogger(SubscriptionService.class);

  Subscription subscription;

  /**
   * get the context of subscription according to the type passed in message body.
   *
   * @param type type of subscription either <strong>streaming</strong> or <strong>callback</strong>
   * @param databroker databroker verticle object
   * @return an object of Subscription class
   */
  private Subscription getSubscriptionContext(SubsType type, DataBrokerService databroker) {
    LOGGER.info("getSubscriptionContext() method started");
    if (type != null && type.equals(SubsType.CALLBACK)) {
      LOGGER.info("callback subscription context");
      return new CallbackSubscription(databroker);
    } else {
      LOGGER.info("streaming subscription context");
      return new StreamingSubscription(databroker);
    }
  }

  /**
   * create a subscription.
   *
   * @param json subscription json
   * @param databroker databroker verticle object
   * @param pgService database verticle object
   * @return a future of JsonObject
   */
  public Future<JsonObject> createSubscription(
      JsonObject json,
      DataBrokerService databroker,
      PostgresService pgService,
      JsonObject authInfo,
      CacheService cacheService) {
    LOGGER.info("createSubscription() method started");
    Promise<JsonObject> promise = Promise.promise();
    SubsType subType = SubsType.valueOf(json.getString(SUB_TYPE));
    if (subscription == null) {
      subscription = getSubscriptionContext(subType, databroker);
    }
    assertNotNull(subscription);
    subscription
        .create(json)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                JsonObject response = handler.result();
                JsonObject brokerResponse = response.getJsonArray("results").getJsonObject(0);
                LOGGER.debug("brokerResponse: " + brokerResponse);

                JsonObject cacheJson =
                    new JsonObject()
                        .put("key", json.getJsonArray("entities").getString(0))
                        .put("type", CATALOGUE_CACHE);
                cacheService
                    .get(cacheJson)
                    .onSuccess(
                        cacheResult -> {
                          LOGGER.debug("cacheResult: " + cacheResult);
                          StringBuilder query =
                              new StringBuilder(
                                  CREATE_SUB_SQL
                                      .replace("$1", brokerResponse.getString("id"))
                                      .replace("$2", subType.type)
                                      .replace("$3", brokerResponse.getString("id"))
                                      .replace("$4", json.getJsonArray("entities").getString(0))
                                      .replace("$5", authInfo.getString("expiry"))
                                      .replace("$6", cacheResult.getString("name"))
                                      .replace("$7", cacheResult.toString())
                                      .replace("$8", authInfo.getString("userid")));

                          LOGGER.debug("query: " + query);
                          pgService.executeQuery(
                              query.toString(),
                              pgHandler -> {
                                if (pgHandler.succeeded()) {
                                  promise.complete(response);
                                } else {
                                  // TODO : rollback mechanism in case of pg error [to unbind/delete
                                  // created sub]
                                  JsonObject res = new JsonObject(pgHandler.cause().getMessage());
                                  promise.fail(generateResponse(res).toString());
                                }
                              });

                          LOGGER.debug("cache result" + cacheResult);
                        });
              } else {
                JsonObject res = new JsonObject(handler.cause().getMessage());
                promise.fail(generateResponse(res).toString());
              }
            });
    return promise.future();
  }

  /**
   * update an existing subscription.
   *
   * @param json subscription json
   * @param databroker databroker verticle object
   * @param pgService database verticle object
   * @return a future of josbObject
   */
  public Future<JsonObject> updateSubscription(
      JsonObject json,
      DataBrokerService databroker,
      PostgresService pgService,
      JsonObject authInfo) {
    LOGGER.info("updateSubscription() method started");
    Promise<JsonObject> promise = Promise.promise();

    String queueName = json.getString(SUBSCRIPTION_ID);
    String entity = json.getJsonArray("entities").getString(0);

    StringBuilder selectQuery =
        new StringBuilder(SELECT_SUB_SQL.replace("$1", queueName).replace("$2", entity));

    LOGGER.debug(selectQuery);
    pgService.executeQuery(
        selectQuery.toString(),
        handler -> {
          if (handler.succeeded()) {
            JsonArray resultArray = handler.result().getJsonArray("result");
            if (resultArray.size() == 0) {
              JsonObject res = new JsonObject();
              res.put(JSON_TYPE, 404)
                  .put(JSON_TITLE, ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                  .put(JSON_DETAIL, "Subscription not found for [queue,entity]");
              promise.fail(res.toString());
            } else {
              StringBuilder updateQuery =
                  new StringBuilder(
                      UPDATE_SUB_SQL
                          .replace("$1", authInfo.getString("expiry"))
                          .replace("$2", queueName)
                          .replace("$3", entity));
              LOGGER.debug(updateQuery);
              pgService.executeQuery(
                  updateQuery.toString(),
                  pgHandler -> {
                    if (pgHandler.succeeded()) {
                      JsonObject response = new JsonObject();
                      JsonArray entities = new JsonArray();

                      entities.add(json.getJsonArray("entities").getString(0));

                      JsonObject results = new JsonObject();
                      results.put("entities", entities);

                      response.put(TYPE, ResponseUrn.SUCCESS_URN.getUrn());
                      response.put(TITLE, "success");
                      response.put(RESULTS, new JsonArray().add(results));

                      promise.complete(response);
                    } else {
                      LOGGER.error(pgHandler.cause());
                      JsonObject res = new JsonObject(pgHandler.cause().getMessage());
                      promise.fail(generateResponse(res).toString());
                    }
                  });
            }
          } else {
            LOGGER.error(handler.cause());
            JsonObject res = new JsonObject(handler.cause().getMessage());
            promise.fail(generateResponse(res).toString());
          }
        });

    return promise.future();
  }

  /**
   * delete a subscription.
   *
   * @param json subscription json
   * @param databroker databroker verticle object
   * @param pgService database verticle object
   * @return a future of josbObject
   */
  public Future<JsonObject> deleteSubscription(
      JsonObject json, DataBrokerService databroker, PostgresService pgService) {
    LOGGER.info("deleteSubscription() method started");
    Promise<JsonObject> promise = Promise.promise();
    SubsType subType = SubsType.valueOf(json.getString(SUB_TYPE));
    if (subscription == null) {
      subscription = getSubscriptionContext(subType, databroker);
    }
    assertNotNull(subscription);
    subscription
        .delete(json)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                StringBuilder query =
                    new StringBuilder(
                        DELETE_SUB_SQL.replace("$1", json.getString(SUBSCRIPTION_ID)));
                LOGGER.debug(query);
                pgService.executeQuery(
                    query.toString(),
                    pgHandler -> {
                      if (pgHandler.succeeded()) {
                        handler.result().remove("status");
                        promise.complete(handler.result());
                      } else {
                        JsonObject res = new JsonObject(pgHandler.cause().getMessage());
                        promise.fail(generateResponse(res).toString());
                      }
                    });
              } else {
                JsonObject res = new JsonObject(handler.cause().getMessage());
                promise.fail(generateResponse(res).toString());
              }
            });
    return promise.future();
  }

  /**
   * get a subscription.
   *
   * @param json subscription json
   * @param databroker databroker verticle object
   * @param pgService database verticle object
   * @return a future of josbObject
   */
  public Future<JsonObject> getSubscription(
      JsonObject json, DataBrokerService databroker, PostgresService pgService) {
    LOGGER.info("getSubscription() method started");
    Promise<JsonObject> promise = Promise.promise();
    SubsType subType = SubsType.valueOf(json.getString(SUB_TYPE));
    if (subscription == null) {
      subscription = getSubscriptionContext(subType, databroker);
    }
    assertNotNull(subscription);
    subscription
        .get(json)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                promise.complete(handler.result());
              } else {
                JsonObject res = new JsonObject(handler.cause().getMessage());
                promise.fail(generateResponse(res).toString());
              }
            });
    return promise.future();
  }

  public Future<JsonObject> getAllSubscriptionQueueForUser(
      JsonObject json, PostgresService pgService) {
    LOGGER.info("getAllSubscriptionQueueForUser() method started");
    Promise<JsonObject> promise = Promise.promise();
    StringBuilder query = new StringBuilder(GET_ALL_QUEUE.replace("$1", json.getString("userid")));

    LOGGER.debug("query: " + query);
    pgService.executeQuery(
        query.toString(),
        pgHandler -> {
          if (pgHandler.succeeded()) {
            promise.complete(pgHandler.result());
          } else {
            // TODO : rollback mechanism in case of pg error [to unbind/delete created sub]
            JsonObject res = new JsonObject(pgHandler.cause().getMessage());
            promise.fail(generateResponse(res).toString());
          }
        });
    return promise.future();
  }

  /**
   * append an existing subscription.
   *
   * @param json subscription json
   * @param databroker databroker verticle object
   * @param pgService database verticle object
   * @return a future of josbObject
   */
  public Future<JsonObject> appendSubscription(
      JsonObject json,
      DataBrokerService databroker,
      PostgresService pgService,
      JsonObject authInfo) {
    LOGGER.info("appendSubscription() method started");
    Promise<JsonObject> promise = Promise.promise();
    SubsType subType = SubsType.valueOf(json.getString(SUB_TYPE));
    if (subscription == null) {
      subscription = getSubscriptionContext(subType, databroker);
    }
    assertNotNull(subscription);
    subscription
        .append(json)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                JsonObject response = handler.result();
                JsonObject brokerSubResult = response.getJsonArray("results").getJsonObject(0);
                StringBuilder query =
                    new StringBuilder(
                        APPEND_SUB_SQL
                            .replace("$1", json.getString(SUBSCRIPTION_ID))
                            .replace("$2", subType.type)
                            .replace("$3", json.getString(SUBSCRIPTION_ID))
                            .replace("$4", json.getJsonArray("entities").getString(0))
                            .replace("$5", authInfo.getString("expiry")));

                LOGGER.debug(query);
                pgService.executeQuery(
                    query.toString(),
                    pgHandler -> {
                      if (pgHandler.succeeded()) {
                        promise.complete(brokerSubResult);
                      } else {
                        // TODO : rollback mechanism in case of pg error [to unbind/delete created
                        // sub]
                        JsonObject res = new JsonObject(pgHandler.cause().getMessage());
                        promise.fail(generateResponse(res).toString());
                      }
                    });
              } else {
                JsonObject res = new JsonObject(handler.cause().getMessage());
                promise.fail(generateResponse(res).toString());
              }
            });
    return promise.future();
  }

  private JsonObject generateResponse(JsonObject response) {
    JsonObject finalResponse = new JsonObject();
    int type = response.getInteger(JSON_TYPE);
    switch (type) {
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
            .put(JSON_DETAIL, "Subscription " + ResponseType.AlreadyExists.getMessage());
        break;
      default:
        finalResponse
            .put(JSON_TYPE, type)
            .put(JSON_TITLE, ResponseType.BadRequestData.getMessage())
            .put(JSON_DETAIL, response.getString(JSON_DETAIL));
        break;
    }
    return finalResponse;
  }
}
