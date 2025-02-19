package iudx.resource.server.databroker.service;

import static iudx.resource.server.apiserver.util.Constants.ID;
import static iudx.resource.server.apiserver.util.Constants.RESOURCE_GROUP;
import static iudx.resource.server.cache.util.CacheType.CATALOGUE_CACHE;
import static iudx.resource.server.database.util.Constants.ERROR;
import static iudx.resource.server.databroker.util.Constants.*;
import static iudx.resource.server.databroker.util.Util.getResponseJson;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.RabbitMQClient;
import iudx.resource.server.apiserver.subscription.model.SubscriptionImplModel;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.common.Vhosts;
import iudx.resource.server.databroker.model.SubscriptionResponseModel;
import iudx.resource.server.databroker.util.PermissionOpType;
import iudx.resource.server.databroker.util.RabbitClient;
import iudx.resource.server.databroker.util.Util;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataBrokerServiceImpl implements DataBrokerService {
  private static final Logger LOGGER = LogManager.getLogger(DataBrokerServiceImpl.class);
  private final String amqpUrl;
  private final int amqpPort;
  CacheService cacheService;
  private String vhostProd;
  private String iudxInternalVhost;
  private String externalVhost;
  private RabbitClient rabbitClient;
  private RabbitMQClient iudxInternalRabbitMqClient;
  private RabbitMQClient iudxRabbitMqClient;
  private JsonObject config;

  public DataBrokerServiceImpl(
      RabbitClient client,
      String amqpUrl,
      int amqpPort,
      CacheService cacheService,
      String iudxInternalVhost,
      String prodVhost,
      String externalVhost,
      RabbitMQClient iudxInternalRabbitMqClient,
      RabbitMQClient iudxRabbitMqClient,
      JsonObject config) {
    this.rabbitClient = client;
    this.amqpUrl = amqpUrl;
    this.amqpPort = amqpPort;
    this.cacheService = cacheService;
    this.vhostProd = prodVhost;
    this.iudxInternalVhost = iudxInternalVhost;
    this.externalVhost = externalVhost;
    this.iudxInternalRabbitMqClient = iudxInternalRabbitMqClient;
    this.iudxRabbitMqClient = iudxRabbitMqClient;
    this.config = config;
    LOGGER.trace("Info : DataBrokerServiceImpl#constructor() completed");
  }

  @Override
  public Future<Void> deleteStreamingSubscription(String queueName, String userid) {
    LOGGER.trace("Info : SubscriptionService#deleteStreamingSubscription() started");
    Promise<Void> promise = Promise.promise();

    rabbitClient
        .deleteQueue(queueName, vhostProd)
        .compose(
            resultHandler -> {
              return rabbitClient.updateUserPermissions(
                  vhostProd, userid, PermissionOpType.DELETE_READ, queueName);
            })
        .onSuccess(
            updatePermissionHandler -> {
              promise.complete();
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error("failed ::" + failureHandler.getMessage());
              promise.fail(
                  getResponseJson(
                          HttpStatusCode.INTERNAL_SERVER_ERROR.getUrn(),
                          INTERNAL_ERROR_CODE,
                          ERROR,
                          QUEUE_DELETE_ERROR)
                      .toString());
            });

    return promise.future();
  }

  @Override
  public Future<SubscriptionResponseModel> registerStreamingSubscription(
      SubscriptionImplModel subscriptionImplModel) {
    LOGGER.trace("Info : SubscriptionService#registerStreamingSubscription() started");
    Promise<SubscriptionResponseModel> promise = Promise.promise();
    ResultContainer resultContainer = new ResultContainer();
    if (subscriptionImplModel != null) {
      String userid = subscriptionImplModel.getControllerModel().getUserId();
      String queueName = userid + "/" + subscriptionImplModel.getControllerModel().getName();
      String entities = subscriptionImplModel.getControllerModel().getEntities();
      LOGGER.debug("queue name is databroker subscription  = {}", queueName);
      rabbitClient
          .createUserIfNotExist(userid, vhostProd)
          .compose(
              checkUserExist -> {
                LOGGER.debug("success :: createUserIfNotExist " + checkUserExist);

                resultContainer.apiKey = checkUserExist.getPassword();
                resultContainer.userId = checkUserExist.getUserId();

                return rabbitClient.createQueue(queueName, vhostProd);
              })
          .compose(
              createQueue -> {
                resultContainer.isQueueCreated = true;

                String routingKey = entities;
                LOGGER.debug("Info : routingKey is " + routingKey);

                JsonArray entitiesArray = new JsonArray();
                String exchangeName;
                if (isGroupResource(
                    new JsonObject().put("type", subscriptionImplModel.getResourcegroup()))) {
                  exchangeName = routingKey;
                  entitiesArray.add(exchangeName + DATA_WILDCARD_ROUTINGKEY);
                } else {
                  exchangeName = subscriptionImplModel.getResourcegroup();
                  entitiesArray.add(exchangeName + "/." + routingKey);
                }
                LOGGER.debug(" Exchange name = {}", exchangeName);
                return rabbitClient.bindQueue(exchangeName, queueName, entitiesArray, vhostProd);
              })
          .compose(
              bindQueueSuccess -> {
                LOGGER.debug("binding Queue successful");
                return rabbitClient.updateUserPermissions(
                    vhostProd, userid, PermissionOpType.ADD_READ, queueName);
              })
          .onSuccess(
              updateUserPermissionHandler -> {
                SubscriptionResponseModel subscriptionResponseModel =
                    new SubscriptionResponseModel(
                        resultContainer.userId,
                        resultContainer.apiKey,
                        queueName,
                        amqpUrl,
                        amqpPort,
                        vhostProd);
                LOGGER.debug(subscriptionResponseModel.toJson());
                promise.complete(subscriptionResponseModel);
              })
          .onFailure(
              failure -> {
                if (resultContainer.isQueueCreated) {
                  Future<Void> resultDeleteQueue = rabbitClient.deleteQueue(queueName, vhostProd);
                  resultDeleteQueue.onComplete(
                      resultHandlerDeleteQueue -> {
                        if (resultHandlerDeleteQueue.succeeded()) {
                          promise.fail(failure.getCause());
                        } else {
                          LOGGER.error("fail:: in deleteQueue " + failure.getMessage());
                          promise.fail(failure.getMessage());
                        }
                      });
                } else {
                  LOGGER.error("fail:: " + failure.getMessage());
                  promise.fail(failure.getMessage());
                }
              });
    }
    return promise.future();
  }

  @Override
  public Future<JsonObject> registerAdaptor(JsonObject request, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    String virtualHost = getVhost(vhost);
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = rabbitClient.registerAdapter(request, virtualHost);
      result.onComplete(
          resultHandler -> {
            if (resultHandler.succeeded()) {
              promise.complete(resultHandler.result());
            } else {
              LOGGER.error("registerAdaptor resultHandler failed : " + resultHandler.cause());
              promise.fail(resultHandler.cause().getMessage());
            }
          });
    }
    return promise.future();
  }

  @Override
  public Future<JsonObject> deleteAdaptor(JsonObject request, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    String virtualHost = getVhost(vhost);
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = rabbitClient.deleteAdapter(request, virtualHost);
      result.onComplete(
          resultHandler -> {
            if (resultHandler.succeeded()) {
              promise.complete(resultHandler.result());
            }
            if (resultHandler.failed()) {
              LOGGER.error("getExchange resultHandler failed : " + resultHandler.cause());
              promise.fail(resultHandler.cause().getMessage());
            }
          });
    }
    return promise.future();
  }

  @Override
  public Future<JsonObject> listAdaptor(JsonObject request, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
    String virtualHost = getVhost(vhost);
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = rabbitClient.listExchangeSubscribers(request, virtualHost);
      result.onComplete(
          resultHandler -> {
            if (resultHandler.succeeded()) {
              promise.complete(resultHandler.result());
            }
            if (resultHandler.failed()) {
              LOGGER.error("deleteAdaptor - resultHandler failed : " + resultHandler.cause());
              promise.fail(resultHandler.cause().getMessage());
            }
          });
    } else {
      promise.fail(finalResponse.toString());
      ;
    }
    return promise.future();
  }

  @Override
  public Future<JsonObject> updateStreamingSubscription(JsonObject request) {
    return null;
  }

  @Override
  public Future<List<String>> appendStreamingSubscription(
      SubscriptionImplModel subscriptionImplModel, String subId) {
    LOGGER.trace("Info : SubscriptionService#appendStreamingSubscription() started");

    Promise<List<String>> promise = Promise.promise();
    JsonObject requestjson = new JsonObject();

    String entities = subscriptionImplModel.getControllerModel().getEntities();

    String queueName = subId;
    requestjson.put(QUEUE_NAME, queueName);

    String userid = subscriptionImplModel.getControllerModel().getUserId();

    rabbitClient
        .listQueueSubscribers(queueName, vhostProd)
        .compose(
            resultHandlerQueue -> {
              LOGGER.debug("Info : " + resultHandlerQueue);
              String routingKey = entities;
              LOGGER.debug("Info : routingKey is " + routingKey);
              JsonArray entitiesArray = new JsonArray();
              String exchangeName;
              if (isGroupResource(
                  new JsonObject().put("type", subscriptionImplModel.getResourcegroup()))) {
                exchangeName = routingKey;
                entitiesArray.add(exchangeName + DATA_WILDCARD_ROUTINGKEY);
              } else {
                exchangeName = subscriptionImplModel.getResourcegroup();
                entitiesArray.add(exchangeName + "/." + routingKey);
              }
              return rabbitClient.bindQueue(exchangeName, queueName, entitiesArray, vhostProd);
            })
        .compose(
            bindQueueSuccess -> {
              LOGGER.debug("bindQueue Handler :: " + bindQueueSuccess);
              LOGGER.debug("binding Queue successful");
              return rabbitClient.updateUserPermissions(
                  vhostProd, userid, PermissionOpType.ADD_READ, queueName);
            })
        .onComplete(
            permissionHandler -> {
              if (permissionHandler.succeeded()) {
                List<String> listEntities = new ArrayList<String>();
                listEntities.add(entities);
                promise.complete(listEntities);
              } else {
                LOGGER.error("failed ::" + permissionHandler.cause());
                promise.fail(permissionHandler.cause().getMessage());
              }
            });

    return promise.future();
  }

  @Override
  public Future<List<String>> listStreamingSubscription(String subscriptionID) {
    LOGGER.trace("Info : SubscriptionService#listStreamingSubscriptions() started");

    Promise<List<String>> promise = Promise.promise();
    if (subscriptionID != null && !subscriptionID.isEmpty()) {
      rabbitClient
          .listQueueSubscribers(subscriptionID, vhostProd)
          .onComplete(
              resultHandler -> {
                if (resultHandler.succeeded()) {
                  LOGGER.debug(resultHandler.result());
                  promise.complete(resultHandler.result());
                }
                if (resultHandler.failed()) {
                  LOGGER.error("failed ::" + resultHandler.cause());
                  promise.fail(
                      getResponseJson(
                              HttpStatusCode.BAD_REQUEST.getUrn(),
                              BAD_REQUEST_CODE,
                              ERROR,
                              QUEUE_LIST_ERROR)
                          .toString());
                }
              });
    } else {
      promise.fail(
          getResponseJson(
                  HttpStatusCode.INTERNAL_SERVER_ERROR.getUrn(),
                  INTERNAL_ERROR_CODE,
                  ERROR,
                  QUEUE_LIST_ERROR)
              .toString());
    }
    return promise.future();
  }

  @Override
  public Future<JsonObject> publishFromAdaptor(JsonArray request, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
    LOGGER.debug("request JsonArray " + request);

    if (request != null && !request.isEmpty()) {

      JsonObject cacheRequestJson = new JsonObject();
      cacheRequestJson.put("type", CATALOGUE_CACHE);
      cacheRequestJson.put("key", request.getJsonObject(0).getJsonArray("entities").getValue(0));
      cacheService
          .get(cacheRequestJson)
          .onComplete(
              cacheHandler -> {
                if (cacheHandler.succeeded()) {
                  JsonObject cacheResult = cacheHandler.result();
                  String resourceGroupId =
                      cacheResult.containsKey(RESOURCE_GROUP)
                          ? cacheResult.getString(RESOURCE_GROUP)
                          : cacheResult.getString(ID);
                  LOGGER.debug("Info : resourceGroupId  " + resourceGroupId);
                  String id =
                      request.getJsonObject(0).getJsonArray("entities").getValue(0).toString();
                  String routingKey = resourceGroupId + "/." + id;
                  request.remove("entities");

                  for (int i = 0; i < request.size(); i++) {
                    JsonObject jsonObject = request.getJsonObject(i);
                    jsonObject.remove("entities");
                    jsonObject.put("id", id);
                  }
                  LOGGER.trace(request);
                  if (resourceGroupId != null && !resourceGroupId.isBlank()) {
                    LOGGER.debug("Info : routingKey  " + routingKey);
                    Buffer buffer = Buffer.buffer(request.encode());
                    iudxRabbitMqClient.basicPublish(
                        resourceGroupId,
                        routingKey,
                        buffer,
                        resultHandler -> {
                          if (resultHandler.succeeded()) {
                            finalResponse.put(STATUS, HttpStatus.SC_OK);
                            LOGGER.info("Success : Message published to queue");
                            promise.complete(finalResponse);
                          } else {
                            finalResponse.put(TYPE, HttpStatus.SC_BAD_REQUEST);
                            LOGGER.error("Fail : " + resultHandler.cause().toString());
                            promise.fail(resultHandler.cause().getMessage());
                          }
                        });
                  }
                } else {
                  LOGGER.error("Item not found");
                }
              });
    }
    return promise.future();
  }

  @Override
  public Future<JsonObject> resetPassword(String userid) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject response = new JsonObject();
    String password = Util.randomPassword.get();

    rabbitClient
        .resetPasswordInRmq(userid, password)
        .onSuccess(
            successHandler -> {
              response.put("type", ResponseUrn.SUCCESS_URN.getUrn());
              response.put(TITLE, "successful");
              response.put(DETAIL, "Successfully changed the password");
              JsonArray result =
                  new JsonArray()
                      .add(new JsonObject().put("username", userid).put("apiKey", password));
              response.put("result", result);
              promise.complete(response);
            })
        .onFailure(
            failurehandler -> {
              JsonObject failureResponse = new JsonObject();
              failureResponse
                  .put("type", 401)
                  .put("title", "not authorized")
                  .put("detail", "not authorized");
              promise.fail(failureResponse.toString());
            });
    return promise.future();
  }

  @Override
  public Future<Void> publishMessage(JsonObject body, String toExchange, String routingKey) {
    Buffer buffer = Buffer.buffer(body.toString());
    Promise<Void> promise = Promise.promise();
    iudxInternalRabbitMqClient
        .basicPublish(toExchange, routingKey, buffer)
        .onSuccess(
            publishSuccess -> {
              LOGGER.debug("publishMessage success");
              promise.complete();
            })
        .onFailure(
            publishFailure -> {
              LOGGER.debug("publishMessage failure");
              promise.fail(
                  getResponseJson(
                          HttpStatusCode.INTERNAL_SERVER_ERROR.getUrn(),
                          INTERNAL_ERROR_CODE,
                          ResponseUrn.QUEUE_ERROR_URN.getMessage(),
                          HttpStatusCode.INTERNAL_SERVER_ERROR.getDescription())
                      .toString());
            });
    return promise.future();
  }

  private boolean isGroupResource(JsonObject jsonObject) {
    return jsonObject.getString("type").equalsIgnoreCase("resourceGroup");
  }

  private String getVhost(String vhost) {
    String vhostKey = Vhosts.valueOf(vhost).value;
    return this.config.getString(vhostKey);
  }

  public class ResultContainer {
    public String apiKey;
    public String userId;
    public boolean isQueueCreated = false;
  }
}
