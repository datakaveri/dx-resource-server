package org.cdpg.dx.databroker.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.function.Supplier;

import io.vertx.serviceproxy.ServiceException;
import org.cdpg.dx.common.ErrorCode;
import org.cdpg.dx.common.ErrorMessage;
import org.cdpg.dx.databroker.client.RabbitClient;
import org.cdpg.dx.databroker.model.ExchangeSubscribersResponse;
import org.cdpg.dx.databroker.model.UserResponseModel;
import org.cdpg.dx.databroker.util.PermissionOpType;
import org.cdpg.dx.databroker.util.Util;
import org.cdpg.dx.databroker.util.Vhosts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

class DataBrokerServiceImplTest {
  private final String userId = "user1";
  private final String queueName = "queue1";
  private final String exchangeName = "exchange1";
  private final String routingKey = "key1";
  private final Vhosts vhost = Vhosts.IUDX_PROD;
  private final String vhostName = "vhost-prod";
  private final String amqpUrl = "localhost";
  private final int amqpPort = 5672;
  @Mock private RabbitClient rabbitClient;
  private DataBrokerServiceImpl dataBrokerService;
  @Mock
  private Supplier<String> passwordSupplier;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    dataBrokerService =
            new DataBrokerServiceImpl(
                    rabbitClient, amqpUrl, amqpPort, "internalVhost", vhostName, "externalVhost");
    Util.randomPassword = passwordSupplier;
  }

  @Test
  void testDeleteQueue_success() {
    when(rabbitClient.deleteQueue(queueName, vhostName)).thenReturn(Future.succeededFuture());
    var future = dataBrokerService.deleteQueue(queueName, userId, vhost);
    assertTrue(future.succeeded());
  }
  @Test
  void testRegisterQueue_success() {
    UserResponseModel userResponseModel = new UserResponseModel();
    when(rabbitClient.createUserIfNotExist(userId, vhostName))
            .thenReturn(Future.succeededFuture(userResponseModel));
    when(rabbitClient.createQueue(queueName, vhostName)).thenReturn(Future.succeededFuture());

    var future = dataBrokerService.registerQueue(userId, queueName, vhost);
    assertTrue(future.succeeded());
    assertEquals(queueName, future.result().getQueueName());
  }

  @Test
  void testQueueBinding_success() {
    when(rabbitClient.bindQueue(exchangeName, queueName, routingKey, vhostName))
            .thenReturn(Future.succeededFuture());

    var future = dataBrokerService.queueBinding(exchangeName, queueName, routingKey, vhost);
    assertTrue(future.succeeded());
  }

  @Test
  void testRegisterExchange_success() {
    UserResponseModel model = new UserResponseModel();
    when(rabbitClient.createUserIfNotExist(userId, vhostName))
            .thenReturn(Future.succeededFuture(model));
    when(rabbitClient.createExchange(exchangeName, vhostName)).thenReturn(Future.succeededFuture());

    var future = dataBrokerService.registerExchange(userId, exchangeName, vhost);
    assertTrue(future.succeeded());
  }

  @Test
  void testDeleteExchange_success() {
    when(rabbitClient.getExchange(exchangeName, vhostName)).thenReturn(Future.succeededFuture());
    when(rabbitClient.deleteExchange(exchangeName, vhostName)).thenReturn(Future.succeededFuture());

    var future = dataBrokerService.deleteExchange(exchangeName, userId, vhost);
    assertTrue(future.succeeded());
  }

  @Test
  void testListExchange_success() {
    ExchangeSubscribersResponse response = new ExchangeSubscribersResponse();
    when(rabbitClient.listExchangeSubscribers(exchangeName, vhostName))
            .thenReturn(Future.succeededFuture(response));

    var future = dataBrokerService.listExchange(exchangeName, vhost);
    assertTrue(future.succeeded());
    assertEquals(response, future.result());
  }

  @Test
  void testUpdatePermission_success() {
    when(rabbitClient.updateUserPermissions(vhostName, userId, PermissionOpType.ADD_READ, queueName))
            .thenReturn(Future.succeededFuture());

    var future =
            dataBrokerService.updatePermission(userId, queueName, PermissionOpType.ADD_READ, vhost);
    assertTrue(future.succeeded());
  }

  @Test
  void testListQueue_success() {
    List<String> mockList = List.of("sub1", "sub2");
    when(rabbitClient.listQueueSubscribers(queueName, vhostName))
            .thenReturn(Future.succeededFuture(mockList));

    var future = dataBrokerService.listQueue(queueName, vhost);
    assertTrue(future.succeeded());
    assertEquals(mockList, future.result());
  }

  @Test
  void testPublishMessageExternal_success() {
    JsonArray message = new JsonArray().add("data");
    when(rabbitClient.publishMessageExternal(exchangeName, routingKey, message))
            .thenReturn(Future.succeededFuture());

    var future = dataBrokerService.publishMessageExternal(exchangeName, routingKey, message);
    assertTrue(future.succeeded());
    assertEquals("success", future.result());
  }

  @Test
  void testResetPassword_Success() {
    String userId = "testUser";
    String mockPassword = "generatedPassword123";
    when(passwordSupplier.get()).thenReturn(mockPassword);
    when(rabbitClient.resetPasswordInRmq(eq(userId), eq(mockPassword))).thenReturn(Future.succeededFuture());
    Future<String> result = dataBrokerService.resetPassword(userId);
    assertTrue(result.succeeded());
    assertEquals(mockPassword, result.result());
    verify(rabbitClient).resetPasswordInRmq(userId, mockPassword);
    verify(passwordSupplier).get();
  }
  @Test
  void testResetPassword_Failure() {
    String userId = "testUser";
    String mockPassword = "generatedPassword123";
    String errorMessage = "RabbitMQ error";
    when(passwordSupplier.get()).thenReturn(mockPassword);

    when(rabbitClient.resetPasswordInRmq(eq(userId), eq(mockPassword)))
            .thenReturn(Future.failedFuture(errorMessage));
    Future<String> result = dataBrokerService.resetPassword(userId);

    assertTrue(result.failed());
    assertEquals(errorMessage, result.cause().getMessage());

    verify(rabbitClient).resetPasswordInRmq(userId, mockPassword);
    verify(passwordSupplier).get();
  }

  @Test
  void testPublishMessageInternal_success() {
    JsonObject body = new JsonObject().put("key", "value");
    when(rabbitClient.publishMessageInternal(body, exchangeName, routingKey))
            .thenReturn(Future.succeededFuture());

    var future = dataBrokerService.publishMessageInternal(body, exchangeName, routingKey);
    assertTrue(future.succeeded());
  }

  @Test
  void testDeleteQueue_failure() {
    String errorMessage = "Queue deletion failed";
    when(rabbitClient.deleteQueue(queueName, vhostName))
            .thenReturn(Future.failedFuture(errorMessage));

    var future = dataBrokerService.deleteQueue(queueName, userId, vhost);
    assertTrue(future.failed());
    assertEquals(errorMessage, future.cause().getMessage());
  }

  @Test
  void testRegisterQueue_failureOnUserCreation() {
    String errorMessage = "User creation failed";
    when(rabbitClient.createUserIfNotExist(userId, vhostName))
            .thenReturn(Future.failedFuture(errorMessage));

    var future = dataBrokerService.registerQueue(userId, queueName, vhost);
    assertTrue(future.failed());
    assertEquals(errorMessage, future.cause().getMessage());
  }

  @Test
  void testRegisterQueue_failureOnQueueCreation() {
    UserResponseModel userResponseModel = new UserResponseModel();
    String errorMessage = "Queue creation failed";
    when(rabbitClient.createUserIfNotExist(userId, vhostName))
            .thenReturn(Future.succeededFuture(userResponseModel));
    when(rabbitClient.createQueue(queueName, vhostName))
            .thenReturn(Future.failedFuture(errorMessage));

    var future = dataBrokerService.registerQueue(userId, queueName, vhost);
    assertTrue(future.failed());
    assertEquals(errorMessage, future.cause().getMessage());
  }

  @Test
  void testQueueBinding_failure() {
    String errorMessage = "Binding failed";
    when(rabbitClient.bindQueue(exchangeName, queueName, routingKey, vhostName))
            .thenReturn(Future.failedFuture(errorMessage));

    var future = dataBrokerService.queueBinding(exchangeName, queueName, routingKey, vhost);
    assertTrue(future.failed());
    assertEquals(errorMessage, future.cause().getMessage());
  }

  @Test
  void testRegisterExchange_failureOnUserCreation() {
    String errorMessage = "User creation failed";
    when(rabbitClient.createUserIfNotExist(userId, vhostName))
            .thenReturn(Future.failedFuture(errorMessage));

    var future = dataBrokerService.registerExchange(userId, exchangeName, vhost);
    assertTrue(future.failed());
    assertEquals(errorMessage, future.cause().getMessage());
  }

  @Test
  void testRegisterExchange_failureOnExchangeCreation() {
    UserResponseModel model = new UserResponseModel();
    String errorMessage = "Exchange creation failed";
    when(rabbitClient.createUserIfNotExist(userId, vhostName))
            .thenReturn(Future.succeededFuture(model));
    when(rabbitClient.createExchange(exchangeName, vhostName))
            .thenReturn(Future.failedFuture(errorMessage));

    var future = dataBrokerService.registerExchange(userId, exchangeName, vhost);
    assertTrue(future.failed());
    assertEquals(errorMessage, future.cause().getMessage());
  }

  @Test
  void testDeleteExchange_failureOnGetExchange() {
    String errorMessage = "Exchange not found";
    when(rabbitClient.getExchange(exchangeName, vhostName))
            .thenReturn(Future.failedFuture(errorMessage));

    var future = dataBrokerService.deleteExchange(exchangeName, userId, vhost);
    assertTrue(future.failed());
    assertEquals(errorMessage, future.cause().getMessage());
  }

  @Test
  void testDeleteExchange_failureOnDelete() {
    String errorMessage = "Deletion failed";
    when(rabbitClient.getExchange(exchangeName, vhostName))
            .thenReturn(Future.succeededFuture());
    when(rabbitClient.deleteExchange(exchangeName, vhostName))
            .thenReturn(Future.failedFuture(errorMessage));

    var future = dataBrokerService.deleteExchange(exchangeName, userId, vhost);
    assertTrue(future.failed());
    assertEquals(errorMessage, future.cause().getMessage());
  }

  @Test
  void testListExchange_failure() {
    String errorMessage = "List subscribers failed";
    when(rabbitClient.listExchangeSubscribers(exchangeName, vhostName))
            .thenReturn(Future.failedFuture(errorMessage));

    var future = dataBrokerService.listExchange(exchangeName, vhost);
    assertTrue(future.failed());
    assertEquals(errorMessage, future.cause().getMessage());
  }

  @Test
  void testUpdatePermission_failure() {
    String errorMessage = "Permission update failed";
    when(rabbitClient.updateUserPermissions(vhostName, userId, PermissionOpType.ADD_READ, queueName))
            .thenReturn(Future.failedFuture(errorMessage));

    var future = dataBrokerService.updatePermission(userId, queueName, PermissionOpType.ADD_READ, vhost);
    assertTrue(future.failed());
    assertEquals(errorMessage, future.cause().getMessage());
  }

  @Test
  void testListQueue_failure() {
    String errorMessage = "List subscribers failed";
    when(rabbitClient.listQueueSubscribers(queueName, vhostName))
            .thenReturn(Future.failedFuture(errorMessage));

    var future = dataBrokerService.listQueue(queueName, vhost);
    assertTrue(future.failed());
    assertTrue(future.cause() instanceof ServiceException);
    assertEquals(ErrorCode.ERROR_BAD_REQUEST, ((ServiceException) future.cause()).failureCode());
  }

  @Test
  void testPublishMessageExternal_failure() {
    JsonArray message = new JsonArray().add("data");
    String errorMessage = "Publish failed";
    when(rabbitClient.publishMessageExternal(exchangeName, routingKey, message))
            .thenReturn(Future.failedFuture(errorMessage));

    var future = dataBrokerService.publishMessageExternal(exchangeName, routingKey, message);
    assertTrue(future.failed());
    assertEquals(errorMessage, future.cause().getMessage());
  }

  @Test
  void testPublishMessageInternal_failure() {
    JsonObject body = new JsonObject().put("key", "value");
    when(rabbitClient.publishMessageInternal(body, exchangeName, routingKey))
            .thenReturn(Future.failedFuture("Publish failed"));

    var future = dataBrokerService.publishMessageInternal(body, exchangeName, routingKey);
    assertTrue(future.failed());
    assertTrue(future.cause() instanceof ServiceException);
    assertEquals(ErrorCode.ERROR_INTERNAL_SERVER, ((ServiceException) future.cause()).failureCode());
    assertEquals(ErrorMessage.INTERNAL_SERVER_ERROR, ((ServiceException) future.cause()).getMessage());
  }
}
