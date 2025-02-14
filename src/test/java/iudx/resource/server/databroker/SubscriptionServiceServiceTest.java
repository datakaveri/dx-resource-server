package iudx.resource.server.databroker;

import static iudx.resource.server.databroker.util.Constants.FAILURE;
import static iudx.resource.server.databroker.util.Constants.TITLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.databroker.util.Constants;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class SubscriptionServiceServiceTest {
    static JsonObject request;
    @Mock
    RabbitClient rabbitClient;
    @Mock
    RabbitMQClient rabbitMQClient;
    @Mock
    PostgresClient pgSQLClient;
    @Mock
    JsonObject config;
    @Mock
    Future<RowSet<Row>> rowSetFuture;
    SubscriptionService service;
    @Mock
    AsyncResult<RowSet<Row>> asyncResult;
    @Mock
    AsyncResult<JsonObject> asyncResult1;
    @Mock
    AsyncResult<Void> voidAsyncResult;
    JsonArray jsonArray;
    @Mock
    Future<JsonObject> jsonObjectFuture;
    @Mock
    Throwable throwable;
    String str;
    String vHost;
    @Mock
    RowSet<Row> rowSet;
    @Mock
    CacheService cacheService;

    public static Stream<Arguments> values() {
        return Stream.of(
                Arguments.of("ABCD/ABCD/ABCD/ABCD/ABCD", "{\"type\":400,\"title\":\"error\",\"detail\":\"Binding failed\"}"),
                Arguments.of("ABCD/ABCD/ABCD", "{\"type\":400,\"title\":\"error\",\"detail\":\"Invalid or null routing key\"}")
        );
    }

    static Stream<Arguments> listCallbackInputValues() {
        return Stream.of(
                Arguments.of(new JsonObject(), "{\"type\":400,\"title\":\"error\",\"detail\":\"Invalid request payload\"}"),
                Arguments.of(SubscriptionServiceServiceTest.request, "{\"type\":400,\"title\":\"error\",\"detail\":\"Invalid request payload\"}")
        );
    }

    static Stream<Arguments> routingKey() {
        return Stream.of(
                Arguments.of(null, "{\"type\":400,\"title\":\"error\",\"detail\":\"Invalid or null routing key\"}"),
                Arguments.of("", "{\"type\":400,\"title\":\"error\",\"detail\":\"Invalid or null routing key\"}")
        );
    }

    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext) {
        vHost = "Dummy vHost";
        str = "Dummy string";
        jsonArray = new JsonArray();
        request = new JsonObject();
        jsonArray.add("ABCD/ABCD/ABCD/ABCD");
        jsonArray.add("EFGH/EFGH/EFGH/EFGH");

        request.put(Constants.SUBSCRIPTION_ID, "Dummy_Queue_Name");
        request.put(Constants.USER_ID, "Dummy_User_ID");
        request.put(Constants.CONSUMER, "Dummy@consumer");
        request.put(Constants.NAME, "Dummy name");
        request.put(Constants.CALLBACKURL, "Dummy callbackurl");
        request.put(Constants.QUEUE, "Dummy Queue");
        request.put(Constants.ENTITIES, jsonArray);
        request.put("apiKey", "Dummy API KEY");
        request.put("type","Resource");
        request.put("resourcegroup","abcbc-abbc");

        when(config.getString(anyString())).thenReturn(str);
        when(config.getInteger(anyString())).thenReturn(200);
        service = new SubscriptionService(rabbitClient, pgSQLClient, config,cacheService);
        vertxTestContext.completeNow();
    }

    @Order(1)
    @Test
    @DisplayName("Test UpdateCallbackSubscription method : Success")
    public void testUpdateCallbackSubscriptionSuccess(VertxTestContext vertxTestContext) {

        JsonArray jsonArray = new JsonArray();
        jsonArray.add("ABCD/ABCD/ABCD/ABCD/ABCD/");
        jsonArray.add("EFGH/EFGH/EFGH/EFGH/EFGH/");
        request.put(Constants.ENTITIES, jsonArray);

        String str = "{\"subscriptionID\":\"consumer/cc92a0dbd4da9a5a024a5395062f6f905c7e5797/Dummy name\"}";
        JsonObject expected = new JsonObject(str);

        when(asyncResult1.succeeded()).thenReturn(true);
        when(asyncResult1.result()).thenReturn(request);
        when(asyncResult.succeeded()).thenReturn(true);
        when(voidAsyncResult.succeeded()).thenReturn(true);

        when(rabbitClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
        when(rabbitClient.getRabbitmqClient()).thenReturn(rabbitMQClient);

        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(rowSetFuture).onComplete(any());
        doAnswer(new Answer<AsyncResult<Void>>() {
            @Override
            public AsyncResult<Void> answer(InvocationOnMock arg3) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg3.getArgument(3)).handle(voidAsyncResult);
                return null;
            }
        }).when(rabbitMQClient).basicPublish(anyString(), anyString(), any(Buffer.class), any());

        service.updateCallbackSubscription(request).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals(expected, handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Order(2)
    @ParameterizedTest
    @MethodSource("values")
    @DisplayName("Test UpdateCallbackSubscription method : Failure")
    public void testUpdateCallbackSubscriptionFailure(String inputString, String str, VertxTestContext vertxTestContext) {
        JsonArray jsonArray = new JsonArray();
        JsonObject expected = new JsonObject(str);
        jsonArray.add(inputString);

        request.put(Constants.ENTITIES, jsonArray);

        lenient().when(asyncResult1.failed()).thenReturn(true);
        lenient().when(asyncResult1.cause()).thenReturn(throwable);
        lenient().when(rabbitClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture);

        lenient().doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        service.updateCallbackSubscription(request).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals(expected.toString(), handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Order(3)
    @Test
    @DisplayName("Test updateCallbackSubscription for null request")
    public void testUpdateCallbackSubscriptionForNullRequest(VertxTestContext vertxTestContext) {
        service.updateCallbackSubscription(null).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("{\"type\":400,\"title\":\"error\",\"detail\":\"Invalid request payload\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Order(4)
    @Test
    @DisplayName("Test appendStreamingSubscription method : Success")
    public void testAppendStreamingSubscriptionSuccess(VertxTestContext vertxTestContext) {

        JsonArray jsonArray = new JsonArray();
        jsonArray.add("ABCD/ABCD/ABCD/ABCD/ABCD/");
        jsonArray.add("EFGH/EFGH/EFGH/EFGH/EFGH/");
        request.put(Constants.ENTITIES, jsonArray);

        lenient().when(rabbitClient.listQueueSubscribers(any(), anyString())).thenReturn(jsonObjectFuture);
        lenient().when(rabbitClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        lenient().when(rabbitClient.updateUserPermissions(anyString(), anyString(), any(), anyString())).thenReturn(jsonObjectFuture);
        lenient().when(asyncResult1.succeeded()).thenReturn(true);
        lenient().when(asyncResult1.result()).thenReturn(request);
        lenient().doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        service.appendStreamingSubscription(request).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals("{\"type\":\"urn:dx:rs:success\",\"title\":\"success\",\"results\":[{\"entities\":[\"ABCD/ABCD/ABCD/ABCD/ABCD/\",\"EFGH/EFGH/EFGH/EFGH/EFGH/\"]}]}",
                        handler.result().toString());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Order(5)
    @Test
    @DisplayName("Test appendStreamingSubscription method : for empty request")
    public void testAppendStreamingSubscriptionWithInvalidInput(VertxTestContext vertxTestContext) {
        service.appendStreamingSubscription(new JsonObject()).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("{\"type\":400,\"title\":\"error\",\"detail\":\"Invalid request payload\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Order(6)
    @Test
    @DisplayName("Test appendStreamingSubscription method : listQueueSubscribers Failure")
    public void testAppendStreamingSubscriptionWithInvalidPayload(VertxTestContext vertxTestContext) {

        when(rabbitClient.listQueueSubscribers(any(), anyString())).thenReturn(jsonObjectFuture);

        when(asyncResult1.succeeded()).thenReturn(false);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        service.appendStreamingSubscription(request).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("{\"type\":400,\"title\":\"error\",\"detail\":\"Invalid request payload\"}",
                        handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Order(7)
    @Test
    @DisplayName("Test registerCallbackSubscription method: Null request")
    public void testRegisterCallbackSubscriptionWithInvalidInput(VertxTestContext vertxTestContext) {
        service.registerCallbackSubscription(null).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause());
            } else {
                assertEquals("{\"type\":400,\"title\":\"error\",\"detail\":\"Invalid request payload\"}",
                        handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }

    @Order(8)
    @Test
    @DisplayName("Test deleteCallbackSubscription method: Null request")
    public void testDeleteCallbackSubscriptionWithInvalidInput(VertxTestContext vertxTestContext) {
        service.deleteCallbackSubscription(null).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause());
            } else {
                assertEquals("{\"type\":400,\"title\":\"error\",\"detail\":\"Invalid request payload\"}",
                        handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }

    @Order(9)
    @ParameterizedTest
    @MethodSource("listCallbackInputValues")
    @DisplayName("Test listCallbackSubscription method: with different input requests")
    public void testListCallbackSubscription(JsonObject input, String expected, VertxTestContext vertxTestContext) {
        lenient().when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
        lenient().when(asyncResult.succeeded()).thenReturn(false);
        lenient().when(asyncResult.cause()).thenReturn(throwable);
        lenient().when(throwable.getMessage()).thenReturn("Dummy failure message");
        lenient().doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(rowSetFuture).onComplete(any());
        service.listCallbackSubscription(input).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause());
            } else {
                assertEquals(expected, handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }

    @Order(10)
    @Test
    @DisplayName("Test registerStreamingSubscription method : Success")
    public void testRegisterStreamingSubscriptionSuccess(VertxTestContext vertxTestContext) {
        JsonArray jsonArray = new JsonArray();
        jsonArray.add("ABCD/ABCD/ABCD/ABCD/ABCD/");
        jsonArray.add("EFGH/EFGH/EFGH/EFGH/EFGH/");
        request.put(Constants.ENTITIES, jsonArray);

        when(rabbitClient.createUserIfNotExist(anyString(), anyString())).thenReturn(jsonObjectFuture);
        when(rabbitClient.createQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(rabbitClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(rabbitClient.updateUserPermissions(anyString(), anyString(), any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult1.succeeded()).thenReturn(true);
        when(asyncResult1.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);

                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        service.registerStreamingSubscription(request).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals("{\"type\":\"urn:dx:rs:success\",\"title\":\"success\",\"results\":[{\"username\":\"Dummy_User_ID\",\"apiKey\":\"Dummy API KEY\",\"id\":\"Dummy_User_ID/Dummy name\",\"URL\":\"Dummy string\",\"port\":200,\"vHost\":\"Dummy string\"}]}", handler.result().toString());
                vertxTestContext.completeNow();

            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Order(11)
    @Test
    @DisplayName("Test updateStreamingSubscription method : Success")
    public void testUpdateStreamingSubscriptionSuccess(VertxTestContext vertxTestContext) {
        JsonArray jsonArray = new JsonArray();
        jsonArray.add("ABCD/ABCD/ABCD/ABCD/ABCD/");
        jsonArray.add("EFGH/EFGH/EFGH/EFGH/EFGH/");
        request.put(Constants.ENTITIES, jsonArray);

        when(rabbitClient.createUserIfNotExist(anyString(), anyString())).thenReturn(jsonObjectFuture);
        when(rabbitClient.createQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(rabbitClient.deleteQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(rabbitClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(rabbitClient.updateUserPermissions(anyString(), anyString(), any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult1.succeeded()).thenReturn(true);
        when(asyncResult1.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);

                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        service.updateStreamingSubscription(request).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals("{\"type\":\"urn:dx:rs:success\",\"title\":\"success\",\"results\":[{\"entities\":[\"ABCD/ABCD/ABCD/ABCD/ABCD/\",\"EFGH/EFGH/EFGH/EFGH/EFGH/\"]}]}", handler.result().toString());
                vertxTestContext.completeNow();

            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Order(12)
    @Test
    @DisplayName("Test deleteStreamingSubscription method : Success")
    public void testDeleteStreamingSubscriptionSuccess(VertxTestContext vertxTestContext) {
        when(rabbitClient.deleteQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(rabbitClient.updateUserPermissions(anyString(), anyString(), any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult1.succeeded()).thenReturn(true);
        when(asyncResult1.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);

                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        service.deleteStreamingSubscription(request).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals("{\"type\":\"urn:dx:rs:success\",\"status\":200,\"title\":\"success\",\"detail\":\"Subscription deleted Successfully\"}", handler.result().toString());
                vertxTestContext.completeNow();

            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Order(13)
    @Test
    @DisplayName("Test listStreamingSubscriptions method : Success")
    public void testListStreamingSubscriptionsSuccess(VertxTestContext vertxTestContext) {
        when(rabbitClient.listQueueSubscribers(any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult1.succeeded()).thenReturn(true);
        when(asyncResult1.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);

                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
    service
        .listStreamingSubscriptions(request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(
                    "urn:dx:rs:success",
                    handler.result().getString("type"));
                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    }

    @Order(14)
    @Test
    @DisplayName("Test registerCallbackSubscription method : Success")
    public void testRegisterCallbackSubscriptionSuccess(VertxTestContext vertxTestContext) {
        JsonArray jsonArray = new JsonArray();
        jsonArray.add("ABCD/ABCD/ABCD/ABCD/ABCD/");
        jsonArray.add("EFGH/EFGH/EFGH/EFGH/EFGH/");
        request.put(Constants.ENTITIES, jsonArray);
        when(rabbitClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult1.succeeded()).thenReturn(true);
        when(asyncResult1.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);

                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        when(voidAsyncResult.succeeded()).thenReturn(true);
        when(rabbitClient.getRabbitmqClient()).thenReturn(rabbitMQClient);
        when(asyncResult.result()).thenReturn(rowSet);
        when(asyncResult.succeeded()).thenReturn(true);
        when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
        doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(rowSetFuture).onComplete(any());
        doAnswer(new Answer<AsyncResult<Void>>() {
            @Override
            public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg0.getArgument(3)).handle(voidAsyncResult);
                return null;
            }
        }).when(rabbitMQClient).basicPublish(anyString(), anyString(), any(Buffer.class), any(Handler.class));

        service.registerCallbackSubscription(request).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals("{\"subscriptionID\":\"consumer/cc92a0dbd4da9a5a024a5395062f6f905c7e5797/Dummy name\"}", handler.result().toString());
                vertxTestContext.completeNow();

            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Order(15)
    @Test
    @DisplayName("Test deleteCallbackSubscription method : Success")
    public void testDeleteCallbackSubscriptionSuccess(VertxTestContext vertxTestContext) {
        when(voidAsyncResult.succeeded()).thenReturn(true);
        when(rabbitClient.getRabbitmqClient()).thenReturn(rabbitMQClient);
        when(asyncResult.result()).thenReturn(rowSet);
        when(asyncResult.succeeded()).thenReturn(true);
        when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
        doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(rowSetFuture).onComplete(any());
        doAnswer(new Answer<AsyncResult<Void>>() {
            @Override
            public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg0.getArgument(3)).handle(voidAsyncResult);
                return null;
            }
        }).when(rabbitMQClient).basicPublish(anyString(), anyString(), any(Buffer.class), any(Handler.class));

        service.deleteCallbackSubscription(request).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals("{\"subscriptionID\":\"consumer/cc92a0dbd4da9a5a024a5395062f6f905c7e5797/Dummy name\"}", handler.result().toString());
                vertxTestContext.completeNow();

            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Order(16)
    @Test
    @DisplayName("Test registerStreamingSubscription method : with userpermission handler failure")
    public void test_registerStreamingSubscription_userpermissionhandler_failed(VertxTestContext vertxTestContext) {
        Future<JsonObject> jsonObjectFuture1 = mock(Future.class);
        AsyncResult<JsonObject> asyncResult2 = mock(AsyncResult.class);

        when(rabbitClient.createUserIfNotExist(anyString(), anyString())).thenReturn(jsonObjectFuture1);
        when(rabbitClient.createQueue(any(), anyString())).thenReturn(jsonObjectFuture1);
        when(rabbitClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture1);
        when(rabbitClient.deleteQueue(any(), anyString())).thenReturn(jsonObjectFuture1);
        when(rabbitClient.updateUserPermissions(anyString(), anyString(), any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult1.succeeded()).thenReturn(false);
        when(asyncResult2.result()).thenReturn(request);
        when(asyncResult2.succeeded()).thenReturn(true);

        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);

                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult2);

                return null;
            }
        }).when(jsonObjectFuture1).onComplete(any());

        service.registerStreamingSubscription(request).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause());

            } else {
                assertEquals("{\"type\":400,\"title\":\"Bad Request data\",\"detail\":\"Binding failed\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }

    @Order(17)
    @Test
    @DisplayName("Test registerStreamingSubscription method : when resultHandlerqueue failed")
    public void test_registerStreamingSubscription_with_resultHandlerqueue_failure(VertxTestContext vertxTestContext) {
        Future<JsonObject> jsonObjectFuture1 = mock(Future.class);
        AsyncResult<JsonObject> asyncResult2 = mock(AsyncResult.class);
        String expected = "Dummy failure message";
        when(rabbitClient.createUserIfNotExist(anyString(), anyString())).thenReturn(jsonObjectFuture1);
        when(rabbitClient.createQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult1.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(expected);
        when(asyncResult1.failed()).thenReturn(true);
        when(asyncResult2.result()).thenReturn(request);
        when(asyncResult2.succeeded()).thenReturn(true);

        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);

                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult2);

                return null;
            }
        }).when(jsonObjectFuture1).onComplete(any());

        service.registerStreamingSubscription(request).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause());

            } else {
                assertEquals(expected, handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }

    @Order(18)
    @Test
    @DisplayName("Test registerStreamingSubscription method : with null routing key")
    public void test_registerStreamingSubscription_with_null_routingkey(VertxTestContext vertxTestContext) {
        Future<JsonObject> jsonObjectFuture1 = mock(Future.class);
        AsyncResult<JsonObject> asyncResult2 = mock(AsyncResult.class);
        jsonArray.add(null);
        request.put(Constants.ENTITIES, jsonArray);
        when(rabbitClient.createUserIfNotExist(anyString(), anyString())).thenReturn(jsonObjectFuture1);
        when(rabbitClient.createQueue(any(), anyString())).thenReturn(jsonObjectFuture1);
        when(rabbitClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(rabbitClient.deleteQueue(any(), anyString())).thenReturn(jsonObjectFuture1);
        when(asyncResult1.cause()).thenReturn(throwable);
        when(asyncResult1.failed()).thenReturn(true);
        when(asyncResult2.result()).thenReturn(request);
        when(asyncResult2.succeeded()).thenReturn(true);

        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);

                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult2);

                return null;
            }
        }).when(jsonObjectFuture1).onComplete(any());

        service.registerStreamingSubscription(request).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause());

            } else {
                assertEquals("{\"type\":400,\"title\":\"Bad Request data\",\"detail\":\"Binding failed\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }

    @Order(19)
    @Test
    @DisplayName("Test registerStreamingSubscription method : with error in payload")
    public void test_registerStreamingSubscription_with_null_request(VertxTestContext vertxTestContext) {

        String expected = "{\"type\":400,\"title\":\"Bad Request data\",\"detail\":\"Invalid request payload\"}";
        service.registerStreamingSubscription(null).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause());

            } else {
                assertEquals(expected, handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }

    @Order(20)
    @Test
    @DisplayName("Test registerStreamingSubscription method : with failure in queue creation")
    public void test_registerStreamingSubscription_with_failed_queue_creation(VertxTestContext vertxTestContext) {
        Future<JsonObject> jsonObjectFuture1 = mock(Future.class);
        AsyncResult<JsonObject> asyncResult2 = mock(AsyncResult.class);
        request.put(Constants.TITLE, FAILURE);
        when(rabbitClient.createUserIfNotExist(anyString(), anyString())).thenReturn(jsonObjectFuture1);
        when(rabbitClient.createQueue(any(), anyString())).thenReturn(jsonObjectFuture1);
        when(asyncResult2.result()).thenReturn(request);
        when(asyncResult2.succeeded()).thenReturn(true);

        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult2);

                return null;
            }
        }).when(jsonObjectFuture1).onComplete(any());

    service
        .registerStreamingSubscription(request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause());

              } else {
                assertEquals(
                    "{\"subscriptionID\":\"Dummy_Queue_Name\",\"userid\":\"Dummy_User_ID\",\"consumer\":\"Dummy@consumer\",\"name\":\"Dummy name\",\"callbackURL\":\"Dummy callbackurl\",\"queue\":\"Dummy Queue\",\"entities\":[\"ABCD/ABCD/ABCD/ABCD\",\"EFGH/EFGH/EFGH/EFGH\"],\"apiKey\":\"Dummy API KEY\",\"type\":\"Resource\",\"resourcegroup\":\"abcbc-abbc\",\"title\":\"failure\"}",
                    handler.cause().getMessage());
                vertxTestContext.completeNow();
              }
            });
    }

    @Order(21)
    @Test
    @DisplayName("Test updateStreamingSubscription method : with null request key")
    public void test_updateStreamingSubscription_null_request(VertxTestContext vertxTestContext) {
        service.updateStreamingSubscription(null).onComplete(handler -> {
            if (handler.succeeded()) {
                System.out.println("Success");
                vertxTestContext.failNow(handler.cause());
            } else {
                assertEquals("{\"type\":400,\"title\":\"error\",\"detail\":\"Invalid request payload\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();

            }
        });
    }

    @Order(22)
    @ParameterizedTest
    @MethodSource("routingKey")
    @DisplayName("Test updateStreamingSubscription method : with different routing key")
    public void test_updateStreamingSubscription_different_routingKey(String value, String expected, VertxTestContext vertxTestContext) {
        Future<JsonObject> jsonObjectFuture1 = mock(Future.class);
        AsyncResult<JsonObject> asyncResult2 = mock(AsyncResult.class);
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(value);
        request.put(Constants.ENTITIES, jsonArray);
        when(rabbitClient.createUserIfNotExist(anyString(), anyString())).thenReturn(jsonObjectFuture1);
        when(rabbitClient.deleteQueue(any(), anyString())).thenReturn(jsonObjectFuture1);
        when(rabbitClient.createQueue(any(), anyString())).thenReturn(jsonObjectFuture1);
        when(asyncResult2.result()).thenReturn(request);
        when(asyncResult2.succeeded()).thenReturn(true);

        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult2);

                return null;
            }
        }).when(jsonObjectFuture1).onComplete(any());
        service.updateStreamingSubscription(request).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause());
            } else {
                assertEquals(expected, handler.cause().getMessage());
                vertxTestContext.completeNow();

            }
        });
    }

    @Order(23)
    @Test
    @DisplayName("Test updateStreamingSubscription method : with binding failure")
    public void test_updateStreamingSubscription_with_binding_failure(VertxTestContext vertxTestContext) {
        Future<JsonObject> jsonObjectFuture1 = mock(Future.class);
        AsyncResult<JsonObject> asyncResult2 = mock(AsyncResult.class);
        String expected = "{\"error\":\"Binding Failed\"}";
        JsonObject request1 = new JsonObject();
        request1.put(TITLE,FAILURE);
        when(rabbitClient.createUserIfNotExist(anyString(), anyString())).thenReturn(jsonObjectFuture1);
        when(rabbitClient.deleteQueue(any(), anyString())).thenReturn(jsonObjectFuture1);
        when(rabbitClient.createQueue(any(), anyString())).thenReturn(jsonObjectFuture1);
        when(rabbitClient.bindQueue(any(),anyString())).thenReturn(jsonObjectFuture1);

        when(asyncResult2.result()).thenReturn(request, request, request, request, request, request1);
        when(asyncResult2.succeeded()).thenReturn(true);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult2);

                return null;
            }
        }).when(jsonObjectFuture1).onComplete(any());

        service.updateStreamingSubscription(request).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause());
            } else {
                assertEquals(expected, handler.cause().getMessage());
                vertxTestContext.completeNow();

            }
        });
    }

    @Order(24)
    @Test
    @DisplayName("Test UpdateCallbackSubscription method : failure in update permission")
    public void test_UpdateCallbackSubscription_with_failed_permissionHandler(VertxTestContext vertxTestContext) {

        Future<JsonObject> jsonObjectFuture1 = mock(Future.class);
        AsyncResult<JsonObject> asyncResult2 = mock(AsyncResult.class);

        JsonArray jsonArray = new JsonArray();
        jsonArray.add("ABCD/ABCD/ABCD/ABCD/ABCD/");
        jsonArray.add("EFGH/EFGH/EFGH/EFGH/EFGH/");
        request.put(Constants.ENTITIES, jsonArray);
        when(rabbitClient.createUserIfNotExist(anyString(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult2.succeeded()).thenReturn(false);
        when(rabbitClient.createQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(rabbitClient.deleteQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(rabbitClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(rabbitClient.updateUserPermissions(anyString(), anyString(), any(), anyString())).thenReturn(jsonObjectFuture1);
        when(asyncResult1.succeeded()).thenReturn(true);
        when(asyncResult1.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);

                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult2);

                return null;
            }
        }).when(jsonObjectFuture1).onComplete(any());
        service.updateStreamingSubscription(request).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("{\"error\":\"user Permission failed\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();

            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Order(25)
    @Test
    @DisplayName("Test UpdateCallbackSubscription method : failure in resultHandlerbind ")
    public void test_UpdateCallbackSubscription_with_failed_resultHandlerbind(VertxTestContext vertxTestContext) {

        Future<JsonObject> jsonObjectFuture1 = mock(Future.class);
        AsyncResult<JsonObject> asyncResult2 = mock(AsyncResult.class);

        JsonArray jsonArray = new JsonArray();
        jsonArray.add("ABCD/ABCD/ABCD/ABCD/ABCD/");
        jsonArray.add("EFGH/EFGH/EFGH/EFGH/EFGH/");
        request.put(Constants.ENTITIES, jsonArray);
        when(rabbitClient.createUserIfNotExist(anyString(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult2.failed()).thenReturn(true);
        when(asyncResult2.cause()).thenReturn(throwable);
        when(rabbitClient.createQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(rabbitClient.deleteQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(rabbitClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture1);
        when(asyncResult1.succeeded()).thenReturn(true);
        when(asyncResult1.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);

                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult2);

                return null;
            }
        }).when(jsonObjectFuture1).onComplete(any());
        service.updateStreamingSubscription(request).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("{\"type\":400,\"title\":\"error\",\"detail\":\"Binding failed\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();

            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Order(26)
    @Test
    @DisplayName("Test UpdateCallbackSubscription method : failure in createQueue ")
    public void test_UpdateCallbackSubscription_with_failed_createQueue(VertxTestContext vertxTestContext) {

        Future<JsonObject> jsonObjectFuture1 = mock(Future.class);
        AsyncResult<JsonObject> asyncResult2 = mock(AsyncResult.class);

        JsonArray jsonArray = new JsonArray();
        jsonArray.add("ABCD/ABCD/ABCD/ABCD/ABCD/");
        jsonArray.add("EFGH/EFGH/EFGH/EFGH/EFGH/");
        request.put(Constants.ENTITIES, jsonArray);
        when(rabbitClient.createUserIfNotExist(anyString(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult2.failed()).thenReturn(true);
        when(asyncResult2.cause()).thenReturn(throwable);
        when(rabbitClient.createQueue(any(), anyString())).thenReturn(jsonObjectFuture1);
        when(rabbitClient.deleteQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult1.succeeded()).thenReturn(true);
        when(asyncResult1.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);

                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult2);

                return null;
            }
        }).when(jsonObjectFuture1).onComplete(any());
        service.updateStreamingSubscription(request).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("{\"type\":500,\"title\":\"error\",\"detail\":\"Creation of Queue failed\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();

            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Order(27)
    @Test
    @DisplayName("Test UpdateCallbackSubscription method : failure in deleteQueueHandler ")
    public void test_UpdateCallbackSubscription_with_failed_deleteQueueHandler(VertxTestContext vertxTestContext) {

        Future<JsonObject> jsonObjectFuture1 = mock(Future.class);
        AsyncResult<JsonObject> asyncResult2 = mock(AsyncResult.class);

        JsonArray jsonArray = new JsonArray();
        jsonArray.add("ABCD/ABCD/ABCD/ABCD/ABCD/");
        jsonArray.add("EFGH/EFGH/EFGH/EFGH/EFGH/");
        request.put(Constants.ENTITIES, jsonArray);
        when(rabbitClient.createUserIfNotExist(anyString(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult2.failed()).thenReturn(true);
        when(asyncResult2.cause()).thenReturn(throwable);
        when(rabbitClient.deleteQueue(any(), anyString())).thenReturn(jsonObjectFuture1);
        when(asyncResult1.succeeded()).thenReturn(true);
        when(asyncResult1.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);

                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult2);

                return null;
            }
        }).when(jsonObjectFuture1).onComplete(any());
        service.updateStreamingSubscription(request).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("{\"type\":500,\"title\":\"error\",\"detail\":\"Deletion of Queue failed\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();

            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Order(28)
    @Test
    @DisplayName("Test registerStreamingSubscription method : with empty routing key")
    public void test_registerStreamingSubscription_empty_routingKey(VertxTestContext vertxTestContext) {
        JsonArray jsonArray = new JsonArray();
        jsonArray.add("");
        jsonArray.add("EFGH/EFGH/EFGH/EFGH/EFGH/");
        request.put(Constants.ENTITIES, jsonArray);

        when(rabbitClient.createUserIfNotExist(anyString(), anyString())).thenReturn(jsonObjectFuture);
        when(rabbitClient.createQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(rabbitClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(rabbitClient.deleteQueue(any(),anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult1.succeeded()).thenReturn(true);
        when(asyncResult1.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);

                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        service.registerStreamingSubscription(request).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("{\"type\":400,\"title\":\"Bad Request data\",\"detail\":\"Invalid or null routing key\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();

            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Order(29)
    @Test
    @DisplayName("Test appendStreamingSubscription method : with empty routing key")
    public void test_appendStreamingSubscription_empty_routingKey(VertxTestContext vertxTestContext) {

        JsonArray jsonArray = new JsonArray();
        jsonArray.add("");
        jsonArray.add("EFGH/EFGH/EFGH/EFGH/EFGH/");
        request.put(Constants.ENTITIES, jsonArray);

        when(rabbitClient.listQueueSubscribers(any(), anyString())).thenReturn(jsonObjectFuture);
        when(rabbitClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult1.succeeded()).thenReturn(true);
        when(asyncResult1.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        service.appendStreamingSubscription(request).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("{\"type\":400,\"title\":\"error\",\"detail\":\"Invalid or null routing key\"}",
                        handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Order(30)
    @Test
    @DisplayName("Test appendStreamingSubscription method : with binding failure")
    public void test_appendStreamingSubscription_with_binding_failure(VertxTestContext vertxTestContext) {
        Future<JsonObject> jsonObjectFuture1 = mock(Future.class);
        AsyncResult<JsonObject> asyncResult2 = mock(AsyncResult.class);
        String expected = "{\"type\":400,\"title\":\"urn:dx:rs:badRequest\",\"detail\":\"Binding failed\"}";
        JsonObject request1 = new JsonObject();
        request1.put(TITLE,FAILURE);
        when(rabbitClient.listQueueSubscribers(any(), anyString())).thenReturn(jsonObjectFuture1);
        when(rabbitClient.bindQueue(any(),anyString())).thenReturn(jsonObjectFuture1);
        when(asyncResult2.result()).thenReturn(request, request, request1);
        when(asyncResult2.succeeded()).thenReturn(true);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult2);

                return null;
            }
        }).when(jsonObjectFuture1).onComplete(any());


        service.appendStreamingSubscription(request).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause());
            } else {
                assertEquals(expected, handler.cause().getMessage());
                vertxTestContext.completeNow();

            }
        });
    }

    @Order(31)
    @Test
    @DisplayName("Test appendStreamingSubscription method : with failure in update user permission")
    public void test_appendStreamingSubscription_with_updateUserPermissions_failure(VertxTestContext vertxTestContext) {

        Future<JsonObject> jsonObjectFuture1 = mock(Future.class);
        AsyncResult<JsonObject> asyncResult2 = mock(AsyncResult.class);

        JsonArray jsonArray = new JsonArray();
        jsonArray.add("ABCD/ABCD/ABCD/ABCD/ABCD/");
        jsonArray.add("EFGH/EFGH/EFGH/EFGH/EFGH/");
        request.put(Constants.ENTITIES, jsonArray);

        when(asyncResult2.succeeded()).thenReturn(false);
        when(rabbitClient.deleteQueue(any(),anyString())).thenReturn(jsonObjectFuture);
        when(rabbitClient.listQueueSubscribers(any(), anyString())).thenReturn(jsonObjectFuture);
        when(rabbitClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(rabbitClient.updateUserPermissions(anyString(), anyString(), any(), anyString())).thenReturn(jsonObjectFuture1);
        when(asyncResult1.succeeded()).thenReturn(true);
        when(asyncResult1.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult2);
                return null;
            }
        }).when(jsonObjectFuture1).onComplete(any());
        service.appendStreamingSubscription(request).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("{\"error\":\"user Permission failed\"}",
                        handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Order(32)
    @Test
    @DisplayName("Test appendStreamingSubscription method : with failure in resultHandlerbind")
    public void test_appendStreamingSubscription_with_resultHandlerbind_failure(VertxTestContext vertxTestContext) {

        Future<JsonObject> jsonObjectFuture1 = mock(Future.class);
        AsyncResult<JsonObject> asyncResult2 = mock(AsyncResult.class);

        JsonArray jsonArray = new JsonArray();
        jsonArray.add("ABCD/ABCD/ABCD/ABCD/ABCD/");
        jsonArray.add("EFGH/EFGH/EFGH/EFGH/EFGH/");
        request.put(Constants.ENTITIES, jsonArray);

        when(asyncResult2.failed()).thenReturn(true);
        when(rabbitClient.listQueueSubscribers(any(), anyString())).thenReturn(jsonObjectFuture);
        when(rabbitClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture1);
        when(asyncResult1.succeeded()).thenReturn(true);
        when(asyncResult1.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult2);
                return null;
            }
        }).when(jsonObjectFuture1).onComplete(any());
        service.appendStreamingSubscription(request).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("{\"type\":400,\"title\":\"error\",\"detail\":\"Binding failed\"}",
                        handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }


    @Order(33)
    @Test
    @DisplayName("Test appendStreamingSubscription method : with NULL routing key")
    public void test_appendStreamingSubscription_with_null_routingKey(VertxTestContext vertxTestContext) {

        Future<JsonObject> jsonObjectFuture1 = mock(Future.class);
        AsyncResult<JsonObject> asyncResult2 = mock(AsyncResult.class);

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(null);
        jsonArray.add("EFGH/EFGH/EFGH/EFGH/EFGH/");
        request.put(Constants.ENTITIES, jsonArray);

        when(asyncResult2.failed()).thenReturn(true);
        when(rabbitClient.listQueueSubscribers(any(), anyString())).thenReturn(jsonObjectFuture);
        when(rabbitClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture1);
        when(rabbitClient.deleteQueue(any(),anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult1.succeeded()).thenReturn(true);
        when(asyncResult1.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult2);
                return null;
            }
        }).when(jsonObjectFuture1).onComplete(any());
        service.appendStreamingSubscription(request).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("{\"type\":400,\"title\":\"error\",\"detail\":\"Invalid or null routing key\"}",
                        handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Order(34)
    @Test
    @DisplayName("Test deleteStreamingSubscription method : For failure in result handler")
    public void test_deleteStreamingSubscription_for_resultHandler_failure(VertxTestContext vertxTestContext)
    {
        when(rabbitClient.deleteQueue(any(),anyString())).thenReturn(jsonObjectFuture);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        when(asyncResult1.failed()).thenReturn(true);
        when(asyncResult1.cause()).thenReturn(throwable);

        service.deleteStreamingSubscription(request).onComplete(handler -> {
            if(handler.succeeded())
            {
                vertxTestContext.failNow(handler.cause());
            }
            else
            {
                assertEquals("{\"type\":500,\"title\":\"error\",\"detail\":\"Deletion of Queue failed\"}",handler.cause().getMessage());
                vertxTestContext.completeNow();

            }
        });
    }

    @Order(35)
    @Test
    @DisplayName("Test listStreamingSubscriptions method : For failure in result handler")
    public void test_listStreamingSubscriptions_for_resultHandler_failure(VertxTestContext vertxTestContext)
    {
        when(rabbitClient.listQueueSubscribers(any(),anyString())).thenReturn(jsonObjectFuture);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        when(asyncResult1.failed()).thenReturn(true);
        when(asyncResult1.cause()).thenReturn(throwable);

        service.listStreamingSubscriptions(request).onComplete(handler -> {
            if(handler.succeeded())
            {
                vertxTestContext.failNow(handler.cause());
            }
            else
            {
                assertEquals("{\"type\":400,\"title\":\"error\",\"detail\":\"Listing of Queue failed\"}",handler.cause().getMessage());
                vertxTestContext.completeNow();

            }
        });
    }

    @Order(36)
    @Test
    @DisplayName("Test registerCallbackSubscription method : empty request")
    public void test_registerCallbackSubscription_with_empty_request(VertxTestContext vertxTestContext)
    {
        JsonArray jsonArray = new JsonArray();
        jsonArray.add("");
        jsonArray.add("EFGH/EFGH/EFGH/EFGH/EFGH/");
        request.put(Constants.ENTITIES, jsonArray);

        JsonObject request1 = new JsonObject();
        request1.put(TITLE,FAILURE);

        when(asyncResult1.succeeded()).thenReturn(true);
        when(asyncResult1.result()).thenReturn(request1);
        when(asyncResult1.cause()).thenReturn(throwable);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(rowSet);
        when(rabbitClient.bindQueue(any(),anyString())).thenReturn(jsonObjectFuture);
        doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(rowSetFuture).onComplete(any());
        service.registerCallbackSubscription(request).onComplete(handler -> {
            if(handler.succeeded())
            {
                vertxTestContext.failNow(handler.cause());
            }
            else
            {
                assertEquals("{\"type\":500,\"title\":\"error\",\"detail\":\"Invalid or null routing key\"}",handler.cause().getMessage());
                vertxTestContext.completeNow();

            }
        });
    }

    @Order(37)
    @Test
    @DisplayName("Test registerCallbackSubscription method : with failure in result handler")
    public void test_registerCallbackSubscription_resulthandler_failure(VertxTestContext vertxTestContext)
    {
        JsonArray jsonArray = new JsonArray();
        jsonArray.add("ABCD/ABCD/ABCD/ABCD/ABCD/");
        jsonArray.add("EFGH/EFGH/EFGH/EFGH/EFGH/");
        request.put(Constants.ENTITIES, jsonArray);
        when(rabbitClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult1.succeeded()).thenReturn(true);
        when(asyncResult1.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);

                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        when(voidAsyncResult.succeeded()).thenReturn(false);
        when(rabbitClient.getRabbitmqClient()).thenReturn(rabbitMQClient);
        when(asyncResult.result()).thenReturn(rowSet);
        when(asyncResult.succeeded()).thenReturn(true);
        when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
        doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(rowSetFuture).onComplete(any());
        doAnswer(new Answer<AsyncResult<Void>>() {
            @Override
            public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg0.getArgument(3)).handle(voidAsyncResult);
                return null;
            }
        }).when(rabbitMQClient).basicPublish(anyString(), anyString(), any(Buffer.class), any(Handler.class));

        service.registerCallbackSubscription(request).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("{\"type\":500,\"title\":\"error\",\"detail\":\"Message publishing failed\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();

            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }


    @Order(38)
    @Test
    @DisplayName("Test registerCallbackSubscription method : with NULL routingKey")
    public void test_registerCallbackSubscription_with_null_routingKey(VertxTestContext vertxTestContext)
    {
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(null);
        jsonArray.add("EFGH/EFGH/EFGH/EFGH/EFGH/");
        request.put(Constants.ENTITIES, jsonArray);
        when(rabbitClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult1.succeeded()).thenReturn(true);
        when(asyncResult1.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);

                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        when(asyncResult.result()).thenReturn(rowSet);
        when(asyncResult.succeeded()).thenReturn(true,true,false,false,true);
        when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
        doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(rowSetFuture).onComplete(any());

        service.registerCallbackSubscription(request).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("{\"type\":400,\"title\":\"error\",\"detail\":\"Invalid or null routing key\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();

            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
        vertxTestContext.completeNow();
    }

    @Order(39)
    @Test
    @DisplayName("Test registerCallbackSubscription method : with failure in executeAsync")
    public void test_registerCallbackSubscription_executeAsync_failure(VertxTestContext vertxTestContext)
    {
        Future<JsonObject> jsonObjectFuture1 = mock(Future.class);
        AsyncResult<JsonObject> asyncResult2 = mock(AsyncResult.class);

        JsonArray jsonArray = new JsonArray();
        jsonArray.add("ABCD/ABCD/ABCD/ABCD/ABCD/");
        jsonArray.add("EFGH/EFGH/EFGH/EFGH/EFGH/");
        request.put(Constants.ENTITIES, jsonArray);
        when(rabbitClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture1);
        when(asyncResult2.failed()).thenReturn(true);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult2);

                return null;
            }
        }).when(jsonObjectFuture1).onComplete(any());

        when(asyncResult.result()).thenReturn(rowSet);
        when(asyncResult.succeeded()).thenReturn(true,true,false,false,true);
        when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
        doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(rowSetFuture).onComplete(any());

        service.registerCallbackSubscription(request).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("{\"type\":400,\"title\":\"error\",\"detail\":\"Binding failed\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();

            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
        vertxTestContext.completeNow();
    }

    @Order(40)
    @Test
    @DisplayName("Test updateCallbackSubscription method : with NULL routing key")
    public void test_updateCallbackSubscription_with_null_routingKey(VertxTestContext vertxTestContext)
    {

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(null);
        jsonArray.add("EFGH/EFGH/EFGH/EFGH/EFGH/");
        request.put(Constants.ENTITIES, jsonArray);

        when(rabbitClient.bindQueue(any(),anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult1.succeeded()).thenReturn(true);
        when(asyncResult1.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);

                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        service.updateCallbackSubscription(request).onComplete(handler -> {
            if(handler.succeeded())
            {
                vertxTestContext.failNow(handler.cause());
            }
            else
            {
                assertEquals("{\"type\":400,\"title\":\"error\",\"detail\":\"Invalid or null routing key\"}",handler.cause().getMessage());
                vertxTestContext.completeNow();

            }
        });
    }

    @Order(41)
    @Test
    @DisplayName("Test UpdateCallbackSubscription method : with failure in resultHandler")
    public void test_updateCallbackSubscription_resultHandler_failure(VertxTestContext vertxTestContext) {

        JsonArray jsonArray = new JsonArray();
        jsonArray.add("ABCD/ABCD/ABCD/ABCD/ABCD/");
        jsonArray.add("EFGH/EFGH/EFGH/EFGH/EFGH/");
        request.put(Constants.ENTITIES, jsonArray);


        when(asyncResult1.succeeded()).thenReturn(true);
        when(asyncResult1.result()).thenReturn(request);
        when(asyncResult.succeeded()).thenReturn(true);
        when(voidAsyncResult.succeeded()).thenReturn(false);

        when(rabbitClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
        when(rabbitClient.getRabbitmqClient()).thenReturn(rabbitMQClient);

        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(rowSetFuture).onComplete(any());
        doAnswer(new Answer<AsyncResult<Void>>() {
            @Override
            public AsyncResult<Void> answer(InvocationOnMock arg3) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg3.getArgument(3)).handle(voidAsyncResult);
                return null;
            }
        }).when(rabbitMQClient).basicPublish(anyString(), anyString(), any(Buffer.class), any());

        service.updateCallbackSubscription(request).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals(null, handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Order(42)
    @Test
    @DisplayName("Test UpdateCallbackSubscription method : with failure in executeAsync")
    public void test_updateCallbackSubscription_executeAsync_failure(VertxTestContext vertxTestContext) {

        JsonArray jsonArray = new JsonArray();
        jsonArray.add("ABCD/ABCD/ABCD/ABCD/ABCD/");
        jsonArray.add("EFGH/EFGH/EFGH/EFGH/EFGH/");
        request.put(Constants.ENTITIES, jsonArray);


        when(asyncResult1.succeeded()).thenReturn(true);
        when(asyncResult1.result()).thenReturn(request);
        when(asyncResult.succeeded()).thenReturn(false);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn("Dummy failure message");
        when(rabbitClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(rowSetFuture).onComplete(any());

        service.updateCallbackSubscription(request).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals(null, handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Order(43)
    @Test
    @DisplayName("Test UpdateCallbackSubscription method : for resultHandlerbind failure")
    public void test_updateCallbackSubscription_failed_resultHandlerbind(VertxTestContext vertxTestContext) {

        JsonArray jsonArray = new JsonArray();
        jsonArray.add("ABCD/ABCD/ABCD/ABCD/ABCD/");
        jsonArray.add("EFGH/EFGH/EFGH/EFGH/EFGH/");
        request.put(Constants.ENTITIES, jsonArray);

        JsonObject request1 = new JsonObject();
        request1.put(TITLE,FAILURE);

        when(asyncResult1.succeeded()).thenReturn(true);
        when(asyncResult1.result()).thenReturn(request1);
        when(rabbitClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture);

        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        service.updateCallbackSubscription(request).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals(null, handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Order(44)
    @Test
    @DisplayName("Test deleteCallbackSubscription method : when delete failed")
    public void test_deleteCallbackSubscription_failed_delete(VertxTestContext vertxTestContext)
    {
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn("Dummy failure message");
        when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
        when(asyncResult.succeeded()).thenReturn(true,false);
        when(asyncResult.result()).thenReturn(rowSet);

        doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(rowSetFuture).onComplete(any());
        service.deleteCallbackSubscription(request).onComplete(handler -> {
            if(handler.succeeded())
            {
                vertxTestContext.failNow(handler.cause());
            }
            else
            {
                assertEquals("{\"type\":500,\"title\":\"error\",\"detail\":\"failure\"}",handler.cause().getMessage());
                vertxTestContext.completeNow();

            }
        });
    }

    @Order(45)
    @Test
    @DisplayName("Test deleteCallbackSubscription method : when message publish failed")
    public void test_deleteCallbackSubscription_publish_message_failure(VertxTestContext vertxTestContext) {
        lenient().when(voidAsyncResult.succeeded()).thenReturn(false);
        lenient().when(rabbitClient.getRabbitmqClient()).thenReturn(rabbitMQClient);
        when(asyncResult.result()).thenReturn(rowSet);
        when(asyncResult.succeeded()).thenReturn(true);
        when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
        lenient().doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(rowSetFuture).onComplete(any());
        lenient().doAnswer(new Answer<AsyncResult<Void>>() {
            @Override
            public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg0.getArgument(3)).handle(voidAsyncResult);
                return null;
            }
        }).when(rabbitMQClient).basicPublish(anyString(), anyString(), any(Buffer.class), any(Handler.class));

        service.deleteCallbackSubscription(request).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("{\"type\":500,\"title\":\"error\",\"detail\":\"Message publishing failed\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();

            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Order(46)
    @Test
    @DisplayName("Test deleteCallbackSubscription method : with invalid request payload")
    public void test_listCallbackSubscription_publish_invalid_requestpayload(VertxTestContext vertxTestContext) {
        when(asyncResult.result()).thenReturn(rowSet);
        when(asyncResult.succeeded()).thenReturn(true);
        when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
        doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(rowSetFuture).onComplete(any());

        service.listCallbackSubscription(request).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("{\"type\":400,\"title\":\"error\",\"detail\":\"Invalid request payload\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();

            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Order(47)
    @Test
    @DisplayName("Test registerCallbackSubscription method : failure in executeAsync")
    public void test_registerCallbackSubscription_failure(VertxTestContext vertxTestContext)
    {
        JsonArray jsonArray = new JsonArray();
        jsonArray.add("ABCD/ABCD/ABCD/ABCD/ABCD/");
        jsonArray.add("EFGH/EFGH/EFGH/EFGH/EFGH/");
        request.put(Constants.ENTITIES, jsonArray);
        when(rabbitClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult1.succeeded()).thenReturn(true);
        when(asyncResult1.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);

                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn("Dummy failure message");
        when(asyncResult.result()).thenReturn(rowSet);
        when(asyncResult.succeeded()).thenReturn(true,false,false,true);
        when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn("Dummy async failure message");
        doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(rowSetFuture).onComplete(any());

        service.registerCallbackSubscription(request).onComplete(handler -> {
            if (handler.failed()) {
                System.out.println("failure");
                assertEquals("", handler.cause().getMessage());

                vertxTestContext.completeNow();

            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });

        vertxTestContext.completeNow();
    }

}
