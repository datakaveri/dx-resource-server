package iudx.resource.server.databroker.listeners;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.QueueOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQConsumer;
import io.vertx.rabbitmq.RabbitMQOptions;
import iudx.resource.server.cache.service.CacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.resource.server.databroker.util.Constants.TOKEN_INVALID_Q;

public class RevokeClientQlistener implements RmqListeners
{
    private static final Logger LOGGER = LogManager.getLogger(RevokeClientQlistener.class);
    private final CacheService cache;
    private final QueueOptions options =
            new QueueOptions().setMaxInternalQueueSize(1000).setKeepMostRecent(true);
    RabbitMQClient client;
    public RevokeClientQlistener(
            Vertx vertx, CacheService cache, RabbitMQOptions config, String vhost) {
        config.setVirtualHost(vhost);
        this.client = RabbitMQClient.create(vertx, config);
        this.cache = cache;
    }

  @Override
  public void start() {
    Future<Void> future = client.start();
    future.onComplete(
        clientStarthandler -> {
          if (clientStarthandler.succeeded()) {
            LOGGER.trace("starting Q listener for revoked clients");
            client.basicConsumer(
                TOKEN_INVALID_Q,
                options,
                rmqConsumer -> {
                  if (rmqConsumer.succeeded()) {
                    RabbitMQConsumer mqConsumer = rmqConsumer.result();
                    mqConsumer.handler(
                        message -> {
                          Buffer body = message.body();
                          if (body != null) {
                            JsonObject invalidClientJson = new JsonObject(body);
                            LOGGER.debug(
                                "received message from revoked-client Q :" + invalidClientJson);
                            String key = invalidClientJson.getString("sub");
                            LOGGER.info("message received from RMQ : " + invalidClientJson);
                            JsonObject cacheJson = new JsonObject();
                            String value = invalidClientJson.getString("expiry");
                            cacheJson.put("type", "CacheType.REVOKED_CLIENT");
                            cacheJson.put("key", key);
                            cacheJson.put("value", value);

                            Future<JsonObject> cacheFuture = cache.refresh(cacheJson);
                            cacheFuture
                                .onSuccess(
                                    successHandler -> {
                                      LOGGER.debug(
                                          "revoked client message published to Cache Verticle");
                                    })
                                .onFailure(
                                    failureHandler -> {
                                      LOGGER.debug(
                                          "revoked client message "
                                              + "published to Cache Verticle fail");
                                    });
                          } else {
                            LOGGER.error("Empty json received from revoke_token queue");
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
