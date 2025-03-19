package iudx.resource.server.databroker.util;

import static iudx.resource.server.databroker.util.Constants.*;
import static iudx.resource.server.databroker.util.Util.encodeValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.serviceproxy.ServiceException;
import iudx.resource.server.databroker.model.UserResponseModel;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@ExtendWith(VertxExtension.class)
public class RabbitClientTest {

  private RabbitClient rabbitClient;
  private RabbitWebClient rabbitWebClient;
  private RabbitWebClient rabbitWebClient2;
  private HttpResponse<Buffer> httpResponse;

  @BeforeEach
  void setUp(Vertx vertx) {
    WebClientOptions options = new WebClientOptions();
    JsonObject config = new JsonObject().put("userName", "testUser").put("password", "testPass");
    rabbitWebClient = mock(RabbitWebClient.class);
    rabbitWebClient2 = mock(RabbitWebClient.class);
    rabbitClient = new RabbitClient(rabbitWebClient);
    httpResponse = mock(HttpResponse.class);
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testDeleteQueue_Success(VertxTestContext testContext) {
    String queueName = "testQueue";
    String vhost = "iudx";
    String url = "/api/queues/" + vhost + "/" + queueName;

    when(httpResponse.statusCode()).thenReturn(HttpStatus.SC_NO_CONTENT);
    when(rabbitWebClient.requestAsync(eq("DELETE"), eq(url)))
        .thenReturn(Future.succeededFuture(httpResponse));

    rabbitClient
        .deleteQueue(queueName, vhost)
        .onComplete(testContext.succeeding(v -> testContext.completeNow()));
  }

  @Test
  void testDeleteQueue_NotFound(VertxTestContext testContext) {
    String queueName = "nonExistentQueue";
    String vhost = "iudx";
    String url = "/api/queues/" + vhost + "/" + queueName;

    when(httpResponse.statusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);
    when(rabbitWebClient.requestAsync(eq("DELETE"), eq(url)))
        .thenReturn(Future.succeededFuture(httpResponse));

    rabbitClient
        .deleteQueue(queueName, vhost)
        .onComplete(
            testContext.failing(
                ex -> {
                  assertTrue(ex.getMessage().contains("Queue does not exist"));
                  testContext.completeNow();
                }));
  }

  @Test
  void testDeleteQueue_Failure(VertxTestContext testContext) {
    String queueName = "errorQueue";
    String vhost = "iudx";
    String url = "/api/queues/" + vhost + "/" + queueName;

    when(rabbitWebClient.requestAsync(eq("DELETE"), eq(url)))
        .thenReturn(Future.failedFuture(new ServiceException(0, QUEUE_DELETE_ERROR)));

    rabbitClient
        .deleteQueue(queueName, vhost)
        .onComplete(
            testContext.failing(
                ex -> {
                  assertTrue(ex.getMessage().contains(QUEUE_DELETE_ERROR));
                  testContext.completeNow();
                }));
  }

  @Test
  void testListQueueSubscribers_Success(VertxTestContext testContext) {
    String queueName = "testQueue";
    String vhost = "iudx";
    String url = "/api/queues/" + vhost + "/" + queueName + "/bindings";

    JsonArray jsonArray =
        new JsonArray()
            .add(new JsonObject().put("routing_key", "key1"))
            .add(new JsonObject().put("routing_key", "key2"))
            .add(new JsonObject().put("routing_key", queueName));

    when(httpResponse.statusCode()).thenReturn(HttpStatus.SC_OK);
    when(httpResponse.body()).thenReturn(Buffer.buffer(jsonArray.encode()));
    when(rabbitWebClient.requestAsync(eq("GET"), eq(url)))
        .thenReturn(Future.succeededFuture(httpResponse));

    rabbitClient
        .listQueueSubscribers(queueName, vhost)
        .onComplete(
            testContext.succeeding(
                subscribers -> {
                  assertNotNull(subscribers);
                  assertEquals(2, subscribers.size());
                  assertTrue(subscribers.contains("key1"));
                  assertTrue(subscribers.contains("key2"));
                  testContext.completeNow();
                }));
  }

  @Test
  void testListQueueSubscribers_QueueNotFound(VertxTestContext testContext) {
    String queueName = "nonExistentQueue";
    String vhost = "iudx";
    String url = "/api/queues/" + vhost + "/" + queueName + "/bindings";

    when(httpResponse.statusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);
    when(rabbitWebClient.requestAsync(eq("GET"), eq(url)))
        .thenReturn(Future.succeededFuture(httpResponse));

    rabbitClient
        .listQueueSubscribers(queueName, vhost)
        .onComplete(
            testContext.failing(
                ex -> {
                  assertInstanceOf(ServiceException.class, ex);
                  assertEquals(4, ((ServiceException) ex).failureCode());
                  testContext.completeNow();
                }));
  }

  @Test
  void testListQueueSubscribers_Failure(VertxTestContext testContext) {
    String queueName = "errorQueue";
    String vhost = "iudx";
    String url = "/api/queues/" + vhost + "/" + queueName + "/bindings";

    when(rabbitWebClient.requestAsync(eq("GET"), eq(url)))
        .thenReturn(Future.failedFuture(new ServiceException(0, QUEUE_LIST_ERROR)));

    rabbitClient
        .listQueueSubscribers(queueName, vhost)
        .onComplete(
            testContext.failing(
                ex -> {
                  assertTrue(ex.getMessage().contains(QUEUE_LIST_ERROR));
                  testContext.completeNow();
                }));
  }

  @Test
  void testCreateUserIfNotExist_UserNotFound_CreatesUser(VertxTestContext testContext) {
    String userId = "testUser";
    String vhost = "iudx";
    String url = "/api/users/" + userId;

    when(httpResponse.statusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);
    when(rabbitWebClient.requestAsync(eq("GET"), eq(url)))
        .thenReturn(Future.succeededFuture(httpResponse));

    UserResponseModel mockUser = new UserResponseModel();
    mockUser.setUserId(userId);
    mockUser.setPassword("testPass");
    Future<UserResponseModel> userCreationFuture = Future.succeededFuture(mockUser);

    RabbitClient spyRabbitClient = Mockito.spy(rabbitClient);
    doReturn(userCreationFuture).when(spyRabbitClient).createUser(any(), any(), any(), any());

    spyRabbitClient
        .createUserIfNotExist(userId, vhost)
        .onComplete(
            testContext.succeeding(
                user -> {
                  assertEquals(userId, user.getUserId());
                  testContext.completeNow();
                }));
  }

  @Test
  void testCreateUserIfNotExist_UserExists(VertxTestContext testContext) {
    String userId = "existingUser";
    String vhost = "iudx";
    String url = "/api/users/" + userId;

    when(httpResponse.statusCode()).thenReturn(HttpStatus.SC_OK);
    when(rabbitWebClient.requestAsync(eq("GET"), eq(url)))
        .thenReturn(Future.succeededFuture(httpResponse));

    rabbitClient
        .createUserIfNotExist(userId, vhost)
        .onComplete(
            testContext.succeeding(
                user -> {
                  assertEquals(userId, user.getUserId());
                  assertEquals(
                      "Use the apiKey returned on registration, if lost please use /resetPassword API",
                      user.getPassword());
                  testContext.completeNow();
                }));
  }

  @Test
  void testCreateUserIfNotExist_ErrorFindingUser(VertxTestContext testContext) {
    String userId = "errorUser";
    String vhost = "iudx";
    String url = "/api/users/" + userId;

    when(rabbitWebClient.requestAsync(eq("GET"), eq(url)))
        .thenReturn(Future.failedFuture(new Throwable(USER_CREATION_ERROR)));

    rabbitClient
        .createUserIfNotExist(userId, vhost)
        .onComplete(
            testContext.failing(
                ex -> {
                  assertInstanceOf(ServiceException.class, ex);
                  assertEquals(USER_CREATION_ERROR, ex.getMessage());
                  testContext.completeNow();
                }));
  }

  @Test
  void testCreateUser_Success(VertxTestContext testContext) {
    String userId = "testUser";
    String password = "randomPassword";
    String vhost = "testVhost";
    String url = "/api/users/" + userId;
    String url2 = "/api/permissions/" + vhost + "/" + encodeValue(userId);
    JsonObject arg = new JsonObject();
    JsonObject arg2 = new JsonObject();
    arg.put(PASSWORD, password);
    arg.put(TAGS, NONE);
    arg2.put(CONFIGURE, DENY);
    arg2.put(WRITE, NONE);
    arg2.put(READ, NONE);
    HttpResponse<Buffer> mockResponse = mock(HttpResponse.class);
    when(rabbitWebClient.requestAsync(eq("PUT"), eq(url), eq(arg)))
        .thenReturn(Future.succeededFuture(mockResponse));
    when(rabbitWebClient.requestAsync(eq("PUT"), eq(url2), eq(arg2)))
        .thenReturn(Future.succeededFuture(mockResponse));
    when(mockResponse.statusCode()).thenReturn(HttpStatus.SC_CREATED);
    Future<UserResponseModel> resultFuture = rabbitClient.createUser(userId, password, vhost, url);
    resultFuture.onComplete(
        result -> {
          assertTrue(result.succeeded());
          UserResponseModel user = result.result();
          assertEquals(userId, user.getUserId());
          assertEquals(password, user.getPassword());
          testContext.completeNow();
        });
  }

  @Test
  void testCreateUser_NetworkFailure(VertxTestContext testContext) {
    String userId = "testUser";
    String password = "randomPassword";
    String vhost = "testVhost";
    String url = "/api/users/" + userId;
    when(rabbitWebClient.requestAsync(eq("PUT"), eq(url), any(JsonObject.class)))
        .thenReturn(Future.failedFuture(new ServiceException(0, CHECK_CREDENTIALS)));
    Future<UserResponseModel> resultFuture = rabbitClient.createUser(userId, password, vhost, url);

    resultFuture.onComplete(
        result -> {
          assertTrue(result.failed());
          assertEquals(CHECK_CREDENTIALS, result.cause().getMessage());
          testContext.completeNow();
        });
  }

  @Test
  void testCreateUser_VhostPermissionFailure(VertxTestContext testContext) {
    String userId = "testUser";
    String password = "randomPassword";
    String vhost = "testVhost";
    String url = "/api/users/" + userId;
    String url2 = "/api/permissions/" + vhost + "/" + encodeValue(userId);
    JsonObject arg2 = new JsonObject();
    arg2.put(CONFIGURE, DENY);
    arg2.put(WRITE, NONE);
    arg2.put(READ, NONE);
    HttpResponse<Buffer> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(HttpStatus.SC_CREATED);
    when(rabbitWebClient.requestAsync(eq("PUT"), eq(url), any()))
        .thenReturn(Future.succeededFuture(mockResponse));
    when(rabbitWebClient.requestAsync(eq("PUT"), eq(url2), eq(arg2)))
        .thenReturn(Future.succeededFuture(mockResponse));
    when(mockResponse.statusCode()).thenReturn(400);

    Future<UserResponseModel> resultFuture = rabbitClient.createUser(userId, password, vhost, url);
    resultFuture.onComplete(
        result -> {
          assertTrue(result.failed());
          assertEquals("Network Issue", result.cause().getMessage());
          testContext.completeNow();
        });
  }
}
