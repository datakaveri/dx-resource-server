package org.cdpg.dx.database.postgres;

import static org.cdpg.dx.common.config.ServiceProxyAddressConstants.POSTGRES_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
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
  private PgPool pool;
  private PostgresService pgService;

  @Override
  public void start() {
    try {
      LOGGER.info("Starting PostgresVerticle...");
      validateConfig(config());

      PgConnectOptions connectOptions = createConnectOptions(config());
      PoolOptions poolOptions = new PoolOptions().setMaxSize(config().getInteger("poolSize"));

      this.pool = PgPool.pool(vertx, connectOptions, poolOptions);

      this.pgService = new PostgresServiceImpl(this.pool);
      this.binder = new ServiceBinder(vertx);
      this.consumer =
          binder.setAddress(POSTGRES_SERVICE_ADDRESS).register(PostgresService.class, pgService);

      LOGGER.info("PostgresVerticle started successfully.");
    } catch (Exception e) {
      LOGGER.error("Failed to start PostgresVerticle: {}", e.getMessage(), e);
    }
  }

  @Override
  public void stop() {
    if (binder != null && consumer != null) {
      binder.unregister(consumer);
    }
    if (pool != null) {
      pool.close();
    }
    LOGGER.info("PostgresVerticle stopped.");
  }

  private PgConnectOptions createConnectOptions(JsonObject config) {
    return new PgConnectOptions()
        .setPort(config.getInteger("databasePort"))
        .setHost(config.getString("databaseIP"))
        .setDatabase(config.getString("databaseName"))
        .setUser(config.getString("databaseUserName"))
        .setPassword(config.getString("databasePassword"))
        .setReconnectAttempts(2)
        .setReconnectInterval(1000L);
  }

  private void validateConfig(JsonObject config) {
    validateField(config.getString("databaseIP"), "databaseIP");
    validateField(config.getInteger("databasePort"), "databasePort");
    validateField(config.getString("databaseName"), "databaseName");
    validateField(config.getString("databaseUserName"), "databaseUserName");
    validateField(config.getString("databasePassword"), "databasePassword");
    validateField(config.getInteger("poolSize"), "poolSize");
  }

  private <T> void validateField(T value, String fieldName) {
    if (value == null
        || (value instanceof String && ((String) value).isEmpty())
        || (value instanceof Integer && (Integer) value <= 0)) {
      throw new IllegalArgumentException("Missing or invalid '" + fieldName + "' configuration");
    }
  }
}
