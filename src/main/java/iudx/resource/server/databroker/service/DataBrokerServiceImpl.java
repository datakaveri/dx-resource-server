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
import iudx.resource.server.databroker.model.*;
import iudx.resource.server.databroker.util.PermissionOpType;
import iudx.resource.server.databroker.util.RabbitClient;
import iudx.resource.server.databroker.util.Util;
import java.util.List;
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
  public Future<Void> deleteQueue(String queueName, String userid) {
    LOGGER.trace("Info : SubscriptionService#deleteStreamingSubscription() started");
    Promise<Void> promise = Promise.promise();

    rabbitClient
        .deleteQueue(queueName, vhostProd)
        .onSuccess(
            deleteQueue -> {
              LOGGER.debug("success :: deleteQueue");
              promise.complete();
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error("failed ::" + failureHandler);
              promise.fail(failureHandler);
            });

    return promise.future();
  }

  @Override
  public Future<RegisterQueueModel> registerQueue(String userId, String queueName) {
    LOGGER.trace("Info : registerQueue() started");
    Promise<RegisterQueueModel> promise = Promise.promise();
    AtomicReference<String> apiKey = new AtomicReference<>(null);

    LOGGER.debug("queue name {}", queueName);
    rabbitClient
        .createUserIfNotExist(userId, vhostProd)
        .compose(
            checkUserExist -> {
              LOGGER.debug("success :: createUserIfNotExist ");
              apiKey.set(checkUserExist.getPassword());
              return rabbitClient.createQueue(queueName, vhostProd);
            })
        .onSuccess(
            createQueue -> {
              LOGGER.debug("success :: createQueue");
              RegisterQueueModel registerQueueModel =
                  new RegisterQueueModel(
                      userId, apiKey.get(), queueName, amqpUrl, amqpPort, vhostProd);
              LOGGER.debug(registerQueueModel.toJson());
              promise.complete(registerQueueModel);
            })
        .onFailure(
            failure -> {
              LOGGER.error("fail:: " + failure.getMessage());
              promise.fail(failure);
            });

    return promise.future();
  }

  @Override
  public Future<Void> queueBinding(String exchangeName, String queueName, String routingKey) {
    LOGGER.trace("Info : queueBinding() started");
    Promise<Void> promise = Promise.promise();
    rabbitClient
        .bindQueue(exchangeName, queueName, routingKey, vhostProd)
        .onSuccess(
            bindQueueSuccess -> {
              LOGGER.debug("binding Queue successful");
              promise.complete();
            })
        .onFailure(
            failure -> {
              LOGGER.error("fail:: " + failure);
              promise.fail(failure);
            });
    return promise.future();
  }

  @Override
  public Future<RegisterExchangeModel> registerExchange(String userId, String exchangeName) {
    Promise<RegisterExchangeModel> promise = Promise.promise();
    AtomicReference<String> apiKey = new AtomicReference<>(null);
    rabbitClient
        .createUserIfNotExist(userId, vhostProd)
        .compose(
            userCreation -> {
              LOGGER.debug("success :: userCreation");
              apiKey.set(userCreation.getPassword());
              return rabbitClient.createExchange(exchangeName, vhostProd);
            })
        .onSuccess(
            createExchangeResult -> {
              LOGGER.debug("Success : Exchange created successfully.");
              RegisterExchangeModel registerExchangeModel =
                  new RegisterExchangeModel(
                      userId, apiKey.get(), exchangeName, amqpUrl, amqpPort, vhostProd);
              LOGGER.debug(registerExchangeModel.toJson());
              promise.complete(registerExchangeModel);
            })
        .onFailure(
            failure -> {
              LOGGER.error("Adaptor creation Failed" + failure);
              promise.fail(failure);
            });

    return promise.future();
  }

  @Override
  public Future<Void> deleteExchange(String exchangeId, String userId) {
    Promise<Void> promise = Promise.promise();
    rabbitClient
        .getExchange(exchangeId, vhostProd)
        .compose(
            getExchangeHandler -> {
              LOGGER.debug("exchange found to delete");
              return rabbitClient.deleteExchange(exchangeId, vhostProd);
            })
        .onSuccess(
            deleteExchange -> {
              LOGGER.debug("Info : " + exchangeId + " adaptor deleted successfully");
              promise.complete();
            })
        .onFailure(
            failure -> {
              LOGGER.error("failed : " + failure);
              promise.fail(failure);
            });
    return promise.future();
  }

  @Override
  public Future<ExchangeSubscribersResponse> listExchange(String exchangeName) {
    Promise<ExchangeSubscribersResponse> promise = Promise.promise();
    Future<ExchangeSubscribersResponse> result =
        rabbitClient.listExchangeSubscribers(exchangeName, vhostProd);
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
  public Future<Void> updatePermission(
      String userId, String queueOrExchangeName, PermissionOpType permissionType) {
    Promise<Void> promise = Promise.promise();
    LOGGER.trace("Info : updatePermission() started");
    rabbitClient
        .updateUserPermissions(vhostProd, userId, permissionType, queueOrExchangeName)
        .onSuccess(
            updateUserPermissionsHandler -> {
              LOGGER.info("permissions updated successfully");
              promise.complete();
            })
        .onFailure(
            failure -> {
              LOGGER.error("failed : " + failure);
              promise.fail(failure);
            });

    return promise.future();
  }

  @Override
  public Future<List<String>> listQueue(String queueName) {
    LOGGER.trace("Info : listQueue() started");
    Promise<List<String>> promise = Promise.promise();
    rabbitClient
        .listQueueSubscribers(queueName, vhostProd)
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
    return promise.future();
  }

  @Override
  public Future<String> publishFromAdaptor(
      String exchangeName, String routingKey, JsonArray request) {
    Promise<String> promise = Promise.promise();
    Buffer buffer = Buffer.buffer(request.encode());
    iudxRabbitMqClient
        .basicPublish(exchangeName, routingKey, buffer)
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
  public Future<String> resetPassword(String userId) {
    Promise<String> promise = Promise.promise();
    String password = Util.randomPassword.get();

    rabbitClient
        .resetPasswordInRmq(userId, password)
        .onSuccess(
            successHandler -> {
              promise.complete(password);
            })
        .onFailure(
            failurehandler -> {
              LOGGER.error("failed ::" + failurehandler);
              promise.fail(failurehandler);
            });
    return promise.future();
  }

  @Override
  public Future<Void> publishMessage(JsonObject body, String exchangeName, String routingKey) {
    Buffer buffer = Buffer.buffer(body.toString());
    Promise<Void> promise = Promise.promise();
    iudxInternalRabbitMqClient
        .basicPublish(exchangeName, routingKey, buffer)
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
}
