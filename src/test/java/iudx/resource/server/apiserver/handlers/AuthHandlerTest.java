package iudx.resource.server.apiserver.handlers;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.apiserver.util.Constants.IUDX_ASYNC_STATUS;
import static iudx.resource.server.common.Api.ADMIN;
import static iudx.resource.server.common.Api.BIND;
import static iudx.resource.server.common.Api.EXCHANGE;
import static iudx.resource.server.common.Api.INGESTION;
import static iudx.resource.server.common.Api.MANAGEMENT;
import static iudx.resource.server.common.Api.QUEUE;
import static iudx.resource.server.common.Api.RESET_PWD;
import static iudx.resource.server.common.Api.RESOURCE_ATTRIBS;
import static iudx.resource.server.common.Api.REVOKE_TOKEN;
import static iudx.resource.server.common.Api.UNBIND;
import static iudx.resource.server.common.Api.VHOST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Map;
import java.util.stream.Stream;

import iudx.resource.server.configuration.Configuration;
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
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.apiserver.util.Constants;
import iudx.resource.server.authenticator.AuthenticationService;


@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class AuthHandlerTest {


  @Mock
  RoutingContext routingContext;
  @Mock
  HttpServerResponse httpServerResponse;
  @Mock
  HttpServerRequest httpServerRequest;
  @Mock
  HttpMethod httpMethod;
  @Mock
  AsyncResult<JsonObject> asyncResult;
  @Mock
  MultiMap map;
  @Mock
  Throwable throwable;
  @Mock
  Future<Void> voidFuture;

  AuthHandler authHandler;
  JsonObject jsonObject;
  private static Configuration configuration;
  private static JsonObject jsonConfig;
  private static String basePath;

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) {
    authHandler = new AuthHandler();
    jsonObject = new JsonObject();
    jsonObject.put("Dummy Key", "Dummy Value");
    jsonObject.put("IID", "Dummy IID value");
    jsonObject.put("USER_ID", "Dummy USER_ID");
    jsonObject.put("EXPIRY", "Dummy EXPIRY");
//    jsonObject.put("basePath","/ngsi-ld/v1");
    lenient().when(httpServerRequest.method()).thenReturn(httpMethod);
    lenient().when(httpMethod.toString()).thenReturn("GET");
    lenient().when(routingContext.request()).thenReturn(httpServerRequest);
    configuration = new Configuration();
    jsonConfig = configuration.configLoader(2,Vertx.vertx());
    basePath = jsonConfig.getString("basePath");

    vertxTestContext.completeNow();
  }

  public static Stream<Arguments> urls() {
    return Stream.of(
            Arguments.of(basePath + ENTITITES_URL_REGEX, basePath + NGSILD_ENTITIES_URL + "(.*)"),
            Arguments.of(basePath + TEMPORAL_URL_REGEX, basePath +  NGSILD_TEMPORAL_URL + "(.*)"),
            Arguments.of(basePath + TEMPORAL_POST_QUERY_URL_REGEX,
                    basePath + NGSILD_POST_TEMPORAL_QUERY_PATH + "(.*)"),
            Arguments.of(basePath + ENTITIES_POST_QUERY_URL_REGEX,
                    basePath + NGSILD_POST_ENTITIES_QUERY_PATH + "(.*)"),
            Arguments.of(basePath + SUBSCRIPTION_URL_REGEX, basePath + NGSILD_SUBSCRIPTION_URL + "(.*)"),
            Arguments.of(basePath + ADAPTER_URL_REGEX, basePath + INGESTION.path + "(.*)"),
            Arguments.of(EXCHANGE_URL_REGEX, MANAGEMENT.path + EXCHANGE.path + "(.*)"),
            Arguments.of(QUEUE_URL_REGEX, MANAGEMENT.path + QUEUE.path + "(.*)"),
            Arguments.of(VHOST_URL_REGEX, MANAGEMENT.path + VHOST.path + "(.*)"),
            Arguments.of(BIND_URL_REGEX, MANAGEMENT.path + BIND.path + "(.*)"),
            Arguments.of(UNBIND_URL_REGEX, MANAGEMENT.path + UNBIND.path + "(.*)"),
            Arguments.of(RESET_URL_REGEX, MANAGEMENT.path + RESET_PWD.path + "(.*)"),
            Arguments.of(REVOKE_TOKEN_REGEX, ADMIN.path + REVOKE_TOKEN.path + "(.*)"),
            Arguments.of(UNIQUE_ATTR_REGEX, ADMIN.path + RESOURCE_ATTRIBS.path),
            Arguments.of(basePath +IUDX_CONSUMER_AUDIT_URL, basePath + IUDX_CONSUMER_AUDIT_URL),
            Arguments.of(basePath +IUDX_PROVIDER_AUDIT_URL,basePath + IUDX_PROVIDER_AUDIT_URL),
            Arguments.of(IUDX_ASYNC_SEARCH, "(.*)/async/search"),
            Arguments.of(IUDX_ASYNC_STATUS, "(.*)/async/status"));
  }


  @ParameterizedTest(name = "{index}) url = {0}, path = {1}")
  @MethodSource("urls")
  @DisplayName("Test handler for succeeded authHandler")
  public void testCanHandleSuccess(String url, String path, VertxTestContext vertxTestContext) {

    JsonObject jsonObject = mock(JsonObject.class);
    RequestBody requestBody = mock(RequestBody.class);

    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(jsonObject);
    when(jsonObject.copy()).thenReturn(jsonObject);
    when(routingContext.body().asJsonObject()).thenReturn(jsonObject);
    when(httpServerRequest.path()).thenReturn(url);
    AuthHandler.authenticator = mock(AuthenticationService.class);
    when(httpServerRequest.headers()).thenReturn(map);
    when(map.get(anyString())).thenReturn("Dummy Token");
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(jsonObject);
    doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
        return null;
      }
    }).when(AuthHandler.authenticator).tokenInterospect(any(), any(), any());

    authHandler.handle(routingContext);

    assertEquals(path, routingContext.request().path());
    assertEquals("Dummy Token", routingContext.request().headers().get(HEADER_TOKEN));
    assertEquals("GET", routingContext.request().method().toString());
    verify(AuthHandler.authenticator, times(1)).tokenInterospect(any(), any(), any());
    verify(routingContext, times(3)).body();

    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test handle method for Item not found")
  public void testCanHandleFailure(VertxTestContext vertxTestContext) {
    authHandler = new AuthHandler();
    String str = Constants.IUDX_ASYNC_STATUS;
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("Dummy Key", "Dummy Value");


    RequestBody requestBody = mock(RequestBody.class);

    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(jsonObject);
    //when(jsonObject.copy()).thenReturn(jsonObject);

    // when(routingContext.body().asJsonObject()).thenReturn(jsonObject);
    when(httpServerRequest.path()).thenReturn(str);
    AuthHandler.authenticator = mock(AuthenticationService.class);
    when(httpServerRequest.headers()).thenReturn(map);
    when(map.get(anyString())).thenReturn("Dummy token");
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Dummy throwable message: Not Found");
    when(routingContext.response()).thenReturn(httpServerResponse);
    when(httpServerResponse.putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);
    when(httpServerResponse.end(anyString())).thenReturn(voidFuture);
    when(asyncResult.succeeded()).thenReturn(false);
    doAnswer((Answer<AsyncResult<JsonObject>>) arg0 -> {
      ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
      return null;
    }).when(AuthHandler.authenticator).tokenInterospect(any(), any(), any());


    authHandler.handle(routingContext);

    assertEquals(Constants.IUDX_ASYNC_STATUS, routingContext.request().path());
    assertEquals("Dummy token", routingContext.request().headers().get(HEADER_TOKEN));
    assertEquals("GET", routingContext.request().method().toString());
    verify(AuthHandler.authenticator, times(1)).tokenInterospect(any(), any(), any());
    verify(httpServerResponse, times(1)).setStatusCode(anyInt());
    verify(httpServerResponse, times(1)).putHeader(anyString(), anyString());
    verify(httpServerResponse, times(1)).end(anyString());
    verify(routingContext, times(2)).body();

    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test handle method for Authentication Failure")
  public void testCanHandleAuthenticationFailure(VertxTestContext vertxTestContext) {
    authHandler = new AuthHandler();
    String str = Constants.IUDX_ASYNC_STATUS;
    JsonObject jsonObject = mock(JsonObject.class);
    Map<String, String> stringMap = mock(Map.class);

    RequestBody requestBody = mock(RequestBody.class);

    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(jsonObject);
    when(jsonObject.copy()).thenReturn(jsonObject);
    when(httpServerRequest.path()).thenReturn(str);

    when(routingContext.pathParams()).thenReturn(stringMap);
    when(routingContext.pathParams().isEmpty()).thenReturn(false);
    AuthHandler.authenticator = mock(AuthenticationService.class);
    when(routingContext.pathParams().containsKey(anyString())).thenReturn(true);
    when(routingContext.pathParams().get(anyString())).thenReturn("Dummy_value");
    when(httpServerRequest.headers()).thenReturn(map);
    when(map.get(anyString())).thenReturn("Dummy token");
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Dummy throwable message: Authentication Failure");
    when(routingContext.response()).thenReturn(httpServerResponse);
    when(httpServerResponse.putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);
    when(httpServerResponse.end(anyString())).thenReturn(voidFuture);
    when(asyncResult.succeeded()).thenReturn(false);
    doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
        return null;
      }
    }).when(AuthHandler.authenticator).tokenInterospect(any(), any(), any());

    authHandler.handle(routingContext);

    assertEquals(Constants.IUDX_ASYNC_STATUS, routingContext.request().path());
    assertEquals("Dummy token", routingContext.request().headers().get(HEADER_TOKEN));
    assertEquals("GET", routingContext.request().method().toString());
    verify(AuthHandler.authenticator, times(1)).tokenInterospect(any(), any(), any());
    verify(httpServerResponse, times(1)).setStatusCode(anyInt());
    verify(httpServerResponse, times(1)).putHeader(anyString(), anyString());
    verify(httpServerResponse, times(1)).end(anyString());
    verify(routingContext, times(2)).body();

    vertxTestContext.completeNow();
  }

  @DisplayName("Test create method")
  @Test
  public void testCanCreate(VertxTestContext vertxTestContext) {
    AuthHandler.authenticator = mock(AuthenticationService.class);
    assertNotNull(AuthHandler.create(Vertx.vertx(),jsonObject));
    vertxTestContext.completeNow();
  }

}
