package iudx.resource.server.databroker.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rabbitmq.RabbitMQClient;
import iudx.resource.server.databroker.model.*;
import iudx.resource.server.databroker.util.PermissionOpType;
import iudx.resource.server.databroker.util.RabbitClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@ExtendWith(VertxExtension.class)
class DataBrokerServiceImplTest {

  @Mock private RabbitClient rabbitClient;
  @Mock private RabbitMQClient iudxInternalRabbitMqClient;
  @Mock private RabbitMQClient iudxRabbitMqClient;

  private DataBrokerServiceImpl dataBrokerService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // Manually instantiate the service
    dataBrokerService =
        new DataBrokerServiceImpl(
            rabbitClient,
            "amqp://localhost",
            5672,
            "iudx_internal_vhost",
            "prod_vhost",
            "external_vhost",
            iudxInternalRabbitMqClient,
            iudxRabbitMqClient);
  }

  @Test
  void testDeleteQueue(VertxTestContext testContext) {
    when(rabbitClient.deleteQueue("testQueue", "prod_vhost")).thenReturn(Future.succeededFuture());

    dataBrokerService
        .deleteQueue("testQueue", "userId")
        .onComplete(testContext.succeeding(v -> testContext.completeNow()));
  }

  @Test
  void testRegisterQueue(VertxTestContext testContext) {
    UserResponseModel userResponse =
        new UserResponseModel(new JsonObject().put("password", "testPass"));
    when(rabbitClient.createUserIfNotExist("userId", "prod_vhost"))
        .thenReturn(Future.succeededFuture(userResponse));
    when(rabbitClient.createQueue("testQueue", "prod_vhost")).thenReturn(Future.succeededFuture());

    dataBrokerService
        .registerQueue("userId", "testQueue")
        .onComplete(
            testContext.succeeding(
                result -> {
                  assertEquals("testQueue", result.getQueueName());
                  testContext.completeNow();
                }));
  }

  @Test
  void testQueueBinding(VertxTestContext testContext) {
    when(rabbitClient.bindQueue("testExchange", "testQueue", "routingKey", "prod_vhost"))
        .thenReturn(Future.succeededFuture());

    dataBrokerService
        .queueBinding("testExchange", "testQueue", "routingKey")
        .onComplete(testContext.succeeding(v -> testContext.completeNow()));
  }

  @Test
  void testRegisterExchange(VertxTestContext testContext) {
    UserResponseModel userResponse =
        new UserResponseModel(new JsonObject().put("password", "testPass"));
    when(rabbitClient.createUserIfNotExist("userId", "prod_vhost"))
        .thenReturn(Future.succeededFuture(userResponse));
    when(rabbitClient.createExchange("testExchange", "prod_vhost"))
        .thenReturn(Future.succeededFuture());

    dataBrokerService
        .registerExchange("userId", "testExchange")
        .onComplete(
            testContext.succeeding(
                result -> {
                  assertEquals("testExchange", result.getExchangeName());
                  testContext.completeNow();
                }));
  }

  @Test
  void testDeleteExchange(VertxTestContext testContext) {
    when(rabbitClient.getExchange("testExchange", "prod_vhost"))
        .thenReturn(Future.succeededFuture());
    when(rabbitClient.deleteExchange("testExchange", "prod_vhost"))
        .thenReturn(Future.succeededFuture());

    dataBrokerService
        .deleteExchange("testExchange", "userId")
        .onComplete(testContext.succeeding(v -> testContext.completeNow()));
  }

  @Test
  void testUpdatePermission(VertxTestContext testContext) {
    when(rabbitClient.updateUserPermissions(
            "prod_vhost", "userId", PermissionOpType.ADD_READ, "testQueue"))
        .thenReturn(Future.succeededFuture());

    dataBrokerService
        .updatePermission("userId", "testQueue", PermissionOpType.ADD_READ)
        .onComplete(testContext.succeeding(v -> testContext.completeNow()));
  }

  @Test
  void testPublishFromAdaptor(VertxTestContext testContext) {
    JsonArray request = new JsonArray().add("message");
    when(iudxRabbitMqClient.basicPublish(eq("testExchange"), eq("routingKey"), any()))
        .thenReturn(Future.succeededFuture());

    dataBrokerService
        .publishFromAdaptor("testExchange", "routingKey", request)
        .onComplete(
            testContext.succeeding(
                result -> {
                  assertEquals("success", result);
                  testContext.completeNow();
                }));
  }

  @Test
  void testResetPassword(VertxTestContext testContext) {
    when(rabbitClient.resetPasswordInRmq(eq("userId"), anyString()))
        .thenReturn(Future.succeededFuture());

    dataBrokerService
        .resetPassword("userId")
        .onComplete(
            testContext.succeeding(
                password -> {
                  assertNotNull(password);
                  testContext.completeNow();
                }));
  }

  @Test
  void testPublishMessage(VertxTestContext testContext) {
    JsonObject message = new JsonObject().put("key", "value");
    when(iudxInternalRabbitMqClient.basicPublish(
            eq("testExchange"), eq("routingKey"), any(Buffer.class)))
        .thenReturn(Future.succeededFuture());

    dataBrokerService
        .publishMessage(message, "testExchange", "routingKey")
        .onComplete(testContext.succeeding(v -> testContext.completeNow()));
  }

  @Test
  void testListExchange(VertxTestContext testContext) {
    ExchangeSubscribersResponse response =
        new ExchangeSubscribersResponse(Map.of("testExchange", List.of("sub1", "sub2")));
    when(rabbitClient.listExchangeSubscribers("testExchange", "prod_vhost"))
        .thenReturn(Future.succeededFuture(response));

    dataBrokerService
        .listExchange("testExchange")
        .onComplete(
            testContext.succeeding(
                result -> {
                  assertEquals(2, result.getSubscribers().get("testExchange").size());
                  testContext.completeNow();
                }));
  }

  @Test
  void testListQueue(VertxTestContext testContext) {
    List<String> subscribers = List.of("user1", "user2");
    when(rabbitClient.listQueueSubscribers("testQueue", "prod_vhost"))
        .thenReturn(Future.succeededFuture(subscribers));

    dataBrokerService
        .listQueue("testQueue")
        .onComplete(
            testContext.succeeding(
                result -> {
                  assertEquals(2, result.size());
                  testContext.completeNow();
                }));
  }

  @Test
  void testExchangeSubscribersResponseToJson() {
    ExchangeSubscribersResponse response =
        new ExchangeSubscribersResponse(Map.of("exchange1", List.of("sub1", "sub2")));
    JsonObject json = response.toJson();
    assertEquals(2, json.getJsonArray("exchange1").size());
  }

  @Test
  void testExchangeSubscribersResponseFromJson() {
    JsonObject json = new JsonObject().put("exchange1", new JsonArray().add("sub1").add("sub2"));
    ExchangeSubscribersResponse response = new ExchangeSubscribersResponse(json);
    assertEquals(2, response.getSubscribers().get("exchange1").size());
  }

  @Test
  void testDeleteQueueFailure(VertxTestContext testContext) {
    when(rabbitClient.deleteQueue("testQueue", "prod_vhost"))
        .thenReturn(Future.failedFuture("Queue deletion failed"));

    dataBrokerService
        .deleteQueue("testQueue", "userId")
        .onComplete(
            testContext.failing(
                err -> {
                  assertEquals("Queue deletion failed", err.getMessage());
                  testContext.completeNow();
                }));
  }

  @Test
  void testRegisterQueueFailure(VertxTestContext testContext) {
    when(rabbitClient.createUserIfNotExist("userId", "prod_vhost"))
        .thenReturn(Future.failedFuture("User creation failed"));

    dataBrokerService
        .registerQueue("userId", "testQueue")
        .onComplete(
            testContext.failing(
                err -> {
                  assertEquals("User creation failed", err.getMessage());
                  testContext.completeNow();
                }));
  }

  @Test
  void testQueueBindingFailure(VertxTestContext testContext) {
    when(rabbitClient.bindQueue("testExchange", "testQueue", "routingKey", "prod_vhost"))
        .thenReturn(Future.failedFuture("Queue binding failed"));

    dataBrokerService
        .queueBinding("testExchange", "testQueue", "routingKey")
        .onComplete(
            testContext.failing(
                err -> {
                  assertEquals("Queue binding failed", err.getMessage());
                  testContext.completeNow();
                }));
  }

  @Test
  void testRegisterExchangeFailure(VertxTestContext testContext) {
    UserResponseModel userResponse =
        new UserResponseModel(new JsonObject().put("password", "testPass"));

    when(rabbitClient.createUserIfNotExist("userId", "prod_vhost"))
        .thenReturn(Future.succeededFuture(userResponse));

    when(rabbitClient.createExchange("testExchange", "prod_vhost"))
        .thenReturn(Future.failedFuture("Exchange creation failed"));

    dataBrokerService
        .registerExchange("userId", "testExchange")
        .onComplete(
            testContext.failing(
                err -> {
                  assertEquals("Exchange creation failed", err.getMessage());
                  testContext.completeNow();
                }));
  }

  @Test
  void testDeleteExchangeFailure(VertxTestContext testContext) {
    when(rabbitClient.getExchange("testExchange", "prod_vhost"))
        .thenReturn(Future.failedFuture("Exchange does not exist"));

    dataBrokerService
        .deleteExchange("testExchange", "userId")
        .onComplete(
            testContext.failing(
                err -> {
                  assertEquals("Exchange does not exist", err.getMessage());
                  testContext.completeNow();
                }));
  }

  @Test
  void testUpdatePermissionFailure(VertxTestContext testContext) {
    when(rabbitClient.updateUserPermissions(
            "prod_vhost", "userId", PermissionOpType.ADD_READ, "testQueue"))
        .thenReturn(Future.failedFuture("Permission update failed"));

    dataBrokerService
        .updatePermission("userId", "testQueue", PermissionOpType.ADD_READ)
        .onComplete(
            testContext.failing(
                err -> {
                  assertEquals("Permission update failed", err.getMessage());
                  testContext.completeNow();
                }));
  }

  @Test
  void testPublishFromAdaptorFailure(VertxTestContext testContext) {
    JsonArray request = new JsonArray().add("message");
    when(iudxRabbitMqClient.basicPublish(eq("testExchange"), eq("routingKey"), any()))
        .thenReturn(Future.failedFuture("Message publish failed"));

    dataBrokerService
        .publishFromAdaptor("testExchange", "routingKey", request)
        .onComplete(
            testContext.failing(
                err -> {
                  assertEquals("Message publish failed", err.getMessage());
                  testContext.completeNow();
                }));
  }

  @Test
  void testResetPasswordFailure(VertxTestContext testContext) {
    when(rabbitClient.resetPasswordInRmq(eq("userId"), anyString()))
        .thenReturn(Future.failedFuture("Password reset failed"));

    dataBrokerService
        .resetPassword("userId")
        .onComplete(
            testContext.failing(
                err -> {
                  assertEquals("Password reset failed", err.getMessage());
                  testContext.completeNow();
                }));
  }

  @Test
  void testPublishMessageFailure(VertxTestContext testContext) {
    JsonObject message = new JsonObject().put("key", "value");
    when(iudxInternalRabbitMqClient.basicPublish(
            eq("testExchange"), eq("routingKey"), any(Buffer.class)))
        .thenReturn(Future.failedFuture("Internal Server Error"));

    dataBrokerService
        .publishMessage(message, "testExchange", "routingKey")
        .onComplete(
            testContext.failing(
                err -> {
                  assertEquals("Internal Server Error", err.getMessage());
                  testContext.completeNow();
                }));
  }

  @Test
  void testListExchangeFailure(VertxTestContext testContext) {
    when(rabbitClient.listExchangeSubscribers("testExchange", "prod_vhost"))
        .thenReturn(Future.failedFuture("Exchange listing failed"));

    dataBrokerService
        .listExchange("testExchange")
        .onComplete(
            testContext.failing(
                err -> {
                  assertEquals("Exchange listing failed", err.getMessage());
                  testContext.completeNow();
                }));
  }

  @Test
  void testListQueueFailure(VertxTestContext testContext) {
    when(rabbitClient.listQueueSubscribers("testQueue", "prod_vhost"))
        .thenReturn(Future.failedFuture("Listing of Queue failed"));

    dataBrokerService
        .listQueue("testQueue")
        .onComplete(
            testContext.failing(
                err -> {
                  assertEquals("Listing of Queue failed", err.getMessage());
                  testContext.completeNow();
                }));
  }
}
