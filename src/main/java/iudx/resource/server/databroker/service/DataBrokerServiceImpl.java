package iudx.resource.server.databroker.service;

import static iudx.resource.server.apiserver.subscription.util.Constants.RESULTS;
import static iudx.resource.server.database.util.Constants.ERROR;
import static iudx.resource.server.databroker.util.Constants.*;
import static iudx.resource.server.databroker.util.Util.getResponseJson;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.subscription.model.SubscriptionImplModel;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.databroker.model.SubscriptionResponseModel;
import iudx.resource.server.databroker.util.PermissionOpType;
import iudx.resource.server.databroker.util.RabbitClient;
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

  public DataBrokerServiceImpl(
      RabbitClient client,
      String amqpUrl,
      int amqpPort,
      CacheService cacheService,
      String iudxInternalVhost,
      String prodVhost,
      String externalVhost) {
    this.rabbitClient = client;
    this.amqpUrl = amqpUrl;
    this.amqpPort = amqpPort;
    this.cacheService = cacheService;
    this.vhostProd = prodVhost;
    this.iudxInternalVhost = iudxInternalVhost;
    this.externalVhost = externalVhost;
  }

  @Override
  public Future<JsonObject> deleteStreamingSubscription(String queueName, String userid) {
    LOGGER.trace("Info : SubscriptionService#deleteStreamingSubscription() started");
    Promise<JsonObject> promise = Promise.promise();
    // TODO: To create Model
        rabbitClient
            .deleteQueue(queueName, vhostProd)
            .compose(
                resultHandler -> {
                  return rabbitClient.updateUserPermissions(
                      vhostProd, userid, PermissionOpType.DELETE_READ, queueName);
                })
            .onSuccess(
                updatePermissionHandler -> {
                  promise.complete(
                      getResponseJson(
                          ResponseUrn.SUCCESS_URN.getUrn(),
                          HttpStatus.SC_OK,
                          SUCCESS,
                          "Subscription deleted Successfully"));
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
    JsonObject registerStreamingSubscriptionResponse = new JsonObject();
    JsonObject requestjson = new JsonObject();
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
                /*JsonObject json = new JsonObject();
                json.put(EXCHANGE_NAME, exchangeName);
                json.put(QUEUE_NAME, queueName);
                json.put(ENTITIES, array);*/
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
                registerStreamingSubscriptionResponse.put(USER_NAME, resultContainer.userId);
                registerStreamingSubscriptionResponse.put(APIKEY, resultContainer.apiKey);
                registerStreamingSubscriptionResponse.put(ID, queueName);
                registerStreamingSubscriptionResponse.put(URL, this.amqpUrl);
                registerStreamingSubscriptionResponse.put(PORT, this.amqpPort);
                registerStreamingSubscriptionResponse.put(VHOST, vhostProd);
                LOGGER.debug(
                    "RegisterStreamingSubscriptionResponse  "
                        + registerStreamingSubscriptionResponse.toString());
                SubscriptionResponseModel subscriptionResponseModel =
                    new SubscriptionResponseModel(
                        resultContainer.userId,
                        resultContainer.apiKey,
                        queueName,
                        amqpUrl,
                        amqpPort,
                        vhostProd);
                LOGGER.debug(subscriptionResponseModel.toJson());
                /*JsonObject response = new JsonObject();
                response.put(TYPE, ResponseUrn.SUCCESS_URN.getUrn());
                response.put(TITLE, "success");
                response.put(
                    RESULTS, new JsonArray().add(registerStreamingSubscriptionResponse));*/
                promise.complete(subscriptionResponseModel);
              })
          .onFailure(
              failure -> {
                if (resultContainer.isQueueCreated) {
                  Future<JsonObject> resultDeleteQueue =
                      rabbitClient.deleteQueue(queueName, vhostProd);
                  resultDeleteQueue.onComplete(
                      resultHandlerDeleteQueue -> {
                        if (resultHandlerDeleteQueue.succeeded()) {
                          promise.fail(/* getResponseJson(
                                                    BAD_REQUEST_CODE, BAD_REQUEST_DATA, BINDING_FAILED)
                                                    .toString()*/ failure.toString());
                        } else {
                          LOGGER.error("fail:: in deleteQueue " + failure.toString());
                          promise.fail(failure.toString());
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
    return null;
  }

  @Override
  public Future<JsonObject> deleteAdaptor(JsonObject request, String vhost) {
    return null;
  }

  @Override
  public Future<JsonObject> listAdaptor(JsonObject request, String vhost) {
    return null;
  }

  @Override
  public Future<JsonObject> updateStreamingSubscription(JsonObject request) {
    return null;
  }

  @Override
  public Future<JsonObject> appendStreamingSubscription(
      SubscriptionImplModel subscriptionImplModel, String subId) {
    LOGGER.trace("Info : SubscriptionService#appendStreamingSubscription() started");

    Promise<JsonObject> promise = Promise.promise();
    JsonObject appendStreamingSubscriptionResponse = new JsonObject();
    JsonObject requestjson = new JsonObject();

    String entities = subscriptionImplModel.getControllerModel().getEntities();
    if (entities == null) {
      if (entities.isEmpty() || entities.isBlank()) {}

      return null;
      // TODO:throw error or move this check to somewhere else
    }
    String queueName = /*request.getString(SUBSCRIPTION_ID);*/ subId;
    requestjson.put(QUEUE_NAME, queueName);

    String userid = /*request.getString(Constants.USER_ID);*/
        subscriptionImplModel.getControllerModel().getUserId();

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
                exchangeName = /*request.getString("resourcegroup")*/
                    subscriptionImplModel.getResourcegroup();
                entitiesArray.add(exchangeName + "/." + routingKey);
              }
              /*JsonObject json = new JsonObject();
              json.put(EXCHANGE_NAME, exchangeName);
              json.put(QUEUE_NAME, queueName);
              json.put(ENTITIES, entitiesArray);*/

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
                appendStreamingSubscriptionResponse.put(ENTITIES, entities);

                JsonObject response = new JsonObject();
                response.put(TYPE, ResponseUrn.SUCCESS_URN.getUrn());
                response.put(TITLE, "success");
                response.put(RESULTS, new JsonArray().add(appendStreamingSubscriptionResponse));

                promise.complete(response);
              } else {
                LOGGER.error("failed ::" + permissionHandler.cause());
                Future<JsonObject> resultDeleteQueue =
                    rabbitClient.deleteQueue(queueName, vhostProd);
                resultDeleteQueue.onComplete(
                    resultHandlerDeleteQueue -> {
                      if (resultHandlerDeleteQueue.succeeded()) {
                        promise.fail(
                            new JsonObject().put(ERROR, "user Permission failed").toString());
                      } else {
                        // TODO: Add logger for delete queue
                      }
                    });
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
                  /* JsonObject response = new JsonObject();
                  response.put(TYPE, ResponseUrn.SUCCESS_URN.getUrn());
                  response.put(TITLE, "success");
                  response.put(RESULTS, new JsonArray().add(resultHandler.result()));*/

                  // ListStreamingSubsModel listStreamingSubsModel = new
                  // ListStreamingSubsModel(ResponseUrn.SUCCESS_URN.getUrn(), "success",
                  // resultHandler.result());

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
  public Future<JsonObject> listvHost(JsonObject request) {
    return null;
  }

  @Override
  public Future<JsonObject> listQueueSubscribers(JsonObject request, String vhost) {
    return null;
  }

  @Override
  public Future<JsonObject> publishFromAdaptor(JsonArray request, String vhost) {
    return null;
  }

  @Override
  public Future<JsonObject> resetPassword(JsonObject request) {
    return null;
  }

  @Override
  public Future<JsonObject> publishHeartbeat(JsonObject request, String vhost) {
    return null;
  }

  @Override
  public Future<JsonObject> publishMessage(JsonObject body, String toExchange, String routingKey) {
    return null;
  }

  private boolean isGroupResource(JsonObject jsonObject) {
    return jsonObject.getString("type").equalsIgnoreCase("resourceGroup");
  }

  public class ResultContainer {
    public String apiKey;
    public String userId;
    public boolean isQueueCreated = false;
  }
}
