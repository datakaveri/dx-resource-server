package iudx.resource.server.database.async;

import static iudx.resource.server.common.Constants.*;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.database.async.util.S3FileOpsHelper;
import iudx.resource.server.database.elastic.ElasticClient;
import iudx.resource.server.database.postgres.PostgresService;

/**
 * The Async worker Verticle.
 *
 * <h1>Async Verticle</h1>
 *
 * <p>The Async Verticle implementation in the IUDX Resource Server exposes the {@link AsyncService}
 * over the Vert.x Event Bus.
 *
 * @version 1.0
 * @since 2022-02-08
 */
public class AsyncVerticle extends AbstractVerticle {

  private AsyncService asyncService;
  private ElasticClient client;
  private PostgresService pgService;
  private S3FileOpsHelper fileOpsHelper;
  private String databaseIp;
  private String user;
  private String password;
  private int databasePort;
  private String filePath;
  private String bucketName;
  private ServiceBinder binder;
  private CacheService cacheService;
  private MessageConsumer<JsonObject> consumer;
  private String tenantPrefix;
  private String clientRegion;
  private String endPoint;
  private String accessKey;
  private String accessSecret;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event Bus against an address, publishes the service with the service discovery
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
    filePath = config().getString("filePath");
    clientRegion = config().getString("clientRegion");
    bucketName = config().getString("bucketName");
    tenantPrefix = config().getString("tenantPrefix");
    endPoint = config().getString("endPoint", null);
    accessKey = config().getString("accessKey", null);
    accessSecret = config().getString("accessSecret", null);
    pgService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);
    cacheService = CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);
    client = new ElasticClient(databaseIp, databasePort, user, password);
    fileOpsHelper =
        new S3FileOpsHelper(endPoint, clientRegion, accessKey, accessSecret, bucketName);

    binder = new ServiceBinder(vertx);
    asyncService =
        new AsyncServiceImpl(
            vertx, client, pgService, fileOpsHelper, filePath, tenantPrefix, cacheService);

    consumer = binder.setAddress(ASYNC_SERVICE_ADDRESS).register(AsyncService.class, asyncService);
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
