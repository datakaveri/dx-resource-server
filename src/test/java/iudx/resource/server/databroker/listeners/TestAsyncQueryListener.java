package iudx.resource.server.databroker.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQConsumer;
import io.vertx.rabbitmq.RabbitMQMessage;
import io.vertx.rabbitmq.RabbitMQOptions;
import iudx.resource.server.database.async.AsyncService;


@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class TestAsyncQueryListener {
  AsyncQueryListener asyncQueryListener;
  @Mock
  Vertx vertx;
  @Mock
  RabbitMQOptions config;
  @Mock
  RabbitMQClient client;
  @Mock
  Future<Void> voidFuture;
  String vHost;
  @Mock
  AsyncResult<RabbitMQConsumer> consumerAsyncResult;
  @Mock
  RabbitMQConsumer rabbitMQConsumer;
  @Mock
  AsyncService asyncService;
  @Mock
  RabbitMQMessage message;
  @Mock
  Throwable throwable;


  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) {
    vHost = "Dummy vHost";
    asyncQueryListener = new AsyncQueryListener(vertx, config, vHost, asyncService);
    asyncQueryListener.client = mock(client.getClass());
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test start method : Success")
  public void test_start_success2(VertxTestContext vertxTestContext) {


    JsonObject object = new JsonObject();
    object.put("requestId", "dummy_key");
    object.put("searchId", "Dummy_unique-attribute");
    object.put("user", "Dummy_value");
    object.put("query", new JsonObject());
    object.put("format","csv");
    object.put("role","comsumer");
    object.put("drl","");
    object.put("did","");
    Buffer buffer = Buffer.buffer(object.toString());


    Future<Void> voidFuture=mock(Future.class);
    AsyncResult<Void> clientStartAsyncResult = mock(AsyncResult.class);
    AsyncResult<RabbitMQConsumer> consumerASyncResult = mock(AsyncResult.class);
    RabbitMQConsumer rmqConsumer = mock(RabbitMQConsumer.class);
    RabbitMQMessage message = mock(RabbitMQMessage.class);


    when(asyncQueryListener.client.start()).thenReturn(voidFuture);
    when(clientStartAsyncResult.succeeded()).thenReturn(true);
    when(consumerASyncResult.succeeded()).thenReturn(true);
    when(consumerASyncResult.result()).thenReturn(rmqConsumer);
    when(message.body()).thenReturn(buffer);



    doAnswer(new Answer<AsyncResult<RabbitMQConsumer>>() {
      @Override
      public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<Void>>) arg0.getArgument(0)).handle(clientStartAsyncResult);
        return null;
      }
    }).when(voidFuture).onComplete(any());

    doAnswer(new Answer<AsyncResult<RabbitMQConsumer>>() {
      @Override
      public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<RabbitMQConsumer>>) arg0.getArgument(2)).handle(consumerASyncResult);
        return null;
      }
    }).when(asyncQueryListener.client).basicConsumer(anyString(), any(), any());

    doAnswer(new Answer<RabbitMQMessage>() {
      @Override
      public RabbitMQMessage answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<RabbitMQMessage>) arg0.getArgument(0)).handle(message);
        return null;
      }
    }).when(rmqConsumer).handler(any());

    asyncQueryListener.start();
    verify(voidFuture, times(1)).onComplete(any());
    verify(clientStartAsyncResult).succeeded();
    verify(asyncService).asyncSearch(anyString(), anyString(), any(),anyString());
    verify(message).body();
    assertEquals(buffer, message.body());
    vertxTestContext.completeNow();

  }


  @Test
  @DisplayName("Test start method : with null message body")
  public void test_start_with_null_body(VertxTestContext vertxTestContext) {

    Future<Void> voidFuture=mock(Future.class);
    AsyncResult<Void> clientStartAsyncResult = mock(AsyncResult.class);
    AsyncResult<RabbitMQConsumer> consumerASyncResult = mock(AsyncResult.class);
    RabbitMQConsumer rmqConsumer = mock(RabbitMQConsumer.class);
    RabbitMQMessage message = mock(RabbitMQMessage.class);


    when(asyncQueryListener.client.start()).thenReturn(voidFuture);
    when(clientStartAsyncResult.succeeded()).thenReturn(true);
    when(consumerASyncResult.succeeded()).thenReturn(true);
    when(consumerASyncResult.result()).thenReturn(rmqConsumer);
    when(message.body()).thenReturn(null);



    doAnswer(new Answer<AsyncResult<RabbitMQConsumer>>() {
      @Override
      public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<Void>>) arg0.getArgument(0)).handle(clientStartAsyncResult);
        return null;
      }
    }).when(voidFuture).onComplete(any());

    doAnswer(new Answer<AsyncResult<RabbitMQConsumer>>() {
      @Override
      public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<RabbitMQConsumer>>) arg0.getArgument(2)).handle(consumerASyncResult);
        return null;
      }
    }).when(asyncQueryListener.client).basicConsumer(anyString(), any(), any());

    doAnswer(new Answer<RabbitMQMessage>() {
      @Override
      public RabbitMQMessage answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<RabbitMQMessage>) arg0.getArgument(0)).handle(message);
        return null;
      }
    }).when(rmqConsumer).handler(any());

    asyncQueryListener.start();
    verify(voidFuture).onComplete(any());
    verify(clientStartAsyncResult).succeeded();
    verify(message).body();
    assertEquals(null, message.body());
    vertxTestContext.completeNow();
  }


  @Test
  @DisplayName("Test start method : Failure")
  public void test_start_failure(VertxTestContext vertxTestContext) {
    when(asyncQueryListener.client.start()).thenReturn(voidFuture);
    when(voidFuture.succeeded()).thenReturn(false);
    asyncQueryListener.start();
    verify(voidFuture).onComplete(any());
    assertFalse(voidFuture.succeeded());
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test start method : Success")
  public void test_start_success3(VertxTestContext vertxTestContext) {


    JsonObject object = new JsonObject();
    object.put("requestId", "dummy_key");
    object.put("searchId", "Dummy_unique-attribute");
    object.put("user", "Dummy_value");
    object.put("query", new JsonObject());
    object.put("format","csv");
    object.put("role","delegate");
    object.put("drl","dummydrl");
    object.put("did","dummydid");
    Buffer buffer = Buffer.buffer(object.toString());


    Future<Void> voidFuture=mock(Future.class);
    AsyncResult<Void> clientStartAsyncResult = mock(AsyncResult.class);
    AsyncResult<RabbitMQConsumer> consumerASyncResult = mock(AsyncResult.class);
    RabbitMQConsumer rmqConsumer = mock(RabbitMQConsumer.class);
    RabbitMQMessage message = mock(RabbitMQMessage.class);


    when(asyncQueryListener.client.start()).thenReturn(voidFuture);
    when(clientStartAsyncResult.succeeded()).thenReturn(true);
    when(consumerASyncResult.succeeded()).thenReturn(true);
    when(consumerASyncResult.result()).thenReturn(rmqConsumer);
    when(message.body()).thenReturn(buffer);



    doAnswer(new Answer<AsyncResult<RabbitMQConsumer>>() {
      @Override
      public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<Void>>) arg0.getArgument(0)).handle(clientStartAsyncResult);
        return null;
      }
    }).when(voidFuture).onComplete(any());

    doAnswer(new Answer<AsyncResult<RabbitMQConsumer>>() {
      @Override
      public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<RabbitMQConsumer>>) arg0.getArgument(2)).handle(consumerASyncResult);
        return null;
      }
    }).when(asyncQueryListener.client).basicConsumer(anyString(), any(), any());

    doAnswer(new Answer<RabbitMQMessage>() {
      @Override
      public RabbitMQMessage answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<RabbitMQMessage>) arg0.getArgument(0)).handle(message);
        return null;
      }
    }).when(rmqConsumer).handler(any());

    asyncQueryListener.start();
    verify(voidFuture, times(1)).onComplete(any());
    verify(clientStartAsyncResult).succeeded();
    verify(asyncService).asyncSearch(anyString(), anyString(), any(),anyString());
    verify(message).body();
    assertEquals(buffer, message.body());
    vertxTestContext.completeNow();

  }
}
