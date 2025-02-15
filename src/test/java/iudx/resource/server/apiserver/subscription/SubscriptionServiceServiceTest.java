package iudx.resource.server.apiserver.subscription;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;


import iudx.resource.server.apiserver.subscription.service.SubscriptionService;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.database.postgres.service.PostgresService;
import iudx.resource.server.databroker.service.DataBrokerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.stream.Stream;

import static iudx.resource.server.metering.util.Constants.USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class SubscriptionServiceServiceTest {
    SubscriptionService service;
    @Mock
    DataBrokerService databroker;
    @Mock
    PostgresService pgService;
    @Mock
    AsyncResult<JsonObject> asyncResult;
    @Mock
    JsonObject json;
    @Mock
    JsonObject authInfo;
    @Mock
    JsonArray jsonArray;
    @Mock
    Future<JsonObject> jsonObjectFuture;
    @Mock
    Throwable throwable;
    @Mock
    CacheService cacheService;

    @Test
    @DisplayName("Test createSubscription Success")
    public void testCreateSubscriptionSuccess(VertxTestContext vertxTestContext) {

        service = new SubscriptionService();
        service.subscription = mock(Subscription.class);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json);
        when(json.getJsonArray(any())).thenReturn(jsonArray);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getString(anyString())).thenReturn("STREAMING");
        when(jsonArray.getString(anyInt())).thenReturn("Dummy String");
        when(authInfo.getString(anyString())).thenReturn("Dummy authInfo value");
        when(service.subscription.create(any())).thenReturn(jsonObjectFuture);

        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(pgService).executeQuery(anyString(), any());

        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        when(cacheService.get(any())).thenReturn(Future.succeededFuture(json));

        service.createSubscription(json, databroker, pgService, authInfo, cacheService).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals(json, handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
        vertxTestContext.completeNow();
    }

    static Stream<Arguments> response() {
        return Stream.of(
                Arguments.of("{ \"type\":409 }",
                        "{\"type\":409,\"title\":\"Already exists\",\"detail\":\"Subscription Already exists\"}"),
                Arguments.of("{ \"type\":200, \"detail\" : \"Dummy detail\" }",
                        "{\"type\":200,\"title\":\"Bad Request\",\"detail\":\"Dummy detail\"}"),
                Arguments.of("{ \"type\":400, \"detail\" : \"Dummy detail\" }",
                        "{\"type\":400,\"title\":\"Bad Request\",\"detail\":\"Dummy detail\"}"),
                Arguments.of("{ \"type\":404 }",
                        "{\"type\":404,\"title\":\"Not Found\",\"detail\":\"Resource not found\"}"),
                Arguments.of("{ \"type\":500, \"detail\" : \"Dummy detail\"  }",
                        "{\"type\":500,\"title\":\"Bad Request\",\"detail\":\"Dummy detail\"}")

        );
    }

    @ParameterizedTest
    @MethodSource("response")
    @DisplayName("Test createSubscription Failure")
    public void testCreateSubscriptionFailure(String type, String result, VertxTestContext vertxTestContext) {

        service = new SubscriptionService();
        service.subscription = mock(Subscription.class);
        when(asyncResult.succeeded()).thenReturn(false);
        when(service.subscription.create(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(type);
        when(json.getString(anyString())).thenReturn("STREAMING");


        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());


        service.createSubscription(json, databroker, pgService, authInfo, cacheService).onComplete(handler -> {
            if (handler.failed()) {
                String throwable = "io.vertx.core.impl.NoStackTraceThrowable: ";
                String expected = throwable + result;
                assertEquals(expected, handler.cause().toString());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test updateSubscription method Success")
    public void testUpdateSubscriptionSuccess(VertxTestContext vertxTestContext) {
        service = new SubscriptionService();
        when(json.getString(anyString())).thenReturn("Dummy queueName");
        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(jsonArray.getString(anyInt())).thenReturn("Dummy entity");
        when(authInfo.getString(anyString())).thenReturn("Dummy authinfo value");
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json);
        when(jsonArray.size()).thenReturn(3);

        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(pgService).executeQuery(anyString(), any());
        service.updateSubscription(json, databroker, pgService, authInfo).onComplete(handler -> {
            if (handler.succeeded()) {
                JsonObject expected = new JsonObject("{\"type\":\"urn:dx:rs:success\",\"title\":\"success\",\"results\":[{\"entities\":[\"Dummy entity\"]}]}\n");
                assertEquals(expected, handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }
    @Test
    @DisplayName("Test updateSubscription method Success")
    public void testUpdateSubscriptionSuccessZero(VertxTestContext vertxTestContext) {
        service = new SubscriptionService();
        when(json.getString(anyString())).thenReturn("Dummy queueName");
        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(jsonArray.getString(anyInt())).thenReturn("Dummy entity");
        lenient().when(authInfo.getString(anyString())).thenReturn("Dummy authinfo value");
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json);
        when(jsonArray.size()).thenReturn(0);

        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(pgService).executeQuery(anyString(), any());
        service.updateSubscription(json, databroker, pgService, authInfo).onComplete(handler -> {
            if (handler.succeeded()) {
                JsonObject expected = new JsonObject().put("type",400)
                        .put("title","urn:dx:rs:resourceNotFound")
                        .put("details","Subscription not found for [queue,entity]");
                assertEquals(handler.cause(),expected);
                vertxTestContext.failNow(handler.cause());
            } else {
                vertxTestContext.completeNow();
            }
        });
    }

    @Test
    @DisplayName("Test updateSubscription method Failure")
    public void testUpdateSubscriptionFailure(VertxTestContext vertxTestContext) {
        service = new SubscriptionService();
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn("{ \"type\":409 }");
        when(asyncResult.succeeded()).thenReturn(false);
        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(json.getString(anyString())).thenReturn("Dummy queueName");
        when(jsonArray.getString(anyInt())).thenReturn("Dummy entity");
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(pgService).executeQuery(anyString(), any());
        service.updateSubscription(json, databroker, pgService, authInfo).onComplete(handler -> {
            if (handler.failed()) {
                String throwable = "io.vertx.core.impl.NoStackTraceThrowable: ";
                String expected = throwable + "{\"type\":409,\"title\":\"Already exists\",\"detail\":\"Subscription Already exists\"}";
                assertEquals(expected, handler.cause().toString());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test deleteSubscription method: Success ")
    public void testDeleteSubscriptionSuccess(VertxTestContext vertxTestContext) {
        when(json.getString(anyString())).thenReturn("STREAMING");
        service = new SubscriptionService();
        service.subscription = mock(Subscription.class);
        when(service.subscription.delete(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(pgService).executeQuery(anyString(), any());
        service.deleteSubscription(json, databroker, pgService).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals(json, handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test deleteSubscription method: Failure ")
    public void testDeleteSubscriptionFailure(VertxTestContext vertxTestContext) {
        lenient().when(json.getString(anyString())).thenReturn("STREAMING");
        service = new SubscriptionService();
        service.subscription = mock(Subscription.class);
        when(service.subscription.delete(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.succeeded()).thenReturn(false);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn("{ \"type\":404 }");

        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());


        service.deleteSubscription(json, databroker, pgService).onComplete(handler -> {
            if (handler.failed()) {
                String throwable = "io.vertx.core.impl.NoStackTraceThrowable: ";
                String expected = throwable + "{\"type\":404,\"title\":\"Not Found\",\"detail\":\"Resource not found\"}";
                assertEquals(expected, handler.cause().toString());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test getSubscription method : Success")
    public void testGetSubscriptionSuccess(VertxTestContext vertxTestContext) {
        lenient().when(json.getString(anyString())).thenReturn("STREAMING");
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json);
        service = new SubscriptionService();
        service.subscription = mock(Subscription.class);
        when(service.subscription.get(any())).thenReturn(jsonObjectFuture);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        service.getSubscription(json, databroker, pgService).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals(json, handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test getSubscription method : Failure")
    public void testGetSubscriptionFailure(VertxTestContext vertxTestContext) {
        lenient().when(json.getString(anyString())).thenReturn("STREAMING");
        when(asyncResult.succeeded()).thenReturn(false);
        when(asyncResult.cause()).thenReturn(throwable);
        service = new SubscriptionService();
        service.subscription = mock(Subscription.class);
        when(throwable.getMessage()).thenReturn("{ \"type\":200, \"detail\" : \"Dummy detail\" }");
        when(service.subscription.get(any())).thenReturn(jsonObjectFuture);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        service.getSubscription(json, databroker, pgService).onComplete(handler -> {
            if (handler.failed()) {
                String throwable = "io.vertx.core.impl.NoStackTraceThrowable: ";
                String expected = throwable + "{\"type\":200,\"title\":\"Bad Request\",\"detail\":\"Dummy detail\"}";
                assertEquals(expected, handler.cause().toString());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test appendSubscription method : Success")
    public void testAppendSubscriptionSuccess(VertxTestContext vertxTestContext) {
        when(json.getString(anyString())).thenReturn("STREAMING");
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json);
        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(jsonArray.getString(anyInt())).thenReturn("Dummy entity");
        when(authInfo.getString(anyString())).thenReturn("Dummy expiry");
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        service = new SubscriptionService();
        service.subscription = mock(Subscription.class);
        when(service.subscription.append(any())).thenReturn(jsonObjectFuture);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(pgService).executeQuery(anyString(), any());
        when(cacheService.get(any())).thenReturn(Future.succeededFuture(json));
        service.appendSubscription(json, databroker, pgService, authInfo,cacheService).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals("success", handler.result().getString("title"));
                assertEquals("urn:dx:rs:success", handler.result().getString("type"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test appendSubscription method : Failure")
    public void testAppendSubscriptionFailure(VertxTestContext vertxTestContext) {
        when(json.getString(anyString())).thenReturn("STREAMING");
        when(asyncResult.succeeded()).thenReturn(false);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn("{ \"type\":400, \"detail\" : \"Dummy detail\" }");

        service = new SubscriptionService();
        service.subscription = mock(Subscription.class);
        when(service.subscription.append(any())).thenReturn(jsonObjectFuture);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        service.appendSubscription(json, databroker, pgService, authInfo,cacheService).onComplete(handler -> {
            if (handler.failed()) {
                String throwable = "io.vertx.core.impl.NoStackTraceThrowable: ";
                String expected = throwable + "{\"type\":400,\"title\":\"Bad Request\",\"detail\":\"Dummy detail\"}";
                assertEquals(expected, handler.cause().toString());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    public void testAllSubscriptionQueue(VertxTestContext vertxTestContext) {
        SubscriptionService subscriptionService = new SubscriptionService();
        JsonObject jsonObject = new JsonObject().put(USER_ID, "89a36273d77dac4cf38114fca1bbe64392547f86");
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json);
        when(json.getString("title")).thenReturn("success");
        when(json.getString("type")).thenReturn("urn:dx:rs:success");

        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(pgService).executeQuery(anyString(), any());


        subscriptionService.getAllSubscriptionQueueForUser(jsonObject, pgService).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals("success", handler.result().getString("title"));
                assertEquals("urn:dx:rs:success", handler.result().getString("type"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }
}
