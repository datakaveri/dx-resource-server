package org.cdpg.dx.uniqueattribute;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.uniqueattribute.client.UniqueAttributeClient;
import org.cdpg.dx.uniqueattribute.client.UniqueAttributeClientImpl;
import org.cdpg.dx.uniqueattribute.service.UniqueAttributeService;
import org.cdpg.dx.uniqueattribute.service.UniqueAttributeServiceImpl;

import static org.cdpg.dx.common.AddressConstants.PG_SERVICE_ADDRESS;
import static org.cdpg.dx.common.AddressConstants.UNIQUE_ATTRIBUTE_SERVICE_ADDRESS;

public class UniqueAttributeVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(UniqueAttributeVerticle.class);
  private UniqueAttributeClient uniqueAttributeClient;
  private UniqueAttributeService uniqueAttributeService;
  private PostgresService postgresService;
  private MessageConsumer<JsonObject> consumer;
  private ServiceBinder binder;

  @Override
  public void start() throws Exception {
    postgresService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);
    uniqueAttributeClient = new UniqueAttributeClientImpl(postgresService);
    uniqueAttributeService = new UniqueAttributeServiceImpl(vertx,uniqueAttributeClient);

    consumer =
        binder
            .setAddress(UNIQUE_ATTRIBUTE_SERVICE_ADDRESS)
            .register(UniqueAttributeService.class, uniqueAttributeService);
    LOGGER.info("UniqueAttribute Verticle deployed.");
  }

  @Override
  public void stop() throws Exception {
    binder.unregister(consumer);
  }
}
