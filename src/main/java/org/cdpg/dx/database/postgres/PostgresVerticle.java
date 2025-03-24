package org.cdpg.dx.database.postgres;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.database.postgres.service.PostgresServiceImpl;

public class PostgresVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LogManager.getLogger(PostgresVerticle.class);
    private MessageConsumer<JsonObject> consumer;
    private ServiceBinder binder;
    private PgPool pgPool;

    @Override
    public void start(Promise<Void> startPromise) {
        JsonObject config = config().getJsonObject("postgres");

        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(config.getInteger("port"))
                .setHost(config.getString("host"))
                .setDatabase(config.getString("database"))
                .setUser(config.getString("user"))
                .setPassword(config.getString("password"));

        PoolOptions poolOptions = new PoolOptions().setMaxSize(config.getInteger("poolSize", 5));
        pgPool = PgPool.pool(vertx, connectOptions, poolOptions);

        PostgresService service = new PostgresServiceImpl(pgPool);
        binder = new ServiceBinder(vertx);
        consumer = binder.setAddress("postgres.service").register(PostgresService.class, service);

        LOGGER.info("Postgres verticle started.");
        startPromise.complete();
    }

    @Override
    public void stop() {
        binder.unregister(consumer);
        pgPool.close();
    }
}