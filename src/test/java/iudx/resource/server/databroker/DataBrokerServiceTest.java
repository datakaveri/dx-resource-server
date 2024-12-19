package iudx.resource.server.databroker;

import static iudx.resource.server.databroker.util.Constants.APIKEY;
import static iudx.resource.server.databroker.util.Constants.BAD_REQUEST_DATA;
import static iudx.resource.server.databroker.util.Constants.DETAIL;
import static iudx.resource.server.databroker.util.Constants.ENTITIES;
import static iudx.resource.server.databroker.util.Constants.ERROR;
import static iudx.resource.server.databroker.util.Constants.EXCHANGE;
import static iudx.resource.server.databroker.util.Constants.EXCHANGE_EXISTS;
import static iudx.resource.server.databroker.util.Constants.EXCHANGE_NAME;
import static iudx.resource.server.databroker.util.Constants.EXCHANGE_NOT_FOUND;
import static iudx.resource.server.databroker.util.Constants.FAILURE;
import static iudx.resource.server.databroker.util.Constants.ID;
import static iudx.resource.server.databroker.util.Constants.NAME;
import static iudx.resource.server.databroker.util.Constants.PASSWORD;
import static iudx.resource.server.databroker.util.Constants.PORT;
import static iudx.resource.server.databroker.util.Constants.QUEUE;
import static iudx.resource.server.databroker.util.Constants.QUEUE_ALREADY_EXISTS;
import static iudx.resource.server.databroker.util.Constants.QUEUE_DOES_NOT_EXISTS;
import static iudx.resource.server.databroker.util.Constants.QUEUE_NAME;
import static iudx.resource.server.databroker.util.Constants.SUBSCRIPTION_ID;
import static iudx.resource.server.databroker.util.Constants.TITLE;
import static iudx.resource.server.databroker.util.Constants.TYPE;
import static iudx.resource.server.databroker.util.Constants.URL;
import static iudx.resource.server.databroker.util.Constants.USER_ID;
import static iudx.resource.server.databroker.util.Constants.USER_NAME;
import static iudx.resource.server.databroker.util.Constants.VHOST;
import static iudx.resource.server.databroker.util.Constants.VHOST_IUDX;
import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

import iudx.resource.server.cache.CacheService;
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

@Disabled
@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DataBrokerServiceTest {


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
  private static String queueName;
  private static JsonArray entities;
  private static JsonObject expected;
  static JsonObject propObj;
  private static String vHost;
  private static int statusOk;
  private static int statusNotFound;
  private static int statusNoContent;
  /* Database Properties */
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
  private static int statusConflict;
  private static Configuration appConfig;
  private static String userid;
  private static String BROKER_PRODUCTION_DOMAIN;
  private static int BROKER_PRODUCTION_PORT;
  @Mock
  static
  CacheService cacheService;
  @Mock
  static RabbitMQClient iudxRabbitMQClient;

  private static final Logger LOGGER = LogManager.getLogger(DataBrokerServiceTest.class);

  @BeforeEach
  @DisplayName("Deploy a verticle")
  void startVertx(Vertx vertx, VertxTestContext testContext) {
    exchangeName = UUID.randomUUID().toString();
    queueName = UUID.randomUUID().toString();
    entities = new JsonArray("[\"id1\", \"id2\"]");
    vHost = "IUDX-Test";
    statusOk = 200;
    statusNotFound = 404;
    statusNoContent = 204;
    statusConflict = 409;

    appConfig = new Configuration();
    JsonObject brokerConfig = appConfig.configLoader(2, vertx);
    BROKER_PRODUCTION_DOMAIN = brokerConfig.getString("brokerAmqpIp");
    BROKER_PRODUCTION_PORT=brokerConfig.getInteger("brokerAmqpPort");

    BROKER_PRODUCTION_DOMAIN = brokerConfig.getString("brokerAmqpIp");
    BROKER_PRODUCTION_PORT=brokerConfig.getInteger("brokerAmqpPort");

    LOGGER.debug("Exchange Name is " + exchangeName);
    LOGGER.debug("Queue Name is " + queueName);

    /* Read the configuration and set the rabbitMQ server properties. */
    properties = new Properties();
    inputstream = null;

    try {

      /*
       * inputstream = new FileInputStream("config.properties"); properties.load(inputstream);
       */

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

      LOGGER.error(ex.toString());

    }

    /* Configure the RabbitMQ Data Broker client with input from config files. */

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

    /* Create a RabbitMQ Clinet with the configuration and vertx cluster instance. */

    client = RabbitMQClient.create(vertx, config);

    /* Create a Vertx Web Client with the configuration and vertx cluster instance. */

    webClient = WebClient.create(vertx, webConfig);

    /* Set Connection Object */
    if (connectOptions == null) {
      connectOptions = new PgConnectOptions().setPort(databasePort).setHost(databaseIP)
              .setDatabase(databaseName).setUser(databaseUserName).setPassword(databasePassword);
    }

    /* Pool options */
    if (poolOptions == null) {
      poolOptions = new PoolOptions().setMaxSize(poolSize);
    }

    /* Create the client pool */
    pgclient = PgPool.pool(vertx, connectOptions, poolOptions);

    /* Create a Json Object for properties */

    propObj = new JsonObject();

    propObj.put("userName", dataBrokerUserName);
    propObj.put(PASSWORD, dataBrokerPassword);
    propObj.put(VHOST, dataBrokerVhost);
    propObj.put("databaseIP", databaseIP);
    propObj.put("databasePort", databasePort);
    propObj.put("databaseName", databaseName);
    propObj.put("databaseUserName", databaseUserName);
    propObj.put("databasePassword", databasePassword);
    propObj.put("databasePoolSize", poolSize);

    /* Call the databroker constructor with the RabbitMQ client. */
    rabbitMQWebClient = new RabbitWebClient(vertx, webConfig, propObj);
    pgClient = new PostgresClient(vertx, connectOptions, poolOptions);
    rabbitMQStreamingClient = new RabbitClient(vertx, config, rabbitMQWebClient, pgClient, brokerConfig);
    databroker = new DataBrokerServiceImpl(rabbitMQStreamingClient, pgClient, brokerConfig,cacheService, /*iudxConfig, vertx,*/ iudxRabbitMQClient);

    userid = UUID.randomUUID().toString();

    testContext.completeNow();
  }


  @Test
  @DisplayName("Testing Create Exchange")
  @Order(1)
  void successCreateExchange(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(EXCHANGE, exchangeName);

    JsonObject request = new JsonObject();
    request.put(EXCHANGE_NAME, exchangeName);
    String vhost= Vhosts.IUDX_PROD.name();
    databroker.createExchange(request,vhost, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Create Exchange response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Creating already existing exchange")
  @Order(2)
  void failCreateExchange(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(TYPE, statusConflict);
    expected.put(TITLE, FAILURE);
    expected.put(DETAIL, EXCHANGE_EXISTS);

    JsonObject request = new JsonObject();
    request.put(EXCHANGE_NAME, exchangeName);
    String vhost= Vhosts.IUDX_PROD.name();
    databroker.createExchange(request,vhost, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Create Exchange response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing Create Queue")
  @Order(3)
  void successCreateQueue(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(QUEUE, queueName);

    JsonObject request = new JsonObject();
    request.put(QUEUE_NAME, queueName);
    String vhost= Vhosts.IUDX_PROD.name();
    databroker.createQueue(request,vhost, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Create Queue response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("Creating already existing queue")
  @Order(4)
  void failCreateQueue(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(TYPE, statusConflict);
    expected.put(TITLE, FAILURE);
    expected.put(DETAIL, QUEUE_ALREADY_EXISTS);

    JsonObject request = new JsonObject();
    request.put(QUEUE_NAME, queueName);
    String vhost= Vhosts.IUDX_PROD.name();
    databroker.createQueue(request,vhost, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Create Exchange response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Binding Exchange and Queue")
  @Order(5)
  void successBindQueue(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(QUEUE, queueName);
    expected.put(EXCHANGE, exchangeName);
    expected.put(ENTITIES, entities);

    JsonObject request = new JsonObject();
    request.put(QUEUE_NAME, queueName);
    request.put(EXCHANGE_NAME, exchangeName);
    request.put(ENTITIES, entities);
    String vhost= Vhosts.IUDX_PROD.name();
    databroker.bindQueue(request,vhost, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Bind Queue response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Listing all bindings of exchange")
  @Order(6)
  void successListExchangeBindings(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(queueName, entities);

    JsonObject request = new JsonObject();
    request.put(ID, exchangeName);
    String vhost= Vhosts.IUDX_PROD.name();
    databroker.listExchangeSubscribers(request,vhost, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("List exchnage bindings response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });

  }



  @Test
  @DisplayName("Listing all bindings of queue")
  @Order(7)
  void successListQueueBindings(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(ENTITIES, entities);

    JsonObject request = new JsonObject();
    request.put(QUEUE_NAME, queueName);
    String vhost= Vhosts.IUDX_PROD.name();
    databroker.listQueueSubscribers(request,vhost, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("List queue bindings response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("Unbinding Exchange and Queue")
  @Order(8)
  void successUnbindQueue(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(QUEUE, queueName);
    expected.put(EXCHANGE, exchangeName);
    expected.put(ENTITIES, entities);

    JsonObject request = new JsonObject();
    request.put(QUEUE_NAME, queueName);
    request.put(EXCHANGE_NAME, exchangeName);
    request.put(ENTITIES, entities);
    String vhost= Vhosts.IUDX_PROD.name();
    databroker.unbindQueue(request,vhost, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Unbind Queue response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("Deleting a Queue")
  @Order(9)
  void successDeleteQueue(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(QUEUE, queueName);

    JsonObject request = new JsonObject();
    request.put(QUEUE_NAME, queueName);
    String vhost= Vhosts.IUDX_PROD.name();
    databroker.deleteQueue(request,vhost, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Delete Queue response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("Deleting an already deleted Queue")
  @Order(10)
  void failDeleteQueue(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(TYPE, statusNotFound);
    expected.put(TITLE, FAILURE);
    expected.put(DETAIL, QUEUE_DOES_NOT_EXISTS);

    JsonObject request = new JsonObject();
    request.put(QUEUE_NAME, queueName);
    String vhost= Vhosts.IUDX_PROD.name();
    databroker.deleteQueue(request,vhost, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Delete Queue response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("Deleting an Exchange")
  @Order(11)
  void successDeleteExchange(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(EXCHANGE, exchangeName);

    JsonObject request = new JsonObject();
    request.put(EXCHANGE_NAME, exchangeName);
    String vhost= Vhosts.IUDX_PROD.name();
    databroker.deleteExchange(request,vhost, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Delete Exchange response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("Deleting an already deleted Exchange")
  @Order(12)
  void failDeleteExchange(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(TYPE, statusNotFound);
    expected.put(TITLE, FAILURE);
    expected.put(DETAIL, EXCHANGE_NOT_FOUND);

    JsonObject request = new JsonObject();
    request.put(EXCHANGE_NAME, exchangeName);
    String vhost= Vhosts.IUDX_PROD.name();
    databroker.deleteExchange(request,vhost, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Delete Exchange response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("Creating a Virtual Host")
  @Order(13)
  void successCreateVhost(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(VHOST, vHost);

    JsonObject request = new JsonObject();
    request.put(VHOST, vHost);

    databroker.createvHost(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Create vHost response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("Deleting a Virtual Host")
  @Order(14)
  void successDeleteVhost(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(VHOST, vHost);

    JsonObject request = new JsonObject();
    request.put(VHOST, vHost);

    databroker.deletevHost(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Delete vHost response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("List all Virtual Hosts")
  @Order(15)
  void successListVhosts(VertxTestContext testContext) {
    JsonObject request = new JsonObject();

    databroker.listvHost(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("List vHost response is : " + response);
        assertTrue(response.containsKey(VHOST));
        assertTrue(response.getJsonArray(VHOST).size() > 1);
      }
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("Publish message to exchange")
  @Order(16)
  void successPublishMessage(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put("status", statusOk);

    JsonObject request = new JsonObject();
    request.put("PM2_5", 4.14);
    request.put("AQI", 64);
    request.put("CO2", 529.23);
    request.put("CO", 0.75);
    request.put("SoundMax", 100.82);
    request.put("PM10", 4.83);
    request.put("Rainfall", 0);
    request.put("id",
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/example.com/aqm/EM_01_0103_01");
    request.put("SoundMin", 73.54);
    request.put("Avg_Humidity", 100);
    request.put("SO2", 17.95);
    request.put("AmbientLight", 32040.91);
    request.put("Pressure", 994.02);
    request.put("ULTRA_VIOLET", 1.67);
    request.put("Avg_Temp", 24.9);
    request.put("Date", "2020-06-05T09:00:01+05:30");
    request.put("O3", 34.59);
    request.put("O2", 19.66);
    request.put("NO2", 50.62);

    String vhost= Vhosts.IUDX_PROD.name();
    databroker.publishFromAdaptor(new JsonArray().add(request),vhost, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Message from adaptor response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("Testing failure case : Register streaming subscription with empty request")
  @Order(17)
  void failedregisterStreamingSubscriptionEmptyRequest(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    expected.put(ERROR, "Error in payload");

    JsonObject request = new JsonObject();
    databroker.registerStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Register subscription response for empty request is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing failure case : Register streaming subscription with non-existing (but valid) routingKey")
  @Order(18)
  void failedregisterStreamingSubscriptionInvalidExchange(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    expected.put(ERROR, "Binding Failed");

    JsonObject request = new JsonObject();
    request.put(NAME, "alias");
    request.put(USER_ID, userid);
    request.put(TYPE, "streaming");
    JsonArray array = new JsonArray();
    array.add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information_123");
    request.put(ENTITIES, array);

    databroker.registerStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Register subscription response for invalid exchange request is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
    testContext.completeNow();

  }

  @Test
  @DisplayName("Testing failure case : Register streaming subscription with routingKey = null")
  @Order(19)
  void failedregisterStreamingSubscriptionNullRoutingKey(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    expected.put(ERROR, "Invalid routingKey");

    JsonObject request = new JsonObject();
    request.put(NAME, "alias");
    request.put(USER_ID, userid);
    request.put(TYPE, "streaming");
    JsonArray array = new JsonArray();
    array.add("");
    request.put(ENTITIES, array);

    databroker.registerStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER
                .debug("Register subscription response for invalid routingKey request is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
    testContext.completeNow();
  }

  @Test
  @DisplayName("Testing failure case : Register streaming subscription with routingKey = invalid key")
  @Order(20)
  void failedregisterStreamingSubscriptionInvalidRoutingKey(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    expected.put(ERROR, "Invalid routingKey");

    JsonObject request = new JsonObject();
    request.put(NAME, "alias");
    request.put(USER_ID, userid);
    request.put(TYPE, "streaming");
    JsonArray array = new JsonArray();
    array.add("iudx.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm");
    request.put(ENTITIES, array);

    databroker.registerStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER
                .debug("Register subscription response for invalid routingKey request is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
    testContext.completeNow();

  }

  @Test
  @DisplayName("Testing success case : Register streaming subscription with valid queue and exchange names")
  @Order(21)
  void successregisterStreamingSubscription(VertxTestContext testContext) {

    String queueName = "alias";
    JsonObject expected = new JsonObject();
    expected.put(SUBSCRIPTION_ID, queueName);
    expected.put(USER_ID, userid);
    expected.put(APIKEY, "123456");
    expected.put(URL, BROKER_PRODUCTION_DOMAIN);
    expected.put(PORT, BROKER_PRODUCTION_PORT);
    expected.put(VHOST, VHOST_IUDX);


    JsonObject request = new JsonObject();
    request.put(NAME, "alias");
    request.put(USER_ID, userid);
    request.put(TYPE, "streaming");
    JsonArray array = new JsonArray();
    array.add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information");

    request.put(ENTITIES, array);

    databroker.registerStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Register subscription response is : " + response);
        JsonObject brokerResponse=response.getJsonArray("results").getJsonObject(0);

        assertTrue(brokerResponse.containsKey(USER_NAME));
        assertTrue(brokerResponse.containsKey(APIKEY));
        assertTrue(brokerResponse.containsKey(URL));
        assertTrue(brokerResponse.containsKey(PORT));
        assertTrue(brokerResponse.containsKey(VHOST));
      }
      testContext.completeNow();
    });
    testContext.completeNow();

  }

  @Test
  @DisplayName("Testing failure case : Register streaming subscription with already existing alias-name")
  @Order(22)
  void failedregisterStreamingSubscriptionAlreadyExistingQueue(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    expected.put(ERROR, "Queue Creation Failed");

    JsonObject request = new JsonObject();
    request.put(NAME, "alias");
    request.put(USER_ID, userid);
    request.put(TYPE, "streaming");
    JsonArray array = new JsonArray();
    array.add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information");
    request.put(ENTITIES, array);

    databroker.registerStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Register subscription response for already existing alias-name request is : "
                + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
    testContext.completeNow();

  }

  @Test
  @DisplayName("Testing success case : get streaming subscription")
  @Order(23)
  void successlistStreamingSubscription(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    JsonArray routingKeys = new JsonArray();
    routingKeys.add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/.*");
    expected.put(ENTITIES, routingKeys);

    JsonObject request = new JsonObject();
    request.put(SUBSCRIPTION_ID, userid+"/alias");
    databroker.listStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("List subscription response is : " + response);
        assertEquals(expected, response.getJsonArray("results").getJsonObject(0));
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing failure case : list streaming subscription (ID not available)")
  @Order(24)
  void failedlistStreamingSubscriptionIdNotFound(VertxTestContext testContext) {
    String id = "63ac4f5d7fd26840f955408b0e4d30f2/alias";
    JsonObject expected = new JsonObject();
    expected.put(TYPE, 404);
    expected.put(TITLE, FAILURE);
    expected.put(DETAIL, QUEUE_DOES_NOT_EXISTS);
    JsonObject request = new JsonObject();
    request.put(SUBSCRIPTION_ID, id);

    databroker.deleteStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("List subscription response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing success case : Update streaming subscription with valid queue and exchange names")
  @Order(25)
  void successupdateStreamingSubscription(VertxTestContext testContext) {

    String queueName = userid+"/alias";
    JsonObject expected = new JsonObject();
//    expected.put(ID, queueName);
//    expected.put(USER_NAME, userid);
//    expected.put(APIKEY, "123456");
//    expected.put(URL, "databroker.iudx.io");
//    expected.put(PORT, "5671");
//    expected.put(VHOST, "IUDX");
    JsonArray array = new JsonArray();
    array.add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood");
    expected.put(ENTITIES, array);

    JsonObject request = new JsonObject();
    request.put(NAME, "alias");
    request.put(USER_ID, userid);
    request.put(TYPE, "streaming");
    array = new JsonArray();
    array.add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood");

    request.put(ENTITIES, array);

    databroker.updateStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Update subscription response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
    testContext.completeNow();

  }


  @Test
  @DisplayName("Testing failure case : Update streaming subscription with empty request")
  @Order(26)
  void failedupdateStreamingSubscriptionEmptyRequest(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    expected.put(TYPE, 400);
    expected.put(TITLE, BAD_REQUEST_DATA);
    expected.put(DETAIL, BAD_REQUEST_DATA);

    JsonObject request = new JsonObject();
    databroker.updateStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Update subscription response for empty request is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing failure case : Update streaming subscription with non-existing (but valid) routingKey")
  @Order(27)
  void failedupdateStreamingSubscriptionInvalidExchange(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    expected.put(ERROR, "Binding Failed");

    JsonObject request = new JsonObject();
    request.put(NAME, "alias");
    request.put(USER_ID,userid );
    request.put(TYPE, "streaming");
    JsonArray array = new JsonArray();
    array.add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information_123");
    request.put(ENTITIES, array);

    databroker.updateStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Update subscription response for invalid exchange request is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
    testContext.completeNow();

  }

  @Test
  @DisplayName("Testing failure case : Update streaming subscription with routingKey = null")
  @Order(28)
  void failedupdateStreamingSubscriptionNullRoutingKey(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    expected.put(ERROR, "Invalid routingKey");

    JsonObject request = new JsonObject();
    request.put(NAME, "alias");
    request.put(USER_ID, userid);
    request.put(TYPE, "streaming");
    JsonArray array = new JsonArray();
    array.add("");
    request.put(ENTITIES, array);

    databroker.registerStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Update subscription response for invalid routingKey request is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
    testContext.completeNow();

  }

  @Test
  @DisplayName("Testing failure case : Update streaming subscription with routingKey = invalid key")
  @Order(29)
  void failedupdateStreamingSubscriptionInvalidRoutingKey(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    expected.put(ERROR, "Invalid routingKey");

    JsonObject request = new JsonObject();
    request.put(NAME, "alias");
    request.put(USER_ID, userid);
    request.put(TYPE, "streaming");
    JsonArray array = new JsonArray();
    array.add("iudx.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm");
    request.put(ENTITIES, array);

    databroker.registerStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Update subscription response for invalid routingKey request is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
    testContext.completeNow();

  }

  @Test
  @DisplayName("Testing success case : get streaming subscription of updated subscription")
  @Order(30)
  void successlistupdatedStreamingSubscription(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    JsonArray routingKeys = new JsonArray();
    routingKeys.add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/.*");
    expected.put(ENTITIES, routingKeys);

    JsonObject request = new JsonObject();
    request.put(SUBSCRIPTION_ID, userid+"/alias-pawan");
    databroker.listStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Get subscription response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing success case : Update (Append) streaming subscription with valid queue and exchange names")
  @Order(31)
  void successappendStreamingSubscription(VertxTestContext testContext) {

    String queueName = userid+"/alias";
    JsonArray array = new JsonArray();
    array.add("varanasismartcity.gov.in/62d1f729edd3d2a1a090cb1c6c89356296963d55/rs.iudx.io/varanasi-env-aqm/.*");

    JsonObject expected = new JsonObject();
    expected.put(SUBSCRIPTION_ID, queueName);
    expected.put(ENTITIES, array);

    JsonObject request = new JsonObject();
    request.put(SUBSCRIPTION_ID, queueName);
    request.put(ENTITIES, array);

    databroker.appendStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Update (Append) subscription response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing failure case : Update(append) streaming subscription with empty request")
  @Order(32)
  void failedappendStreamingSubscriptionEmptyRequest(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    expected.put(ERROR, "Error in payload");

    JsonObject request = new JsonObject();
    databroker.appendStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Update (Append) subscription response for empty request is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing failure case : Update (append) streaming subscription with non-existing (but valid) routingKey")
  @Order(33)
  void failedappendStreamingSubscriptionInvalidExchange(VertxTestContext testContext) {

    String queueName = userid+"/alias";
    JsonArray array = new JsonArray();
    array.add("iudx.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm1/.*");

    JsonObject expected = new JsonObject();
    expected.put(ERROR, "Binding Failed");

    JsonObject request = new JsonObject();
    request.put(SUBSCRIPTION_ID, queueName);
    request.put(ENTITIES, array);

    databroker.appendStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug(
                "Update (Append) subscription response for invalid exchange request is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing failure case : Update (Append) streaming subscription with routingKey = null")
  @Order(34)
  void failedappendStreamingSubscriptionNullRoutingKey(VertxTestContext testContext) {

    String queueName = userid+"/alias";
    JsonArray array = new JsonArray();
    array.add("");

    JsonObject expected = new JsonObject();
    expected.put(ERROR, "Invalid routingKey");

    JsonObject request = new JsonObject();
    request.put(SUBSCRIPTION_ID, queueName);
    request.put(ENTITIES, array);

    databroker.appendStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Update (Append) subscription response for invalid routingKey request is : "
                + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }


  @Test
  @DisplayName("Testing failure case : Update (append)streaming subscription with routingKey = invalid key")
  @Order(35)
  void failedappendStreamingSubscriptionInvalidRoutingKey(VertxTestContext testContext) {

    String queueName = userid+"/alias";
    JsonArray array = new JsonArray();
    array.add("rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm");

    JsonObject expected = new JsonObject();
    expected.put(ERROR, "Invalid routingKey");

    JsonObject request = new JsonObject();
    request.put(SUBSCRIPTION_ID, queueName);
    request.put(ENTITIES, array);

    databroker.appendStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug(
                "Update (append)subscription response for invalid routingKey request is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing success case : get streaming subscription of updated (append) subscription")
  @Order(36)
  void successlistappenddStreamingSubscription(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    JsonArray routingKeys = new JsonArray();
    routingKeys.add(
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/.*");
    routingKeys.add(
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm1/.*");
    expected.put(ENTITIES, routingKeys);

    JsonObject request = new JsonObject();
    request.put(SUBSCRIPTION_ID, "google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias-pawan");
    databroker.listStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Get subscription (after append) response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing success case : Update (Append) streaming subscription with invalid valid queue (subscriptionID)")
  @Order(37)
  void failureappendStreamingSubscriptionQueueNotPresent(VertxTestContext testContext) {

    String queueName = "non-existing-queue";
    JsonArray array = new JsonArray();
    array.add(
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm1/.*");

    JsonObject expected = new JsonObject();
    expected.put(ERROR, "Invalid routingKey");

    JsonObject request = new JsonObject();
    request.put(SUBSCRIPTION_ID, queueName);
    request.put(ENTITIES, array);

    databroker.appendStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Update (Append) with invalid subscriptionID response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing success case : delete streaming subscription")
  @Order(38)
  void successdeleteStreamingSubscription(VertxTestContext testContext) {
    String queueName = "google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias-pawan";
    JsonObject expected = new JsonObject();
    expected.put(SUBSCRIPTION_ID, queueName);
    JsonObject request = new JsonObject();
    request.put(SUBSCRIPTION_ID, "google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias-pawan");

    databroker.deleteStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Delete subscription response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing failure case : delete streaming subscription (ID not available)")
  @Order(39)
  void faileddeleteStreamingSubscriptionIdNotFound(VertxTestContext testContext) {
    String id = "google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias";
    JsonObject expected = new JsonObject();
    expected.put(TYPE, 404);
    expected.put(TITLE, FAILURE);
    expected.put(DETAIL, QUEUE_DOES_NOT_EXISTS);
    JsonObject request = new JsonObject();
    request.put(SUBSCRIPTION_ID, id);

    databroker.deleteStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        LOGGER.debug("Delete subscription response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }
}


