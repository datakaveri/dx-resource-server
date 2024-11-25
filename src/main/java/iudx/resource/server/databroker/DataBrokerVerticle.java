package iudx.resource.server.databroker;

import static iudx.resource.server.common.Constants.ASYNC_SERVICE_ADDRESS;
import static iudx.resource.server.common.Constants.BROKER_SERVICE_ADDRESS;
import static iudx.resource.server.common.Constants.CACHE_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.common.Vhosts;
import iudx.resource.server.database.async.AsyncService;
import iudx.resource.server.databroker.listeners.AsyncQueryListener;
import iudx.resource.server.databroker.listeners.RevokeClientQlistener;
import iudx.resource.server.databroker.listeners.RmqListeners;
import iudx.resource.server.databroker.listeners.UniqueAttribQlistener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Data Broker Verticle.
 *
 * <h1>Data Broker Verticle</h1>
 *
 * <p>The Data Broker Verticle implementation in the IUDX Resource Server exposes the {@link
 * iudx.resource.server.databroker.DataBrokerService} over the Vert.x Event Bus.
 *
 * @version 1.0
 * @since 2020-05-31
 */
public class DataBrokerVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(DataBrokerVerticle.class);
  /*RabbitMQOptions iudxConfig;*/
  private DataBrokerService databroker;
  private RabbitMQOptions config;
  private String dataBrokerIp;
  private int dataBrokerPort;
  private int dataBrokerManagementPort;
  private String dataBrokerUserName;
  private String dataBrokerPassword;
  private int connectionTimeout;
  private int requestedHeartbeat;
  private int handshakeTimeout;
  private int requestedChannelMax;
  private int networkRecoveryInterval;
  private WebClientOptions webConfig;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;
  /* Database Properties */
  private String databaseIp;
  private int databasePort;
  private String databaseName;
  private String databaseUserName;
  private String databasePassword;
  private int poolSize;
  private PoolOptions poolOptions;
  private PgConnectOptions connectOptions;
  private RabbitClient rabbitClient;
  private RabbitWebClient rabbitWebClient;
  private PostgresClient pgClient;
  private CacheService cache;
  private AsyncService asyncService;
  private RabbitMQClient iudxRabbitMqClient;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   */
  @Override
  public void start() throws Exception {

    /* Read the configuration and set the rabbitMQ server properties. */
    dataBrokerIp = config().getString("dataBrokerIP");
    dataBrokerPort = config().getInteger("dataBrokerPort");
    dataBrokerManagementPort = config().getInteger("dataBrokerManagementPort");
    dataBrokerUserName = config().getString("dataBrokerUserName");
    dataBrokerPassword = config().getString("dataBrokerPassword");
    connectionTimeout = config().getInteger("connectionTimeout");
    requestedHeartbeat = config().getInteger("requestedHeartbeat");
    handshakeTimeout = config().getInteger("handshakeTimeout");
    requestedChannelMax = config().getInteger("requestedChannelMax");
    networkRecoveryInterval = config().getInteger("networkRecoveryInterval");
    databaseIp = config().getString("postgresDatabaseIP");
    databasePort = config().getInteger("postgresDatabasePort");
    databaseName = config().getString("postgresDatabaseName");
    databaseUserName = config().getString("postgresDatabaseUserName");
    databasePassword = config().getString("postgresDatabasePassword");
    poolSize = config().getInteger("postgrespoolSize");

    /* Configure the RabbitMQ Data Broker client with input from config files. */

    config = new RabbitMQOptions();
    config.setUser(dataBrokerUserName);
    config.setPassword(dataBrokerPassword);
    config.setHost(dataBrokerIp);
    config.setPort(dataBrokerPort);
    config.setConnectionTimeout(connectionTimeout);
    config.setRequestedHeartbeat(requestedHeartbeat);
    config.setHandshakeTimeout(handshakeTimeout);
    config.setRequestedChannelMax(requestedChannelMax);
    config.setNetworkRecoveryInterval(networkRecoveryInterval);
    config.setAutomaticRecoveryEnabled(true);

    RabbitMQOptions iudxConfig = new RabbitMQOptions(config);
    String prodVhost = config().getString(Vhosts.IUDX_PROD.value);

    iudxConfig.setVirtualHost(prodVhost);

    webConfig = new WebClientOptions();
    webConfig.setKeepAlive(true);
    webConfig.setConnectTimeout(86400000);
    webConfig.setDefaultHost(dataBrokerIp);
    webConfig.setDefaultPort(dataBrokerManagementPort);
    webConfig.setKeepAliveTimeout(86400000);

    /* Create a RabbitMQ Clinet with the configuration and vertx cluster instance. */

    RabbitMQClient.create(vertx, config);

    /* Create a Vertx Web Client with the configuration and vertx cluster instance. */

    WebClient.create(vertx, webConfig);

    /* Set Connection Object */
    if (connectOptions == null) {
      connectOptions =
          new PgConnectOptions()
              .setPort(databasePort)
              .setHost(databaseIp)
              .setDatabase(databaseName)
              .setUser(databaseUserName)
              .setPassword(databasePassword);
    }

    /* Pool options */
    if (poolOptions == null) {
      poolOptions = new PoolOptions().setMaxSize(poolSize);
    }

    /* Create the client pool */
    PgPool.pool(vertx, connectOptions, poolOptions);

    /* Create a Json Object for properties */

    JsonObject propObj = new JsonObject();

    propObj.put("userName", dataBrokerUserName);
    propObj.put("password", dataBrokerPassword);
    propObj.put("databaseIP", databaseIp);
    propObj.put("databasePort", databasePort);
    propObj.put("databaseName", databaseName);
    propObj.put("databaseUserName", databaseUserName);
    propObj.put("databasePassword", databasePassword);
    propObj.put("databasePoolSize", poolSize);

    /* Call the databroker constructor with the RabbitMQ client. */

    rabbitWebClient = new RabbitWebClient(vertx, webConfig, propObj);
    pgClient = new PostgresClient(vertx, connectOptions, poolOptions);
    rabbitClient = new RabbitClient(vertx, config, rabbitWebClient, pgClient, config());
    cache = CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);
    binder = new ServiceBinder(vertx);
    iudxRabbitMqClient = RabbitMQClient.create(vertx, iudxConfig);
    iudxRabbitMqClient
        .start()
        .onSuccess(
            iudxRabbitClientStart -> {
              LOGGER.info("RMQ client started for Prod Vhost");
            })
        .onFailure(
            iudxRabbitClientStart -> {
              LOGGER.fatal("RMQ client startup failed");
            });
    databroker =
        new DataBrokerServiceImpl(
            rabbitClient, pgClient, config(), cache, /*iudxConfig, vertx,*/ iudxRabbitMqClient);
    asyncService = AsyncService.createProxy(vertx, ASYNC_SERVICE_ADDRESS);

    String internalVhost = config().getString(Vhosts.IUDX_INTERNAL.value);
    RmqListeners revokeQlistener = new RevokeClientQlistener(vertx, cache, config, internalVhost);
    RmqListeners uniqueAttrQlistener =
        new UniqueAttribQlistener(vertx, cache, config, internalVhost);
    RmqListeners asyncQueryQlistener =
        new AsyncQueryListener(vertx, config, internalVhost, asyncService);

    // start
    revokeQlistener.start();
    uniqueAttrQlistener.start();
    asyncQueryQlistener.start();

    /* Publish the Data Broker service with the Event Bus against an address. */

    consumer =
        binder.setAddress(BROKER_SERVICE_ADDRESS).register(DataBrokerService.class, databroker);
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
