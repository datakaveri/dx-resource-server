/*
package iudx.resource.server.databroker;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import iudx.resource.server.databroker.util.PermissionOpType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.apache.http.HttpStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class RabbitMQClientTest {

    RabbitClient rabbitClient;
    String userID;
    String password;
    @Mock
    RabbitMQOptions rabbitConfigs;
    Vertx vertxObj;
    @Mock
    RabbitWebClient webClient;
    @Mock
    PostgresClient pgSQLClient;
    @Mock
    JsonObject configs;
    @Mock
    Future<HttpResponse<Buffer>> httpResponseFuture;
    @Mock
    Future<RowSet<Row>> rowSetFuture;
    @Mock
    AsyncResult<HttpResponse<Buffer>> httpResponseAsyncResult;
    @Mock
    HttpResponse<Buffer> bufferHttpResponse;
    @Mock
    Buffer buffer;
    @Mock
    AsyncResult<RowSet<Row>> rowSetAsyncResult;
    @Mock
    Throwable throwable;
    PermissionOpType type;
    JsonObject request;
    JsonArray jsonArray;
    String vHost;
    JsonObject expected;
    @Mock
    RabbitClient.AdaptorResultContainer adaptorResultContainer;

    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext) {
        userID = "Dummy UserID";
        password = "Dummy password";
        vertxObj = Vertx.vertx();
        vHost = "Dummy vHost";
        expected = new JsonObject();
        when(configs.getString(anyString())).thenReturn("Dummy string");
        when(configs.getInteger(anyString())).thenReturn(400);
        when(rabbitConfigs.setVirtualHost(anyString())).thenReturn(rabbitConfigs);
        request = new JsonObject();
        jsonArray = new JsonArray();
        jsonArray.add(0, "{\"Dummy key\" : \"Dummy value\"}");
        request.put("exchangeName", "Dummy exchangeName");
        request.put("queueName", "Dummy Queue name");
        request.put("id", "Dummy ID");
        request.put("vHost", "Dummy vHost");
        request.put("entities", jsonArray);
        rabbitClient = new RabbitClient(vertxObj, rabbitConfigs, webClient, pgSQLClient, configs);
        vertxTestContext.completeNow();
    }


    @Test
    @DisplayName("Test resetPwdInDb method : Failure")
    public void testResetPwdInDbFailure(VertxTestContext vertxTestContext) {
        when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
        when(rowSetAsyncResult.succeeded()).thenReturn(false);
        doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(rowSetAsyncResult);
                return null;
            }
        }).when(rowSetFuture).onComplete(any());
        rabbitClient.resetPwdInDb(userID, password).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("Error : Write to database failed", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test resetPwdInDb method : Success")
    public void testResetPwdInDbSuccess(VertxTestContext vertxTestContext) {
        when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
        when(rowSetAsyncResult.succeeded()).thenReturn(true);
        doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(rowSetAsyncResult);
                return null;
            }
        }).when(rowSetFuture).onComplete(any());
        rabbitClient.resetPwdInDb(userID, password).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals("{\"status\":\"success\"}", handler.result().toString());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test testGetUserInDb method : Success")
    public void testGetUserInDbSuccess(VertxTestContext vertxTestContext) {
        RowSet<Row> rowSet = mock(RowSet.class);
        when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
        when(rowSetAsyncResult.succeeded()).thenReturn(true);
        when(rowSetAsyncResult.result()).thenReturn(rowSet);

        doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(rowSetAsyncResult);
                return null;
            }
        }).when(rowSetFuture).onComplete(any());
        rabbitClient.getUserInDb(userID).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals("{\"apiKey\":null}", handler.result().toString());
                vertxTestContext.completeNow();

            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test testGetUserInDb method : Failure")
    public void testGetUserInDbFailure(VertxTestContext vertxTestContext) {
        when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
        when(rowSetAsyncResult.succeeded()).thenReturn(false);
        doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(rowSetAsyncResult);
                return null;
            }
        }).when(rowSetFuture).onComplete(any());
        rabbitClient.getUserInDb(userID).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause());

            } else {
                assertEquals("Error : Get ID from database failed", handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }

    static Stream<Arguments> inputStatusCode() {
        return Stream.of(
                Arguments.of(201, "{\"type\":200,\"title\":\"topic_permissions\",\"detail\":\"topic permission set\"}"),
                Arguments.of(204, "{\"type\":200,\"title\":\"topic_permissions\",\"detail\":\"topic permission already set\"}"),
                Arguments.of(400, "{\"type\":500,\"title\":\"topic_permissions\",\"detail\":\"Error in setting Topic permissions\"}")
        );
    }

    @ParameterizedTest
    @MethodSource("inputStatusCode")
    @DisplayName("Test setTopicPermissions method : with different status code")
    public void testSetTopicPermissions(int statusCode, String expected, VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString(), any())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(statusCode);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.setTopicPermissions("Dummy vHost", "Dummy adaptorID", userID).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals(expected, handler.result().toString());
            } else {
                assertEquals(expected, handler.cause().getMessage());
            }
        });
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test setTopicPermissions method : Failure")
    public void testSetTopicPermissionsFailure(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString(), any())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        expected.put("type", 500);
        expected.put("title", "topic_permissions");
        expected.put("detail", "Error in setting Topic permissions");

        rabbitClient.setTopicPermissions("Dummy vHost", "Dummy adaptorID", userID).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause());
            } else {
                JsonObject result = new JsonObject(handler.cause().getMessage());
                assertEquals(expected.getString("title"), result.getString("title"));
                assertEquals(expected.getString("detail"), result.getString("detail"));
                assertEquals(expected.getInteger("type"), result.getInteger("type"));
                vertxTestContext.completeNow();
            }
        });
    }

    @Test
    @DisplayName("Test createExchange method : for status code 400")
    public void testCreateExchangeForBadRequest(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString(), any())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(400);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        expected.put("type", 400);
        expected.put("title", "failure");
        expected.put("detail", "Exchange already exists with different properties");
        rabbitClient.createExchange(request, "Dummy vHost").onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals(expected.getString("title"), handler.result().getString("title"));
                assertEquals(expected.getString("detail"), handler.result().getString("detail"));
                assertEquals(expected.getInteger("type"), handler.result().getInteger("type"));
                vertxTestContext.completeNow();
                vertxTestContext.completeNow();

            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test createExchange method : Failure")
    public void testCreateExchangeFailure(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString(), any())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);


        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        expected.put("type", 500);
        expected.put("title", "error");
        expected.put("detail", "Creation of Exchange failed");
        rabbitClient.createExchange(request, "Dummy vHost").onComplete(handler -> {
            if (handler.failed()) {
                JsonObject result = new JsonObject(handler.cause().getMessage());
                assertEquals(expected.getString("title"), result.getString("title"));
                assertEquals(expected.getString("detail"), result.getString("detail"));
                assertEquals(expected.getInteger("type"), result.getInteger("type"));
                vertxTestContext.completeNow();

            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test getExchange method : For status code 404")
    public void testGetExchangeForSC_NOT_FOUND(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(404);
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        expected.put("type", 404);
        expected.put("title", "failure");
        expected.put("detail", "Exchange not found");
        rabbitClient.getExchange(request, "Dummy vHost").onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals(expected.getString("title"), handler.result().getString("title"));
                assertEquals(expected.getString("detail"), handler.result().getString("detail"));
                assertEquals(expected.getInteger("type"), handler.result().getInteger("type"));
                vertxTestContext.completeNow();
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test getExchange method : For status code 401")
    public void testGetExchangeForEXCHANGE_NOT_FOUND(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(401);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.getExchange(request, "Dummy vHost").onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("getExchange_statusthrowable", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test getExchange method : With null request")
    public void testGetExchangeFailure(VertxTestContext vertxTestContext) {
        rabbitClient.getExchange(new JsonObject(), "Dummy vHost").onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("exchangeName not provided", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }


    @Test
    @DisplayName("Test deleteExchange : Failure")
    public void testDeleteExchange(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        expected.put("type", 500);
        expected.put("title", "error");
        expected.put("detail", "Deletion of Exchange failed");
        rabbitClient.deleteExchange(request, "Dummy vHost").onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause());
            } else {
                JsonObject result = new JsonObject(handler.cause().getMessage());
                assertEquals(expected.getString("title"), result.getString("title"));
                assertEquals(expected.getString("detail"), result.getString("detail"));
                assertEquals(expected.getInteger("type"), result.getInteger("type"));
                vertxTestContext.completeNow();
            }
        });
    }

    @Test
    @DisplayName("Test listExchangeSubscribers method : For status code 404")
    public void testListExchangeSubscribersForSC_NOT_FOUND(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(404);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        expected.put("type", 404);
        expected.put("title", "failure");
        expected.put("detail", "Exchange not found");
        rabbitClient.listExchangeSubscribers(request, "Dummy vHost").onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals(expected.getString("title"), handler.result().getString("title"));
                assertEquals(expected.getString("detail"), handler.result().getString("detail"));
                assertEquals(expected.getInteger("type"), handler.result().getInteger("type"));
                vertxTestContext.completeNow();
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test listExchangeSubscribers method : Failure")
    public void testListExchangeSubscribersFailure(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        expected.put("type", 500);
        expected.put("title", "failure");
        expected.put("detail", "Internal server error");
        rabbitClient.listExchangeSubscribers(request, "Dummy vHost").onComplete(handler -> {
            if (handler.failed()) {
                JsonObject result = new JsonObject(handler.cause().getMessage());
                assertEquals(expected.getString("title"), result.getString("title"));
                assertEquals(expected.getString("detail"), result.getString("detail"));
                assertEquals(expected.getInteger("type"), result.getInteger("type"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test createQueue method : when status code is 400")
    public void testCreateQueue(VertxTestContext vertxTestContext) {

        when(webClient.requestAsync(anyString(), anyString(), any())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(400);
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        expected.put("type", 400);
        expected.put("title", "failure");
        expected.put("detail", "Queue already exists with different properties");
        rabbitClient.createQueue(request, "Dummy vHost").onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals(expected.getString("title"), handler.result().getString("title"));
                assertEquals(expected.getString("detail"), handler.result().getString("detail"));
                assertEquals(expected.getInteger("type"), handler.result().getInteger("type"));
                vertxTestContext.completeNow();
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test createQueue method : when status code is 400")
    public void testCreateQueueFailure(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString(), any())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        expected.put("type", 500);
        expected.put("title", "failure");
        expected.put("detail", "Creation of Queue failed");
        rabbitClient.createQueue(request, "Dummy vHost").onComplete(handler -> {
            if (handler.failed()) {
                JsonObject result = new JsonObject(handler.cause().getMessage());
                assertEquals(expected.getString("title"), result.getString("title"));
                assertEquals(expected.getString("detail"), result.getString("detail"));
                assertEquals(expected.getInteger("type"), result.getInteger("type"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test deleteQueue method : Failure")
    public void testDeleteQueueFailure(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        expected.put("type", 500);
        expected.put("title", "failure");
        expected.put("detail", "Deletion of Queue failed");
        rabbitClient.deleteQueue(request, "Dummy vHost").onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause());
            } else {
                JsonObject result = new JsonObject(handler.cause().getMessage());
                assertEquals(expected.getString("title"), result.getString("title"));
                assertEquals(expected.getString("detail"), result.getString("detail"));
                assertEquals(expected.getInteger("type"), result.getInteger("type"));
                vertxTestContext.completeNow();
            }
        });
    }

    @Test
    @DisplayName("Test bindQueue method : Failure")
    public void testBindQueueFailure(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString(), any())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        expected.put("type", 500);
        expected.put("title", "failure");
        expected.put("detail", "error in queue binding with adaptor");
        rabbitClient.bindQueue(request, "Dummy vHost").onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause());
            } else {
                JsonObject result = new JsonObject(handler.cause().getMessage());
                assertEquals(expected.getString("title"), result.getString("title"));
                assertEquals(expected.getString("detail"), result.getString("detail"));
                assertEquals(expected.getInteger("type"), result.getInteger("type"));
                vertxTestContext.completeNow();
            }
        });
    }

    @Test
    @DisplayName("Test unbindQueue method : status code 404")
    public void testUnbindQueueForSC_NOT_FOUND(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(404);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        expected.put("type", 404);
        expected.put("title", "failure");
        expected.put("detail", "Queue/Exchange/Routing Key does not exist");
        rabbitClient.unbindQueue(request, "Dummy vHost").onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals(expected.getString("title"), handler.result().getString("title"));
                assertEquals(expected.getString("detail"), handler.result().getString("detail"));
                assertEquals(expected.getInteger("type"), handler.result().getInteger("type"));
                vertxTestContext.completeNow();
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test unbindQueue method : Failure")
    public void testUnbindQueueFailure(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        expected.put("type", 500);
        expected.put("title", "failure");
        expected.put("detail", "error in queue binding with adaptor");
        rabbitClient.unbindQueue(request, "Dummy vHost").onComplete(handler -> {
            if (handler.failed()) {
                JsonObject result = new JsonObject(handler.cause().getMessage());
                assertEquals(expected.getString("title"), result.getString("title"));
                assertEquals(expected.getString("detail"), result.getString("detail"));
                assertEquals(expected.getInteger("type"), result.getInteger("type"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test createvHost method : when status code is 204 ")
    public void testCreatevHostWhenSC_NO_CONTENT(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(204);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        expected.put("type", 409);
        expected.put("title", "failure");
        expected.put("detail", "vHost already exists");
        rabbitClient.createvHost(request).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals(expected.getString("title"), handler.result().getString("title"));
                assertEquals(expected.getString("detail"), handler.result().getString("detail"));
                assertEquals(expected.getInteger("type"), handler.result().getInteger("type"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test createvHost method : Failure")
    public void testCreatevHostFailure(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        expected.put("type", 500);
        expected.put("title", "failure");
        expected.put("detail", "Creation of vHost failed");
        rabbitClient.createvHost(request).onComplete(handler -> {
            if (handler.failed()) {
                JsonObject result = new JsonObject(handler.cause().getMessage());
                assertEquals(expected.getString("title"), result.getString("title"));
                assertEquals(expected.getString("detail"), result.getString("detail"));
                assertEquals(expected.getInteger("type"), result.getInteger("type"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test deletevHost method : when status code is 400 ")
    public void testDeletevHostWhenSC_NOT_FOUND(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(400);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.deletevHost(request).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals("{}", handler.result().toString());
                vertxTestContext.completeNow();

            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test deletevHost method : Failure")
    public void testDeletevHostFailure(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        expected.put("type", 500);
        expected.put("title", "failure");
        expected.put("detail", "Deletion of vHost failed");
        rabbitClient.deletevHost(request).onComplete(handler -> {
            if (handler.failed()) {
                JsonObject result = new JsonObject(handler.cause().getMessage());
                assertEquals(expected.getString("title"), result.getString("title"));
                assertEquals(expected.getString("detail"), result.getString("detail"));
                assertEquals(expected.getInteger("type"), result.getInteger("type"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test listvHost method : for status code 404 ")
    public void testListvHostWhenSC_NOT_FOUND(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(404);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        expected.put("type", 404);
        expected.put("title", "failure");
        expected.put("detail", "No vhosts found");
        rabbitClient.listvHost(request).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals(expected.getString("title"), handler.result().getString("title"));
                assertEquals(expected.getString("detail"), handler.result().getString("detail"));
                assertEquals(expected.getInteger("type"), handler.result().getInteger("type"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test listvHost method : Failure")
    public void testListvHostHostFailure(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        expected.put("type", 500);
        expected.put("title", "failure");
        expected.put("detail", "Listing of vHost failed");
        rabbitClient.listvHost(request).onComplete(handler -> {
            if (handler.failed()) {
                JsonObject result = new JsonObject(handler.cause().getMessage());
                assertEquals(expected.getString("title"), result.getString("title"));
                assertEquals(expected.getString("detail"), result.getString("detail"));
                assertEquals(expected.getInteger("type"), result.getInteger("type"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test listQueueSubscribers method : for status code 404 ")
    public void testListQueueSubscribersWhenSC_NOT_FOUND(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(404);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        expected.put("type", 404);
        expected.put("title", "failure");
        expected.put("detail", "Queue does not exist");
        rabbitClient.listQueueSubscribers(request, vHost).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals(expected.getString("title"), handler.result().getString("title"));
                assertEquals(expected.getString("detail"), handler.result().getString("detail"));
                assertEquals(expected.getInteger("type"), handler.result().getInteger("type"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test listQueueSubscribers method : Failure")
    public void testListQueueSubscribersFailure(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        expected.put("type", 500);
        expected.put("title", "failure");
        expected.put("detail", "Listing of Queue failed");
        rabbitClient.listQueueSubscribers(request, vHost).onComplete(handler -> {
            if (handler.failed()) {
                JsonObject result = new JsonObject(handler.cause().getMessage());
                assertEquals(expected.getString("title"), result.getString("title"));
                assertEquals(expected.getString("detail"), result.getString("detail"));
                assertEquals(expected.getInteger("type"), result.getInteger("type"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test getExchange method : error")
    public void test_getExchange_with_error(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.getExchange(request, vHost).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause());
            } else {
                assertEquals("getExchange_errorthrowable", handler.cause().getMessage());
                vertxTestContext.completeNow();

            }
        });
    }

    @Test
    @DisplayName("Test getExchange method : when SC_NOT_FOUND")
    public void test_deletevHost_when_SC_NOT_FOUND(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(SC_NOT_FOUND);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        expected.put("type", 404);
        expected.put("title", "failure");
        expected.put("detail", "No vhosts found");
        rabbitClient.deletevHost(request).onComplete(handler -> {
            if (handler.failed()) {
                vertxTestContext.failNow(handler.cause());
            } else {
                assertEquals(expected.getString("title"), handler.result().getString("title"));
                assertEquals(expected.getString("detail"), handler.result().getString("detail"));
                assertEquals(expected.getInteger("type"), handler.result().getInteger("type"));
                vertxTestContext.completeNow();
            }
        });
    }

    @Test
    @DisplayName("Test registerAdapter method : with empty adaptorID")
    public void test_registerAdapter_empty_empty_adaptorID(VertxTestContext vertxTestContext) {
        JsonArray jsonArray = new JsonArray();
        jsonArray.add("");
        request.put("entities", jsonArray);
        expected.put("type", 400);
        expected.put("title", "Bad Request data");
        expected.put("detail", "Invalid/Missing Parameters");
        rabbitClient.registerAdapter(request, vHost).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause());
            } else {
                JsonObject result = new JsonObject(handler.cause().getMessage());
                assertEquals(expected.getString("title"), result.getString("title"));
                assertEquals(expected.getString("detail"), result.getString("detail"));
                assertEquals(expected.getInteger("type"), result.getInteger("type"));
                vertxTestContext.completeNow();
            }
        });
    }

    @Test
    @DisplayName("Test deleteAdapter method : Success ")
    public void test_deleteAdapter_success(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(200);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        expected.put("type", 200);
        expected.put("title", "success");
        expected.put("detail", "adaptor deleted");
        rabbitClient.deleteAdapter(request, vHost).onComplete(handler -> {
            if (handler.failed()) {
                vertxTestContext.failNow(handler.cause());
            } else {
                assertEquals(expected.getString("title"), handler.result().getString("title"));
                assertEquals(expected.getString("detail"), handler.result().getString("detail"));
                assertEquals(expected.getInteger("type"), handler.result().getInteger("type"));
                vertxTestContext.completeNow();
            }
        });
    }

    @Test
    @DisplayName("Test createUserIfNotExist method : Success")
    public void test_createUserIfNotExist_success(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(webClient.requestAsync(anyString(), anyString(), any())).thenReturn(httpResponseFuture);
        when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(rowSetAsyncResult);
                return null;
            }
        }).when(rowSetFuture).onComplete(any());
        when(bufferHttpResponse.statusCode()).thenReturn(SC_NOT_FOUND, SC_CREATED, SC_CREATED);
        when(rowSetAsyncResult.succeeded()).thenReturn(true);
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.createUserIfNotExist("Dummy/userID", "Dummy vHost").onComplete(handler -> {
            expected.put("userid", "Dummy/userID");
            expected.put("type", 200);
            expected.put("title", "vhostPermissions");
            expected.put("details", "write permission set");
            expected.put("vhostPermissions", "Dummy vHost");
            if (handler.succeeded()) {
                assertEquals(expected.getString("userid"), handler.result().getString("userid"));
                assertEquals(expected.getInteger("type"), handler.result().getInteger("type"));
                assertEquals(expected.getString("title"), handler.result().getString("title"));
                assertEquals(expected.getString("details"), handler.result().getString("details"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }


    @Test
    @DisplayName("Test createUserIfNotExist method : Failure")
    public void test_createUserIfNotExist_failure(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(webClient.requestAsync(anyString(), anyString(), any())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true, false);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(404);
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.createUserIfNotExist("Dummy/userID", "Dummy vHost").onComplete(handler -> {
            JsonObject result = new JsonObject(handler.cause().getMessage());
            expected.put("type", 500);
            expected.put("title", "error");
            expected.put("detail", "User creation failed");
            if (handler.failed()) {
                assertEquals(expected.getInteger("type"), result.getInteger("type"));
                assertEquals(expected.getString("title"), result.getString("title"));
                assertEquals(expected.getString("detail"), result.getString("detail"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }


    @Test
    @DisplayName("Test createUser method : Failure")
    public void test_createUser_with_failure_in_setting_vhostPermissions(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString(), any())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true, false);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(201);
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.createUser("Dummy/userID", "Dummy password", vHost, "Dummy/url.com").onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("Error : error in setting vhostPermissions", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test createUser method : with error in saving credentials")
    public void test_createUser_error(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString(), any())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(201);
        when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
        doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(rowSetAsyncResult);
                return null;
            }
        }).when(rowSetFuture).onComplete(any());
        when(rowSetAsyncResult.succeeded()).thenReturn(false);
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.createUser("Dummy/userID", "Dummy password", vHost, "Dummy/url.com").onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("Error : error in saving credentials", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test createUser method : with Network error")
    public void test_createUser_during_network_error(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString(), any())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(400);
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());

        rabbitClient.createUser("Dummy/userID", "Dummy password", vHost, "Dummy/url.com").onComplete(handler -> {
            if (handler.failed()) {
                expected.put("failure", "Network Issue");
                assertEquals(expected.toString(), handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test setVhostPermissions method :failure")
    public void test_setVhostPermissions_failure(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString(), any())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(400);
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());

        rabbitClient.setVhostPermissions("Dummy/shaUsername", vHost).onComplete(handler -> {
            if (handler.failed()) {
                expected.put("configure", "");
                expected.put("write", "None");
                expected.put("read", "None");
                assertEquals(expected.toString(), handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test queueBinding method :failure")
    public void test_queueBinding_failure(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString(), any())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);
        when(throwable.getCause()).thenReturn(throwable);
        when(throwable.toString()).thenReturn("Dummy failure message");
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        adaptorResultContainer.adaptorId="dummy_adapterid";
        adaptorResultContainer.type="resource";
        adaptorResultContainer.id="dummy_id";
        adaptorResultContainer.resourceServer="dummy_server";
        adaptorResultContainer.userid="dummy_userid";
        adaptorResultContainer.vhost="Dummy vHost";
        adaptorResultContainer.isExchnageCreated=true;
        adaptorResultContainer.apiKey="dummy api";
        rabbitClient.queueBinding(adaptorResultContainer, vHost).onComplete(handler -> {
            if (handler.failed()) {
                expected.put("type", 500);
                expected.put("title", "error");
                expected.put("detail", "error in queue binding with adaptor");
                JsonObject result = new JsonObject(handler.cause().getMessage());
                assertEquals(expected.getInteger("type"), result.getInteger("type"));
                assertEquals(expected.getString("title"), result.getString("title"));
                assertEquals(expected.getString("detail"), result.getString("detail"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test updateUserPermissions method :failure")
    public void test_updateUserPermissions_failure(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString(), any())).thenReturn(httpResponseFuture);
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(SC_OK, SC_NOT_FOUND);
        when(bufferHttpResponse.body()).thenReturn(buffer);
        when(buffer.toString()).thenReturn("[{ \"write\" : \"value\"}]");
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.updateUserPermissions(vHost, "Dummy userid", PermissionOpType.ADD_WRITE, "Dummy resource id").onComplete(handler -> {
            if (handler.failed()) {
                expected.put("type", "urn:dx:rs:badRequest");
                expected.put("status", 404);
                expected.put("title", "urn:dx:rs:badRequest");
                expected.put("detail", null);
                JsonObject result = new JsonObject(handler.cause().getMessage());
                assertEquals(expected.getString("type"), result.getString("type"));
                assertEquals(expected.getString("title"), result.getString("title"));
                assertEquals(expected.getString("status"), result.getString("status"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test updateUserPermissions method : with SC_INTERNAL_SERVER_ERROR")
    public void test_updateUserPermissions_with_SC_INTERNAL_SERVER_ERROR(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString(), any())).thenReturn(httpResponseFuture);
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true, false);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn("Dummy failure message");
        when(bufferHttpResponse.statusCode()).thenReturn(SC_OK, SC_NOT_FOUND);
        when(bufferHttpResponse.body()).thenReturn(buffer);
        when(buffer.toString()).thenReturn("[{ \"write\" : \"value\"}]");
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.updateUserPermissions(vHost, "Dummy userid", PermissionOpType.ADD_WRITE, "Dummy resource id").onComplete(handler -> {
            if (handler.failed()) {
                verify(webClient, times(1)).requestAsync(anyString(), anyString(), any());
                expected.put("type", "urn:dx:rs:badRequest");
                expected.put("status", 500);
                expected.put("title", "urn:dx:rs:badRequest");
                expected.put("detail", null);
                JsonObject result = new JsonObject(handler.cause().getMessage());
                assertEquals(expected.getString("type"), result.getString("type"));
                assertEquals(expected.getString("title"), result.getString("title"));
                assertEquals(expected.getString("status"), result.getString("status"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test getRabbitMQClient method")
    public void test_getRabbitMQClient(VertxTestContext vertxTestContext) {
        assertNotNull(rabbitClient.getRabbitmqClient());
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test listExchangeSubscribers method : with HttpStatus.SC_OK")
    public void test_listExchangeSubscribers_with_statusCode_200(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(SC_OK);
        when(bufferHttpResponse.body()).thenReturn(buffer);
        when(buffer.toString()).thenReturn("[{ \"write\" : \"value\",\"destination\" : \"destination_value\",\"routing_key\" : \"routing_value\"}]");
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.listExchangeSubscribers(request, vHost).onComplete(handler -> {
            if (handler.succeeded()) {
                List<String> expected_list = new ArrayList<>();
                expected_list.add("\"routing_value\"");
                expected.put("destination_value", expected_list);
                assertEquals(expected.getString("destination_value"), handler.result().getString("destination_value"));
                assertTrue(handler.result().containsKey("destination_value"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test listvHost method : with HttpStatus.SC_OK")
    public void test_listvHost_with_statusCode_200(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(SC_OK);
        when(bufferHttpResponse.body()).thenReturn(buffer);
        when(buffer.toString()).thenReturn("[{ \"name\" : \"vHost_name\",\"write\" : \"value\",\"destination\" : \"destination_value\",\"routing_key\" : \"routing_value\"}]");
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.listvHost(request).onComplete(handler -> {
            if (handler.succeeded()) {
                List<String> expected_list = new ArrayList<>();
                expected_list.add("\"vHost_name\"");
                expected.put("vHost", expected_list);
                assertTrue(handler.result().containsKey("vHost"));
                assertEquals(expected.getString("vHost"), handler.result().getString("vHost"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    static Stream<Arguments> routingKeys() {
        return Stream.of(
                Arguments.of("\"abcd_value\" : \"abcd_name\"", ""),
                Arguments.of("\"routing_key\" : \"routing_value\"", "\"routing_value\""),
                Arguments.of("\"routing_key\" : \"\"", "\"\"")
        );
    }

    @ParameterizedTest
    @MethodSource("routingKeys")
    @DisplayName("Test listQueueSubscribers method : with HttpStatus.SC_OK")
    public void test_listQueueSubscribers_with_statusCode_200(String routing, String expectedValue, VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(SC_OK);
        when(bufferHttpResponse.body()).thenReturn(buffer);
        when(buffer.toString()).thenReturn("[{ \"name\" : \"vHost_name\",\"write\" : \"value\",\"destination\" : \"destination_value\"," + routing + " }]");
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.listQueueSubscribers(request, vHost).onComplete(handler -> {
            if (handler.succeeded()) {
                List<String> expected_list = new ArrayList<>();
                expected_list.add(expectedValue);
                expected.put("entities", expected_list);
                assertTrue(handler.result().containsKey("entities"));
                assertEquals(expected.getString("entities"), handler.result().getString("entities"));

                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test unbindQueue method : with HttpStatus.SC_NO_CONTENT")
    public void test_unbindQueue_with_statusCode_204(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(SC_NO_CONTENT);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.unbindQueue(request, vHost).onComplete(handler -> {
            if (handler.succeeded()) {
                List<String> expected_list = new ArrayList<>();
                expected_list.add("\"{\\\"Dummy key\\\" : \\\"Dummy value\\\"}\"");
                expected.put("exchange", "Dummy exchangeName");
                expected.put("queue", "Dummy Queue name");
                expected.put("entities", expected_list);
                assertTrue(handler.result().containsKey("entities"));
                assertEquals(expected.getString("exchange"), handler.result().getString("exchange"));
                assertEquals(expected.getString("queue"), handler.result().getString("queue"));
                assertEquals(expected.getString("entities"), handler.result().getString("entities"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test deleteAdapter method : when SC_NOT_FOUND")
    public void test_deleteAdapter_when_SC_NOT_FOUND(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(SC_NOT_FOUND);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        expected.put("type", 404);
        expected.put("title", "not found");
        expected.put("detail", "Exchange not found");
        rabbitClient.deleteAdapter(request, vHost).onComplete(handler -> {
            if (handler.failed()) {
                JsonObject result = new JsonObject(handler.cause().getMessage());
                assertEquals(expected.getInteger("type"), result.getInteger("type"));
                assertEquals(expected.getString("title"), result.getString("title"));
                assertEquals(expected.getString("detail"), result.getString("detail"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test deleteAdapter method : failure")
    public void test_deleteAdapter_failure(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true, false);
        when(httpResponseAsyncResult.failed()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(SC_OK);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);
        when(throwable.toString()).thenReturn("Dummy failure message");
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        expected.put("type", 500);
        expected.put("title", "Adaptor deleted");
        expected.put("detail", "Dummy failure message");
        rabbitClient.deleteAdapter(request, vHost).onComplete(handler -> {
            if (handler.failed()) {
                JsonObject result = new JsonObject(handler.cause().getMessage());
                assertEquals(expected.getInteger("type"), result.getInteger("type"));
                assertEquals(expected.getString("title"), result.getString("title"));
                assertEquals(expected.getString("detail"), result.getString("detail"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test deleteAdapter method : Something went wrong in deleting adaptor")
    public void test_can_deleteAdapter_during_error(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true, false);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(SC_OK);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);
        when(throwable.toString()).thenReturn("Dummy failure message");
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        expected.put("type", 400);
        expected.put("title", "bad request");
        expected.put("detail", "nothing to delete");
        rabbitClient.deleteAdapter(request, vHost).onComplete(handler -> {
            if (handler.failed()) {
                JsonObject result = new JsonObject(handler.cause().getMessage());
                assertEquals(expected.getInteger("type"), result.getInteger("type"));
                assertEquals(expected.getString("title"), result.getString("title"));
                assertEquals(expected.getString("detail"), result.getString("detail"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }
}
*/
