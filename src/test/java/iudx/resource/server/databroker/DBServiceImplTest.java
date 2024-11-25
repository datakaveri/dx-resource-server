package iudx.resource.server.databroker;

import static iudx.resource.server.databroker.util.Constants.ID;
import static iudx.resource.server.databroker.util.Constants.USER_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import iudx.resource.server.cache.CacheService;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class DBServiceImplTest {

  JsonObject request;
  String throwableMessage;
  DataBrokerServiceImpl databroker;
  String vHost;
  @Mock Future<JsonObject> jsonObjectFuture;
  @Mock AsyncResult<JsonObject> asyncResult;
  @Mock Throwable throwable;
  @Mock RabbitClient webClient;
  @Mock PostgresClient pgClient;
  @Mock RabbitMQClient rabbitMQClient, iudxRabbitMQClient;
  @Mock AsyncResult<Void> asyncResult1;
  DataBrokerServiceImpl databrokerSpy;
  JsonObject expected;
  @Mock CacheService cacheService;
  @Mock RabbitMQOptions iudxConfig;
  @Mock Vertx vertx;

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) {
    vHost = "IUDX_INTERNAL";
    JsonObject config = mock(JsonObject.class);
    request = new JsonObject();
    expected = new JsonObject();
    request.put("Dummy key", "Dummy value");
    request.put(ID, "Dummy ID");
    request.put("status", "Dummy status");
    request.put("routingKey", "routingKeyValue");
    request.put("type", HttpStatus.SC_OK);
    throwableMessage = "Dummy failure message";
    when(config.getString(anyString())).thenReturn("internalVhost");
    databroker =
        new DataBrokerServiceImpl(
            webClient, pgClient, config, cacheService, /* iudxConfig, vertx,*/ iudxRabbitMQClient);
    databrokerSpy = spy(databroker);
    vertxTestContext.completeNow();
  }

  public JsonObject expected_success() {
    expected.put("Dummy key", "Dummy value");
    expected.put("id", "Dummy ID");
    expected.put("status", "Dummy status");
    expected.put("type", 200);
    expected.put("routingKey", "routingKeyValue");
    return expected;
  }

  public JsonObject expected_failure() {
    expected.put("type", 400);
    expected.put("title", "Bad Request data");
    expected.put("detail", "Bad Request data");
    return expected;
  }

  @Test
  @Order(2)
  @DisplayName("Test publishFromAdaptor method : Success")
  public void test_publishFromAdaptor_Success(VertxTestContext vertxTestContext) {

    request = new JsonObject();
    request.put("Dummy key", "Dummy value");
    request.put(ID, "Dummy/ID/abcd/abcd");
    request.put("status", "Dummy status");
    request.put("routingKey", "routingKeyValue");
    request.put("type", HttpStatus.SC_OK);
    request.put("entities", new JsonArray().add("5b7556b5-0779-4c47-9cf2-3f209779aa22"));
    /*when(webClient.getRabbitmqClient()).thenReturn(rabbitMQClient);*/
    when(asyncResult1.succeeded()).thenReturn(true);

    JsonObject providerJson =
        new JsonObject()
            .put("provider", "8b95ab80-2aaf-4636-a65e-7f2563d0d371")
            .put("id", "5b7556b5-0779-4c47-9cf2-3f209779aa22")
            .put("resourceGroup", "dummy_resource");

    when(cacheService.get(any())).thenReturn(Future.succeededFuture(providerJson));

    doAnswer(
            new Answer<AsyncResult<Void>>() {
              @Override
              public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg0.getArgument(3)).handle(asyncResult1);
                return null;
              }
            })
        .when(iudxRabbitMQClient)
        .basicPublish(anyString(), anyString(), any(Buffer.class), any(Handler.class));
    expected.put("status", 200);

    databroker.publishFromAdaptor(
        request,
        vHost,
        handler -> {
          if (handler.succeeded()) {
            assertEquals(expected, handler.result());
            assertEquals(200, handler.result().getInteger("status"));
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(3)
  @DisplayName("Test publishHeartbeat method : Success")
  public void test_publishHeartbeat_success(VertxTestContext vertxTestContext) {
    JsonObject queue = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add("Dummy status");
    queue.put("efgh", jsonArray);

    when(webClient.getExchange(any(), anyString())).thenReturn(jsonObjectFuture);
    when(asyncResult.result()).thenReturn(request, queue);
    when(webClient.listExchangeSubscribers(any(), anyString())).thenReturn(jsonObjectFuture);
    when(webClient.getRabbitmqClient()).thenReturn(rabbitMQClient);
    when(asyncResult1.succeeded()).thenReturn(true);

    doAnswer(
            new Answer<AsyncResult<Void>>() {
              @Override
              public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg0.getArgument(3)).handle(asyncResult1);
                return null;
              }
            })
        .when(rabbitMQClient)
        .basicPublish(anyString(), anyString(), any(Buffer.class), any(Handler.class));
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(jsonObjectFuture)
        .onComplete(any());
    expected.put("type", "success");
    expected.put("queueName", "efgh");
    expected.put("routingKey", "Dummy status");
    expected.put("detail", "routingKey matched");
    databroker.publishHeartbeat(
        request,
        vHost,
        handler -> {
          if (handler.succeeded()) {
            assertEquals("success", handler.result().getString("type"));
            assertEquals(expected, handler.result());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(4)
  @DisplayName("Test publishHeartbeat method : Failure")
  public void test_publishHeartbeat_failure(VertxTestContext vertxTestContext) {
    JsonObject queue = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add("Dummy status");
    queue.put("efgh", jsonArray);

    when(webClient.getExchange(any(), anyString())).thenReturn(jsonObjectFuture);
    when(asyncResult.result()).thenReturn(request, queue);
    when(webClient.listExchangeSubscribers(any(), anyString())).thenReturn(jsonObjectFuture);
    when(webClient.getRabbitmqClient()).thenReturn(rabbitMQClient);
    when(asyncResult1.succeeded()).thenReturn(false);
    when(asyncResult1.cause()).thenReturn(throwable);

    doAnswer(
            new Answer<AsyncResult<Void>>() {
              @Override
              public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg0.getArgument(3)).handle(asyncResult1);
                return null;
              }
            })
        .when(rabbitMQClient)
        .basicPublish(anyString(), anyString(), any(Buffer.class), any(Handler.class));
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(jsonObjectFuture)
        .onComplete(any());
    expected.put("messagePublished", "failed");
    expected.put("type", "error");
    expected.put("detail", "routingKey not matched");
    databroker.publishHeartbeat(
        request,
        vHost,
        handler -> {
          if (handler.failed()) {
            System.out.println(handler.cause().getMessage());
            assertEquals(expected.toString(), handler.cause().getMessage());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(5)
  @DisplayName("Test publishHeartbeat method : routingKey mismatch")
  public void test_publishHeartbeat_with_routingKey_mismatch(VertxTestContext vertxTestContext) {
    JsonObject queue = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add("status");
    queue.put("efgh", jsonArray);

    when(webClient.getExchange(any(), anyString())).thenReturn(jsonObjectFuture);
    when(asyncResult.result()).thenReturn(request, queue);
    when(webClient.listExchangeSubscribers(any(), anyString())).thenReturn(jsonObjectFuture);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(jsonObjectFuture)
        .onComplete(any());
    databroker.publishHeartbeat(
        request,
        vHost,
        handler -> {
          if (handler.failed()) {
            assertEquals(
                "publishHeartbeat - routingKey [ Dummy status ] not matched with [ status ] for queue [ efgh ]",
                handler.cause().getMessage());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(6)
  @DisplayName("Test publishHeartbeat method : with empty queue")
  public void test_publishHeartbeat_with_empty_queue(VertxTestContext vertxTestContext) {
    JsonObject queue = new JsonObject();
    when(webClient.getExchange(any(), anyString())).thenReturn(jsonObjectFuture);
    when(asyncResult.result()).thenReturn(request, queue);
    when(webClient.listExchangeSubscribers(any(), anyString())).thenReturn(jsonObjectFuture);

    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(jsonObjectFuture)
        .onComplete(any());
    databroker.publishHeartbeat(
        request,
        vHost,
        handler -> {
          if (handler.failed()) {
            assertEquals(
                "publishHeartbeat method - Oops !! None queue bound with given exchange",
                handler.cause().getMessage());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(7)
  @DisplayName("Test publishHeartbeat method : when routing key is empty")
  public void test_publishHeartbeat_with_empty_routingKey(VertxTestContext vertxTestContext) {
    JsonObject queue = new JsonObject();
    request = new JsonObject();
    request.put("Dummy key", "Dummy value");
    request.put(ID, "Dummy ID");
    request.put("status", "");
    request.put("routingKey", "routingKeyValue");
    request.put("type", HttpStatus.SC_NOT_FOUND);
    databroker.publishHeartbeat(
        request,
        vHost,
        handler -> {
          if (handler.failed()) {
            assertEquals(
                "publishHeartbeat - adaptor and routingKey not provided to publish message",
                handler.cause().getMessage());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(8)
  @DisplayName("Test publishHeartbeat method : when adapter does not exist")
  public void test_publishHeartbeat_with_non_existing_adapter(VertxTestContext vertxTestContext) {
    JsonObject queue = new JsonObject();
    request = new JsonObject();
    request.put("Dummy key", "Dummy value");
    request.put(ID, "Dummy ID");
    request.put("status", "Dummy status");
    request.put("routingKey", "routingKeyValue");
    request.put("type", HttpStatus.SC_NOT_FOUND);
    when(webClient.getExchange(any(), anyString())).thenReturn(jsonObjectFuture);
    when(asyncResult.result()).thenReturn(request, queue);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(jsonObjectFuture)
        .onComplete(any());
    databroker.publishHeartbeat(
        request,
        vHost,
        handler -> {
          if (handler.failed()) {
            assertEquals(
                "Either adaptor does not exist or some other error to publish message",
                handler.cause().getMessage());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(9)
  @DisplayName("Test publishHeartbeat method : when request is NULL")
  public void test_publishHeartbeat_with_NULL_request(VertxTestContext vertxTestContext) {

    databroker.publishHeartbeat(
        null,
        vHost,
        handler -> {
          if (handler.failed()) {
            assertEquals(
                "publishHeartbeat - request is null to publish message",
                handler.cause().getMessage());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(10)
  @DisplayName("Test resetPassword method : Failure")
  public void test_resetPassword_failure(VertxTestContext vertxTestContext) {

    request.put(USER_ID, "Dummy User ID");
    doAnswer(Answer -> Future.succeededFuture(true)).when(webClient).getUserInDb(anyString());
    expected.put("type", 401);
    expected.put("title", "not authorized");
    expected.put("detail", "not authorized");
    databroker.resetPassword(
        request,
        handler -> {
          if (handler.failed()) {
            assertEquals(expected.toString(), handler.cause().getMessage());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(11)
  @DisplayName("Test resetPassword method : Success")
  public void test_resetPassword_success(VertxTestContext vertxTestContext) {
    request.put(USER_ID, "Dummy User ID");
    JsonObject mockJsonObject = mock(JsonObject.class);
    doAnswer(Answer -> Future.succeededFuture(mockJsonObject))
        .when(webClient)
        .getUserInDb(anyString());
    doAnswer(Answer -> Future.succeededFuture(mockJsonObject))
        .when(webClient)
        .resetPasswordInRmq(anyString(), anyString());
    doAnswer(Answer -> Future.succeededFuture(mockJsonObject))
        .when(webClient)
        .resetPwdInDb(anyString(), anyString());

    databroker.resetPassword(
        request,
        handler -> {
          if (handler.succeeded()) {
            JsonArray jsonArray = handler.result().getJsonArray("result");
            JsonObject object = jsonArray.getJsonObject(0);
            String actual = object.getString("username");
            assertEquals("Dummy User ID", actual);
            assertEquals("urn:dx:rs:success", handler.result().getString("type"));
            assertEquals("successful", handler.result().getString("title"));
            assertNotNull(handler.result().getString("result"));
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(12)
  @DisplayName("Test publishMessage method : Success")
  public void test_publishMessage_success(VertxTestContext vertxTestContext) {
    when(webClient.getRabbitmqClient()).thenReturn(rabbitMQClient);
    when(rabbitMQClient.isConnected()).thenReturn(true);
    doAnswer(Answer -> Future.succeededFuture())
        .when(rabbitMQClient)
        .basicPublish(anyString(), anyString(), any(Buffer.class));
    databroker.publishMessage(
        request,
        "Dummy string to Exchange",
        "Dummy routing Key",
        handler -> {
          if (handler.succeeded()) {
            assertTrue(handler.result().containsKey("type"));
            assertEquals("urn:dx:rs:success", handler.result().getString("type"));
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(13)
  @DisplayName("Test publishMessage method : Failure")
  public void test_publishMessage_failure(VertxTestContext vertxTestContext) {
    when(webClient.getRabbitmqClient()).thenReturn(rabbitMQClient);
    when(rabbitMQClient.isConnected()).thenReturn(true);
    doAnswer(Answer -> Future.failedFuture("Dummy failure message"))
        .when(rabbitMQClient)
        .basicPublish(anyString(), anyString(), any(Buffer.class));
    databroker.publishMessage(
        request,
        "Dummy string to Exchange",
        "Dummy routing Key",
        handler -> {
          expected.put("type", "urn:dx:rs:QueueError");
          expected.put("status", 400);
          expected.put("title", null);
          expected.put("detail", "Dummy failure message");
          if (handler.failed()) {
            JsonObject expected = new JsonObject(handler.cause().getMessage());
            assertEquals("Dummy failure message", expected.getString("detail"));
            assertEquals("urn:dx:rs:QueueError", expected.getString("type"));
            assertEquals(400, expected.getInteger("status"));
            assertEquals(expected.toString(), handler.cause().getMessage());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(14)
  @DisplayName("Test registerStreamingSubscription method : Failure")
  public void test_registerStreamingSubscription_failure(VertxTestContext vertxTestContext) {
    DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
    when(DataBrokerServiceImpl.subscriptionService.registerStreamingSubscription(any()))
        .thenReturn(jsonObjectFuture);
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.failed()).thenReturn(true);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn(throwableMessage);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(jsonObjectFuture)
        .onComplete(any());
    databroker.registerStreamingSubscription(
        request,
        handler -> {
          if (handler.failed()) {
            assertEquals("Dummy failure message", handler.cause().getMessage());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(15)
  @DisplayName("Test registerStreamingSubscription method : with empty request")
  public void test_registerStreamingSubscription_for_empty_request(
      VertxTestContext vertxTestContext) {
    DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
    databroker.registerStreamingSubscription(
        new JsonObject(),
        handler -> {
          if (handler.failed()) {
            assertEquals(expected_failure().toString(), handler.cause().getMessage());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(16)
  @DisplayName("Test updateStreamingSubscription method : Failure")
  public void test_updateStreamingSubscription_failure(VertxTestContext vertxTestContext) {
    DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
    when(DataBrokerServiceImpl.subscriptionService.updateStreamingSubscription(any()))
        .thenReturn(jsonObjectFuture);
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.failed()).thenReturn(true);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn(throwableMessage);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(jsonObjectFuture)
        .onComplete(any());
    databroker.updateStreamingSubscription(
        request,
        handler -> {
          if (handler.failed()) {
            assertEquals("Dummy failure message", handler.cause().getMessage());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(17)
  @DisplayName("Test updateStreamingSubscription method : with empty request")
  public void test_updateStreamingSubscription_for_empty_request(
      VertxTestContext vertxTestContext) {
    DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
    databroker.updateStreamingSubscription(
        new JsonObject(),
        handler -> {
          if (handler.failed()) {
            assertEquals(expected_failure().toString(), handler.cause().getMessage());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(18)
  @DisplayName("Test appendStreamingSubscription method : Failure")
  public void test_appendStreamingSubscription_failure(VertxTestContext vertxTestContext) {
    DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
    when(DataBrokerServiceImpl.subscriptionService.appendStreamingSubscription(any()))
        .thenReturn(jsonObjectFuture);
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.failed()).thenReturn(true);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn(throwableMessage);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(jsonObjectFuture)
        .onComplete(any());
    databroker.appendStreamingSubscription(
        request,
        handler -> {
          if (handler.failed()) {
            assertEquals("Dummy failure message", handler.cause().getMessage());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(19)
  @DisplayName("Test appendStreamingSubscription method : with empty request")
  public void test_appendStreamingSubscription_for_empty_request(
      VertxTestContext vertxTestContext) {
    DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
    databroker.appendStreamingSubscription(
        new JsonObject(),
        handler -> {
          if (handler.failed()) {
            assertEquals(expected_failure().toString(), handler.cause().getMessage());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(20)
  @DisplayName("Test createExchange method : success")
  public void test_createExchange_success(VertxTestContext vertxTestContext) {
    when(webClient.createExchange(any(), anyString())).thenReturn(jsonObjectFuture);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(jsonObjectFuture)
        .onComplete(any());
    databroker.createExchange(
        request,
        vHost,
        handler -> {
          if (handler.succeeded()) {
            assertEquals(request, handler.result());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(21)
  @DisplayName("Test deleteExchange method : success")
  public void test_deleteExchange_success(VertxTestContext vertxTestContext) {
    when(webClient.deleteExchange(any(), anyString())).thenReturn(jsonObjectFuture);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(jsonObjectFuture)
        .onComplete(any());
    databroker.deleteExchange(
        request,
        vHost,
        handler -> {
          if (handler.succeeded()) {
            assertEquals(request, handler.result());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(22)
  @DisplayName("Test listExchangeSubscribers method : success")
  public void test_listExchangeSubscribers_success(VertxTestContext vertxTestContext) {
    when(webClient.listExchangeSubscribers(any(), anyString())).thenReturn(jsonObjectFuture);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(jsonObjectFuture)
        .onComplete(any());
    databroker.listExchangeSubscribers(
        request,
        vHost,
        handler -> {
          if (handler.succeeded()) {
            assertEquals(request, handler.result());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(23)
  @DisplayName("Test createQueue method : success")
  public void test_createQueue_success(VertxTestContext vertxTestContext) {
    when(webClient.createQueue(any(), anyString())).thenReturn(jsonObjectFuture);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(jsonObjectFuture)
        .onComplete(any());
    databroker.createQueue(
        request,
        vHost,
        handler -> {
          if (handler.succeeded()) {
            assertEquals(request, handler.result());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(24)
  @DisplayName("Test deleteQueue method : success")
  public void test_deleteQueue_success(VertxTestContext vertxTestContext) {
    when(webClient.deleteQueue(any(), anyString())).thenReturn(jsonObjectFuture);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(jsonObjectFuture)
        .onComplete(any());
    databroker.deleteQueue(
        request,
        vHost,
        handler -> {
          if (handler.succeeded()) {
            assertEquals(request, handler.result());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(25)
  @DisplayName("Test bindQueue method : success")
  public void test_bindQueue_success(VertxTestContext vertxTestContext) {
    when(webClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(jsonObjectFuture)
        .onComplete(any());
    databroker.bindQueue(
        request,
        vHost,
        handler -> {
          if (handler.succeeded()) {
            assertEquals(request, handler.result());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(26)
  @DisplayName("Test unbindQueue method : success")
  public void test_unbindQueue_success(VertxTestContext vertxTestContext) {
    when(webClient.unbindQueue(any(), anyString())).thenReturn(jsonObjectFuture);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(jsonObjectFuture)
        .onComplete(any());
    databroker.unbindQueue(
        request,
        vHost,
        handler -> {
          if (handler.succeeded()) {
            assertEquals(request, handler.result());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(27)
  @DisplayName("Test createvHost method : success")
  public void test_createvHost_success(VertxTestContext vertxTestContext) {
    when(webClient.createvHost(any())).thenReturn(jsonObjectFuture);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(jsonObjectFuture)
        .onComplete(any());
    databroker.createvHost(
        request,
        handler -> {
          if (handler.succeeded()) {
            assertEquals(request, handler.result());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(28)
  @DisplayName("Test deletevHost method : success")
  public void test_deletevHost_success(VertxTestContext vertxTestContext) {
    when(webClient.deletevHost(any())).thenReturn(jsonObjectFuture);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(jsonObjectFuture)
        .onComplete(any());
    databroker.deletevHost(
        request,
        handler -> {
          if (handler.succeeded()) {
            assertEquals(request, handler.result());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(29)
  @DisplayName("Test listvHost method : success")
  public void test_listvHost_success(VertxTestContext vertxTestContext) {
    when(webClient.listvHost(any())).thenReturn(jsonObjectFuture);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(jsonObjectFuture)
        .onComplete(any());
    databroker.listvHost(
        request,
        handler -> {
          if (handler.succeeded()) {
            assertEquals(request, handler.result());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(30)
  @DisplayName("Test listQueueSubscribers method : success")
  public void test_listQueueSubscribers_success(VertxTestContext vertxTestContext) {
    when(webClient.listQueueSubscribers(any(), anyString())).thenReturn(jsonObjectFuture);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(jsonObjectFuture)
        .onComplete(any());
    databroker.listQueueSubscribers(
        request,
        vHost,
        handler -> {
          if (handler.succeeded()) {
            assertEquals(request, handler.result());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(31)
  @DisplayName("Test publishFromAdaptor method : Failure")
  public void test_publishFromAdaptor_Failure(VertxTestContext vertxTestContext) {

    request = new JsonObject();
    request.put("Dummy key", "Dummy value");
    request.put(ID, "Dummy/ID/abcd/abcd");
    request.put("status", "Dummy status");
    request.put("routingKey", "routingKeyValue");
    request.put("type", HttpStatus.SC_OK);
    request.put("entities", new JsonArray().add("5b7556b5-0779-4c47-9cf2-3f209779aa22"));
    lenient().when(webClient.getRabbitmqClient()).thenReturn(rabbitMQClient);
    when(asyncResult1.succeeded()).thenReturn(false);
    when(asyncResult1.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn(throwableMessage);

    doAnswer(
            new Answer<AsyncResult<Void>>() {
              @Override
              public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg0.getArgument(3)).handle(asyncResult1);
                return null;
              }
            })
        .when(iudxRabbitMQClient)
        .basicPublish(anyString(), anyString(), any(Buffer.class), any(Handler.class));
    expected.put("status", 200);
    JsonObject providerJson =
        new JsonObject()
            .put("provider", "8b95ab80-2aaf-4636-a65e-7f2563d0d371")
            .put("entities", new JsonArray().add("5b7556b5-0779-4c47-9cf2-3f209779aa22"))
            .put("resourceGroup", "dummy_resource");

    when(cacheService.get(any())).thenReturn(Future.succeededFuture(providerJson));
    databroker.publishFromAdaptor(
        request,
        vHost,
        handler -> {
          if (handler.failed()) {
            assertEquals("Dummy failure message", handler.cause().getMessage());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }
}
