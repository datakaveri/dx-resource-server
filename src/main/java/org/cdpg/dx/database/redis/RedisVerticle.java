package org.cdpg.dx.database.redis;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.RedisOptions;
import io.vertx.serviceproxy.ServiceBinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.database.redis.service.RedisService;
import org.cdpg.dx.database.redis.service.RedisServiceImpl;

import static org.cdpg.dx.common.config.ServiceProxyAddressConstants.REDIS_SERVICE_ADDRESS;


public class RedisVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LogManager.getLogger(RedisVerticle.class);

    private MessageConsumer<JsonObject> consumer;
    private ServiceBinder binder;
    private RedisService redisService;
    private JsonObject config;

    @Override
    public void start() {
        LOGGER.info("Starting Redis verticle...");

        config = config();  // Load configuration

        String redisUri = buildRedisUri();
        LOGGER.info("Connecting to Redis with URI: {}", redisUri);

        RedisOptions options = new RedisOptions().setConnectionString(redisUri);
        redisService = new RedisServiceImpl(vertx, options);

        binder = new ServiceBinder(vertx);
        consumer = binder.setAddress(REDIS_SERVICE_ADDRESS).register(RedisService.class, redisService);

        LOGGER.info("Redis verticle started.");
    }

    @Override
    public void stop() {
        if (consumer != null) {
            binder.unregister(consumer);
            LOGGER.info("Redis verticle stopped.");
        }
    }

    private String buildRedisUri() {
        return new StringBuilder()
                .append("redis://")
                .append(config.getString("redisUsername"))
                .append(":")
                .append(config.getString("redisPassword"))
                .append("@")
                .append(config.getString("redisHost"))
                .append(":")
                .append(config.getInteger("redisPort"))
                .toString();
    }
}
