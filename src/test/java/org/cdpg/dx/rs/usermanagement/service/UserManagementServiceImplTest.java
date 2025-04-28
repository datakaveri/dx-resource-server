package org.cdpg.dx.rs.usermanagement.service;

import static org.cdpg.dx.common.util.ProxyAddressConstants.DATA_BROKER_SERVICE_ADDRESS;
import static org.junit.jupiter.api.Assertions.*;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rabbitmq.RabbitMQClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.databroker.DataBrokerVerticle;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.databroker.util.Vhosts;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserManagementServiceImplTest {
  private static final Logger LOGGER = LogManager.getLogger(UserManagementServiceImplTest.class);

  @Container
  private static final RabbitMQContainer rabbitMQContainer =
      new RabbitMQContainer("rabbitmq:4.0-management")
          .withExposedPorts(5672, 15672)
          .withEnv("RABBITMQ_DEFAULT_USER", "guest")
          .withEnv("RABBITMQ_DEFAULT_PASS", "guest")
          .withEnv("RABBITMQ_DEFAULT_VHOST", "IUDX");

  private UserManagementServiceImpl userManagementService;
  private DataBrokerService dataBrokerService;
  private RabbitMQClient rabbitMQClient;

  @BeforeAll
  void setup(Vertx vertx, VertxTestContext testContext) {

    deployVerticles(vertx)
        .onSuccess(
            v -> {
              LOGGER.info("All setup completed successfully");
              testContext.completeNow();
            })
        .onFailure(
            err -> {
              LOGGER.error("Setup failed: {}", err.getMessage(), err);
              testContext.failNow(err);
            });
  }

  private Future<Void> deployVerticles(Vertx vertx) {
    Promise<Void> promise = Promise.promise();

    JsonObject databrokerConfig =
        new JsonObject()
            .put("dataBrokerIP", rabbitMQContainer.getHost())
            .put("dataBrokerPort", rabbitMQContainer.getMappedPort(5672)) // AMQP port
            .put("prodVhost", "IUDX")
            .put("internalVhost", "IUDX-INTERNAL")
            .put("externalVhost", "IUDX-EXTERNAL")
            .put("dataBrokerUserName", "guest")
            .put("dataBrokerPassword", "guest")
            .put(
                "dataBrokerManagementPort",
                rabbitMQContainer.getMappedPort(15672)) // Management port
            .put("connectionTimeout", 10000) // Increased timeout
            .put("requestedHeartbeat", 60) // Increased heartbeat
            .put("handshakeTimeout", 10000) // Increased timeout
            .put("requestedChannelMax", 5)
            .put("networkRecoveryInterval", 5000) // Increased recovery interval
            .put("automaticRecoveryEnabled", true)
            .put("brokerAmqpPort", rabbitMQContainer.getMappedPort(5672)) // AMQP port
            .put("brokerAmqpIp", rabbitMQContainer.getHost());

    vertx
        .deployVerticle(
            new DataBrokerVerticle(), new DeploymentOptions().setConfig(databrokerConfig))
        .onSuccess(v -> {
            dataBrokerService = DataBrokerService.createProxy(vertx, DATA_BROKER_SERVICE_ADDRESS);
            userManagementService = new UserManagementServiceImpl(dataBrokerService);
            promise.complete();
        })
        .onFailure(err -> promise.fail(err));

    return promise.future();
  }


  @Test
    void testResetPassword(VertxTestContext testContext) {
      userManagementService.resetPassword("fd47486b-3497-4248-ac1e-082e4d37a66c")
              .onSuccess(v -> testContext.completeNow())
              .onFailure(err -> testContext.failNow(err));
  }
}
