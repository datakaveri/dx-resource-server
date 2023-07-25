package iudx.resource.server.database.archives;

import static iudx.resource.server.common.Constants.CACHE_SERVICE_ADDRESS;
import static iudx.resource.server.common.Constants.DATABASE_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.database.elastic.ElasticClient;

/**
 * The Database Verticle.
 *
 * <h1>Database Verticle</h1>
 *
 * <p>The Database Verticle implementation in the the IUDX Resource Server exposes the {@link
 * iudx.resource.server.database.archives.DatabaseService} over the Vert.x Event Bus.
 *
 * @version 1.0
 * @since 2020-05-31
 */
public class DatabaseVerticle extends AbstractVerticle {

  private DatabaseService database;
  private ElasticClient client;
  private String databaseIp;
  private String user;
  private String password;
  private String timeLimit;
  private int databasePort;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;
  private String tenantPrefix;
  private CacheService cacheService;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   *
   * @throws Exception which is a start up exception.
   */
  @Override
  public void start() throws Exception {

    databaseIp = config().getString("databaseIP");
    databasePort = config().getInteger("databasePort");
    user = config().getString("dbUser");
    password = config().getString("dbPassword");
    timeLimit = config().getString("timeLimit");
    tenantPrefix = config().getString("tenantPrefix");
    cacheService = CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);
    client = new ElasticClient(databaseIp, databasePort, user, password);
    binder = new ServiceBinder(vertx);
    database = new DatabaseServiceImpl(client, timeLimit, tenantPrefix, cacheService);

    consumer =
        binder.setAddress(DATABASE_SERVICE_ADDRESS).register(DatabaseService.class, database);
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
