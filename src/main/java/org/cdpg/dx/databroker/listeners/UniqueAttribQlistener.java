package org.cdpg.dx.databroker.listeners;

import static org.cdpg.dx.common.ErrorCode.ERROR_INTERNAL_SERVER;
import static org.cdpg.dx.common.ErrorMessage.INTERNAL_SERVER_ERROR;
import static org.cdpg.dx.databroker.util.Constants.UNIQUE_ATTR_Q;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.QueueOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.serviceproxy.ServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.databroker.listeners.util.BroadcastEventType;
import org.cdpg.dx.uniqueattribute.service.UniqueAttributeService;

public class UniqueAttribQlistener {
  private static final Logger LOGGER = LogManager.getLogger(UniqueAttribQlistener.class);
  private final QueueOptions options =
      new QueueOptions().setMaxInternalQueueSize(1000).setKeepMostRecent(true);
  private final RabbitMQClient iudxInternalRabbitMqClient;
  private final UniqueAttributeService uniqueAttributeService;

  public UniqueAttribQlistener(
      RabbitMQClient iudxInternalRabbitMqClient, UniqueAttributeService uniqueAttributeService) {
    this.iudxInternalRabbitMqClient = iudxInternalRabbitMqClient;
    this.uniqueAttributeService = uniqueAttributeService;
  }

  public Future<Void> start() {
    Promise<Void> promise = Promise.promise();
    iudxInternalRabbitMqClient
        .start()
        .compose(
            success -> {
              LOGGER.info("Unique Attribute Queue listener started");
              return iudxInternalRabbitMqClient.basicConsumer(UNIQUE_ATTR_Q, options);
            })
        .onSuccess(
            uniqueHandler -> {
              uniqueHandler.handler(
                  message -> {
                    Buffer body = message.body();
                    if (body != null) {
                      consumeAndRefreshUniqueCache(body, promise);
                    } else {
                      LOGGER.info("Empty json received from revoke_token queue");
                    }
                  });
            })
        .onFailure(
            failure -> {
              LOGGER.error("failed to start " + failure.getMessage());
              promise.fail(new ServiceException(ERROR_INTERNAL_SERVER, INTERNAL_SERVER_ERROR));
            });
    return promise.future();
  }

  private void consumeAndRefreshUniqueCache(Buffer body, Promise<Void> promise) {
    JsonObject uniqueAttribJson = new JsonObject(body);
    LOGGER.debug("received message from unique-attrib Q :" + uniqueAttribJson);
    String id = uniqueAttribJson.getString("id");
    String value = uniqueAttribJson.getString("unique-attribute");
    String eventType = uniqueAttribJson.getString("eventType");
    BroadcastEventType event = BroadcastEventType.from(eventType);
    LOGGER.debug("Broadcast event " + event);
    if (event == null) {
      LOGGER.error("Invalid BroadcastEventType [ null ] ");
      return;
    }
    if (event.equals(BroadcastEventType.CREATE)) {
      uniqueAttributeService
          .putUniqueAttributeInCache(id, value)
          .onSuccess(
              successHandler -> {
                LOGGER.debug("unique attrib message published");
                promise.complete();
              })
          .onFailure(
              failureHandler -> {
                LOGGER.debug("unique attrib message published fail");
                promise.fail(new ServiceException(ERROR_INTERNAL_SERVER, INTERNAL_SERVER_ERROR));
              });
    }
  }
}
