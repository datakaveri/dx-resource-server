package org.cdpg.dx.rs.subscription.service;

import static org.cdpg.dx.common.util.ProxyAddressConstants.DATA_BROKER_SERVICE_ADDRESS;
import static org.cdpg.dx.common.util.ProxyAddressConstants.PG_SERVICE_ADDRESS;
import static org.mockito.Mockito.when;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.catalogue.service.CatalogueService;
import org.cdpg.dx.catalogue.service.CatalogueServiceImpl;
import org.cdpg.dx.database.postgres.PostgresVerticle;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.databroker.DataBrokerVerticle;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.rs.subscription.model.PostSubscriptionModel;
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
public class SubscriptionServiceImplTest {
  private static final Logger LOGGER = LogManager.getLogger(SubscriptionServiceImplTest.class);
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

  private SubscriptionServiceImpl subscriptionService;
  private PostgresService postgresService;
  private DataBrokerService dataBrokerService;
  private CatalogueService catalogueService;

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

      String createEnumSubType =
          "DO $$ BEGIN "
              + "IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'sub_type') THEN "
              + "CREATE TYPE sub_type AS ENUM ('STREAMING', 'CALLBACK'); "
              + "END IF; END $$;";

      String createEnumItem =
          "DO $$ BEGIN "
              + "IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'item') THEN "
              + "CREATE TYPE item AS ENUM ('RESOURCE', 'RESOURCE_GROUP'); "
              + "END IF; END $$;";

      String createTableQuery =
          """
                          CREATE TABLE IF NOT EXISTS public.subscriptions (
                              _id character varying NOT NULL,
                              queue_name character varying NOT NULL,
                              entity character varying NOT NULL,
                              expiry timestamp without time zone NOT NULL,
                              created_at timestamp without time zone NOT NULL DEFAULT now(),
                              modified_at timestamp without time zone NOT NULL DEFAULT now(),
                              _type sub_type NOT NULL,
                              dataset_name character varying,
                              dataset_json json,
                              user_id uuid,
                              resource_group uuid,
                              delegator_id uuid,
                              item_type item,
                              provider_id uuid,
                              CONSTRAINT sub_pk PRIMARY KEY (queue_name, entity)
                          );
                          """;

      stmt.execute(createEnumSubType);
      stmt.execute(createEnumItem);
      stmt.execute(createTableQuery);

    } catch (Exception e) {
      throw new RuntimeException("Failed to create table", e);
    }

    // Step 2: Deploy PostgresVerticle with the config
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
            rmqId -> {
              postgresService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);
              dataBrokerService = DataBrokerService.createProxy(vertx, DATA_BROKER_SERVICE_ADDRESS);
              catalogueService = Mockito.mock(CatalogueServiceImpl.class);
              subscriptionService =
                  new SubscriptionServiceImpl(postgresService, dataBrokerService, catalogueService);

              return new HelperDataBrokerMethod(dataBrokerService).createExchange();
            })
        .onSuccess(
            v -> {
              testContext.completeNow();
            })
        .onFailure(
            err -> {
              LOGGER.error(err.getMessage());
              testContext.failNow(err);
            });
  }

  @Order(1)
  @Test
  void testCreateSubscription_success(VertxTestContext context) {

    PostSubscriptionModel postSub = new PostSubscriptionModel();
    postSub.setUserId("fd47486b-3497-4248-ac1e-082e4d37a66c");
    postSub.setName("my-sub");
    postSub.setEntityId("83c2e5c2-3574-4e11-9530-2b1fbdfce832");
    postSub.setExpiry("2025-05-01T00:00:00Z");
    postSub.setDelegatorId("fd47486b-3497-4248-ac1e-082e4d37a66c");
    postSub.setSubscriptionType("STREAMING");

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
            .put("id", "83c2e5c2-3574-4e11-9530-2b1fbdfce832");

    when(catalogueService.fetchCatalogueInfo("83c2e5c2-3574-4e11-9530-2b1fbdfce832"))
        .thenReturn(Future.succeededFuture(catalogueResult));

    subscriptionService
        .createSubscription(postSub)
        .onSuccess(
            result -> {
              context.completeNow();
            })
        .onFailure(
            failure -> {
              context.failed();
            });
  }
 // @Order(2)
 // @Test
  void testGetSubscription_success(VertxTestContext context) {

    subscriptionService.getSubscription("fd47486b-3497-4248-ac1e-082e4d37a66c/my-sub")
        .onSuccess(
            result -> {
              context.completeNow();
            })
        .onFailure(
            failure -> {
              context.failed();
            });
  }

 // @Order(3)
 // @Test
  void testUpdateSubscription_success(VertxTestContext context) {
    subscriptionService.updateSubscription("83c2e5c2-3574-4e11-9530-2b1fbdfce832","my-sub2","2025-05-01T00:00:00Z")
        .onSuccess(
            result -> {
              context.completeNow();
            })
        .onFailure(
            failure -> {
              context.failed();
    });
  }

  @Order(4)
  @Test
  void testAppendSubscription_success(VertxTestContext context) {
    PostSubscriptionModel postSub = new PostSubscriptionModel();
    postSub.setUserId("fd47486b-3497-4248-ac1e-082e4d37a66c");
    postSub.setName("my-sub4");
    postSub.setEntityId("83c2e5c2-3574-4e11-9530-2b1fbdfce832");
    postSub.setExpiry("2025-05-01T00:00:00Z");
    postSub.setDelegatorId("fd47486b-3497-4248-ac1e-082e4d37a66c");
    postSub.setSubscriptionType("STREAMING");

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
                    .put("id", "83c2e5c2-3574-4e11-9530-2b1fbdfce832");

    when(catalogueService.fetchCatalogueInfo("83c2e5c2-3574-4e11-9530-2b1fbdfce832"))
            .thenReturn(Future.succeededFuture(catalogueResult));
    subscriptionService.appendSubscription(postSub,"fd47486b-3497-4248-ac1e-082e4d37a66c/my-sub")
            .onSuccess(
                    result -> {
                      context.completeNow();
                    })
            .onFailure(
                    failure -> {
                      context.failed();
                    });
  }

  //@Order(5)
  //@Test
  void testDeleteSubscription_success(VertxTestContext context) {

    subscriptionService.deleteSubscription("fd47486b-3497-4248-ac1e-082e4d37a66c/my-sub","fd47486b-3497-4248-ac1e-082e4d37a66c")
        .onSuccess(
            result -> {
              context.completeNow();
            })
        .onFailure(
            failure -> {
              context.failed();
            });
  }

  @Order(6)
  @Test
    void testFailureRmqContainer(VertxTestContext context) {
      rabbitMQContainer.stop();
      subscriptionService.getSubscription("fd47486b-3497-4248-ac1e-082e4d37a66c/my-sub")
        .onSuccess(
            result -> {
              context.failed();
            })
        .onFailure(
            failure -> {
              context.completeNow();
            });
    }

    @Order(7)
    @Test
    void testFailurePostgresContainer(VertxTestContext context) {
        POSTGRES.stop();
        subscriptionService.deleteSubscription("fd47486b-3497-4248-ac1e-082e4d37a66c/my-sub","fd47486b-3497-4248-ac1e-082e4d37a66c")
                .onSuccess(
                        result -> {
                            context.failed();
                        })
                .onFailure(
                        failure -> {
                            context.completeNow();
                        });
    }


}
