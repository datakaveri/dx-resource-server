package org.cdpg.dx.databroker.client;

import static org.cdpg.dx.container.RMQContainer.rabbitMQContainer;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rabbitmq.RabbitMQClient;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.container.RMQContainer;
import org.cdpg.dx.databroker.util.PermissionOpType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
// @Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(VertxExtension.class)
public class RabbitClientTest {
  private static final Logger LOGGER = LogManager.getLogger(RabbitClientTest.class);
  private static final String USERNAME = "guest";
  private static final String PASSWORD = "guest";
  private static final String VHOST = "iudx";
  Vertx vertx;

  /*@Container
  private static final RabbitMQContainer rabbitMQContainer =
      new RabbitMQContainer("rabbitmq:4.0-management")
          .withExposedPorts(5672, 15672)
          .withEnv("RABBITMQ_DEFAULT_USER", USERNAME)
          .withEnv("RABBITMQ_DEFAULT_PASS", PASSWORD)
          .withEnv("RABBITMQ_DEFAULT_VHOST", VHOST)
          .withNetwork(Network.newNetwork());*/

  private RabbitWebClient rabbitWebClient;
  private RabbitMQClient rabbitMQClient;
  private RabbitClient rabbitClient;
  private RMQContainer rmqContainer;

  @BeforeAll
  void setup() throws Exception {
    vertx = Vertx.vertx();
    rmqContainer = new RMQContainer(vertx);
    LOGGER.info(
        "rabbit class  {}:{} ::::{}",
        rmqContainer.getContainer().getHost(),
        rmqContainer.getContainer().getMappedPort(15672),
        rmqContainer.getContainer().getAmqpPort());
    rabbitClient = rmqContainer.getRabbitClient();
  }

  /*@AfterAll
  void tearDown() {
      vertx.close();
      LOGGER.debug("RMQ container stopped called");
      rmqContainer.stop();
  }*/

  @AfterAll
  void tearDown() {
    vertx.close();
    rmqContainer.stop();
  }

  @Order(1)
  @Test
  @DisplayName("Should create user if not exists")
  void testCreateUserIfNotExist(VertxTestContext testContext) {
    rabbitClient
        .createUserIfNotExist(USERNAME, VHOST)
        .onSuccess(
            v -> {
              testContext.completeNow();
            })
        .onFailure(
            err -> {
              testContext.failNow(err);
            });
  }

  @Order(2)
  @Test
  @DisplayName("user if exists")
  void testCreateUserIfNotExist2(VertxTestContext testContext) {
    rabbitClient
        .createUserIfNotExist(USERNAME, VHOST)
        .onSuccess(
            v -> {
              testContext.completeNow();
            })
        .onFailure(
            err -> {
              testContext.failNow(err);
            });
  }

  @Order(3)
  @Test
  @DisplayName("create queue")
  void testCreateQueue(VertxTestContext testContext) {
    String queueName = "queue_test";

    rabbitClient
        .createQueue(queueName, VHOST)
        .onSuccess(
            v -> {
              testContext.completeNow();
            })
        .onFailure(
            err -> {
              testContext.failNow(err);
            });
  }

  @Order(4)
  @Test
  @DisplayName("list queue ")
  void testListQueueSubscribers(VertxTestContext testContext) {
    String queueName = "queue_test";

    rabbitClient
        .listQueueSubscribers(queueName, VHOST)
        .onSuccess(
            v -> {
              testContext.completeNow();
            })
        .onFailure(
            err -> {
              testContext.failNow(err);
            });
  }

  @Order(5)
  @Test
  @DisplayName("create queue conflict")
  void testCreateQueueConflict(VertxTestContext testContext) {
    String queueName = "queue_test";

    rabbitClient
        .createQueue(queueName, VHOST)
        .onSuccess(
            v -> {
              testContext.failed();
            })
        .onFailure(
            err -> {
              testContext.completeNow();
            });
  }

  @Order(6)
  @Test
  @DisplayName("exchange create not exist")
  void testCreateExchangeNotExist(VertxTestContext testContext) {
    String exchangeName = "queue_exchange";

    rabbitClient
        .createExchange(exchangeName, VHOST)
        .onSuccess(
            v -> {
              testContext.completeNow();
            })
        .onFailure(
            err -> {
              testContext.failNow(err);
            });
  }

  @Order(7)
  @Test
  @DisplayName("exchange create exist")
  void testCreateExchangeExist(VertxTestContext testContext) {
    String exchangeName = "queue_exchange";

    rabbitClient
        .createExchange(exchangeName, VHOST)
        .onSuccess(
            v -> {
              testContext.failed();
            })
        .onFailure(
            err -> {
              testContext.completeNow();
            });
  }

  @Order(8)
  @Test
  @DisplayName("bind queue exist")
  void testBindQueue(VertxTestContext testContext) {
    String queueName = "queue_test";
    String exchangeName = "queue_exchange";
    String routingKey = "routing";

    rabbitClient
        .bindQueue(exchangeName, queueName, routingKey, VHOST)
        .onSuccess(
            v -> {
              testContext.completeNow();
            })
        .onFailure(
            err -> {
              testContext.failNow(err);
            });
  }

  @Order(9)
  @Test
  @DisplayName("bind queue not exists")
  void testBindQueueFailed(VertxTestContext testContext) {
    String queueName = "queue_test2";
    String exchangeName = "queue_exchange";
    String routingKey = "routing";

    rabbitClient
        .bindQueue(exchangeName, queueName, routingKey, VHOST)
        .onSuccess(
            v -> {
              testContext.failed();
            })
        .onFailure(
            err -> {
              testContext.completeNow();
            });
  }

  @Order(10)
  @Test
  @DisplayName("Update User Permissions")
  void testUpdateUserPermissions(VertxTestContext testContext) {
    String queueName = "queue_test";
    rabbitClient
        .updateUserPermissions(VHOST, USERNAME, PermissionOpType.ADD_READ, queueName)
        .onSuccess(
            v -> {
              testContext.completeNow();
            })
        .onFailure(
            err -> {
              testContext.failNow(err);
            });
  }

  @Order(11)
  @Test
  @DisplayName("Update User Permissions Failed")
  void testUpdateUserPermissionsNotExist(VertxTestContext testContext) {
    String userId = "test_user";
    String queueName = "queue_test";
    rabbitClient
        .updateUserPermissions(VHOST, userId, PermissionOpType.ADD_READ, queueName)
        .onSuccess(
            v -> {
              testContext.failed();
            })
        .onFailure(
            err -> {
              testContext.completeNow();
            });
  }

  @Order(12)
  @Test
  @DisplayName("Update User Permissions: Delete")
  void testUpdateUserPermissionsDelete(VertxTestContext testContext) {
    String queueName = "queue_test";
    rabbitClient
        .updateUserPermissions(VHOST, USERNAME, PermissionOpType.DELETE_READ, queueName)
        .onSuccess(
            v -> {
              testContext.completeNow();
            })
        .onFailure(
            err -> {
              testContext.failNow(err);
            });
  }

  @Order(13)
  @Test
  @DisplayName("Get exchange")
  void testGetExchange(VertxTestContext testContext) {
    String exchangeName = "queue_exchange";
    rabbitClient
        .getExchange(exchangeName, VHOST)
        .onSuccess(
            v -> {
              testContext.completeNow();
            })
        .onFailure(
            err -> {
              testContext.failNow(err);
            });
  }

  @Order(14)
  @Test
  @DisplayName("Get exchange not found")
  void testGetExchangeNotFound(VertxTestContext testContext) {
    String exchangeName = "queue_exchange3";
    rabbitClient
        .getExchange(exchangeName, VHOST)
        .onSuccess(
            v -> {
              testContext.failed();
            })
        .onFailure(
            err -> {
              testContext.completeNow();
            });
  }

  @Order(15)
  @Test
  @DisplayName("List Exchange Subscribers")
  void testListExchangeSubscribers(VertxTestContext testContext) {
    String exchangeName = "queue_exchange";
    rabbitClient
        .listExchangeSubscribers(exchangeName, VHOST)
        .onSuccess(
            v -> {
              testContext.completeNow();
            })
        .onFailure(
            err -> {
              testContext.failNow(err);
            });
  }

  @Order(16)
  @Test
  @DisplayName("Delete Exchange")
  void testDeleteExchange(VertxTestContext testContext) {
    String exchangeName = "queue_exchange";
    rabbitClient
        .deleteExchange(exchangeName, VHOST)
        .onSuccess(
            v -> {
              testContext.completeNow();
            })
        .onFailure(
            err -> {
              testContext.failNow(err);
            });
  }

  @Order(17)
  @Test
  @DisplayName("delete queue exists")
  void testDeleteQueueExist(VertxTestContext testContext) {
    String queueName = "queue_test";
    rabbitClient
        .deleteQueue(queueName, VHOST)
        .onSuccess(
            v -> {
              testContext.completeNow();
            })
        .onFailure(
            err -> {
              testContext.failNow(err);
            });
  }

  @Order(18)
  @Test
  @DisplayName("delete queue not exists")
  void testDeleteQueue(VertxTestContext testContext) {
    String queueName = "queue_test1";

    rabbitClient
        .deleteQueue(queueName, VHOST)
        .onFailure(
            v -> {
              testContext.completeNow();
            });
  }

  @Order(19)
  @Test
  @DisplayName("Delete Exchange not found")
  void testDeleteExchangeNotFound(VertxTestContext testContext) {
    String exchangeName = "queue_exchange5";
    rabbitClient
        .deleteExchange(exchangeName, VHOST)
        .onSuccess(
            v -> {
              testContext.failed();
            })
        .onFailure(
            err -> {
              testContext.completeNow();
            });
  }

  @Order(20)
  @Test
  @DisplayName("Reset password in rmq")
  void testResetPassword(VertxTestContext testContext) {
    rabbitClient
        .resetPasswordInRmq(USERNAME, VHOST)
        .onSuccess(
            v -> {
              testContext.completeNow();
            })
        .onFailure(
            err -> {
              testContext.failNow(err);
            });
  }

  @Order(21)
  @Test
  @DisplayName("publish message internal")
  void testPublishMessageInternal(VertxTestContext testContext) {
    String exchangeName = "test_exchange";
    String routingKey = "routing";
    JsonObject body = new JsonObject();
    rabbitClient
        .publishMessageInternal(body, exchangeName, routingKey)
        .onSuccess(
            v -> {
              testContext.completeNow();
            })
        .onFailure(
            err -> {
              testContext.failNow(err);
            });
  }

  @Order(22)
  @Test
  @DisplayName("publish message external")
  void testPublishMessageExternal(VertxTestContext testContext) {
    String exchangeName = "test_exchange";
    String routingKey = "routing";
    JsonArray body = new JsonArray();
    rabbitClient
        .publishMessageExternal(exchangeName, routingKey, body)
        .onSuccess(
            v -> {
              testContext.completeNow();
            })
        .onFailure(
            err -> {
              testContext.failNow(err);
            });
  }

  @Order(23)
  @Test
  @DisplayName("All operations should fail after container shutdown")
  void testFailuresUsingCheckpoints(VertxTestContext context) {
    rabbitMQContainer.stop();

    int expectedFailures = 11;
    Checkpoint checkpoint = context.checkpoint(expectedFailures);

    String queueName = "queue_test";
    String exchangeName = "queue_exchange";
    String userId = "integrationUser";

    Consumer<Future<?>> checkFail =
        future ->
            future
                .onSuccess(v -> context.failNow(new RuntimeException("Expected failure")))
                .onFailure(err -> checkpoint.flag());

    checkFail.accept(rabbitClient.createUserIfNotExist(userId, VHOST));
    checkFail.accept(rabbitClient.createQueue(queueName, VHOST));
    checkFail.accept(rabbitClient.listQueueSubscribers(queueName, VHOST));
    checkFail.accept(rabbitClient.createExchange(exchangeName, VHOST));
    checkFail.accept(rabbitClient.bindQueue(exchangeName, queueName, "routing", VHOST));
    checkFail.accept(
        rabbitClient.updateUserPermissions(VHOST, userId, PermissionOpType.ADD_READ, queueName));
    checkFail.accept(rabbitClient.getExchange(exchangeName, VHOST));
    checkFail.accept(rabbitClient.listExchangeSubscribers(exchangeName, VHOST));
    checkFail.accept(rabbitClient.deleteExchange(exchangeName, VHOST));
    checkFail.accept(rabbitClient.deleteQueue(queueName, VHOST));
    checkFail.accept(rabbitClient.resetPasswordInRmq(userId, VHOST));
  }
}
