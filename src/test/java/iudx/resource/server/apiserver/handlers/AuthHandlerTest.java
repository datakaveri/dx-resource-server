package iudx.resource.server.apiserver.handlers;

import static iudx.resource.server.apiserver.util.Constants.*;
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

import iudx.resource.server.common.Api;
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


@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class AuthHandlerTest {/*{


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
  private static String dxApiBasePath;

  private static Api apis;
  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) {
    jsonObject = new JsonObject();
    jsonObject.put("Dummy Key", "Dummy Value");
    jsonObject.put("IID", "Dummy IID value");
    jsonObject.put("USER_ID", "Dummy USER_ID");
    jsonObject.put("EXPIRY", "Dummy EXPIRY");
    jsonObject.put("dxApiBasePath","/ngsi-ld/v1");
    lenient().when(httpServerRequest.method()).thenReturn(httpMethod);
    lenient().when(httpMethod.toString()).thenReturn("GET");
    lenient().when(routingContext.request()).thenReturn(httpServerRequest);
    dxApiBasePath = jsonObject.getString("dxApiBasePath");
    dxApiBasePath = "/ngsi-ld/v1";
    apis = Api.getInstance(dxApiBasePath);

    authHandler = AuthHandler.create(Vertx.vertx(),apis);
    vertxTestContext.completeNow();
  }

  public static Stream<Arguments> urls() {
    dxApiBasePath = "/ngsi-ld/v1";
    apis = Api.getInstance(dxApiBasePath);
    return Stream.of(

        Arguments.of(Constants.EXCHANGE_URL_REGEX, IUDX_MANAGEMENT_URL + EXCHANGE_PATH + "(.*)"),
        Arguments.of(Constants.QUEUE_URL_REGEX, IUDX_MANAGEMENT_URL + QUEUE_PATH + "(.*)"),
        Arguments.of(Constants.VHOST_URL_REGEX, IUDX_MANAGEMENT_URL + VHOST + "(.*)"),
        Arguments.of(Constants.BIND_URL_REGEX, IUDX_MANAGEMENT_URL + BIND + "(.*)"),
        Arguments.of(Constants.UNBIND_URL_REGEX, IUDX_MANAGEMENT_URL + UNBIND + "(.*)"),
        Arguments.of(apis.getManagementBasePath()+"(.*)",  dxApiBasePath + RESET_PWD + "(.*)"),
        Arguments.of(Constants.REVOKE_TOKEN_REGEX, ADMIN + REVOKE_TOKEN + "(.*)"),
        Arguments.of(Constants.UNIQUE_ATTR_REGEX, ADMIN + RESOURCE_ATTRIBS),
        Arguments.of(apis.getIudxConsumerAuditUrl(), apis.getIudxConsumerAuditUrl()),
        Arguments.of(apis.getIudxProviderAuditUrl(), apis.getIudxProviderAuditUrl()),
        Arguments.of(apis.getIudxAsyncSearchApi(), apis.getIudxAsyncSearchApi()),
        Arguments.of(apis.getIudxAsyncStatusApi(), apis.getIudxAsyncStatusApi()),
        Arguments.of(apis.getMonthlyOverview(), apis.getMonthlyOverview()),
        Arguments.of(apis.getSummaryPath(), apis.getSummaryPath()),
        Arguments.of(apis.getEntitiesUrl(), apis.getEntitiesUrl()),
        Arguments.of(apis.getTemporalUrl(), apis.getTemporalUrl()),
        Arguments.of(apis.getPostEntitiesQueryPath(), apis.getPostEntitiesQueryPath()),
        Arguments.of(apis.getIngestionPath(), apis.getIngestionPath()),
        Arguments.of(apis.getPostTemporalQueryPath(), apis.getPostTemporalQueryPath()),
        Arguments.of(apis.getSubscriptionUrl(), apis.getSubscriptionUrl()));
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
    when(routingContext.request()).thenReturn(httpServerRequest);
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
    assertNotNull(AuthHandler.create(Vertx.vertx(),apis));
    vertxTestContext.completeNow();
  }

*/
}
