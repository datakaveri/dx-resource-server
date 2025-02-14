package iudx.resource.server.apiserver.subscription;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.databroker.DataBrokerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)

public class StreamingSubscriptionTestService {
    @Mock
    DataBrokerService databroker;
    @Mock
    PostgresService pgService;
    StreamingSubscription service;
    @Mock
    JsonObject subscription;
    @Mock
    AsyncResult<JsonObject> asyncResult;
    @Mock
    Throwable throwable;
    JsonObject expected;
    String failureMessage;

    @BeforeEach
    @DisplayName("Test getInstance method")
    public void setUp(VertxTestContext vertxTestContext) {
        expected = new JsonObject();
        expected.put("Dummy key", "Dummy value");
        failureMessage = "Dummy failure message";
        service = new StreamingSubscription(databroker);
        vertxTestContext.completeNow();
    }

    public void failureMocks() {
        lenient().when(asyncResult.succeeded()).thenReturn(false);
        lenient().when(asyncResult.cause()).thenReturn(throwable);
        lenient().when(throwable.getMessage()).thenReturn(failureMessage);
    }

    public void successMocks() {
        when(asyncResult.result()).thenReturn(expected);
        when(asyncResult.succeeded()).thenReturn(true);
    }

    @DisplayName("Test create method : Success")
    @Test
    public void testCreateSuccess(VertxTestContext vertxTestContext) {
        successMocks();
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(databroker).registerStreamingSubscription(any(), any());

        service.create(subscription).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals(expected, handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
        vertxTestContext.completeNow();
    }

    @DisplayName("Test create method : Failure")
    @Test
    public void testCreateFailure(VertxTestContext vertxTestContext) {

        failureMocks();
        lenient().doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(databroker).registerStreamingSubscription(any(), any());
        service.create(subscription).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
        vertxTestContext.completeNow();
    }

    @DisplayName("Test update method : Success")
    @Test
    public void testUpdateSuccess(VertxTestContext vertxTestContext) {
        successMocks();
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(databroker).updateStreamingSubscription(any(), any());

        service.update(subscription).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals(expected, handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
        vertxTestContext.completeNow();
    }

    @DisplayName("Test update method : Failure")
    @Test
    public void testUpdateFailure(VertxTestContext vertxTestContext) {

        failureMocks();
        lenient().doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(databroker).updateStreamingSubscription(any(), any());
        service.update(subscription).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
        vertxTestContext.completeNow();
    }

    @DisplayName("Test append method : Success")
    @Test
    public void testAppendSuccess(VertxTestContext vertxTestContext) {
        successMocks();
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(databroker).appendStreamingSubscription(any(), any());

        service.append(subscription).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals(expected, handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
        vertxTestContext.completeNow();
    }

    @DisplayName("Test append method : Failure")
    @Test
    public void testAppendFailure(VertxTestContext vertxTestContext) {

        failureMocks();
        lenient().doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(databroker).appendStreamingSubscription(any(), any());

        service.append(subscription).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
        vertxTestContext.completeNow();
    }

    @DisplayName("Test delete method : Success")
    @Test
    public void testDeleteSuccess(VertxTestContext vertxTestContext) {
        successMocks();
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(databroker).deleteStreamingSubscription(any(), any());

        service.delete(subscription).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals(expected, handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
        vertxTestContext.completeNow();
    }

    @DisplayName("Test delete method : Failure")
    @Test
    public void testDeleteFailure(VertxTestContext vertxTestContext) {

        failureMocks();
        lenient().doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(databroker).deleteStreamingSubscription(any(), any());

        service.delete(subscription).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
        vertxTestContext.completeNow();
    }

    @DisplayName("Test get method : Success")
    @Test
    public void testGetSuccess(VertxTestContext vertxTestContext) {
        successMocks();
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(databroker).listStreamingSubscription(any(), any());

        service.get(subscription).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals(expected, handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
        vertxTestContext.completeNow();
    }

    @DisplayName("Test get method : Failure")
    @Test
    public void testGetFailure(VertxTestContext vertxTestContext) {

        failureMocks();
        lenient().doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(databroker).listStreamingSubscription(any(), any());

        service.get(subscription).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
        vertxTestContext.completeNow();
    }
}
