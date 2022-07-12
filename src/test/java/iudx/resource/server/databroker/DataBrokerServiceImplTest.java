package iudx.resource.server.databroker;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rabbitmq.RabbitMQClient;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static iudx.resource.server.databroker.util.Constants.ID;
import static iudx.resource.server.databroker.util.Constants.USER_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class DataBrokerServiceImplTest {

    JsonObject request;
    String throwableMessage;
    DataBrokerServiceImpl databroker;
    String vHost;
    @Mock
    Future<JsonObject> jsonObjectFuture;
    @Mock
    AsyncResult<JsonObject> asyncResult;
    @Mock
    Throwable throwable;
    @Mock
    RabbitClient webClient;
    @Mock
    PostgresClient pgClient;
    @Mock
    RabbitMQClient rabbitMQClient;
    @Mock
    AsyncResult<Void> asyncResult1;
    DataBrokerServiceImpl databrokerSpy;

    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext) {
        vHost = "IUDX_INTERNAL";
        JsonObject config = mock(JsonObject.class);
        request = new JsonObject();
        request.put("Dummy key", "Dummy value");
        request.put(ID, "Dummy ID");
        request.put("status", "Dummy status");
        request.put("routingKey", "routingKeyValue");
        request.put("type", HttpStatus.SC_OK);
        throwableMessage = "Dummy failure message";
        when(config.getString(anyString())).thenReturn("internalVhost");
        databroker = new DataBrokerServiceImpl(webClient, pgClient, config);
        databrokerSpy = spy(databroker);
        vertxTestContext.completeNow();
    }

    @Test
    @Order(1)
    @DisplayName("Test registerCallbackSubscription method : Success")
    public void testRegisterCallbackSubscriptionSuccess(VertxTestContext vertxTestContext) {

        DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
        when(DataBrokerServiceImpl.subscriptionService.registerCallbackSubscription(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.registerCallbackSubscription(request, handler -> {
            if (handler.succeeded()) {
                assertEquals(request, handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(2)
    @DisplayName("Test registerCallbackSubscription : Failure")
    public void testRegisterCallbackSubscriptionFailure(VertxTestContext vertxTestContext) {

        DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
        when(DataBrokerServiceImpl.subscriptionService.registerCallbackSubscription(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());


        databroker.registerCallbackSubscription(request, handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }


    @Test
    @Order(3)
    @DisplayName("Test updateCallbackSubscription method : Success")
    public void testUpdateCallbackSubscriptionSuccess(VertxTestContext vertxTestContext) {

        DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
        when(DataBrokerServiceImpl.subscriptionService.updateCallbackSubscription(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        databroker.updateCallbackSubscription(request, handler -> {
            if (handler.succeeded()) {
                assertEquals(request, handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(4)
    @DisplayName("Test updateCallbackSubscription : Failure")
    public void testUpdateCallbackSubscriptionFailure(VertxTestContext vertxTestContext) {

        DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
        when(DataBrokerServiceImpl.subscriptionService.updateCallbackSubscription(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        databroker.updateCallbackSubscription(request, handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(5)
    @DisplayName("Test deleteCallbackSubscription method : Success")
    public void testDeleteCallbackSubscriptionSuccess(VertxTestContext vertxTestContext) {

        DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
        when(DataBrokerServiceImpl.subscriptionService.deleteCallbackSubscription(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        databroker.deleteCallbackSubscription(request, handler -> {
            if (handler.succeeded()) {
                assertEquals(request, handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(6)
    @DisplayName("Test deleteCallbackSubscription : Failure")
    public void testDeleteCallbackSubscriptionFailure(VertxTestContext vertxTestContext) {

        DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
        when(DataBrokerServiceImpl.subscriptionService.deleteCallbackSubscription(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        databroker.deleteCallbackSubscription(request, handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(7)
    @DisplayName("Test listCallbackSubscription method : Success")
    public void testListCallbackSubscriptionSuccess(VertxTestContext vertxTestContext) {

        DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
        when(DataBrokerServiceImpl.subscriptionService.listCallbackSubscription(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        databroker.listCallbackSubscription(request, handler -> {
            if (handler.succeeded()) {
                assertEquals(request, handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(8)
    @DisplayName("Test listCallbackSubscription : Failure")
    public void testListCallbackSubscriptionFailure(VertxTestContext vertxTestContext) {

        DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
        when(DataBrokerServiceImpl.subscriptionService.listCallbackSubscription(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        databroker.listCallbackSubscription(request, handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(9)
    @DisplayName("test updateAdaptor method ")
    public void test_updateAdaptor(VertxTestContext testContext) {
        DataBrokerService result = databroker.updateAdaptor(new JsonObject(), "Dummy vHost", AsyncResult::succeeded);
        assertNull(result);
        testContext.completeNow();
    }

    @Test
    @Order(10)
    @DisplayName("Test getExchange method : Failure")
    public void test_getExchange_failure(VertxTestContext vertxTestContext) {
        when(webClient.getExchange(any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.getExchange(request, vHost, handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(11)
    @DisplayName("Test getExchange method : Success")
    public void test_getExchange_success(VertxTestContext vertxTestContext) {
        when(webClient.getExchange(any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.getExchange(request, vHost, handler -> {
            if (handler.succeeded()) {
                assertEquals("{\"Dummy key\":\"Dummy value\",\"id\":\"Dummy ID\",\"status\":\"Dummy status\",\"routingKey\":\"routingKeyValue\",\"type\":200}", handler.result().toString());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(12)
    @DisplayName("Test deleteAdaptor method : Success")
    public void test_deleteAdaptor_success(VertxTestContext vertxTestContext) {
        when(webClient.deleteAdapter(any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.deleteAdaptor(request, vHost, handler -> {
            if (handler.succeeded()) {
                assertEquals("{\"Dummy key\":\"Dummy value\",\"id\":\"Dummy ID\",\"status\":\"Dummy status\",\"routingKey\":\"routingKeyValue\",\"type\":200}", handler.result().toString());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(13)
    @DisplayName("Test listAdaptor method : Success")
    public void test_listAdaptor_success(VertxTestContext vertxTestContext) {
        when(webClient.listExchangeSubscribers(any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.listAdaptor(request, vHost, handler -> {
            if (handler.succeeded()) {
                assertEquals("{\"Dummy key\":\"Dummy value\",\"id\":\"Dummy ID\",\"status\":\"Dummy status\",\"routingKey\":\"routingKeyValue\",\"type\":200}", handler.result().toString());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(14)
    @DisplayName("Test updateStreamingSubscription method : Success")
    public void test_updateStreamingSubscription_success(VertxTestContext vertxTestContext) {
        DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
        when(DataBrokerServiceImpl.subscriptionService.updateStreamingSubscription(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.updateStreamingSubscription(request, handler -> {
            if (handler.succeeded()) {
                assertEquals("{\"Dummy key\":\"Dummy value\",\"id\":\"Dummy ID\",\"status\":\"Dummy status\",\"routingKey\":\"routingKeyValue\",\"type\":200}", handler.result().toString());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(15)
    @DisplayName("Test deleteStreamingSubscription method : Success")
    public void test_deleteStreamingSubscription_success(VertxTestContext vertxTestContext) {
        DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
        when(DataBrokerServiceImpl.subscriptionService.deleteStreamingSubscription(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.deleteStreamingSubscription(request, handler -> {
            if (handler.succeeded()) {
                assertEquals("{\"Dummy key\":\"Dummy value\",\"id\":\"Dummy ID\",\"status\":\"Dummy status\",\"routingKey\":\"routingKeyValue\",\"type\":200}", handler.result().toString());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(16)
    @DisplayName("Test appendStreamingSubscription method : Success")
    public void test_appendStreamingSubscription_success(VertxTestContext vertxTestContext) {
        DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
        when(DataBrokerServiceImpl.subscriptionService.appendStreamingSubscription(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.appendStreamingSubscription(request, handler -> {
            if (handler.succeeded()) {
                assertEquals("{\"Dummy key\":\"Dummy value\",\"id\":\"Dummy ID\",\"status\":\"Dummy status\",\"routingKey\":\"routingKeyValue\",\"type\":200}", handler.result().toString());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(17)
    @DisplayName("Test queryDecoder method")
    public void test_queryDecoder(VertxTestContext vertxTestContext) {
        JsonObject result = databroker.queryDecoder(request);
        assertEquals("{}", result.toString());
        vertxTestContext.completeNow();
    }

    @Test
    @Order(18)
    @DisplayName("Test updatevHost method")
    public void test_updatevHost(VertxTestContext vertxTestContext) {
        DataBrokerService result = databroker.updatevHost(request, AsyncResult::succeeded);
        assertNull(result);
        vertxTestContext.completeNow();
    }

    @Test
    @Order(19)
    @DisplayName("Test updateExchange method")
    public void test_updateExchange(VertxTestContext vertxTestContext) {
        DataBrokerService result = databroker.updateExchange(request, vHost, AsyncResult::succeeded);
        assertNull(result);
        vertxTestContext.completeNow();
    }


    @Test
    @Order(20)
    @DisplayName("Test updateQueue method")
    public void test_updateQueue(VertxTestContext vertxTestContext) {
        DataBrokerService result = databroker.updateQueue(request, vHost, AsyncResult::succeeded);
        assertNull(result);
        vertxTestContext.completeNow();
    }

    @Test
    @Order(21)
    @DisplayName("Test listStreamingSubscription method : Success")
    public void test_listStreamingSubscription_success(VertxTestContext vertxTestContext) {
        DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
        when(DataBrokerServiceImpl.subscriptionService.listStreamingSubscriptions(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.listStreamingSubscription(request, handler -> {
            if (handler.succeeded()) {
                assertEquals("{\"Dummy key\":\"Dummy value\",\"id\":\"Dummy ID\",\"status\":\"Dummy status\",\"routingKey\":\"routingKeyValue\",\"type\":200}", handler.result().toString());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(22)
    @DisplayName("Test getExchange method : Success")
    public void test_registerAdaptor_success(VertxTestContext vertxTestContext) {
        when(webClient.registerAdapter(any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.registerAdaptor(request, vHost, handler -> {
            if (handler.succeeded()) {
                assertEquals("{\"Dummy key\":\"Dummy value\",\"id\":\"Dummy ID\",\"status\":\"Dummy status\",\"routingKey\":\"routingKeyValue\",\"type\":200}", handler.result().toString());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(23)
    @DisplayName("Test registerStreamingSubscription method : Success")
    public void test_registerStreamingSubscription_success(VertxTestContext vertxTestContext) {
        DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
        when(DataBrokerServiceImpl.subscriptionService.registerStreamingSubscription(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.registerStreamingSubscription(request, handler -> {
            if (handler.succeeded()) {
                assertEquals("{\"Dummy key\":\"Dummy value\",\"id\":\"Dummy ID\",\"status\":\"Dummy status\",\"routingKey\":\"routingKeyValue\",\"type\":200}", handler.result().toString());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(24)
    @DisplayName("Test deleteStreamingSubscription method : Failure")
    public void test_deleteStreamingSubscription_failure(VertxTestContext vertxTestContext) {
        DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
        when(DataBrokerServiceImpl.subscriptionService.deleteStreamingSubscription(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.deleteStreamingSubscription(request, handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(25)
    @DisplayName("Test listStreamingSubscription method : Failure")
    public void test_listStreamingSubscription_failure(VertxTestContext vertxTestContext) {
        DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
        when(DataBrokerServiceImpl.subscriptionService.listStreamingSubscriptions(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.listStreamingSubscription(request, handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(26)
    @DisplayName("Test updateCallbackSubscription method : Failure")
    public void test_updateCallbackSubscription_failure(VertxTestContext vertxTestContext) {
        DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
        when(DataBrokerServiceImpl.subscriptionService.updateCallbackSubscription(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.updateCallbackSubscription(request, handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(27)
    @DisplayName("Test deleteCallbackSubscription method : Failure")
    public void test_deleteCallbackSubscription_failure(VertxTestContext vertxTestContext) {
        DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
        when(DataBrokerServiceImpl.subscriptionService.deleteCallbackSubscription(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.deleteCallbackSubscription(request, handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(28)
    @DisplayName("Test listCallbackSubscription method : Failure")
    public void test_listCallbackSubscription_failure(VertxTestContext vertxTestContext) {
        DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
        when(DataBrokerServiceImpl.subscriptionService.listCallbackSubscription(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.listCallbackSubscription(request, handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(29)
    @DisplayName("Test createExchange method : Failure")
    public void test_createExchange_failure(VertxTestContext vertxTestContext) {
        when(webClient.createExchange(any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.createExchange(request, vHost, handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(30)
    @DisplayName("Test deleteExchange method : Failure")
    public void test_deleteExchange_failure(VertxTestContext vertxTestContext) {
        when(webClient.deleteExchange(any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.deleteExchange(request, vHost, handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(31)
    @DisplayName("Test listExchangeSubscribers method : Failure")
    public void test_listExchangeSubscribers_failure(VertxTestContext vertxTestContext) {
        when(webClient.listExchangeSubscribers(any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.listExchangeSubscribers(request, vHost, handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(32)
    @DisplayName("Test createQueue method : Failure")
    public void test_createQueue_failure(VertxTestContext vertxTestContext) {
        when(webClient.createQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.createQueue(request, vHost, handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(33)
    @DisplayName("Test deleteQueue method : Failure")
    public void test_deleteQueue_failure(VertxTestContext vertxTestContext) {
        when(webClient.deleteQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.deleteQueue(request, vHost, handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(34)
    @DisplayName("Test bindQueue method : Failure")
    public void test_bindQueue_failure(VertxTestContext vertxTestContext) {
        when(webClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.bindQueue(request, vHost, handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(35)
    @DisplayName("Test unbindQueue method : Failure")
    public void test_unbindQueue_failure(VertxTestContext vertxTestContext) {
        when(webClient.unbindQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.unbindQueue(request, vHost, handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(36)
    @DisplayName("Test createvHost method : Failure")
    public void test_createvHost_failure(VertxTestContext vertxTestContext) {
        when(webClient.createvHost(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.createvHost(request, handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(37)
    @DisplayName("Test deletevHost method : Failure")
    public void test_deletevHost_failure(VertxTestContext vertxTestContext) {
        when(webClient.deletevHost(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.deletevHost(request, handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(38)
    @DisplayName("Test listvHost method : Failure")
    public void test_listvHost_failure(VertxTestContext vertxTestContext) {
        when(webClient.listvHost(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.listvHost(request, handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(39)
    @DisplayName("Test listQueueSubscribers method : Failure")
    public void test_listQueueSubscribers_failure(VertxTestContext vertxTestContext) {
        when(webClient.listQueueSubscribers(any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.listQueueSubscribers(request, vHost, handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(40)
    @DisplayName("Test listAdaptor method : Failure")
    public void test_listAdaptor_failure(VertxTestContext vertxTestContext) {
        when(webClient.listExchangeSubscribers(any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.listAdaptor(request, vHost, handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(41)
    @DisplayName("Test listAdaptor method : When request is empty")
    public void test_listAdaptor_with_empty_request(VertxTestContext vertxTestContext) {
        databroker.listAdaptor(new JsonObject(), vHost, handler -> {
            if (handler.failed()) {
                assertEquals("{}", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(42)
    @DisplayName("Test deleteStreamingSubscription method : When request is empty")
    public void test_deleteStreamingSubscription_with_empty_request(VertxTestContext vertxTestContext) {
        databroker.deleteStreamingSubscription(new JsonObject(), handler -> {
            if (handler.failed()) {
                assertEquals("{\"type\":400,\"title\":\"Bad Request data\",\"detail\":\"Bad Request data\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(42)
    @DisplayName("Test listStreamingSubscription method : When request is empty")
    public void test_listStreamingSubscription_with_empty_request(VertxTestContext vertxTestContext) {
        databroker.listStreamingSubscription(new JsonObject(), handler -> {
            if (handler.failed()) {
                assertEquals("{\"type\":400,\"title\":\"Bad Request data\",\"detail\":\"Bad Request data\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(43)
    @DisplayName("Test registerCallbackSubscription method : When request is empty")
    public void test_registerCallbackSubscription_with_empty_request(VertxTestContext vertxTestContext) {
        databroker.registerCallbackSubscription(new JsonObject(), handler -> {
            if (handler.failed()) {
                assertEquals("{\"type\":400,\"title\":\"Bad Request data\",\"detail\":\"Bad Request data\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(44)
    @DisplayName("Test deleteCallbackSubscription method : When request is empty")
    public void test_deleteCallbackSubscription_with_empty_request(VertxTestContext vertxTestContext) {
        databroker.deleteCallbackSubscription(new JsonObject(), handler -> {
            if (handler.failed()) {
                assertEquals("{\"type\":400,\"title\":\"Bad Request data\",\"detail\":\"Bad Request data\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(45)
    @DisplayName("Test listCallbackSubscription method : When request is empty")
    public void test_listCallbackSubscription_with_empty_request(VertxTestContext vertxTestContext) {
        databroker.listCallbackSubscription(new JsonObject(), handler -> {
            if (handler.failed()) {
                assertEquals("{\"type\":400,\"title\":\"Bad Request data\",\"detail\":\"Bad Request data\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(46)
    @DisplayName("Test updateCallbackSubscription method : When request is empty")
    public void test_updateCallbackSubscription_with_empty_request(VertxTestContext vertxTestContext) {
        databroker.updateCallbackSubscription(new JsonObject(), handler -> {
            if (handler.failed()) {
                assertEquals("{\"type\":400,\"title\":\"Bad Request data\",\"detail\":\"Bad Request data\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(47)
    @DisplayName("Test publishFromAdaptor method : Success")
    public void test_publishFromAdaptor_Success(VertxTestContext vertxTestContext) {

        request = new JsonObject();
        request.put("Dummy key", "Dummy value");
        request.put(ID, "Dummy/ID/abcd/abcd");
        request.put("status", "Dummy status");
        request.put("routingKey", "routingKeyValue");
        request.put("type", HttpStatus.SC_OK);
        when(webClient.getRabbitMQClient()).thenReturn(rabbitMQClient);
        when(asyncResult1.succeeded()).thenReturn(true);

        doAnswer(new Answer<AsyncResult<Void>>() {
            @Override
            public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg0.getArgument(3)).handle(asyncResult1);
                return null;
            }
        }).when(rabbitMQClient).basicPublish(anyString(), anyString(), any(Buffer.class), any(Handler.class));
        databroker.publishFromAdaptor(request, vHost, handler -> {
            if (handler.succeeded()) {
                assertEquals("{\"status\":200}", handler.result().toString());
                assertEquals(200, handler.result().getInteger("status"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(48)
    @DisplayName("Test publishHeartbeat method : Success")
    public void test_publishHeartbeat_success(VertxTestContext vertxTestContext) {
        JsonObject queue = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        jsonArray.add("Dummy status");
        queue.put("efgh", jsonArray);

        when(webClient.getExchange(any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult.result()).thenReturn(request, queue);
        when(webClient.listExchangeSubscribers(any(), anyString())).thenReturn(jsonObjectFuture);
        when(webClient.getRabbitMQClient()).thenReturn(rabbitMQClient);
        when(asyncResult1.succeeded()).thenReturn(true);

        doAnswer(new Answer<AsyncResult<Void>>() {
            @Override
            public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg0.getArgument(3)).handle(asyncResult1);
                return null;
            }
        }).when(rabbitMQClient).basicPublish(anyString(), anyString(), any(Buffer.class), any(Handler.class));
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.publishHeartbeat(request, vHost, handler -> {
            if (handler.succeeded()) {
                assertEquals("success", handler.result().getString("type"));
                assertEquals("{\"type\":\"success\",\"queueName\":\"efgh\",\"routingKey\":\"Dummy status\",\"detail\":\"routingKey matched\"}", handler.result().toString());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });

    }

    @Test
    @Order(49)
    @DisplayName("Test publishHeartbeat method : Failure")
    public void test_publishHeartbeat_failure(VertxTestContext vertxTestContext) {
        JsonObject queue = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        jsonArray.add("Dummy status");
        queue.put("efgh", jsonArray);

        when(webClient.getExchange(any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult.result()).thenReturn(request, queue);
        when(webClient.listExchangeSubscribers(any(), anyString())).thenReturn(jsonObjectFuture);
        when(webClient.getRabbitMQClient()).thenReturn(rabbitMQClient);
        when(asyncResult1.succeeded()).thenReturn(false);
        when(asyncResult1.cause()).thenReturn(throwable);

        doAnswer(new Answer<AsyncResult<Void>>() {
            @Override
            public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg0.getArgument(3)).handle(asyncResult1);
                return null;
            }
        }).when(rabbitMQClient).basicPublish(anyString(), anyString(), any(Buffer.class), any(Handler.class));
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.publishHeartbeat(request, vHost, handler -> {
            if (handler.failed()) {
                assertEquals("{\"messagePublished\":\"failed\",\"type\":\"error\",\"detail\":\"routingKey not matched\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(50)
    @DisplayName("Test publishHeartbeat method : routingKey mismatch")
    public void test_publishHeartbeat_with_routingKey_mismatch(VertxTestContext vertxTestContext) {
        JsonObject queue = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        jsonArray.add("status");
        queue.put("efgh", jsonArray);

        when(webClient.getExchange(any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult.result()).thenReturn(request, queue);
        when(webClient.listExchangeSubscribers(any(), anyString())).thenReturn(jsonObjectFuture);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.publishHeartbeat(request, vHost, handler -> {
            if (handler.failed()) {
                assertEquals("publishHeartbeat - routingKey [ Dummy status ] not matched with [ status ] for queue [ efgh ]", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(51)
    @DisplayName("Test publishHeartbeat method : with empty queue")
    public void test_publishHeartbeat_with_empty_queue(VertxTestContext vertxTestContext) {
        JsonObject queue = new JsonObject();
        when(webClient.getExchange(any(), anyString())).thenReturn(jsonObjectFuture);
        when(asyncResult.result()).thenReturn(request, queue);
        when(webClient.listExchangeSubscribers(any(), anyString())).thenReturn(jsonObjectFuture);

        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.publishHeartbeat(request, vHost, handler -> {
            if (handler.failed()) {
                assertEquals("publishHeartbeat method - Oops !! None queue bound with given exchange", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(52)
    @DisplayName("Test publishHeartbeat method : when routing key is empty")
    public void test_publishHeartbeat_with_empty_routingKey(VertxTestContext vertxTestContext) {
        JsonObject queue = new JsonObject();
        request = new JsonObject();
        request.put("Dummy key", "Dummy value");
        request.put(ID, "Dummy ID");
        request.put("status", "");
        request.put("routingKey", "routingKeyValue");
        request.put("type", HttpStatus.SC_NOT_FOUND);
        databroker.publishHeartbeat(request, vHost, handler -> {
            if (handler.failed()) {
                assertEquals("publishHeartbeat - adaptor and routingKey not provided to publish message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(53)
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
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.publishHeartbeat(request, vHost, handler -> {
            if (handler.failed()) {
                assertEquals("Either adaptor does not exist or some other error to publish message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(54)
    @DisplayName("Test publishHeartbeat method : when request is NULL")
    public void test_publishHeartbeat_with_NULL_request(VertxTestContext vertxTestContext) {

        databroker.publishHeartbeat(null, vHost, handler -> {
            if (handler.failed()) {
                assertEquals("publishHeartbeat - request is null to publish message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(55)
    @DisplayName("Test resetPassword method : Failure")
    public void test_resetPassword_failure(VertxTestContext vertxTestContext) {

        request.put(USER_ID, "Dummy User ID");
        doAnswer(Answer -> Future.succeededFuture(true)).when(webClient).getUserInDb(anyString());
        databroker.resetPassword(request, handler -> {
            if (handler.failed()) {
                assertEquals("{\"type\":401,\"title\":\"not authorized\",\"detail\":\"not authorized\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(56)
    @DisplayName("Test resetPassword method : Success")
    public void test_resetPassword_success(VertxTestContext vertxTestContext) {
        request.put(USER_ID, "Dummy User ID");
        JsonObject mockJsonObject = mock(JsonObject.class);
        doAnswer(Answer -> Future.succeededFuture(mockJsonObject)).when(webClient).getUserInDb(anyString());
        doAnswer(Answer -> Future.succeededFuture(mockJsonObject)).when(webClient).resetPasswordInRMQ(anyString(), anyString());
        doAnswer(Answer -> Future.succeededFuture(mockJsonObject)).when(webClient).resetPwdInDb(anyString(), anyString());

        databroker.resetPassword(request, handler -> {
            if (handler.succeeded()) {
                JsonArray jsonArray = handler.result().getJsonArray("result");
                JsonObject object = jsonArray.getJsonObject(0);
                String actual = object.getString("username");
                assertEquals("Dummy User ID", actual);
                assertEquals("urn:dx:rs:success", handler.result().getString("type"));
                assertEquals("Successfully changed the password", handler.result().getString("title"));
                assertNotNull(handler.result().getString("result"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(57)
    @DisplayName("Test publishMessage method : Success")
    public void test_publishMessage_success(VertxTestContext vertxTestContext) {
        when(webClient.getRabbitMQClient()).thenReturn(rabbitMQClient);
        when(rabbitMQClient.isConnected()).thenReturn(true);
        doAnswer(Answer -> Future.succeededFuture()).when(rabbitMQClient).basicPublish(anyString(), anyString(), any(Buffer.class));
        databroker.publishMessage(request, "Dummy string to Exchange", "Dummy routing Key", handler -> {
            if (handler.succeeded()) {
                System.out.println(handler.result());
                assertTrue(handler.result().containsKey("type"));
                assertEquals("urn:dx:rs:success", handler.result().getString("type"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @Order(58)
    @DisplayName("Test publishMessage method : Failure")
    public void test_publishMessage_failure(VertxTestContext vertxTestContext) {
        when(webClient.getRabbitMQClient()).thenReturn(rabbitMQClient);
        when(rabbitMQClient.isConnected()).thenReturn(true);
        doAnswer(Answer -> Future.failedFuture("Dummy failure message")).when(rabbitMQClient).basicPublish(anyString(), anyString(), any(Buffer.class));
        databroker.publishMessage(request, "Dummy string to Exchange", "Dummy routing Key", handler -> {
            if (handler.failed()) {
                JsonObject expected = new JsonObject(handler.cause().getMessage());
                assertEquals("Dummy failure message",expected.getString("detail"));
                assertEquals("urn:dx:rs:QueueError",expected.getString("type"));
                assertEquals(400,expected.getInteger("status"));
                assertEquals("{\"type\":\"urn:dx:rs:QueueError\",\"status\":400,\"title\":null,\"detail\":\"Dummy failure message\"}",handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }
}
