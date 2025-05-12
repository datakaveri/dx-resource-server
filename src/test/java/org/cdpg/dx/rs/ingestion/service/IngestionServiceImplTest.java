package org.cdpg.dx.rs.ingestion.service;

import static org.cdpg.dx.common.util.ProxyAddressConstants.DATA_BROKER_SERVICE_ADDRESS;
import static org.cdpg.dx.common.util.ProxyAddressConstants.PG_SERVICE_ADDRESS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.catalogue.service.CatalogueService;
import org.cdpg.dx.catalogue.service.CatalogueServiceImpl;
import org.cdpg.dx.database.postgres.PostgresVerticle;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.databroker.DataBrokerVerticle;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.rs.ingestion.dao.impl.IngestionDAOImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IngestionServiceImplTest {
  private static final Logger LOGGER = LogManager.getLogger(IngestionServiceImplTest.class);

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:15")
          .withDatabaseName("testdb")
          .withUsername("testuser")
          .withPassword("testpass");

  @Container
  private static final RabbitMQContainer rabbitMQContainer =
      new RabbitMQContainer("rabbitmq:4.0-management")
          .withExposedPorts(5672, 15672)
          .withEnv("RABBITMQ_DEFAULT_USER", "guest")
          .withEnv("RABBITMQ_DEFAULT_PASS", "guest")
          .withEnv("RABBITMQ_DEFAULT_VHOST", "IUDX");

  private IngestionServiceImpl ingestionServiceImpl;
  private PostgresService postgresService;
  private DataBrokerService dataBrokerService;
  private CatalogueService catalogueService;
  private IngestionDAOImpl ingestionDAOImpl;

  @BeforeEach
  void initMocks() throws Exception {
    MockitoAnnotations.openMocks(this).close();
  }

  @BeforeAll
  void setup(Vertx vertx, VertxTestContext testContext) {

    String jdbcUrl = POSTGRES.getJdbcUrl();
    try (Connection conn =
            DriverManager.getConnection(jdbcUrl, POSTGRES.getUsername(), POSTGRES.getPassword());
        Statement stmt = conn.createStatement()) {
      String enableExtension = "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";";
      String createTableQuery =
          """
              CREATE TABLE IF NOT EXISTS adaptors_details (
                  _id UUID NOT NULL DEFAULT uuid_generate_v4(),
                  exchange_name VARCHAR NOT NULL,
                  resource_id VARCHAR NOT NULL,
                  dataset_name VARCHAR NOT NULL,
                  dataset_details_json JSONB NOT NULL,
                  user_id VARCHAR NOT NULL,
                  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                  modified_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                  providerid UUID,
                  CONSTRAINT adaptors_details_pkey PRIMARY KEY (_id),
                  CONSTRAINT exchange_name_unique UNIQUE (exchange_name)
              );
              """;
      String createTrigger =
"""
CREATE OR REPLACE TRIGGER update_ad_created
BEFORE INSERT
ON public.adaptors_details
FOR EACH ROW
EXECUTE FUNCTION public.update_created();
""";
      String createFunction =
"""
CREATE OR REPLACE FUNCTION update_created()
RETURNS TRIGGER AS $$
BEGIN
  NEW.created_at := now();
  NEW.modified_at := now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
""";
      stmt.execute(enableExtension);
      stmt.execute(createFunction);
      stmt.execute(createTableQuery);
      stmt.execute(createTrigger);

    } catch (Exception e) {
      throw new RuntimeException("Failed to create table", e);
    }

    JsonObject config =
        new JsonObject()
            .put("databaseIP", POSTGRES.getHost())
            .put("databasePort", POSTGRES.getMappedPort(5432))
            .put("databaseName", POSTGRES.getDatabaseName())
            .put("databaseUserName", POSTGRES.getUsername())
            .put("databasePassword", POSTGRES.getPassword())
            .put("poolSize", 5);

    JsonObject databrokerConfig =
        new JsonObject()
            .put("dataBrokerIP", rabbitMQContainer.getHost())
            .put("dataBrokerPort", rabbitMQContainer.getMappedPort(15672))
            .put("prodVhost", "IUDX")
            .put("internalVhost", "IUDX-INTERNAL")
            .put("externalVhost", "IUDX-EXTERNAL")
            .put("dataBrokerUserName", "guest")
            .put("dataBrokerPassword", "guest")
            .put("dataBrokerManagementPort", rabbitMQContainer.getMappedPort(15672))
            .put("connectionTimeout", 6000)
            .put("requestedHeartbeat", 6)
            .put("handshakeTimeout", 6000)
            .put("requestedChannelMax", 5)
            .put("networkRecoveryInterval", 500)
            .put("automaticRecoveryEnabled", "true")
            .put("brokerAmqpPort", rabbitMQContainer.getAmqpPort())
            .put("brokerAmqpIp", rabbitMQContainer.getHost());

    vertx
        .deployVerticle(new PostgresVerticle(), new DeploymentOptions().setConfig(config))
        .compose(
            pgId -> {
              return vertx.deployVerticle(
                  new DataBrokerVerticle(), new DeploymentOptions().setConfig(databrokerConfig));
            })
        .compose(
            v -> {
              postgresService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);
              dataBrokerService = DataBrokerService.createProxy(vertx, DATA_BROKER_SERVICE_ADDRESS);
              catalogueService = Mockito.mock(CatalogueServiceImpl.class);
              ingestionDAOImpl = new IngestionDAOImpl(postgresService);
              ingestionServiceImpl =
                  new IngestionServiceImpl(catalogueService, dataBrokerService, ingestionDAOImpl);

              return createDefaultQueues(vertx);
            })
        .onSuccess(
            result -> {
              testContext.completeNow();
            })
        .onFailure(
            err -> {
              LOGGER.error(err.getMessage());
              testContext.failNow(err);
            });
  }

  private Future<Void> createDefaultQueues(Vertx vertx) {
    Promise<Void> promise = Promise.promise();

    WebClientOptions options =
        new WebClientOptions()
            .setDefaultHost(rabbitMQContainer.getHost())
            .setDefaultPort(rabbitMQContainer.getMappedPort(15672))
            .setSsl(false);

    WebClient client = WebClient.create(vertx, options);

    JsonObject config =
        new JsonObject()
            .put("auto_delete", false)
            .put("durable", true)
            .put("arguments", new JsonObject());

    Future<Void> f1 =
        client
            .put("/api/queues/IUDX/database")
            .putHeader("Authorization", basicAuth("guest", "guest"))
            .sendJsonObject(config)
            .compose(res -> Future.succeededFuture());

    Future<Void> f2 =
        client
            .put("/api/queues/IUDX/subscriptions-monitoring")
            .putHeader("Authorization", basicAuth("guest", "guest"))
            .sendJsonObject(config)
            .compose(res -> Future.succeededFuture());

    Future<Void> f3 =
        client
            .put("/api/queues/IUDX/redis-latest")
            .putHeader("Authorization", basicAuth("guest", "guest"))
            .sendJsonObject(config)
            .compose(res -> Future.succeededFuture());

    CompositeFuture.all(f1, f2, f3).onSuccess(v -> promise.complete()).onFailure(promise::fail);

    return promise.future();
  }

  private String basicAuth(String username, String password) {
    String auth = username + ":" + password;
    return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
  }

  @Order(1)
  @Test
  void testCreateIngestion_success(VertxTestContext testContext) {
    JsonObject catalogueResult =
        new JsonObject()
            .put("resourceGroup", "8b95ab80-2aaf-4636-a65e-7f2563d0d371")
            .put("accessPolicy", "SECURE")
            .put("provider", "bbeacb12-5e54-339d-92e0-d8e063b551a8")
            .put("instance", "surat")
            .put(
                "description",
                "Realtime bus position information from Surat city public transit buses.")
            .put("label", "Surat Transit Realtime Position-UUID")
            .put("type", new JsonArray().add("iudx:Resource").add("iudx:TransitManagement"))
            .put("name", "surat-itms-live-eta")
            .put("id", "935f2045-f5c6-4c76-b14a-c29a88589bf3")
            .put("type", new JsonArray().add("iudx:Resource"));

    when(catalogueService.fetchCatalogueInfo("935f2045-f5c6-4c76-b14a-c29a88589bf3"))
        .thenReturn(Future.succeededFuture(catalogueResult));
    ingestionServiceImpl
        .registerAdapter(
            "935f2045-f5c6-4c76-b14a-c29a88589bf3", "b2c27f3f-2524-4a84-816e-91f9ab23f837")
        .onSuccess(v -> testContext.completeNow())
        .onFailure(err -> testContext.failNow(err));
  }

  @Order(2)
  @Test
  void testGetAdapter_success(VertxTestContext testContext) {

    ingestionServiceImpl
        .getAdapterDetails("8b95ab80-2aaf-4636-a65e-7f2563d0d371")
        .onSuccess(v -> testContext.completeNow())
        .onFailure(err -> testContext.failNow(err));
  }

  @Order(3)
  @Test
  void testDeleteAdapter_success(VertxTestContext testContext) {

    ingestionServiceImpl
        .deleteAdapter(
            "8b95ab80-2aaf-4636-a65e-7f2563d0d371", "b2c27f3f-2524-4a84-816e-91f9ab23f837")
        .onSuccess(v -> testContext.completeNow())
        .onFailure(err -> testContext.failNow(err));
  }
}
