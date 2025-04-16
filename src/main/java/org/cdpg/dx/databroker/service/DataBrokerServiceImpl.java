package org.cdpg.dx.databroker.service;

import static org.cdpg.dx.common.ErrorCode.ERROR_BAD_REQUEST;
import static org.cdpg.dx.common.ErrorCode.ERROR_INTERNAL_SERVER;
import static org.cdpg.dx.common.ErrorMessage.INTERNAL_SERVER_ERROR;
import static org.cdpg.dx.databroker.util.Constants.QUEUE_LIST_ERROR;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.databroker.client.RabbitClient;
import org.cdpg.dx.databroker.model.ExchangeSubscribersResponse;
import org.cdpg.dx.databroker.model.RegisterExchangeModel;
import org.cdpg.dx.databroker.model.RegisterQueueModel;
import org.cdpg.dx.databroker.util.PermissionOpType;
import org.cdpg.dx.databroker.util.Util;
import org.cdpg.dx.databroker.util.Vhosts;

public class DataBrokerServiceImpl implements DataBrokerService {
  private static final Logger LOGGER = LogManager.getLogger(DataBrokerServiceImpl.class);
  private final String amqpUrl;
  private final int amqpPort;
  private final String vhostProd;
  private final String iudxInternalVhost;
  private final String externalVhost;
  private final RabbitClient rabbitClient;

  public DataBrokerServiceImpl(
      RabbitClient client,
      String amqpUrl,
      int amqpPort,
      String iudxInternalVhost,
      String prodVhost,
      String externalVhost) {
    this.rabbitClient = client;
    this.amqpUrl = amqpUrl;
    this.amqpPort = amqpPort;
    this.vhostProd = prodVhost;
    this.iudxInternalVhost = iudxInternalVhost;
    this.externalVhost = externalVhost;
    LOGGER.trace("Info : DataBrokerServiceImpl#constructor() completed");
  }

  @Override
  public Future<Void> deleteQueue(String queueName, String userid, Vhosts vhosts) {
    LOGGER.trace("Info : deleteQueue() started");
    Promise<Void> promise = Promise.promise();

    rabbitClient
        .deleteQueue(queueName, getVhost(vhosts))
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
  public Future<RegisterQueueModel> registerQueue(String userId, String queueName, Vhosts vhosts) {
    LOGGER.trace("Info : registerQueue() started");
    Promise<RegisterQueueModel> promise = Promise.promise();
    AtomicReference<String> apiKey = new AtomicReference<>(null);

    LOGGER.debug("queue name {}", queueName);
    rabbitClient
        .createUserIfNotExist(userId, getVhost(vhosts))
        .compose(
            checkUserExist -> {
              LOGGER.debug("success :: createUserIfNotExist ");
              apiKey.set(checkUserExist.getPassword());
              return rabbitClient.createQueue(queueName, getVhost(vhosts));
            })
        .onSuccess(
            createQueue -> {
              LOGGER.debug("success :: createQueue");
              RegisterQueueModel registerQueueModel =
                  new RegisterQueueModel(
                      userId, apiKey.get(), queueName, amqpUrl, amqpPort, getVhost(vhosts));
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
  public Future<Void> queueBinding(
      String exchangeName, String queueName, String routingKey, Vhosts vhosts) {
    LOGGER.trace("Info : queueBinding() started");
    Promise<Void> promise = Promise.promise();
    rabbitClient
        .bindQueue(exchangeName, queueName, routingKey, getVhost(vhosts))
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
  public Future<RegisterExchangeModel> registerExchange(
      String userId, String exchangeName, Vhosts vhosts) {
    LOGGER.trace("Info : registerExchange() started");
    Promise<RegisterExchangeModel> promise = Promise.promise();
    AtomicReference<String> apiKey = new AtomicReference<>(null);
    String vhost = getVhost(vhosts);
    rabbitClient
        .createUserIfNotExist(userId, vhost)
        .compose(
            userCreation -> {
              LOGGER.debug("success :: userCreation");
              apiKey.set(userCreation.getPassword());
              return rabbitClient.createExchange(exchangeName, vhost);
            })
        .onSuccess(
            createExchangeResult -> {
              LOGGER.debug("Success : Exchange created successfully.");
              RegisterExchangeModel registerExchangeModel =
                  new RegisterExchangeModel(
                      userId, apiKey.get(), exchangeName, amqpUrl, amqpPort, vhost);
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
  public Future<Void> deleteExchange(String exchangeId, String userId, Vhosts vhosts) {
    LOGGER.trace("Info : deleteExchange() started");
    Promise<Void> promise = Promise.promise();
    rabbitClient
        .getExchange(exchangeId, getVhost(vhosts))
        .compose(
            getExchangeHandler -> {
              LOGGER.debug("exchange found to delete");
              return rabbitClient.deleteExchange(exchangeId, getVhost(vhosts));
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
  public Future<ExchangeSubscribersResponse> listExchange(String exchangeName, Vhosts vhosts) {
    Promise<ExchangeSubscribersResponse> promise = Promise.promise();
    Future<ExchangeSubscribersResponse> result =
        rabbitClient.listExchangeSubscribers(exchangeName, getVhost(vhosts));
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
      String userId, String queueOrExchangeName, PermissionOpType permissionType, Vhosts vhosts) {
    Promise<Void> promise = Promise.promise();
    LOGGER.trace("Info : updatePermission() started");
    rabbitClient
        .updateUserPermissions(getVhost(vhosts), userId, permissionType, queueOrExchangeName)
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
  public Future<List<String>> listQueue(String queueName, Vhosts vhosts) {
    LOGGER.trace("Info : listQueue() started");
    Promise<List<String>> promise = Promise.promise();
    rabbitClient
        .listQueueSubscribers(queueName, getVhost(vhosts))
        .onComplete(
            resultHandler -> {
              if (resultHandler.succeeded()) {
                LOGGER.debug(resultHandler.result());
                promise.complete(resultHandler.result());
              } else {
                LOGGER.error("failed ::" + resultHandler.cause());
                promise.fail(new ServiceException(ERROR_BAD_REQUEST, QUEUE_LIST_ERROR));
              }
            });
    return promise.future();
  }

  @Override
  public Future<String> publishMessageExternal(
      String exchangeName, String routingKey, JsonArray request) {
    Promise<String> promise = Promise.promise();
    rabbitClient
        .publishMessageExternal(exchangeName, routingKey, request)
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
  public Future<Void> publishMessageInternal(
      JsonObject body, String exchangeName, String routingKey) {
    Promise<Void> promise = Promise.promise();
    rabbitClient
        .publishMessageInternal(body, exchangeName, routingKey)
        .onSuccess(
            publishSuccess -> {
              LOGGER.debug("publishMessage success");
              promise.complete();
            })
        .onFailure(
            publishFailure -> {
              LOGGER.debug("publishMessage failure");
              promise.fail(new ServiceException(ERROR_INTERNAL_SERVER, INTERNAL_SERVER_ERROR));
            });
    return promise.future();
  }

  private String getVhost(Vhosts vhosts) {
    return switch (vhosts.value) {
      case "prodVhost" -> vhostProd;
      case "internalVhost" -> iudxInternalVhost;
      case "externalVhost" -> externalVhost;
      default -> throw new IllegalArgumentException("Invalid vhost");
    };
  }
}
