package iudx.resource.server.databroker;


import static iudx.resource.server.databroker.util.Constants.*;
import static iudx.resource.server.databroker.util.Util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;


@ExtendWith(VertxExtension.class)
@TestMethodOrder(OrderAnnotation.class)
public class CallbackSubscriptionTest {

  private static final Logger logger = LoggerFactory.getLogger(CallbackSubscriptionTest.class);

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
  static JsonObject propObj;
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

  private static RabbitMQStreamingClient rabbitMQStreamingClient;
  private static RabbitMQWebClient rabbitMQWebClient;
  private static PostgresQLClient pgClient;

  @BeforeAll
  @DisplayName("Initialize the Databroker class with web client and rabbitmq client")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {

    /* Read the configuration and set the rabbitMQ server properties. */
    properties = new Properties();
    inputstream = null;

    try {

      inputstream = new FileInputStream("config.properties");
      properties.load(inputstream);

      dataBrokerIP = properties.getProperty("dataBrokerIP");
      dataBrokerPort = Integer.parseInt(properties.getProperty("dataBrokerPort"));
      dataBrokerManagementPort =
          Integer.parseInt(properties.getProperty("dataBrokerManagementPort"));
      dataBrokerVhost = properties.getProperty("dataBrokerVhost");
      dataBrokerUserName = properties.getProperty("dataBrokerUserName");
      dataBrokerPassword = properties.getProperty("dataBrokerPassword");
      connectionTimeout = Integer.parseInt(properties.getProperty("connectionTimeout"));
      requestedHeartbeat = Integer.parseInt(properties.getProperty("requestedHeartbeat"));
      handshakeTimeout = Integer.parseInt(properties.getProperty("handshakeTimeout"));
      requestedChannelMax = Integer.parseInt(properties.getProperty("requestedChannelMax"));
      networkRecoveryInterval = Integer.parseInt(properties.getProperty("networkRecoveryInterval"));
      databaseIP = properties.getProperty("callbackDatabaseIP");
      databasePort = Integer.parseInt(properties.getProperty("callbackDatabasePort"));
      databaseName = properties.getProperty("callbackDatabaseName");
      databaseUserName = properties.getProperty("callbackDatabaseUserName");
      databasePassword = properties.getProperty("callbackDatabasePassword");
      poolSize = Integer.parseInt(properties.getProperty("callbackpoolSize"));

    } catch (Exception ex) {
      logger.info(ex.toString());
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

    /* Call the databroker constructor with the RabbitMQ client Vertx web client. */
    rabbitMQWebClient = new RabbitMQWebClient(vertx, webConfig, propObj);
    rabbitMQStreamingClient = new RabbitMQStreamingClient(vertx, config, rabbitMQWebClient);
    pgClient = new PostgresQLClient(vertx, connectOptions, poolOptions);
    databroker = new DataBrokerServiceImpl(rabbitMQStreamingClient, pgClient, dataBrokerVhost);
    testContext.completeNow();

  }

  @Test
  @DisplayName("Testing failure case : register callback subscription with empty request")
  @Order(1)
  void failedregisterCallbackSubscriptionEmptyRequest(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    expected.put(ERROR, "Error in payload");

    JsonObject request = new JsonObject();
    databroker.registerCallbackSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Register subscription response for empty request is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing failure case : Register callback subscription with non-existing (but valid) routingKey")
  @Order(2)
  void failedregisterCallbackSubscriptionInvalidExchange(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    expected.put(ERROR, "Binding Failed");

    JsonObject request = new JsonObject();
    request.put(NAME, "test-callback");
    request.put(CONSUMER, "pawan@google.org");
    request.put(TYPE, "callback");
    request.put(CALLBACKURL, "http://localhost:9088/api");
    request.put(QUEUE, "callback.data");
    JsonArray array = new JsonArray();
    array.add(
        "iudx.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/varanasi.iudx.org.in/varanasi-aqm-2/EM_01_0103_02");
    request.put(ENTITIES, array);

    databroker.registerCallbackSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Register callback subscription response for invalid exchange request is : "
            + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing failure case : Register callback subscription with routingKey = null")
  @Order(3)
  void failedregisterCallbackSubscriptionNullRoutingKey(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    expected.put(ERROR, "Invalid routingKey");

    JsonObject request = new JsonObject();
    request.put(NAME, "test-callback");
    request.put(CONSUMER, "pawan@google.org");
    request.put(TYPE, "callback");
    request.put(CALLBACKURL, "http://localhost:9088/api");
    request.put(QUEUE, "callback.data");
    JsonArray array = new JsonArray();
    array.add("");
    request.put(ENTITIES, array);

    databroker.registerCallbackSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info(
            "Register callback subscription response invalid routingKey request is : " + response);
        assertEquals(expected, response);
        // assertTrue(response.getString(ERROR).equalsIgnoreCase("Invalid routingKey"));
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing failure case : Register callback subscription with routingKey = invalid  key")
  @Order(4)
  void failedregisterCallbackSubscriptionInvalidRoutingKey(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    expected.put(ERROR, "Invalid routingKey");

    JsonObject request = new JsonObject();
    request.put(NAME, "test-callback");
    request.put(CONSUMER, "pawan@google.org");
    request.put(TYPE, "callback");
    request.put(CALLBACKURL, "http://localhost:9088/api");
    request.put(QUEUE, "callback.data");
    JsonArray array = new JsonArray();
    array.add(
        "iudx.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm");
    request.put(ENTITIES, array);

    databroker.registerCallbackSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Register callback subscription response for invalid routingKey request is : "
            + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing success case : register callback subscription with valid data")
  @Order(5)
  void successregisterCallbackSubscription(VertxTestContext testContext) {
    String subscriptionID = "google.org/63ac4f5d7fd26840f955408b0e4d30f2/test-callback";
    JsonObject expected = new JsonObject();
    expected.put(SUBSCRIPTION_ID, subscriptionID);

    JsonObject request = new JsonObject();
    request.put(NAME, "test-callback");
    request.put(CONSUMER, "pawan@google.org");
    request.put(TYPE, "callback");
    request.put(CALLBACKURL, "http://localhost:9088/api");
    request.put(QUEUE, "callback.data");
    JsonArray array = new JsonArray();
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_02");
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_03");
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm2/EM_01_0103_04");
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm1/.*");

    request.put(ENTITIES, array);
    databroker.registerCallbackSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("successregisterCallbackSubscription response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing failure case : Register callback subscription with duplicate subscriptionID")
  @Order(6)
  void failedregisterCallbackSubscriptionDuplicateSubscriptionID(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    expected.put(ERROR, "duplicate key value violates unique constraint");

    JsonObject request = new JsonObject();
    request.put(NAME, "test-callback");
    request.put(CONSUMER, "pawan@google.org");
    request.put(TYPE, "callback");
    request.put(CALLBACKURL, "http://localhost:9088/api");
    request.put(QUEUE, "callback.data");
    JsonArray array = new JsonArray();
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm1/.*");
    request.put(ENTITIES, array);
    databroker.registerCallbackSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info(
            "Register callback subscription response for duplicate subscriptionID request is :  "
                + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing success case : list callback subscription with valid data")
  @Order(7)
  void successlistCallbackSubscription(VertxTestContext testContext) throws InterruptedException {
    String subscriptionID = "google.org/63ac4f5d7fd26840f955408b0e4d30f2/test-callback";
    JsonObject expected = new JsonObject();
    expected.put(SUBSCRIPTION_ID, subscriptionID);
    expected.put(CALLBACKURL, "http://localhost:9088/api");
    JsonArray array = new JsonArray();
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_02");
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_03");
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm2/EM_01_0103_04");
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm1/.*");

    expected.put(ENTITIES, array);
 
    JsonObject request = new JsonObject();
    request.put(NAME, "test-callback");
    request.put(CONSUMER, "pawan@google.org");
    request.put(TYPE, "streaming");
    request.put(CALLBACKURL, "http://localhost:9088/api");

    request.put(QUEUE, "callback.data");

    databroker.listCallbackSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("list subscription response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing failure case : list callback subscription with not valid subscriptionid")
  @Order(8)
  void failedlistCallbackSubscriptionNotvalidSubsciptionId(VertxTestContext testContext) throws InterruptedException {
    JsonObject expected = new JsonObject();
    expected.put(ERROR, "Error in payload");

    JsonObject request = new JsonObject();
    request.put(NAME, "alias-pawan");
    request.put(CONSUMER, "pawan@google.org");
    request.put(TYPE, "streaming");
    request.put(CALLBACKURL, "https://rbccps.org/api");
    request.put(QUEUE, "callback.data");
    JsonArray array = new JsonArray();
    array.add("");
    request.put(ENTITIES, array);

    databroker.listCallbackSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info(" list callback subscription response not valid subscriptionid request is : "
            + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }
  
  @Test
  @DisplayName("Testing success case : register callback update subscription with valid data")
  @Order(9)
  void successupdateCallbackSubscription(VertxTestContext testContext) {
    String subscriptionID = "google.org/63ac4f5d7fd26840f955408b0e4d30f2/test-callback";
    JsonObject expected = new JsonObject();
    expected.put(SUBSCRIPTION_ID, subscriptionID);

    JsonObject request = new JsonObject();
    request.put(NAME, "test-callback");
    request.put(CONSUMER, "pawan@google.org");
    request.put(TYPE, "callback");
    request.put(CALLBACKURL, "http://localhost:9088/api");
    request.put(QUEUE, "callback.data");
    JsonArray array = new JsonArray();
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_10");
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_11");
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm2/EM_01_0103_12");
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm1/.*");

    request.put(ENTITIES, array);
    databroker.updateCallbackSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Delete subscription response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing failure case : delete callback subscription with valid data")
  @Order(10)
  void failuredeleteCallbackSubscription(VertxTestContext testContext) {
    String error = "Call Back ID not found";
    JsonObject expected = new JsonObject();
    expected.put(ERROR, error);

    JsonObject request = new JsonObject();
    request.put(NAME, "test-callback-non-existing-id");
    request.put(CONSUMER, "pawan@google.org");
    databroker.deleteCallbackSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Delete subscription response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }
  
  @Test
  @DisplayName("Testing success case : delete callback subscription with valid data")
  @Order(11)
  void successdeleteCallbackSubscription(VertxTestContext testContext) {
    String subscriptionID = "google.org/63ac4f5d7fd26840f955408b0e4d30f2/test-callback";
    JsonObject expected = new JsonObject();
    expected.put(SUBSCRIPTION_ID, subscriptionID);

    JsonObject request = new JsonObject();
    request.put(NAME, "test-callback");
    request.put(CONSUMER, "pawan@google.org");
    databroker.deleteCallbackSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Delete subscription response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  

}
