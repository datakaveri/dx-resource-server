package org.cdpg.dx.rs.admin.service;

import static org.cdpg.dx.common.util.ProxyAddressConstants.DATA_BROKER_SERVICE_ADDRESS;
import static org.cdpg.dx.common.util.ProxyAddressConstants.PG_SERVICE_ADDRESS;
import static org.junit.jupiter.api.Assertions.*;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.database.postgres.PostgresVerticle;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.databroker.DataBrokerVerticle;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.databroker.util.Vhosts;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminServiceImplTest2 {
    private static final Logger LOGGER = LogManager.getLogger(AdminServiceImplTest2.class);

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
                    .withEnv("RABBITMQ_DEFAULT_VHOST", "IUDX-INTERNAL");

    private AdminServiceImpl adminService;
    private PostgresService postgresService;
    private DataBrokerService dataBrokerService;

    private RabbitMQClient rabbitMQClient;
    @BeforeAll
    void setup(Vertx vertx, VertxTestContext testContext) {

        String jdbcUrl = POSTGRES.getJdbcUrl();
        try (Connection conn =
                     DriverManager.getConnection(jdbcUrl, POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement stmt = conn.createStatement()) {

            String createTrigger =
                    """
                                      CREATE OR REPLACE TRIGGER update_ad_created
                                      BEFORE INSERT
                                      ON unique_attributes
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

            String createTableQuery =
                    """
                    CREATE TABLE IF NOT EXISTS public.unique_attributes
                    (
                        _id uuid NOT NULL DEFAULT uuid_generate_v4(),
                        resource_id character varying COLLATE pg_catalog."default" NOT NULL,
                        unique_attribute character varying COLLATE pg_catalog."default" NOT NULL,
                        created_at timestamp without time zone NOT NULL,
                        modified_at timestamp without time zone NOT NULL,
                        CONSTRAINT unique_attrib_pk PRIMARY KEY (_id),
                        CONSTRAINT resource_id_unique UNIQUE (resource_id)
                    );
                    """;
            String createTableQuery2 =
                    """
                        CREATE TABLE IF NOT EXISTS public.revoked_tokens
                          (
                              _id uuid NOT NULL,
                              expiry timestamp without time zone NOT NULL,
                              created_at timestamp without time zone NOT NULL,
                              modified_at timestamp without time zone NOT NULL,
                              CONSTRAINT revoke_tokens_pk PRIMARY KEY (_id)
                          );
                        """;
            String enableExtension = "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";";

            stmt.execute(enableExtension);
            stmt.execute(createFunction);
            stmt.execute(createTableQuery);
            stmt.execute(createTableQuery2);
            stmt.execute(createTrigger);

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
                        .put("brokerAmqpPort",rabbitMQContainer.getMappedPort(15672))
                        .put("brokerAmqpIp", rabbitMQContainer.getHost());
        RabbitMQOptions rmqOptions =
                new RabbitMQOptions()
                        .setUser("guest")
                        .setPassword("guest")
                        .setVirtualHost("IUDX-INTERNAL")
                        .setHost(rabbitMQContainer.getHost())
                        .setPort(rabbitMQContainer.getAmqpPort());

        rabbitMQClient = RabbitMQClient.create(vertx, rmqOptions);

        vertx
                .deployVerticle(new PostgresVerticle(), new DeploymentOptions().setConfig(config))
                .compose(
                        pgId -> {
                            return vertx.deployVerticle(
                                    new DataBrokerVerticle(), new DeploymentOptions().setConfig(databrokerConfig));
                        }).compose(
                        dbId -> {
                            LOGGER.info("in starting ");
                            LOGGER.info("RabbitWebClient connecting to {}:{}", rabbitMQContainer.getHost(), rabbitMQContainer.getMappedPort(15672));
                            return rabbitMQClient.start().timeout(5000, TimeUnit.MILLISECONDS);
                        })
                .compose(
                        v -> {
                            LOGGER.info("started");
                            postgresService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);
                            dataBrokerService = DataBrokerService.createProxy(vertx, DATA_BROKER_SERVICE_ADDRESS);
                            adminService = new AdminServiceImpl(postgresService, dataBrokerService);
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            return dataBrokerService.registerExchange(
                                    "fd47486b-3497-4248-ac1e-082e4d37a66c",
                                    "latest-data-unique-attributes",
                                    Vhosts.IUDX_INTERNAL
                            );
                        }).onSuccess(
                        v -> {
                            LOGGER.info("AdminService deployed" + v.toJson());
                            testContext.completeNow();
                        })
                .onFailure(
                        err -> {
                            LOGGER.error(err.getMessage());
                            testContext.failNow(err);
                        });
    }

    private Future<Void> createDefaultExchange(Vertx vertx){
        Promise<Void> promise = Promise.promise();
        WebClientOptions options = new WebClientOptions()
                .setDefaultHost(rabbitMQContainer.getHost())
                .setDefaultPort(rabbitMQContainer.getMappedPort(15672))
                .setSsl(false);

        WebClient client = WebClient.create(vertx, options);


        JsonObject config = new JsonObject()
                .put("auto_delete", false)
                .put("durable", true)
                .put("arguments", new JsonObject());

        client.put("/api/exchanges/IUDX-INTERNAL/latest-data-unique-attributes")
                .putHeader("Authorization", basicAuth("guest", "guest"))
                .sendJsonObject(config).onSuccess(res -> promise.complete()).onFailure(err -> promise.fail(err));
        return promise.future();
    }
    private String basicAuth(String username, String password) {
        String auth = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    }

  /*@Order(1)
  @Test
  void testRevokedToken_success(VertxTestContext testContext) {
    adminService.revokedTokenRequest("fd47486b-3497-4248-ac1e-082e4d37a66c")
            .onSuccess(v -> testContext.completeNow())
            .onFailure(err -> testContext.failNow(err));
  }*/

    @Order(2)
    @Test
    void testCreateUnique_success(VertxTestContext testContext) {
        adminService.createUniqueAttribute("8b95ab80-2aaf-4636-a65e-7f2563d0d371","license_plate")
                .onSuccess(v -> testContext.completeNow())
                .onFailure(err -> testContext.failNow(err));
    }
}
