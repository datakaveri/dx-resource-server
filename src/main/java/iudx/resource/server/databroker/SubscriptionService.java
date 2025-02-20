package iudx.resource.server.databroker;

import static iudx.resource.server.databroker.util.Constants.*;
import static iudx.resource.server.databroker.util.Util.getResponseJson;
import static iudx.resource.server.databroker.util.Util.getSha;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.common.Vhosts;
import iudx.resource.server.databroker.util.PermissionOpType;
import java.time.OffsetDateTime;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SubscriptionService {
  private static final Logger LOGGER = LogManager.getLogger(SubscriptionService.class);
  CacheService cacheService;
  private String vhost;
  private int totalBindCount;
  private int totalBindSuccess;
  private RabbitClient rabbitClient;
  private PostgresClient pgSqlClient;
  private String amqpUrl;
  private int amqpPort;

  SubscriptionService(
      RabbitClient rabbitClient,
      PostgresClient pgSqlClient,
      JsonObject config,
      CacheService cacheService) {
    this.rabbitClient = rabbitClient;
    this.pgSqlClient = pgSqlClient;
    this.vhost = config.getString(Vhosts.IUDX_PROD.value);
    this.amqpUrl = config.getString("brokerAmqpIp");
    this.amqpPort = config.getInteger("brokerAmqpPort");
    this.cacheService = cacheService;
  }

  Future<JsonObject> registerStreamingSubscription(JsonObject request) {
    LOGGER.trace("Info : SubscriptionService#registerStreamingSubscription() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject registerStreamingSubscriptionResponse = new JsonObject();
    JsonObject requestjson = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String userid = request.getString(USER_ID);
      String queueName = userid + "/" + request.getString("name");
      LOGGER.debug("queue name is databrokeer subscription  = {}", queueName);
      Future<JsonObject> resultCreateUser = rabbitClient.createUserIfNotExist(userid, VHOST_IUDX);
      resultCreateUser.onComplete(
          resultCreateUserhandler -> {
            if (resultCreateUserhandler.succeeded()) {
              JsonObject result = resultCreateUserhandler.result();
              LOGGER.debug("success :: createUserIfNotExist " + result);
              JsonArray entitites = request.getJsonArray(ENTITIES);
              LOGGER.debug("Info : Request Access for " + entitites);
              LOGGER.debug("Info : No of bindings to do : " + entitites.size());
              totalBindCount = entitites.size();
              totalBindSuccess = 0;
              requestjson.put(QUEUE_NAME, queueName);
              Future<JsonObject> resultqueue = rabbitClient.createQueue(requestjson, vhost);
              String streamingUserName = result.getString(USER_ID);
              String apiKey = result.getString("apiKey");
              resultqueue.onComplete(
                  resultHandlerqueue -> {
                    if (resultHandlerqueue.succeeded()) {
                      JsonObject createQueueResponse = (JsonObject) resultHandlerqueue.result();
                      if (createQueueResponse.containsKey(TITLE)
                          && createQueueResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
                        LOGGER.error("failed ::" + resultHandlerqueue.cause());
                        promise.fail(createQueueResponse.toString());
                      } else {
                        for (Object currentEntity : entitites) {
                          String routingKey = (String) currentEntity;
                          LOGGER.debug("Info : routingKey is " + routingKey);
                          if (routingKey != null) {
                            if (routingKey.isEmpty() || routingKey.isBlank() || routingKey == "") {
                              LOGGER.error("failed :: Invalid (or) NULL routingKey");
                              Future<JsonObject> resultDeletequeue =
                                  rabbitClient.deleteQueue(requestjson, vhost);
                              resultDeletequeue.onComplete(
                                  resultHandlerDeletequeue -> {
                                    if (resultHandlerDeletequeue.succeeded()) {
                                      promise.fail(
                                          getResponseJson(
                                                  BAD_REQUEST_CODE,
                                                  BAD_REQUEST_DATA,
                                                  INVALID_ROUTING_KEY)
                                              .toString());
                                    }
                                  });
                            } else {
                              JsonArray array = new JsonArray();
                              String exchangeName;
                              if (isGroupResource(request)) {
                                exchangeName = routingKey;
                                array.add(exchangeName + DATA_WILDCARD_ROUTINGKEY);
                              } else {
                                exchangeName = request.getString("resourcegroup");
                                LOGGER.debug("exchange name  = {} ", exchangeName);
                                array.add(exchangeName + "/." + routingKey);
                              }
                              LOGGER.debug(" Exchange name = {}", exchangeName);
                              JsonObject json = new JsonObject();
                              json.put(EXCHANGE_NAME, exchangeName);
                              json.put(QUEUE_NAME, queueName);
                              json.put(ENTITIES, array);
                              Future<JsonObject> resultbind = rabbitClient.bindQueue(json, vhost);
                              resultbind.onComplete(
                                  resultHandlerbind -> {
                                    if (resultHandlerbind.succeeded()) {
                                      // count++
                                      totalBindSuccess += 1;
                                      LOGGER.debug(
                                          "sucess :: totalBindSuccess "
                                              + totalBindSuccess
                                              + resultHandlerbind.result());

                                      JsonObject bindResponse =
                                          (JsonObject) resultHandlerbind.result();
                                      if (bindResponse.containsKey(TITLE)
                                          && bindResponse
                                              .getString(TITLE)
                                              .equalsIgnoreCase(FAILURE)) {
                                        LOGGER.error("failed ::" + resultHandlerbind.cause());
                                        Future<JsonObject> resultDeletequeue =
                                            rabbitClient.deleteQueue(requestjson, vhost);
                                        resultDeletequeue.onComplete(
                                            resultHandlerDeletequeue -> {
                                              if (resultHandlerDeletequeue.succeeded()) {
                                                promise.fail(
                                                    getResponseJson(
                                                            BAD_REQUEST_CODE,
                                                            BAD_REQUEST_DATA,
                                                            BINDING_FAILED)
                                                        .toString());
                                              }
                                            });
                                      } else if (totalBindSuccess == totalBindCount) {
                                        rabbitClient
                                            .updateUserPermissions(
                                                vhost, userid, PermissionOpType.ADD_READ, queueName)
                                            .onComplete(
                                                userPermissionHandler -> {
                                                  if (userPermissionHandler.succeeded()) {
                                                    registerStreamingSubscriptionResponse.put(
                                                        USER_NAME, streamingUserName);
                                                    registerStreamingSubscriptionResponse.put(
                                                        APIKEY, apiKey);
                                                    registerStreamingSubscriptionResponse.put(
                                                        ID, queueName);
                                                    registerStreamingSubscriptionResponse.put(
                                                        URL, this.amqpUrl);
                                                    registerStreamingSubscriptionResponse.put(
                                                        PORT, this.amqpPort);
                                                    registerStreamingSubscriptionResponse.put(
                                                        VHOST, this.vhost);

                                                    JsonObject response = new JsonObject();
                                                    response.put(
                                                        TYPE, ResponseUrn.SUCCESS_URN.getUrn());
                                                    response.put(TITLE, "success");
                                                    response.put(
                                                        RESULTS,
                                                        new JsonArray()
                                                            .add(
                                                                registerStreamingSubscriptionResponse));

                                                    promise.complete(response);
                                                  } else {
                                                    Future<JsonObject> resultDeletequeue =
                                                        rabbitClient.deleteQueue(
                                                            requestjson, vhost);
                                                    resultDeletequeue.onComplete(
                                                        resultHandlerDeletequeue -> {
                                                          if (resultHandlerDeletequeue
                                                              .succeeded()) {
                                                            promise.fail(
                                                                getResponseJson(
                                                                        BAD_REQUEST_CODE,
                                                                        BAD_REQUEST_DATA,
                                                                        BINDING_FAILED)
                                                                    .toString());
                                                          }
                                                        });
                                                  }
                                                });
                                      }
                                    } else if (resultHandlerbind.failed()) {
                                      LOGGER.error("failed ::" + resultHandlerbind.cause());
                                      Future<JsonObject> resultDeletequeue =
                                          rabbitClient.deleteQueue(requestjson, vhost);
                                      resultDeletequeue.onComplete(
                                          resultHandlerDeletequeue -> {
                                            if (resultHandlerDeletequeue.succeeded()) {
                                              /*
                                               * used promise.tryFail() instead of promise.fail() to
                                               * avoid java.lang.IllegalStateException: Result is
                                               * already complete
                                               */
                                              promise.tryFail(
                                                  getResponseJson(
                                                          BAD_REQUEST_CODE,
                                                          BAD_REQUEST_DATA,
                                                          BINDING_FAILED)
                                                      .toString());
                                            }
                                          });
                                    }
                                  });
                            }
                          } else {
                            LOGGER.error("failed :: Invalid (or) NULL routingKey");
                            Future<JsonObject> resultDeletequeue =
                                rabbitClient.deleteQueue(requestjson, vhost);
                            resultDeletequeue.onComplete(
                                resultHandlerDeletequeue -> {
                                  if (resultHandlerDeletequeue.succeeded()) {
                                    promise.tryFail(
                                        getResponseJson(
                                                BAD_REQUEST_CODE,
                                                BAD_REQUEST_DATA,
                                                INVALID_ROUTING_KEY)
                                            .toString());
                                  }
                                });
                          }
                        }
                      }
                    } else if (resultHandlerqueue.failed()) {
                      LOGGER.error("Fail ::" + resultHandlerqueue.cause());
                      promise.fail(resultHandlerqueue.cause().getMessage());
                    }
                  });
            }
          });
    } else {
      LOGGER.error("Fail : Error in payload");
      promise.fail(getResponseJson(BAD_REQUEST_CODE, BAD_REQUEST_DATA, PAYLOAD_ERROR).toString());
    }
    return promise.future();
  }

  Future<JsonObject> updateStreamingSubscription(JsonObject request) {
    LOGGER.trace("Info : SubscriptionService#updateStreamingSubscription() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject updateStreamingSubscriptionResponse = new JsonObject();
    JsonObject requestjson = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String userid = request.getString(USER_ID);
      // String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
      String queueName = userid + "/" + request.getString("name");
      Future<JsonObject> resultCreateUser = rabbitClient.createUserIfNotExist(userid, VHOST_IUDX);
      resultCreateUser.onComplete(
          resultCreateUserhandler -> {
            if (resultCreateUserhandler.succeeded()) {
              JsonObject result = resultCreateUserhandler.result();
              LOGGER.debug("success :: createUserIfNotExist " + result);

              JsonArray entitites = request.getJsonArray(ENTITIES);
              LOGGER.debug("Info : Request Access for " + entitites);
              LOGGER.debug("Info : No of bindings to do : " + entitites.size());
              totalBindCount = entitites.size();
              totalBindSuccess = 0;
              requestjson.put(QUEUE_NAME, queueName);
              Future<JsonObject> deleteQueue = rabbitClient.deleteQueue(requestjson, vhost);
              deleteQueue.onComplete(
                  deleteQueuehandler -> {
                    if (deleteQueuehandler.succeeded()) {
                      LOGGER.debug("success :: Deleted Queue " + deleteQueuehandler.result());
                      Future<JsonObject> resultqueue = rabbitClient.createQueue(requestjson, vhost);
                      resultqueue.onComplete(
                          resultHandlerqueue -> {
                            if (resultHandlerqueue.succeeded()) {
                              LOGGER.debug(
                                  "success :: Create Queue " + resultHandlerqueue.result());
                              JsonObject createQueueResponse =
                                  (JsonObject) resultHandlerqueue.result();
                              if (createQueueResponse.containsKey(TITLE)
                                  && createQueueResponse
                                      .getString(TITLE)
                                      .equalsIgnoreCase(FAILURE)) {
                                LOGGER.error("failed ::" + resultHandlerqueue.cause());
                                promise.fail(createQueueResponse.toString());
                              } else {
                                LOGGER.debug("Success : Queue Created");
                                for (Object currentEntity : entitites) {
                                  String routingKey = (String) currentEntity;
                                  LOGGER.debug("Info : routingKey is " + routingKey);
                                  if (routingKey != null) {
                                    if (routingKey.isEmpty()
                                        || routingKey.isBlank()
                                        || routingKey == "") {
                                      LOGGER.error("failed :: Invalid (or) NULL routingKey");

                                      Future<JsonObject> resultDeletequeue =
                                          rabbitClient.deleteQueue(requestjson, vhost);

                                      resultDeletequeue.onComplete(
                                          resultHandlerDeletequeue -> {
                                            if (resultHandlerDeletequeue.succeeded()) {
                                              promise.fail(
                                                  getResponseJson(
                                                          BAD_REQUEST_CODE,
                                                          ERROR,
                                                          INVALID_ROUTING_KEY)
                                                      .toString());
                                            }
                                          });
                                    } else {
                                      JsonArray array = new JsonArray();
                                      String exchangeName;
                                      if (isGroupResource(request)) {
                                        exchangeName = routingKey;
                                        array.add(exchangeName + DATA_WILDCARD_ROUTINGKEY);
                                      } else {
                                        exchangeName = request.getString("resourcegroup");
                                        array.add(exchangeName + "/." + routingKey);
                                      }
                                      JsonObject json = new JsonObject();
                                      json.put(EXCHANGE_NAME, exchangeName);
                                      json.put(QUEUE_NAME, queueName);
                                      json.put(ENTITIES, array);
                                      Future<JsonObject> resultbind =
                                          rabbitClient.bindQueue(json, vhost);
                                      resultbind.onComplete(
                                          resultHandlerbind -> {
                                            if (resultHandlerbind.succeeded()) {
                                              // count++
                                              totalBindSuccess += 1;
                                              LOGGER.info(
                                                  "sucess :: totalBindSuccess "
                                                      + totalBindSuccess
                                                      + resultHandlerbind.result());

                                              JsonObject bindResponse =
                                                  (JsonObject) resultHandlerbind.result();
                                              if (bindResponse.containsKey(TITLE)
                                                  && bindResponse
                                                      .getString(TITLE)
                                                      .equalsIgnoreCase(FAILURE)) {
                                                LOGGER.error(
                                                    "failed ::" + resultHandlerbind.cause());
                                                Future<JsonObject> resultDeletequeue =
                                                    rabbitClient.deleteQueue(requestjson, vhost);
                                                resultDeletequeue.onComplete(
                                                    resultHandlerDeletequeue -> {
                                                      if (resultHandlerDeletequeue.succeeded()) {
                                                        promise.tryFail(
                                                            new JsonObject()
                                                                .put(ERROR, "Binding Failed")
                                                                .toString());
                                                      }
                                                    });
                                              } else if (totalBindSuccess == totalBindCount) {

                                                rabbitClient
                                                    .updateUserPermissions(
                                                        vhost,
                                                        userid,
                                                        PermissionOpType.ADD_READ,
                                                        queueName)
                                                    .onComplete(
                                                        permissionHandler -> {
                                                          if (permissionHandler.succeeded()) {
                                                            updateStreamingSubscriptionResponse.put(
                                                                ENTITIES, entitites);

                                                            JsonObject response = new JsonObject();
                                                            response.put(
                                                                TYPE,
                                                                ResponseUrn.SUCCESS_URN.getUrn());
                                                            response.put(TITLE, "success");
                                                            response.put(
                                                                RESULTS,
                                                                new JsonArray()
                                                                    .add(
                                                                        updateStreamingSubscriptionResponse));
                                                            promise.complete(response);
                                                          } else {
                                                            LOGGER.error(
                                                                "failed ::"
                                                                    + permissionHandler.cause());
                                                            Future<JsonObject> resultDeletequeue =
                                                                rabbitClient.deleteQueue(
                                                                    requestjson, vhost);
                                                            resultDeletequeue.onComplete(
                                                                resultHandlerDeletequeue -> {
                                                                  if (resultHandlerDeletequeue
                                                                      .succeeded()) {
                                                                    promise.fail(
                                                                        new JsonObject()
                                                                            .put(
                                                                                ERROR,
                                                                                "user Permission failed")
                                                                            .toString());
                                                                  }
                                                                });
                                                          }
                                                        });
                                              }
                                            } else if (resultHandlerbind.failed()) {
                                              LOGGER.error("failed ::" + resultHandlerbind.cause());
                                              Future<JsonObject> resultDeletequeue =
                                                  rabbitClient.deleteQueue(requestjson, vhost);
                                              resultDeletequeue.onComplete(
                                                  resultHandlerDeletequeue -> {
                                                    if (resultHandlerDeletequeue.succeeded()) {
                                                      promise.tryFail(
                                                          getResponseJson(
                                                                  BAD_REQUEST_CODE,
                                                                  ERROR,
                                                                  BINDING_FAILED)
                                                              .toString());
                                                    }
                                                  });
                                            }
                                          });
                                    }
                                  } else {
                                    LOGGER.error("failed :: Invalid (or) NULL routingKey");
                                    Future<JsonObject> resultDeletequeue =
                                        rabbitClient.deleteQueue(requestjson, vhost);
                                    resultDeletequeue.onComplete(
                                        resultHandlerDeletequeue -> {
                                          if (resultHandlerDeletequeue.succeeded()) {
                                            promise.fail(
                                                getResponseJson(
                                                        BAD_REQUEST_CODE,
                                                        ERROR,
                                                        INVALID_ROUTING_KEY)
                                                    .toString());
                                          }
                                        });
                                  }
                                }
                              }
                            } else if (resultHandlerqueue.failed()) {
                              LOGGER.error("failed ::" + resultHandlerqueue.cause());
                              promise.fail(
                                  getResponseJson(INTERNAL_ERROR_CODE, ERROR, QUEUE_CREATE_ERROR)
                                      .toString());
                            }
                          });
                    } else if (deleteQueuehandler.failed()) {
                      LOGGER.error("failed ::" + deleteQueuehandler.cause());
                      promise.fail(
                          getResponseJson(INTERNAL_ERROR_CODE, ERROR, QUEUE_DELETE_ERROR)
                              .toString());
                    }
                  });
            }
          });
    } else {
      LOGGER.error("Error in payload");
      promise.fail(getResponseJson(BAD_REQUEST_CODE, ERROR, PAYLOAD_ERROR).toString());
    }
    return promise.future();
  }

  Future<JsonObject> appendStreamingSubscription(JsonObject request) {
    LOGGER.trace("Info : SubscriptionService#appendStreamingSubscription() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject appendStreamingSubscriptionResponse = new JsonObject();
    JsonObject requestjson = new JsonObject();
    if (request != null && !request.isEmpty()) {
      JsonArray entitites = request.getJsonArray(ENTITIES);
      LOGGER.debug("Info : Request Access for " + entitites);
      LOGGER.debug("Info : No of bindings to do : " + entitites.size());
      totalBindCount = entitites.size();
      totalBindSuccess = 0;
      String queueName = request.getString(SUBSCRIPTION_ID);
      requestjson.put(QUEUE_NAME, queueName);
      Future<JsonObject> result = rabbitClient.listQueueSubscribers(requestjson, vhost);
      String userid = request.getString(USER_ID);
      result.onComplete(
          resultHandlerqueue -> {
            if (resultHandlerqueue.succeeded()) {
              JsonObject listQueueResponse = (JsonObject) resultHandlerqueue.result();
              LOGGER.debug("Info : " + listQueueResponse);
              if (listQueueResponse.containsKey(TITLE)
                  && listQueueResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
                promise.fail(listQueueResponse.toString());
              } else {
                for (Object currentEntity : entitites) {
                  String routingKey = (String) currentEntity;
                  LOGGER.debug("Info : routingKey is " + routingKey);
                  if (routingKey != null) {
                    if (routingKey.isEmpty() || routingKey.isBlank() || routingKey == "") {
                      LOGGER.error("failed :: Invalid (or) NULL routingKey");
                      promise.fail(
                          getResponseJson(BAD_REQUEST_CODE, ERROR, INVALID_ROUTING_KEY).toString());
                    } else {
                      JsonArray array = new JsonArray();
                      String exchangeName;
                      if (isGroupResource(request)) {
                        exchangeName = routingKey;
                        array.add(exchangeName + DATA_WILDCARD_ROUTINGKEY);
                      } else {
                        exchangeName = request.getString("resourcegroup");
                        array.add(exchangeName + "/." + routingKey);
                      }
                      JsonObject json = new JsonObject();
                      json.put(EXCHANGE_NAME, exchangeName);
                      json.put(QUEUE_NAME, queueName);
                      json.put(ENTITIES, array);

                      Future<JsonObject> resultbind = rabbitClient.bindQueue(json, vhost);
                      resultbind.onComplete(
                          resultHandlerbind -> {
                            if (resultHandlerbind.succeeded()) {
                              // count++
                              totalBindSuccess += 1;
                              LOGGER.debug(
                                  "sucess :: totalBindSuccess "
                                      + totalBindSuccess
                                      + resultHandlerbind.result());

                              JsonObject bindResponse = (JsonObject) resultHandlerbind.result();
                              if (bindResponse.containsKey(TITLE)
                                  && bindResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
                                LOGGER.error("failed ::" + resultHandlerbind.cause());
                                // promise.fail(new JsonObject().put(ERROR, "Binding
                                // Failed").toString());
                                promise.tryFail(
                                    getResponseJson(
                                            BAD_REQUEST_CODE,
                                            ResponseUrn.BAD_REQUEST_URN.getUrn(),
                                            BINDING_FAILED)
                                        .toString());
                              } else if (totalBindSuccess == totalBindCount) {
                                rabbitClient
                                    .updateUserPermissions(
                                        vhost, userid, PermissionOpType.ADD_READ, queueName)
                                    .onComplete(
                                        permissionHandler -> {
                                          if (permissionHandler.succeeded()) {
                                            appendStreamingSubscriptionResponse.put(
                                                ENTITIES, entitites);

                                            JsonObject response = new JsonObject();
                                            response.put(TYPE, ResponseUrn.SUCCESS_URN.getUrn());
                                            response.put(TITLE, "success");
                                            response.put(
                                                RESULTS,
                                                new JsonArray()
                                                    .add(appendStreamingSubscriptionResponse));

                                            promise.complete(response);
                                          } else {
                                            LOGGER.error("failed ::" + permissionHandler.cause());
                                            Future<JsonObject> resultDeletequeue =
                                                rabbitClient.deleteQueue(requestjson, vhost);
                                            resultDeletequeue.onComplete(
                                                resultHandlerDeletequeue -> {
                                                  if (resultHandlerDeletequeue.succeeded()) {
                                                    promise.fail(
                                                        new JsonObject()
                                                            .put(ERROR, "user Permission failed")
                                                            .toString());
                                                  }
                                                });
                                          }
                                        });
                              }
                            } else if (resultHandlerbind.failed()) {
                              LOGGER.error("failed ::" + resultHandlerbind.cause());
                              promise.tryFail(
                                  getResponseJson(BAD_REQUEST_CODE, ERROR, BINDING_FAILED)
                                      .toString());
                            }
                          });
                    }
                  } else {
                    LOGGER.error("failed :: Invalid (or) NULL routingKey");
                    Future<JsonObject> resultDeletequeue =
                        rabbitClient.deleteQueue(requestjson, vhost);
                    resultDeletequeue.onComplete(
                        resultHandlerDeletequeue -> {
                          if (resultHandlerDeletequeue.succeeded()) {
                            promise.fail(
                                getResponseJson(BAD_REQUEST_CODE, ERROR, INVALID_ROUTING_KEY)
                                    .toString());
                          }
                        });
                  }
                }
              }
            } else {
              LOGGER.error("Fail : Error in payload");
              promise.fail(getResponseJson(BAD_REQUEST_CODE, ERROR, PAYLOAD_ERROR).toString());
            }
          });
    } else {
      LOGGER.error("Fail : Error in payload");
      promise.fail(getResponseJson(BAD_REQUEST_CODE, ERROR, PAYLOAD_ERROR).toString());
    }
    return promise.future();
  }

  Future<JsonObject> deleteStreamingSubscription(JsonObject request) {
    LOGGER.trace("Info : SubscriptionService#deleteStreamingSubscription() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject deleteStreamingSubscription = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String queueName = request.getString(SUBSCRIPTION_ID);
      String userid = request.getString(USER_ID);
      JsonObject requestBody = new JsonObject();
      requestBody.put(QUEUE_NAME, queueName);
      Future<JsonObject> result = rabbitClient.deleteQueue(requestBody, vhost);
      result.onComplete(
          resultHandler -> {
            if (resultHandler.succeeded()) {
              JsonObject deleteQueueResponse = (JsonObject) resultHandler.result();
              if (deleteQueueResponse.containsKey(TITLE)
                  && deleteQueueResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
                LOGGER.debug("failed :: Response is " + deleteQueueResponse);
                promise.fail(deleteQueueResponse.toString());
              } else {
                deleteStreamingSubscription.mergeIn(
                    getResponseJson(
                        ResponseUrn.SUCCESS_URN.getUrn(),
                        HttpStatus.SC_OK,
                        SUCCESS,
                        "Subscription deleted Successfully"));
                Future.future(
                    fu ->
                        rabbitClient.updateUserPermissions(
                            vhost, userid, PermissionOpType.DELETE_READ, queueName));
                promise.complete(deleteStreamingSubscription);
              }
            }
            if (resultHandler.failed()) {
              LOGGER.error("failed ::" + resultHandler.cause());
              promise.fail(
                  getResponseJson(INTERNAL_ERROR_CODE, ERROR, QUEUE_DELETE_ERROR).toString());
            }
          });
    }
    return promise.future();
  }

  Future<JsonObject> listStreamingSubscriptions(JsonObject request) {
    LOGGER.trace("Info : SubscriptionService#listStreamingSubscriptions() started");
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String queueName = request.getString(SUBSCRIPTION_ID);
      JsonObject requestBody = new JsonObject();
      requestBody.put(QUEUE_NAME, queueName);
      Future<JsonObject> result = rabbitClient.listQueueSubscribers(requestBody, vhost);
      result.onComplete(
          resultHandler -> {
            if (resultHandler.succeeded()) {
              JsonObject listQueueResponse = (JsonObject) resultHandler.result();
              if (listQueueResponse.containsKey(TITLE)
                  && listQueueResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
                LOGGER.error("failed :: Response is " + listQueueResponse);
                promise.fail(listQueueResponse.toString());
              } else {
                LOGGER.debug(listQueueResponse);
                JsonObject response = new JsonObject();
                response.put(TYPE, ResponseUrn.SUCCESS_URN.getUrn());
                response.put(TITLE, "success");
                response.put(RESULTS, new JsonArray().add(listQueueResponse));

                promise.complete(response);
              }
            }
            if (resultHandler.failed()) {
              LOGGER.error("failed ::" + resultHandler.cause());
              promise.fail(getResponseJson(BAD_REQUEST_CODE, ERROR, QUEUE_LIST_ERROR).toString());
            }
          });
    }
    return promise.future();
  }

  Future<JsonObject> registerCallbackSubscription(JsonObject request) {
    LOGGER.trace("Info : SubscriptionService#registerCallbackSubscription() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject registerCallbackSubscriptionResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String userName = request.getString(CONSUMER);
      String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
      String subscriptionId = domain + "/" + getSha(userName) + "/" + request.getString(NAME);
      JsonObject publishjson = new JsonObject();
      publishjson.put(SUBSCRIPTION_ID, subscriptionId);
      publishjson.put(OPERATION, "create");
      JsonObject requestjson = new JsonObject();

      LOGGER.debug("Info : Call Back registration ID check starts");
      String query = SELECT_CALLBACK.replace("$1", subscriptionId);
      LOGGER.debug("Info : " + query);
      pgSqlClient
          .executeAsync(query)
          .onComplete(
              resultHandlerSelectID -> {
                if (resultHandlerSelectID.succeeded()) {
                  RowSet<Row> result = resultHandlerSelectID.result();
                  /* Iterating Rows for getting entity, callbackurl, username and password */
                  String subscriptionIdDb = null;
                  if (result.size() > 0) {
                    for (Row row : result) {
                      subscriptionIdDb = row.getString(0);
                      LOGGER.debug(subscriptionIdDb);
                    }
                  }
                  if (subscriptionId.equalsIgnoreCase(subscriptionIdDb)) {
                    LOGGER.error("error : Call Back registration has duplicate ID");
                    registerCallbackSubscriptionResponse
                        .clear()
                        .mergeIn(getResponseJson(INTERNAL_ERROR_CODE, SQL_ERROR, DUPLICATE_KEY));
                    promise.fail(registerCallbackSubscriptionResponse.toString());
                  } else {

                    String callbackUrl = request.getString(CALLBACKURL);
                    String queueName = request.getString(QUEUE);
                    JsonArray entitites = request.getJsonArray(ENTITIES);
                    totalBindCount = entitites.size();
                    totalBindSuccess = 0;
                    requestjson.put(QUEUE_NAME, queueName);

                    for (Object currentEntity : entitites) {
                      String routingKey = (String) currentEntity;
                      LOGGER.debug("routingKey is " + routingKey);
                      if (routingKey != null) {
                        if (routingKey.isEmpty()
                            || routingKey.isBlank()
                            || routingKey == ""
                            || routingKey.split("/").length != 5) {
                          LOGGER.error("failed :: Invalid (or) NULL routingKey");
                          registerCallbackSubscriptionResponse
                              .clear()
                              .mergeIn(
                                  getResponseJson(INTERNAL_ERROR_CODE, ERROR, INVALID_ROUTING_KEY));
                          promise.fail(registerCallbackSubscriptionResponse.toString());
                        } else {
                          LOGGER.debug("Info : Valid ID :: Call Back registration starts");
                          String exchangeName =
                              routingKey.substring(0, routingKey.lastIndexOf("/"));
                          JsonArray array = new JsonArray();
                          array.add(exchangeName + DATA_WILDCARD_ROUTINGKEY);
                          JsonObject json = new JsonObject();
                          json.put(EXCHANGE_NAME, exchangeName);
                          json.put(QUEUE_NAME, queueName);
                          json.put(ENTITIES, array);

                          Future<JsonObject> resultbind = rabbitClient.bindQueue(json, vhost);
                          resultbind.onComplete(
                              resultHandlerbind -> {
                                if (resultHandlerbind.succeeded()) {
                                  totalBindSuccess += 1;
                                  LOGGER.debug(
                                      "sucess :: totalBindSuccess "
                                          + totalBindSuccess
                                          + resultHandlerbind.result());
                                  JsonObject bindResponse = (JsonObject) resultHandlerbind.result();
                                  if (bindResponse.containsKey(TITLE)
                                      && bindResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
                                    LOGGER.error("failed ::" + resultHandlerbind.cause());
                                    String deleteQuery =
                                        DELETE_CALLBACK.replace("$1", subscriptionId);
                                    pgSqlClient
                                        .executeAsync(deleteQuery)
                                        .onComplete(
                                            resulthandlerdel -> {
                                              if (resulthandlerdel.succeeded()) {
                                                registerCallbackSubscriptionResponse
                                                    .clear()
                                                    .mergeIn(
                                                        getResponseJson(
                                                            INTERNAL_ERROR_CODE,
                                                            ERROR,
                                                            BINDING_FAILED));
                                                promise.tryFail(
                                                    registerCallbackSubscriptionResponse
                                                        .toString());
                                              }
                                            });
                                  } else if (totalBindSuccess == totalBindCount) {
                                    OffsetDateTime dateTime = OffsetDateTime.now();
                                    String insertQuery =
                                        INSERT_CALLBACK
                                            .replace("$1", subscriptionId)
                                            .replace("$2", callbackUrl)
                                            .replace("$3", entitites.toString())
                                            .replace("$4", dateTime.toString())
                                            .replace("$5", dateTime.toString())
                                            .replace("$6", dateTime.toString());
                                    pgSqlClient
                                        .executeAsync(insertQuery)
                                        .onComplete(
                                            ar -> {
                                              if (ar.succeeded()) {
                                                String exchangename = "callback.notification";
                                                String routingkey = "create";

                                                JsonObject jsonpg = new JsonObject();
                                                jsonpg.put("body", publishjson.toString());
                                                Buffer messageBuffer =
                                                    Buffer.buffer(jsonpg.toString());
                                                rabbitClient
                                                    .getRabbitmqClient()
                                                    .basicPublish(
                                                        exchangename,
                                                        routingkey,
                                                        messageBuffer,
                                                        resultHandler -> {
                                                          if (resultHandler.succeeded()) {
                                                            registerCallbackSubscriptionResponse
                                                                .put(
                                                                    "subscriptionID",
                                                                    subscriptionId);
                                                            LOGGER.debug(
                                                                "Message published to queue");
                                                            promise.complete(
                                                                registerCallbackSubscriptionResponse);
                                                          } else {
                                                            String deleteQuery =
                                                                DELETE_CALLBACK.replace(
                                                                    "$1", subscriptionId);
                                                            pgSqlClient
                                                                .executeAsync(deleteQuery)
                                                                .onComplete(
                                                                    deletepg -> {
                                                                      if (deletepg.succeeded()) {
                                                                        registerCallbackSubscriptionResponse
                                                                            .clear()
                                                                            .mergeIn(
                                                                                getResponseJson(
                                                                                    INTERNAL_ERROR_CODE,
                                                                                    ERROR,
                                                                                    MSG_PUBLISH_FAILED));
                                                                        promise.fail(
                                                                            registerCallbackSubscriptionResponse
                                                                                .toString());
                                                                      }
                                                                    });
                                                          }
                                                        });
                                              } else {
                                                LOGGER.error("failed ::" + ar.cause().getMessage());
                                                String deleteQuery =
                                                    DELETE_CALLBACK.replace("$1", subscriptionId);
                                                pgSqlClient
                                                    .executeAsync(deleteQuery)
                                                    .onComplete(
                                                        resultHandlerDeletequeuepg -> {
                                                          if (resultHandlerDeletequeuepg
                                                              .succeeded()) {
                                                            registerCallbackSubscriptionResponse
                                                                .clear()
                                                                .mergeIn(
                                                                    getResponseJson(
                                                                        INTERNAL_ERROR_CODE,
                                                                        SQL_ERROR,
                                                                        DUPLICATE_KEY));
                                                            promise.fail(
                                                                registerCallbackSubscriptionResponse
                                                                    .toString());
                                                          }
                                                        });
                                              }
                                            });
                                  }
                                } else if (resultHandlerbind.failed()) {
                                  LOGGER.error("failed ::" + resultHandlerbind.cause());
                                  registerCallbackSubscriptionResponse
                                      .clear()
                                      .mergeIn(
                                          getResponseJson(BAD_REQUEST_CODE, ERROR, BINDING_FAILED));
                                  promise.tryFail(registerCallbackSubscriptionResponse.toString());
                                }
                              });
                        }
                      } else {
                        // TODO : DOUBT : why future passing even its handler failed ?.
                        /*
                         * The future was passing because of promise.complete() in the else
                         * condition Even though the routing key is NULL
                         */
                        LOGGER.error("failed :: Invalid (or) NULL routingKey");
                        registerCallbackSubscriptionResponse
                            .clear()
                            .mergeIn(getResponseJson(BAD_REQUEST_CODE, ERROR, INVALID_ROUTING_KEY));
                        //                promise.complete(registerCallbackSubscriptionResponse);
                        promise.fail(registerCallbackSubscriptionResponse.toString());
                      }
                    }
                  }
                }
              });
    } else {
      LOGGER.error("Fail : Error in payload");
      registerCallbackSubscriptionResponse
          .clear()
          .mergeIn(getResponseJson(BAD_REQUEST_CODE, ERROR, PAYLOAD_ERROR));
      promise.fail(registerCallbackSubscriptionResponse.toString());
    }
    return promise.future();
  }

  Future<JsonObject> updateCallbackSubscription(JsonObject request) {
    LOGGER.trace("Info : SubscriptionService#updateCallbackSubscription() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject updateCallbackSubscriptionResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String userName = request.getString("consumer");
      String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
      String subscriptionId = domain + "/" + getSha(userName) + "/" + request.getString("name");
      JsonObject publishjson = new JsonObject();
      publishjson.put("subscriptionID", subscriptionId);
      publishjson.put("operation", "update");
      String queueName = request.getString("queue");
      JsonArray entities = request.getJsonArray("entities");
      totalBindCount = entities.size();
      totalBindSuccess = 0;
      JsonObject requestjson = new JsonObject();
      requestjson.put(QUEUE_NAME, queueName);
      for (Object currentEntity : entities) {
        String routingKey = (String) currentEntity;
        LOGGER.debug("Info : routingKey is " + routingKey);
        if (routingKey != null) {
          if (routingKey.isEmpty()
              || routingKey.isBlank()
              || routingKey == ""
              || routingKey.split("/").length != 5) {
            LOGGER.error("failed :: Invalid (or) NULL routingKey");
            updateCallbackSubscriptionResponse
                .clear()
                .mergeIn(getResponseJson(BAD_REQUEST_CODE, ERROR, INVALID_ROUTING_KEY));
            promise.fail(updateCallbackSubscriptionResponse.toString());
          } else {

            String exchangeName = routingKey.substring(0, routingKey.lastIndexOf("/"));
            JsonArray array = new JsonArray();
            array.add(exchangeName + DATA_WILDCARD_ROUTINGKEY);
            JsonObject json = new JsonObject();
            json.put(EXCHANGE_NAME, exchangeName);
            json.put(QUEUE_NAME, queueName);
            json.put(ENTITIES, array);
            Future<JsonObject> resultbind = rabbitClient.bindQueue(json, vhost);
            resultbind.onComplete(
                resultHandlerbind -> {
                  if (resultHandlerbind.succeeded()) {
                    // count++
                    totalBindSuccess += 1;
                    LOGGER.debug(
                        "sucess :: totalBindSuccess "
                            + totalBindSuccess
                            + resultHandlerbind.result());
                    JsonObject bindResponse = (JsonObject) resultHandlerbind.result();
                    if (bindResponse.containsKey(TITLE)
                        && bindResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
                      LOGGER.error("failed ::" + resultHandlerbind.cause());

                      updateCallbackSubscriptionResponse.put(ERROR, "Binding Failed");
                      updateCallbackSubscriptionResponse
                          .clear()
                          .mergeIn(getResponseJson(BAD_REQUEST_CODE, ERROR, BINDING_FAILED));
                      promise.tryFail(updateCallbackSubscriptionResponse.toString());
                    } else if (totalBindSuccess == totalBindCount) {
                      String updateQuery =
                          UPDATE_CALLBACK
                              .replace("$1", entities.toString())
                              .replace("$2", subscriptionId);
                      pgSqlClient
                          .executeAsync(updateQuery)
                          .onComplete(
                              ar -> {
                                if (ar.succeeded()) {
                                  String exchangename = "callback.notification";
                                  String routingkey = "update";

                                  JsonObject jsonpg = new JsonObject();
                                  jsonpg.put("body", publishjson.toString());
                                  Buffer messageBuffer = Buffer.buffer(jsonpg.toString());
                                  rabbitClient
                                      .getRabbitmqClient()
                                      .basicPublish(
                                          exchangename,
                                          routingkey,
                                          messageBuffer,
                                          resultHandler -> {
                                            if (resultHandler.succeeded()) {
                                              updateCallbackSubscriptionResponse.put(
                                                  "subscriptionID", subscriptionId);
                                              LOGGER.debug("Info : Message published to queue");
                                              promise.complete(updateCallbackSubscriptionResponse);
                                            } else {
                                              LOGGER.error("Fail : Message published failed");
                                              updateCallbackSubscriptionResponse
                                                  .clear()
                                                  .mergeIn(
                                                      getResponseJson(
                                                          INTERNAL_ERROR_CODE,
                                                          ERROR,
                                                          MSG_PUBLISH_FAILED));
                                              promise.fail(
                                                  updateCallbackSubscriptionResponse.toString());
                                            }
                                          });
                                } else {
                                  LOGGER.error("failed ::" + ar.cause().getMessage());
                                  updateCallbackSubscriptionResponse
                                      .clear()
                                      .mergeIn(
                                          getResponseJson(
                                              INTERNAL_ERROR_CODE, SQL_ERROR, DUPLICATE_KEY));
                                  promise.fail(updateCallbackSubscriptionResponse.toString());
                                }
                              });
                    }
                  } else if (resultHandlerbind.failed()) {
                    LOGGER.error("failed ::" + resultHandlerbind.cause());
                    updateCallbackSubscriptionResponse
                        .clear()
                        .mergeIn(getResponseJson(BAD_REQUEST_CODE, ERROR, BINDING_FAILED));
                    promise.fail(updateCallbackSubscriptionResponse.toString());
                  }
                });
          }
        } else {
          LOGGER.error("failed :: Invalid (or) NULL routingKey");
          updateCallbackSubscriptionResponse
              .clear()
              .mergeIn(getResponseJson(BAD_REQUEST_CODE, ERROR, INVALID_ROUTING_KEY));
          promise.fail(updateCallbackSubscriptionResponse.toString());
        }
      }

    } else {
      LOGGER.error("Error in payload");
      updateCallbackSubscriptionResponse
          .clear()
          .mergeIn(getResponseJson(BAD_REQUEST_CODE, ERROR, PAYLOAD_ERROR));
      promise.fail(updateCallbackSubscriptionResponse.toString());
    }
    return promise.future();
  }

  Future<JsonObject> deleteCallbackSubscription(JsonObject request) {
    LOGGER.trace("Info : SubscriptionService#deleteCallbackSubscription() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject deleteCallbackSubscriptionResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String userName = request.getString(CONSUMER);
      String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
      String subscriptionId = domain + "/" + getSha(userName) + "/" + request.getString(NAME);
      LOGGER.debug("Info : Call Back registration ID check starts");
      String selectQuery = SELECT_CALLBACK.replace("$1", subscriptionId);
      pgSqlClient
          .executeAsync(selectQuery)
          .onComplete(
              resultHandlerSelectID -> {
                if (resultHandlerSelectID.succeeded()) {
                  RowSet<Row> result = resultHandlerSelectID.result();
                  /* Iterating Rows for getting entity, callbackurl, username and password */
                  String subscriptionIdDb = null;
                  if (result.size() > 0) {
                    for (Row row : result) {
                      subscriptionIdDb = row.getString(0);
                      LOGGER.debug("Info : " + subscriptionIdDb);
                    }
                  }
                  JsonObject publishjson = new JsonObject();
                  publishjson.put(SUBSCRIPTION_ID, subscriptionId);
                  publishjson.put(OPERATION, "delete");
                  String deleteQuery = DELETE_CALLBACK.replace("$1", subscriptionId);
                  pgSqlClient
                      .executeAsync(deleteQuery)
                      .onComplete(
                          ar -> {
                            if (ar.succeeded()) {
                              String exchangename = "callback.notification";
                              String routingkey = "delete";
                              JsonObject jsonpg = new JsonObject();
                              jsonpg.put("body", publishjson.toString());
                              Buffer messageBuffer = Buffer.buffer(jsonpg.toString());
                              rabbitClient
                                  .getRabbitmqClient()
                                  .basicPublish(
                                      exchangename,
                                      routingkey,
                                      messageBuffer,
                                      resultHandler -> {
                                        if (resultHandler.succeeded()) {
                                          deleteCallbackSubscriptionResponse.put(
                                              SUBSCRIPTION_ID, subscriptionId);
                                          LOGGER.debug("Info : Message published to queue");
                                        } else {
                                          LOGGER.debug("Info : Message published failed");
                                          deleteCallbackSubscriptionResponse
                                              .clear()
                                              .mergeIn(
                                                  getResponseJson(
                                                      INTERNAL_ERROR_CODE,
                                                      ERROR,
                                                      MSG_PUBLISH_FAILED));
                                          promise.tryFail(
                                              deleteCallbackSubscriptionResponse.toString());
                                        }
                                        promise.tryComplete(deleteCallbackSubscriptionResponse);
                                      });
                            } else {
                              LOGGER.error("failed ::" + ar.cause().getMessage());
                              deleteCallbackSubscriptionResponse.put(ERROR, "delete failed");
                              deleteCallbackSubscriptionResponse
                                  .clear()
                                  .mergeIn(getResponseJson(INTERNAL_ERROR_CODE, ERROR, FAILURE));
                              promise.fail(deleteCallbackSubscriptionResponse.toString());
                            }
                          });
                }
              });
    } else {
      LOGGER.error("Fail : Error in payload");
      deleteCallbackSubscriptionResponse
          .clear()
          .mergeIn(getResponseJson(BAD_REQUEST_CODE, ERROR, PAYLOAD_ERROR));
      promise.fail(deleteCallbackSubscriptionResponse.toString());
    }
    return promise.future();
  }

  Future<JsonObject> listCallbackSubscription(JsonObject request) {
    LOGGER.trace("Info : SubscriptionService#listCallbackSubscription() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject listCallbackSubscriptionResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String userName = request.getString(CONSUMER);
      String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
      String subscriptionId = domain + "/" + getSha(userName) + "/" + request.getString(NAME);
      String selectQuery = SELECT_CALLBACK.replace("$1", subscriptionId);
      pgSqlClient
          .executeAsync(selectQuery)
          .onComplete(
              ar -> {
                if (ar.succeeded()) {
                  RowSet<Row> result = ar.result();
                  LOGGER.debug("Info : " + ar.result().size() + " rows");
                  /* Iterating Rows for getting entity, callbackurl, username and password */
                  if (result.size() > 0) {
                    for (Row row : result) {
                      String subscriptionIdDb = row.getString(0);
                      String callBackUrl = row.getString(1);
                      JsonArray entities = (JsonArray) row.getValue(2);
                      listCallbackSubscriptionResponse.put(SUBSCRIPTION_ID, subscriptionIdDb);
                      listCallbackSubscriptionResponse.put(CALLBACKURL, callBackUrl);
                      listCallbackSubscriptionResponse.put(ENTITIES, entities);
                    }
                    promise.complete(listCallbackSubscriptionResponse);
                  } else {
                    LOGGER.error("Error :payload error" + ar.cause());
                    listCallbackSubscriptionResponse
                        .clear()
                        .mergeIn(getResponseJson(BAD_REQUEST_CODE, ERROR, PAYLOAD_ERROR));
                    promise.fail(listCallbackSubscriptionResponse.toString());
                  }
                } else {
                  LOGGER.error("Error :payload error" + ar.cause());
                  listCallbackSubscriptionResponse
                      .clear()
                      .mergeIn(getResponseJson(BAD_REQUEST_CODE, ERROR, PAYLOAD_ERROR));
                  promise.fail(listCallbackSubscriptionResponse.toString());
                }
              });
    } else {
      LOGGER.error("Error :payload error");
      listCallbackSubscriptionResponse
          .clear()
          .mergeIn(getResponseJson(BAD_REQUEST_CODE, ERROR, PAYLOAD_ERROR));
      promise.fail(listCallbackSubscriptionResponse.toString());
    }
    return promise.future();
  }

  private boolean isGroupResource(JsonObject jsonObject) {
    return jsonObject.getString("type").equalsIgnoreCase("resourceGroup");
  }
}
