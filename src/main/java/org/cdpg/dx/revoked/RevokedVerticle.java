package org.cdpg.dx.revoked;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.revoked.client.RevokedClient;
import org.cdpg.dx.revoked.client.RevokedClientImpl;
import org.cdpg.dx.revoked.service.RevokedService;
import org.cdpg.dx.revoked.service.RevokedServiceImpl;

public class RevokedVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LogManager.getLogger(RevokedVerticle.class);
    private RevokedClient revokedClient;
    private RevokedService revokedService;
    private PostgresService postgresService;
    private MessageConsumer<JsonObject> consumer;
    private ServiceBinder binder;
    @Override
    public void start() throws Exception {
        postgresService = PostgresService.createProxy(vertx, "Postgress.address");
        revokedClient = new RevokedClientImpl(postgresService);
        revokedService = new RevokedServiceImpl(vertx,revokedClient);
        consumer =
                binder
                        .setAddress("REVOKED_ADDRESS")
                        .register(RevokedService.class, revokedService);
        LOGGER.info("Revoked Verticle deployed.");
    }

    @Override
    public void stop() throws Exception {
        binder.unregister(consumer);
    }
}
