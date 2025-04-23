package org.cdpg.dx.database.postgres;
import static org.cdpg.dx.common.AddressConstants.PG_SERVICE_ADDRESS;

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
  private PoolOptions poolOptions;
  private PgPool pool;

  private String databaseIp;
  private int databasePort;
  private String databaseName;
  private String databaseUserName;
  private String databasePassword;
  private int poolSize;
  private PostgresService pgService;

  @Override
  public void start(Promise<Void> startPromise) {
    try {
      LOGGER.info("PostgresVerticle start() invoked.");
      System.out.println(">>> Postgres verticle started.");
      // Read configuration
      databaseIp = config().getString("databaseIP");
      databasePort = config().getInteger("databasePort");
      databaseName = config().getString("databaseName");
      databaseUserName = config().getString("databaseUserName");
      databasePassword = config().getString("databasePassword");
      poolSize = config().getInteger("poolSize");
      System.out.println(">>> Config >>>" +  config().toString());

      LOGGER.info("DATABASE CONFIG: IP={}, PORT={}, NAME={}, USER={}", databaseIp, databasePort, databaseName, databaseUserName);

      // Set up database connection
      PgConnectOptions connectOptions = new PgConnectOptions()
        .setPort(databasePort)
        .setHost(databaseIp)
        .setDatabase(databaseName)
        .setUser(databaseUserName)
        .setPassword(databasePassword)
        .setReconnectAttempts(2)
        .setReconnectInterval(1000L);

      // Set Schema Properties (if needed)
//      Map<String, String> schemaProp = Map.of("search_path", "your_schema");
//      connectOptions.setProperties(schemaProp);

      // Create connection pool
      this.poolOptions = new PoolOptions().setMaxSize(poolSize);
      this.pool = PgPool.pool(vertx, connectOptions, poolOptions);

      // Register Postgres Service on Event Bus
      pgService = new PostgresServiceImpl(this.pool);
      binder = new ServiceBinder(vertx);
      consumer = binder.setAddress(PG_SERVICE_ADDRESS).register(PostgresService.class, pgService);

      LOGGER.info("Postgres verticle started.");
      startPromise.complete();  // Signal success
    } catch (Exception e) {
      LOGGER.error("Failed to start Postgres verticle", e);
      startPromise.fail(e);  // Signal failure
    }

    pool.query("SELECT 1").execute(ar -> {
      if (ar.succeeded()) {
        System.out.println("Successfully connected to the database!");
      } else {
        System.out.println("Failed to connect to the database");
      }
    });
  }

  @Override
  public void stop() {
    if (binder != null && consumer != null) {
      binder.unregister(consumer);
    }
    if (pool != null) {
      pool.close();
    }
    LOGGER.info("Postgres verticle stopped.");
  }
}
