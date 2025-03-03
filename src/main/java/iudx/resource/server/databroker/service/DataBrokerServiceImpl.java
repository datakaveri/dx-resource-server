package iudx.resource.server.databroker.service;

import static iudx.resource.server.common.HttpStatusCode.INTERNAL_SERVER_ERROR;
import static iudx.resource.server.databroker.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.serviceproxy.ServiceException;
import iudx.resource.server.apiserver.ingestion.model.IngestionModel;
import iudx.resource.server.apiserver.subscription.model.SubscriptionImplModel;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.databroker.model.ExchangeSubscribersResponse;
import iudx.resource.server.databroker.model.IngestionResponseModel;
import iudx.resource.server.databroker.model.SubscriptionResponseModel;
import iudx.resource.server.databroker.util.PermissionOpType;
import iudx.resource.server.databroker.util.RabbitClient;
import iudx.resource.server.databroker.util.Util;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataBrokerServiceImpl implements DataBrokerService {
  private static final Logger LOGGER = LogManager.getLogger(DataBrokerServiceImpl.class);
  private final String amqpUrl;
  private final int amqpPort;
  private final String vhostProd;
  private final String iudxInternalVhost;
  private final String externalVhost;
  private final RabbitClient rabbitClient;
  private final RabbitMQClient iudxInternalRabbitMqClient;
  private final RabbitMQClient iudxRabbitMqClient;

  public DataBrokerServiceImpl(
      RabbitClient client,
      String amqpUrl,
      int amqpPort,
      String iudxInternalVhost,
      String prodVhost,
      String externalVhost,
      RabbitMQClient iudxInternalRabbitMqClient,
      RabbitMQClient iudxRabbitMqClient) {
    this.rabbitClient = client;
    this.amqpUrl = amqpUrl;
    this.amqpPort = amqpPort;
    this.vhostProd = prodVhost;
    this.iudxInternalVhost = iudxInternalVhost;
    this.externalVhost = externalVhost;
    this.iudxInternalRabbitMqClient = iudxInternalRabbitMqClient;
    this.iudxRabbitMqClient = iudxRabbitMqClient;
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
              promise.fail(new ServiceException(0, QUEUE_DELETE_ERROR));
            });

    return promise.future();
  }

  @Override
  public Future<SubscriptionResponseModel> registerStreamingSubscription(
      SubscriptionImplModel subscriptionImplModel) {
    LOGGER.trace("Info : SubscriptionService#registerStreamingSubscription() started");
    Promise<SubscriptionResponseModel> promise = Promise.promise();
    AtomicReference<String> apiKey = new AtomicReference<>(null);
    AtomicReference<String> userId = new AtomicReference<>(null);
    AtomicBoolean isQueueCreated = new AtomicBoolean(false);
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

                apiKey.set(checkUserExist.getPassword());
                userId.set(checkUserExist.getUserId());

                return rabbitClient.createQueue(queueName, vhostProd);
              })
          .compose(
              createQueue -> {
                isQueueCreated.set(true);

                String routingKey = entities;
                LOGGER.debug("Info : routingKey is " + routingKey);
                String topic;
                String exchangeName;
                if (isGroupResource(
                    new JsonObject().put("type", subscriptionImplModel.getResourceGroup()))) {
                  exchangeName = routingKey;
                  topic = exchangeName + DATA_WILDCARD_ROUTINGKEY;
                } else {
                  exchangeName = subscriptionImplModel.getResourceGroup();
                  topic = exchangeName + "/." + routingKey;
                }
                LOGGER.debug(" Exchange name = {}", exchangeName);
                return rabbitClient.bindQueue(exchangeName, queueName, topic, vhostProd);
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
                        userId.get(), apiKey.get(), queueName, amqpUrl, amqpPort, vhostProd);
                LOGGER.debug(subscriptionResponseModel.toJson());
                promise.complete(subscriptionResponseModel);
              })
          .onFailure(
              failure -> {
                LOGGER.error("fail:: " + failure.getMessage());
                if (isQueueCreated.get()) {
                  Future<Void> resultDeleteQueue = rabbitClient.deleteQueue(queueName, vhostProd);
                  resultDeleteQueue.onComplete(
                      resultHandlerDeleteQueue -> {
                        if (resultHandlerDeleteQueue.succeeded()) {
                          promise.complete();
                        } else {
                          LOGGER.error("fail:: in deleteQueue " + failure /*.getMessage()*/);
                          promise.fail(failure);
                        }
                      });
                } else {
                  LOGGER.error("fail:: " + failure);
                  promise.fail(failure);
                }
              });
    }
    return promise.future();
  }

  @Override
  public Future<IngestionResponseModel> registerAdaptor(IngestionModel ingestionModel) {
    Promise<IngestionResponseModel> promise = Promise.promise();

    AtomicReference<String> apiKey = new AtomicReference<>(null);
    AtomicReference<String> userId = new AtomicReference<>(null);
    AtomicBoolean isExchangeCreated = new AtomicBoolean(false);
    String topic;
    if (ingestionModel.getType().equalsIgnoreCase("resourceGroup")) {
      topic = ingestionModel.getResourceIdForIngestion() + DATA_WILDCARD_ROUTINGKEY;
    } else {
      topic = ingestionModel.getResourceIdForIngestion() + "/." + ingestionModel.getEntities();
    }
    rabbitClient
        .createUserIfNotExist(ingestionModel.getUserId(), vhostProd)
        .compose(
            userCreation -> {
              LOGGER.debug("success :: userCreation ");

              apiKey.set(userCreation.getPassword());
              userId.set(userCreation.getUserId());

              return rabbitClient.createExchange(
                  ingestionModel.getResourceIdForIngestion(), vhostProd);
            })
        .compose(
            createExchangeResult -> {
              isExchangeCreated.set(true);
              LOGGER.debug("Success : Exchange created successfully.");
              return rabbitClient.updateUserPermissions(
                  vhostProd,
                  ingestionModel.getUserId(),
                  PermissionOpType.ADD_WRITE,
                  ingestionModel.getResourceIdForIngestion());
            })
        .compose(
            updatePermissionHandler -> {
              return rabbitClient.bindQueue(
                  ingestionModel.getResourceIdForIngestion(), QUEUE_DATA, topic, vhostProd);
            })
        .compose(
            dataBasseQueueBinding -> {
              LOGGER.debug("Success : Data Base Queue Binding successful.");
              return rabbitClient.bindQueue(
                  ingestionModel.getResourceIdForIngestion(), REDIS_LATEST, topic, vhostProd);
            })
        .compose(
            redisQueueBinding -> {
              LOGGER.debug("Success : Redis Queue Binding successful.");
              return rabbitClient.bindQueue(
                  ingestionModel.getResourceIdForIngestion(), QUEUE_AUDITING, topic, vhostProd);
            })
        .onSuccess(
            queueSubscriptionMonitoringBinding -> {
              LOGGER.debug("Success : Queue Subscription Monitoring Binding successful.");
              IngestionResponseModel ingestionResponseModel =
                  new IngestionResponseModel(
                      ingestionModel.getUserId(),
                      apiKey.get(),
                      ingestionModel.getResourceIdForIngestion(),
                      amqpUrl,
                      amqpPort,
                      vhostProd);
              LOGGER.debug(ingestionResponseModel.toJson());
              promise.complete(ingestionResponseModel);
            })
        .onFailure(
            failure -> {
              LOGGER.error("Adaptor creation Failed");
              LOGGER.error("fail:: " + failure.getMessage());
              if (isExchangeCreated.get()) {
                Future.future(
                    fu ->
                        rabbitClient.deleteExchange(
                            ingestionModel.getResourceIdForIngestion(), vhostProd));
              }
              promise.fail(failure);
            });

    return promise.future();
  }

  @Override
  public Future<Void> deleteAdaptor(String exchangeId, String userId) {
    Promise<Void> promise = Promise.promise();
    rabbitClient
        .getExchange(exchangeId, vhostProd)
        .compose(
            getExchangeHandler -> {
              LOGGER.debug("exchange found to delete");
              return rabbitClient.deleteExchange(exchangeId, vhostProd);
            })
        .compose(
            deleteExchange -> {
              LOGGER.debug("Info : " + exchangeId + " adaptor deleted successfully");
              return rabbitClient.updateUserPermissions(
                  vhostProd, userId, PermissionOpType.DELETE_WRITE, exchangeId);
            })
        .onSuccess(
            updateUserPermissionsHandler -> {
              LOGGER.info("permissions updated successfully");
              promise.complete();
            })
        .onFailure(
            failure -> {
              LOGGER.error("deleteAdaptor failed : " + failure);
              promise.fail(failure);
            });
    return promise.future();
  }

  @Override
  public Future<ExchangeSubscribersResponse> listAdaptor(String adaptorId) {
    Promise<ExchangeSubscribersResponse> promise = Promise.promise();
    Future<ExchangeSubscribersResponse> result =
        rabbitClient.listExchangeSubscribers(adaptorId, vhostProd);
    result.onComplete(
        resultHandler -> {
          if (resultHandler.succeeded()) {
            promise.complete(resultHandler.result());
          }
          if (resultHandler.failed()) {
            LOGGER.error("deleteAdaptor - resultHandler failed : " + resultHandler.cause());
            promise.fail(resultHandler.cause());
          }
        });
    return promise.future();
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
              String topic;
              String exchangeName;
              if (isGroupResource(
                  new JsonObject().put("type", subscriptionImplModel.getResourceGroup()))) {
                exchangeName = routingKey;
                topic = exchangeName + DATA_WILDCARD_ROUTINGKEY;
              } else {
                exchangeName = subscriptionImplModel.getResourceGroup();
                topic = exchangeName + "/." + routingKey;
              }
              return rabbitClient.bindQueue(exchangeName, queueName, topic, vhostProd);
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
                promise.fail(permissionHandler.cause());
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
                } else {
                  LOGGER.error("failed ::" + resultHandler.cause());
                  promise.fail(new ServiceException(8, QUEUE_LIST_ERROR));
                }
              });
    } else {
      promise.fail(new ServiceException(0, QUEUE_LIST_ERROR));
    }
    return promise.future();
  }

  @Override
  public Future<String> publishFromAdaptor(
      String resourceGroupId, String routingKey, JsonArray request) {
    Promise<String> promise = Promise.promise();
    Buffer buffer = Buffer.buffer(request.encode());
    iudxRabbitMqClient
        .basicPublish(resourceGroupId, routingKey, buffer)
        .onSuccess(
            resultHandler -> {
              LOGGER.info("Success : Message published to queue");
              promise.complete("success");
            })
        .onFailure(
            failure -> {
              LOGGER.error("Fail : " + failure.getMessage());
              promise.fail(failure);
            });
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
              promise.fail(new ServiceException(0, INTERNAL_SERVER_ERROR.getDescription()));
            });
    return promise.future();
  }

  private boolean isGroupResource(JsonObject jsonObject) {
    return jsonObject.getString("type").equalsIgnoreCase("resourceGroup");
  }
}
