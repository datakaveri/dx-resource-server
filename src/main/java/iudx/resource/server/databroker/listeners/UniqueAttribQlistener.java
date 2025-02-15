package iudx.resource.server.databroker.listeners;

import static iudx.resource.server.common.Constants.UNIQUE_ATTR_Q;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.QueueOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQConsumer;
import io.vertx.rabbitmq.RabbitMQOptions;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.cache.cachelmpl.CacheType;
import iudx.resource.server.common.BroadcastEventType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UniqueAttribQlistener implements RmqListeners {

  private static final Logger LOGGER = LogManager.getLogger(UniqueAttribQlistener.class);
  private final CacheService cache;
  private final QueueOptions options =
      new QueueOptions().setMaxInternalQueueSize(1000).setKeepMostRecent(true);
  RabbitMQClient client;

  public UniqueAttribQlistener(
      Vertx vertx, CacheService cache, RabbitMQOptions config, String vhost) {
    config.setVirtualHost(vhost);
    this.client = RabbitMQClient.create(vertx, config);
    this.cache = cache;
  }

  @Override
  public void start() {
    Future<Void> future = client.start();
    future.onComplete(
        startClienthandler -> {
          if (startClienthandler.succeeded()) {
            LOGGER.trace("starting Q listener for unique-attributes");
            client.basicConsumer(
                UNIQUE_ATTR_Q,
                options,
                rmqConsumer -> {
                  if (rmqConsumer.succeeded()) {
                    RabbitMQConsumer mqConsumer = rmqConsumer.result();
                    mqConsumer.handler(
                        message -> {
                          Buffer body = message.body();
                          if (body != null) {
                            JsonObject uniqueAttribJson = new JsonObject(body);
                            LOGGER.debug(
                                "received message from unique-attrib Q :" + uniqueAttribJson);
                            String value = uniqueAttribJson.getString("unique-attribute");
                            String eventType = uniqueAttribJson.getString("eventType");
                            BroadcastEventType event = BroadcastEventType.from(eventType);
                            LOGGER.debug(event);
                            JsonObject cacheJson = new JsonObject();
                            cacheJson.put("type", CacheType.UNIQUE_ATTRIBUTE);

                            if (event == null) {
                              LOGGER.error("Invalid BroadcastEventType [ null ] ");
                              return;
                            }

                            if (event.equals(BroadcastEventType.CREATE)) {
                              // pass key and value only for create event, for others update,delete
                              // cache will
                              // fetch from DB.
                              String key = uniqueAttribJson.getString("id");
                              cacheJson.put("key", key);
                              cacheJson.put("value", value);
                            }

                            Future<JsonObject> cacheFuture = cache.refresh(cacheJson);
                            cacheFuture
                                .onSuccess(
                                    successHandler -> {
                                      LOGGER.debug(
                                          "unique attrib message published to Cache Verticle");
                                    })
                                .onFailure(
                                    failureHandler -> {
                                      LOGGER.debug(
                                          "unique attrib message published to Cache Verticle fail");
                                    });

                          } else {
                            LOGGER.info("Empty json received from revoke_token queue");
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
