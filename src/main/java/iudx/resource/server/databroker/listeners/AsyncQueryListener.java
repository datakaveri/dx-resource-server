package iudx.resource.server.databroker.listeners;

import static iudx.resource.server.apiserver.util.Constants.HEADER_RESPONSE_FILE_FORMAT;
import static iudx.resource.server.authenticator.Constants.*;
import static iudx.resource.server.common.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.QueueOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQConsumer;
import io.vertx.rabbitmq.RabbitMQOptions;
import iudx.resource.server.database.async.AsyncService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AsyncQueryListener implements RmqListeners {

  private static final Logger LOGGER = LogManager.getLogger(AsyncQueryListener.class);
  private final QueueOptions options =
      new QueueOptions().setMaxInternalQueueSize(2).setKeepMostRecent(true);
  private final AsyncService asyncService;
  RabbitMQClient client;

  public AsyncQueryListener(
      Vertx vertx, RabbitMQOptions config, String vhost, AsyncService asyncService) {
    config.setVirtualHost(vhost);
    this.client = RabbitMQClient.create(vertx, config);
    this.asyncService = asyncService;
  }

  @Override
  public void start() {
    Future<Void> future = client.start();
    future.onComplete(
        startHandler -> {
          if (startHandler.succeeded()) {
            LOGGER.trace("starting Q listener for Async query");
            client.basicConsumer(
                ASYNC_QUERY_Q,
                options,
                asyncQListenerHandler -> {
                  if (asyncQListenerHandler.succeeded()) {
                    RabbitMQConsumer mqConsumer = asyncQListenerHandler.result();
                    mqConsumer.handler(
                        message -> {
                          Buffer body = message.body();
                          if (body != null) {
                            JsonObject asyncQueryJson = new JsonObject(body);
                            LOGGER.debug("received message from async-query Q :" + asyncQueryJson);
                            String requestId = asyncQueryJson.getString("requestId");
                            String searchId = asyncQueryJson.getString("searchId");
                            String format = asyncQueryJson.getString(HEADER_RESPONSE_FILE_FORMAT);
                            JsonObject query = asyncQueryJson.getJsonObject("query");
                            LOGGER.debug("query received from RMQ : {}", query);
                            asyncService.asyncSearch(requestId, searchId, query, format);
                          } else {
                            LOGGER.error("Empty json received from async query queue");
                          }
                        });
                  }
                });
          } else {
            LOGGER.error("Rabbit client startup failed.");
          }
        });
  }
}
