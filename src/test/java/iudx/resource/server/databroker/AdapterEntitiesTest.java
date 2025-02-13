package iudx.resource.server.databroker;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.sqlclient.PoolOptions;
import iudx.resource.server.common.Vhosts;
import iudx.resource.server.configuration.Configuration;
import org.mockito.Mock;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdapterEntitiesTest {
/*

  static DataBrokerService databroker;
  static private Properties properties;
  static private InputStream inputstream;
  static private String dataBrokerIP;
  static private int dataBrokerPort;
  static private int dataBrokerManagementPort;
  static private String dataBrokerVhost;
  static private String dataBrokerUserName;
  static private String dataBrokerPassword;
  static private int connectionTimeout;
  static private int requestedHeartbeat;
  static private int handshakeTimeout;
  static private int requestedChannelMax;
  static private int networkRecoveryInterval;
  static private WebClient webClient;
  static private WebClientOptions webConfig;
  private static RabbitMQOptions config;
  private static RabbitMQClient client;
  private static String exchangeName;
  static JsonObject propObj;
  private static String resourceGroup;
  private static String resourceServer;
  private static String consumer;
  private static String provider;

  private static String id, anotherid, userName2Delete;
  private static String anotherProvider;
  */
/* Database Properties *//*

  private static String databaseIP;
  private static int databasePort;
  private static String databaseName;
  private static String databaseUserName;
  private static String databasePassword;
  private static int poolSize;
  private static PgConnectOptions connectOptions;
  private static PoolOptions poolOptions;
  private static PgPool pgclient;
  private static RabbitClient rabbitMQStreamingClient;
  private static RabbitWebClient rabbitMQWebClient;
  private static PostgresClient pgClient;
  private static Configuration appConfig;
  @Mock static CacheService cacheService;
  @Mock
  static RabbitMQClient iudxRabbitMQClient;

  private static final Logger LOGGER = LogManager.getLogger(AdapterEntitiesTest.class);

  @BeforeAll
  @DisplayName("Initialize the Databroker class with web client and rabbitmq client")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {

    */
/* Read the configuration and set the rabbitMQ server properties. *//*

    appConfig = new Configuration();
    JsonObject brokerConfig = appConfig.configLoader(2, vertx);

    try {



      dataBrokerIP = brokerConfig.getString("dataBrokerIP");
      dataBrokerPort = brokerConfig.getInteger("dataBrokerPort");
      dataBrokerManagementPort =
              brokerConfig.getInteger("dataBrokerManagementPort");
      dataBrokerVhost = brokerConfig.getString("dataBrokerVhost");
      dataBrokerUserName = brokerConfig.getString("dataBrokerUserName");
      dataBrokerPassword = brokerConfig.getString("dataBrokerPassword");
      connectionTimeout = brokerConfig.getInteger("connectionTimeout");
      requestedHeartbeat = brokerConfig.getInteger("requestedHeartbeat");
      handshakeTimeout = brokerConfig.getInteger("handshakeTimeout");
      requestedChannelMax = brokerConfig.getInteger("requestedChannelMax");
      networkRecoveryInterval = brokerConfig.getInteger("networkRecoveryInterval");
      databaseIP = brokerConfig.getString("postgresDatabaseIP");
      databasePort = brokerConfig.getInteger("postgresDatabasePort");
      databaseName = brokerConfig.getString("postgresDatabaseName");
      databaseUserName = brokerConfig.getString("postgresDatabaseUserName");
      databasePassword = brokerConfig.getString("postgresDatabasePassword");
      poolSize = brokerConfig.getInteger("postgrespoolSize");

    } catch (Exception ex) {
      LOGGER.info(ex.toString());
    }

    */
/* Configure the RabbitMQ Data Broker client with input from config files. *//*


    config = new RabbitMQOptions();
    config.setUser(dataBrokerUserName);
    config.setPassword(dataBrokerPassword);
    config.setHost(dataBrokerIP);
    config.setPort(dataBrokerPort);
    config.setVirtualHost(dataBrokerVhost);
    config.setConnectionTimeout(connectionTimeout);
    config.setRequestedHeartbeat(requestedHeartbeat);
    config.setHandshakeTimeout(handshakeTimeout);
    config.setRequestedChannelMax(requestedChannelMax);
    config.setNetworkRecoveryInterval(networkRecoveryInterval);
    config.setAutomaticRecoveryEnabled(true);


    webConfig = new WebClientOptions();
    webConfig.setKeepAlive(true);
    webConfig.setConnectTimeout(86400000);
    webConfig.setDefaultHost(dataBrokerIP);
    webConfig.setDefaultPort(dataBrokerManagementPort);
    webConfig.setKeepAliveTimeout(86400000);
    RabbitMQOptions iudxConfig = new RabbitMQOptions(config);
    String prodVhost = "IUDX";
    iudxConfig.setVirtualHost(prodVhost);

    */
/* Create a RabbitMQ Clinet with the configuration and vertx cluster instance. *//*

    client = RabbitMQClient.create(vertx, config);
    */
/* Create a Vertx Web Client with the configuration and vertx cluster instance. *//*


    webClient = WebClient.create(vertx, webConfig);

    */
/* Set Connection Object *//*

    if (connectOptions == null) {
      connectOptions = new PgConnectOptions().setPort(databasePort).setHost(databaseIP)
              .setDatabase(databaseName).setUser(databaseUserName).setPassword(databasePassword);
    }

    */
/* Pool options *//*

    if (poolOptions == null) {
      poolOptions = new PoolOptions().setMaxSize(poolSize);
    }

    */
/* Create the client pool *//*

    pgclient = PgPool.pool(vertx, connectOptions, poolOptions);

    */
/* Create a Json Object for properties *//*

    propObj = new JsonObject();
    propObj.put("userName", dataBrokerUserName);
    propObj.put(Constants.PASSWORD, dataBrokerPassword);
    propObj.put(Constants.VHOST, dataBrokerVhost);
    propObj.put("databaseIP", databaseIP);
    propObj.put("databasePort", databasePort);
    propObj.put("databaseName", databaseName);
    propObj.put("databaseUserName", databaseUserName);
    propObj.put("databasePassword", databasePassword);
    propObj.put("databasePoolSize", poolSize);

    */
/* Call the databroker constructor with the RabbitMQ client Vertx web client. *//*


    rabbitMQWebClient = new RabbitWebClient(vertx, webConfig, propObj);
    pgClient = new PostgresClient(vertx, connectOptions, poolOptions);
    rabbitMQStreamingClient = new RabbitClient(vertx, config, rabbitMQWebClient, pgClient, brokerConfig);
    databroker = new DataBrokerServiceImpl(rabbitMQStreamingClient, pgClient, brokerConfig,cacheService, */
/*iudxConfig, vertx,*//*
 iudxRabbitMQClient);

    resourceGroup = brokerConfig.getString("testResourceGroup");
    resourceServer = brokerConfig.getString("testResourceServer");
    consumer = UUID.randomUUID().toString();
    provider = UUID.randomUUID().toString();
    testContext.completeNow();

  }

  @Test
  @DisplayName("Testing registerAdaptor method with a new adaptor (adaptor-1) registration (with new user)")
  @Order(1)
  void successRegisterAdaptor(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request.put(Constants.ENTITIES, new JsonArray().add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information_1"));
    request.put(Constants.USER_ID, consumer);
    request.put(JSON_PROVIDER, provider);

    JsonObject expected = new JsonObject();
    expected.put(Constants.USER_NAME, Constants.USER_NAME_TEST_EXAMPLE);
    expected.put(Constants.APIKEY, Constants.APIKEY_TEST_EXAMPLE);
    expected.put(Constants.ID, id);
    expected.put(Constants.VHOST, Constants.VHOST_IUDX);

    String vhost= Vhosts.IUDX_PROD.name();

    databroker.registerAdaptor(request,vhost, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("inside  successRegisterAdaptor - RegisteAdaptor response is : " + response);
        assertTrue(response.containsKey(USER_NAME));
        assertTrue(response.containsKey(APIKEY));
        assertTrue(response.containsKey(URL));
        assertTrue(response.containsKey(PORT));
        assertTrue(response.containsKey(VHOST));

        id = response.getString(Constants.ID);
        userName2Delete = response.getString(USER_NAME);
        // assertEquals(expected, response);
      }
      testContext.completeNow();
    });
    testContext.completeNow();
  }

  @Test
  @DisplayName("Testing getExchange method for exchange created after new adaptor (adaptor-1) registration")
  @Order(2)
  void successGetExchange(VertxTestContext testContext) throws InterruptedException {
    JsonObject request = new JsonObject();
//    request.put(Constants.ID, id);
    request.put(Constants.ID,"id");
    JsonObject expected = new JsonObject();
    expected.put(Constants.TYPE, 200);
    expected.put(Constants.TITLE, Constants.SUCCESS);
    expected.put(Constants.DETAIL, Constants.EXCHANGE_FOUND);
    LOGGER.debug(id);
    String vhost= Vhosts.IUDX_PROD.name();
    databroker.getExchange(request,vhost, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("inside  successGetExchange - getExchange response : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
    testContext.completeNow();

  }

  @Test
  @DisplayName("Testing listAdaptor method for bindings done after new adaptor (adaptor-1) registration")
  @Order(3)
  void successListAdaptor(VertxTestContext testContext) throws InterruptedException {
    Thread.sleep(1000);
    JsonObject request = new JsonObject();
//    request.put(Constants.ID, id);
    request.put(Constants.ID,"id");
    request.put("exchangeName", id);// TODO : discuss conflict between impl and test code
    LOGGER.debug("request: " + request);
    JsonObject expected = new JsonObject();
    JsonArray adaptorLogs_entities = new JsonArray();
    JsonArray database_entities = new JsonArray();
    adaptorLogs_entities.add(id + Constants.DATA_ISSUE);
    adaptorLogs_entities.add(id + Constants.DOWNSTREAM_ISSUE);
    adaptorLogs_entities.add(id + Constants.HEARTBEAT);
    database_entities.add(id + Constants.ALLOW_ROUTING_KEY);
    expected.put(Constants.QUEUE_ADAPTOR_LOGS, adaptorLogs_entities);
    expected.put(Constants.QUEUE_DATA, database_entities);
    expected.put(Constants.REDIS_LATEST, database_entities);

    String vhost= Vhosts.IUDX_PROD.name();
    databroker.listAdaptor(request,vhost, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("inside test - listAdaptor response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
    testContext.completeNow();

  }

  @Test
  @DisplayName("Testing publishHeartbeat method for publication of heartbeat data")
  @Order(4)
  void successPublishHeartbeat(VertxTestContext testContext) throws InterruptedException {
    JsonObject request = new JsonObject();
    request.put(Constants.ID, id);
    request.put(Constants.STATUS, "heartbeat");

    JsonObject expected = new JsonObject();
    expected.put(Constants.TYPE, Constants.SUCCESS);
    expected.put(Constants.QUEUE_NAME, Constants.QUEUE_ADAPTOR_LOGS);
    expected.put(Constants.ROUTING_KEY, id + ".heartbeat");
    expected.put(Constants.DETAIL, "routingKey matched");

    String vhost= Vhosts.IUDX_PROD.name();
    databroker.publishHeartbeat(request,vhost, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("inside test - publishHeartbeat response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
    testContext.completeNow();

  }

  @Test
  @DisplayName("Testing registerAdaptor method for another adaptor (adaptor-2) registration (with an existing user)")
  @Order(5)
  void successRegisterAdaptorwithExistingUser(VertxTestContext testContext)
          throws InterruptedException {
    anotherProvider = Constants.USER_NAME_TEST_EXAMPLE;

    JsonObject request = new JsonObject();
//    request.put(Constants.JSON_RESOURCE_GROUP, resourceGroup);
//    request.put(Constants.JSON_RESOURCE_SERVER, resourceServer);
    request.put(Constants.ENTITIES, new JsonArray().add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information1"));
    request.put(Constants.USER_ID, consumer);
    request.put(JSON_PROVIDER, anotherProvider);

    JsonObject expected = new JsonObject();
    expected.put(Constants.USER_NAME, Constants.USER_NAME_TEST_EXAMPLE);
    expected.put(Constants.APIKEY, Constants.APIKEY_TEST_EXAMPLE);
    expected.put(Constants.ID, anotherid);
    expected.put(Constants.VHOST, Constants.VHOST_IUDX);
    String vhost= Vhosts.IUDX_PROD.name();
    databroker.registerAdaptor(request,vhost, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        anotherid = response.getString(Constants.ID);
        LOGGER.debug(
                "inside  successRegisterAdaptor with existing user - RegisteAdaptor response is : "
                        + response);
        assertTrue(response.containsKey(USER_NAME));
        assertTrue(response.containsKey(APIKEY));
        assertTrue(response.containsKey(URL));
        assertTrue(response.containsKey(PORT));
        assertTrue(response.containsKey(VHOST));
      }
      testContext.completeNow();
    });
    testContext.completeNow();

  }

  @Test
  @DisplayName("Testing getExchange method for exchanges created after another adaptor (adaptor-2) registration")
  @Order(6)
  void successGetExchangeExistingUser(VertxTestContext testContext) throws InterruptedException {
    JsonObject request = new JsonObject();
//    LOGGER.debug("Exchange name :"+anotherid);
//    request.put(Constants.ID, anotherid);
    request.put(Constants.ID,"id");
    JsonObject expected = new JsonObject();
    expected.put(Constants.TYPE, 200);
    expected.put(Constants.TITLE, Constants.SUCCESS);
    expected.put(Constants.DETAIL, Constants.EXCHANGE_FOUND);
    String vhost= Vhosts.IUDX_PROD.name();
    databroker.getExchange(request,vhost, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("inside  successGetExchange - getExchange response : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
    testContext.completeNow();

  }

  @Test
  @DisplayName("Testing listAdaptor method for bindings done after another adaptor (adaptor-2) registration")
  @Order(7)
  void successListAdaptorExistingUser(VertxTestContext testContext) throws InterruptedException {
    Thread.sleep(1000);
    JsonObject request = new JsonObject();
//    request.put(Constants.ID, anotherid);
    request.put(Constants.ID,"id");
//    request.put("exchangeName", anotherid);// TODO : discuss conflict between impl and test code
    request.put("exchangeName", "id");

    JsonObject expected = new JsonObject();
    JsonArray adaptorLogs_entities = new JsonArray();
    JsonArray database_entities = new JsonArray();
    adaptorLogs_entities.add(anotherid + Constants.DATA_ISSUE);
    adaptorLogs_entities.add(anotherid + Constants.DOWNSTREAM_ISSUE);
    adaptorLogs_entities.add(anotherid + Constants.HEARTBEAT);
    database_entities.add(anotherid + Constants.ALLOW_ROUTING_KEY);
    expected.put(Constants.QUEUE_ADAPTOR_LOGS, adaptorLogs_entities);
    expected.put(Constants.QUEUE_DATA, database_entities);
    expected.put(Constants.REDIS_LATEST, database_entities);
    String vhost= Vhosts.IUDX_PROD.name();
    databroker.listAdaptor(request,vhost, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("inside test - listAdaptor response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
    testContext.completeNow();

  }

  @Test
  @DisplayName("Testing registerAdaptor method for registering an adaptor (adaptor-2) which was already registered")
  @Order(8)
  void failureRegisterAdaptorwithExistingUser(VertxTestContext testContext)
          throws InterruptedException {
    Thread.sleep(1000);
    JsonObject request = new JsonObject();
//    request.put(Constants.JSON_RESOURCE_GROUP, resourceGroup);
//    request.put(Constants.JSON_RESOURCE_SERVER, resourceServer);
    request.put(Constants.ENTITIES, new JsonArray().add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information"));
    request.put(Constants.USER_ID, consumer);
    request.put(JSON_PROVIDER, provider);

    JsonObject expected = new JsonObject();
    expected.put(Constants.DETAILS, Constants.EXCHANGE_EXISTS);
    String vhost= Vhosts.IUDX_PROD.name();
    databroker.registerAdaptor(request,vhost, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("inside  failureRegisterAdaptorwithExistingUser - RegisteAdaptor response is : "
                + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
    testContext.completeNow();

  }

  @Test
  @DisplayName("Testing deleteAdaptor method for deleting new (adaptor-1) adaptor")
  @Order(9)
  void successDeleteAdaptor(VertxTestContext testContext) throws InterruptedException {
    JsonObject expected = new JsonObject();
    expected.put(Constants.ID, id);
    expected.put(Constants.TYPE, 200);
    expected.put(Constants.TITLE, Constants.SUCCESS);
    expected.put(Constants.DETAIL, "adaptor deleted");

    JsonObject request = new JsonObject();
//    request.put(Constants.ID, id);
    request.put(Constants.ID,"id");
    String vhost= Vhosts.IUDX_PROD.name();
    databroker.deleteAdaptor(request,vhost, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("inside test - DeleteAdaptor response is : " + response);
        assertEquals(ResponseType.Ok.getCode(), handler.result().getInteger(Constants.TYPE));
        testContext.completeNow();
      } else {
//        testContext.failNow(handler.cause());
        assertEquals("{\"type\":500,\"title\":\"bad request\",\"detail\":\"nothing to delete\"}",handler.cause().getMessage());
        testContext.completeNow();
      }
    });
    testContext.completeNow();

  }

  @Test
  @DisplayName("Testing deleteAdaptor method for deleting another (adaptor-2) adaptor")
  @Order(10)
  void successDeleteAdaptorExistingUser(VertxTestContext testContext) throws InterruptedException {
    JsonObject expected = new JsonObject();
    expected.put(Constants.ID, anotherid);
    expected.put(Constants.TYPE, 200);
    expected.put(Constants.TITLE, Constants.SUCCESS);
    expected.put(Constants.DETAIL, "adaptor deleted");

    JsonObject request = new JsonObject();
//    request.put(Constants.ID, anotherid);
    request.put(Constants.ID,"id");
    String vhost= Vhosts.IUDX_PROD.name();
    databroker.deleteAdaptor(request,vhost, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("inside test - DeleteAdaptor response is : " + response);
        assertEquals(200, response.getInteger(Constants.TYPE));
      }
      testContext.completeNow();
    });
    testContext.completeNow();

  }

  @Test
  @DisplayName("Testing registerAdaptor with invalid ID")
  @Order(11)
  void failureRegisterInvalidAdaptorID(VertxTestContext testContext) throws InterruptedException {
    JsonObject request = new JsonObject();
//    request.put(Constants.JSON_RESOURCE_GROUP, resourceGroup + "+()*&");
//    request.put(Constants.JSON_RESOURCE_SERVER, resourceServer);
    request.put(Constants.ENTITIES, new JsonArray().add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information()*&"));
    request.put(Constants.CONSUMER, Constants.CONSUMER_TEST_EXAMPLE);

    JsonObject expected = new JsonObject();
    expected.put(Constants.ERROR, "invalid id");
    String vhost= Vhosts.IUDX_PROD.name();
    databroker.registerAdaptor(request,vhost, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("inside invalid ID test - RegisteAdaptor response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
    testContext.completeNow();

  }

  @Test
  @Order(11)
  static void cleanUp(VertxTestContext testContext) {

    if (userName2Delete != null) {
      LOGGER.debug("cleanup : delete user "+userName2Delete);
      String url = "/api/users/bulk-delete";// + Util.encodeValue(userName2Delete);
      JsonObject request=new JsonObject();
      request.put("users", new JsonArray().add(userName2Delete));

      rabbitMQWebClient.requestAsync(Constants.REQUEST_DELETE, url,request).onComplete(handler -> {
        if (handler.succeeded()) {
          LOGGER.debug(userName2Delete + " deleted");
        } else {
          LOGGER.error(handler.cause());
          LOGGER.debug(userName2Delete + " deletion failed");
        }
        testContext.completeNow();
      });
      testContext.completeNow();

    }

  }

  @Test
  public void ResponseTypeTestFromCode(VertxTestContext vertxTestContext){
    assertNull(ResponseType.fromCode(1));
    vertxTestContext.completeNow();
  }
*/
}


