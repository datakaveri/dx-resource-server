package iudx.resource.server.databroker;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import iudx.resource.server.cache.CacheService;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static iudx.resource.server.databroker.util.Constants.ID;
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
    JsonObject expected;
    @Mock
    CacheService cacheService;
    @Mock
    RabbitMQOptions iudxConfig;
    @Mock
    Vertx vertx;
    @Mock
    RabbitMQClient iudxRabbitMQClient;

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
        databroker = new DataBrokerServiceImpl(webClient, pgClient, config,cacheService, /*iudxConfig, vertx,*/ iudxRabbitMQClient);
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
        expected = expected_success();
        databroker.getExchange(request, vHost, handler -> {
            if (handler.succeeded()) {
                assertEquals(expected, handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    public JsonObject expected_success()
    {
        expected.put("Dummy key","Dummy value");
        expected.put("id","Dummy ID");
        expected.put("status","Dummy status");
        expected.put("type",200);
        expected.put("routingKey","routingKeyValue");
        return expected;
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
        expected = expected_success();
        databroker.deleteAdaptor(request, vHost, handler -> {
            if (handler.succeeded()) {
                assertEquals(expected, handler.result());
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
        expected = expected_success();
        databroker.listAdaptor(request, vHost, handler -> {
            if (handler.succeeded()) {
                assertEquals(expected, handler.result());
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
        expected = expected_success();
        databroker.updateStreamingSubscription(request, handler -> {
            if (handler.succeeded()) {
                assertEquals(expected, handler.result());
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
        expected = expected_success();
        databroker.deleteStreamingSubscription(request, handler -> {
            if (handler.succeeded()) {
                assertEquals(expected, handler.result());
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
        expected = expected_success();
        databroker.appendStreamingSubscription(request, handler -> {
            if (handler.succeeded()) {
                assertEquals(expected, handler.result());
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
        expected = expected_success();
        databroker.listStreamingSubscription(request, handler -> {
            if (handler.succeeded()) {
                assertEquals(expected, handler.result());
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
        expected = expected_success();
        databroker.registerAdaptor(request, vHost, handler -> {
            if (handler.succeeded()) {
                assertEquals(expected, handler.result());
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
                expected = expected_success();
                assertEquals(expected, handler.result());
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

    public JsonObject expected_failure()
    {
        expected.put("type",400);
        expected.put("title","Bad Request data");
        expected.put("detail","Bad Request data");
        return expected;
    }
    @Test
    @Order(42)
    @DisplayName("Test deleteStreamingSubscription method : When request is empty")
    public void test_deleteStreamingSubscription_with_empty_request(VertxTestContext vertxTestContext) {
        expected = expected_failure();
        databroker.deleteStreamingSubscription(new JsonObject(), handler -> {
            if (handler.failed()) {
                assertEquals(expected.toString(), handler.cause().getMessage());
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
        expected = expected_failure();
        databroker.listStreamingSubscription(new JsonObject(), handler -> {
            if (handler.failed()) {
                assertEquals(expected.toString(), handler.cause().getMessage());
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
        expected = expected_failure();
        databroker.registerCallbackSubscription(new JsonObject(), handler -> {
            if (handler.failed()) {
                assertEquals(expected.toString(), handler.cause().getMessage());
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
        expected = expected_failure();
        databroker.deleteCallbackSubscription(new JsonObject(), handler -> {
            if (handler.failed()) {
                assertEquals(expected.toString(), handler.cause().getMessage());
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
        expected = expected_failure();
        databroker.listCallbackSubscription(new JsonObject(), handler -> {
            if (handler.failed()) {
                assertEquals(expected.toString(), handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

}
