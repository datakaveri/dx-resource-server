package iudx.resource.server.authenticator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.Handler;
import iudx.resource.server.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.micrometer.core.ipc.http.HttpSender.Method;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.cache.cachelmpl.CacheType;
import iudx.resource.server.configuration.Configuration;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.metering.MeteringService;
import org.mockito.stubbing.Answer;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class JwtAuthServiceImplTest {
  @Mock
  HttpRequest<Buffer> httpRequest;
  @Mock
  HttpResponse<Buffer> httpResponse;

  @Mock
  AsyncResult<HttpResponse<Buffer>> asyncResult;
  @Mock
  HttpRequest<Buffer> httpRequestMock;
  @Mock
  HttpResponse<Buffer> httpResponseMock;

  private static final Logger LOGGER = LogManager.getLogger(JwtAuthServiceImplTest.class);
  private static JsonObject authConfig;
  private static JwtAuthenticationServiceImpl jwtAuthenticationService;
  private static Configuration config;
  private static String openId;
  private static String closeId;
  private static String invalidId;
  private static PostgresService pgService;
  private static CacheService cacheService;
  private static MeteringService meteringService;
  private static Api apis;

  @BeforeAll
  @DisplayName("Initialize Vertx and deploy Auth Verticle")
  static void init(Vertx vertx, VertxTestContext testContext) {
    config = new Configuration();
    authConfig = config.configLoader(1, vertx);
    authConfig.put("dxApiBasePath","/ngsi-ld/v1");
    authConfig.put("dxCatalogueBasePath", "/iudx/cat/v1");
    authConfig.put("dxAuthBasePath", "/auth/v1");

    apis = Api.getInstance("/ngsi-ld/v1");
    JWTAuthOptions jwtAuthOptions = new JWTAuthOptions();
    jwtAuthOptions.addPubSecKey(
            new PubSecKeyOptions()
                    .setAlgorithm("ES256")
                    .setBuffer("-----BEGIN PUBLIC KEY-----\n" +
                            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE8BKf2HZ3wt6wNf30SIsbyjYPkkTS\n" +
                            "GGyyM2/MGF/zYTZV9Z28hHwvZgSfnbsrF36BBKnWszlOYW0AieyAUKaKdg==\n" +
                            "-----END PUBLIC KEY-----\n" +
                            ""));
    jwtAuthOptions.getJWTOptions().setIgnoreExpiration(true);// ignore token expiration only
    // for
    // test
    JWTAuth jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);

    cacheService = Mockito.mock(CacheService.class);
    meteringService=Mockito.mock(MeteringService.class);
    WebClient webClient = AuthenticationVerticle.createWebClient(vertx, authConfig, true);
    jwtAuthenticationService =
            new JwtAuthenticationServiceImpl(jwtAuth);

    // since test token doesn't contains valid id's, so forcibly put some dummy id in cache
    // for
    // test.
    openId =
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood";
    closeId =
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information";
    invalidId = "example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group1";
//    iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information

//    jwtAuthenticationService.resourceIdCache.put(openId, "OPEN");
//    jwtAuthenticationService.resourceIdCache.put(closeId, "CLOSED");
//    jwtAuthenticationService.resourceIdCache.put(invalidId, "CLOSED");
//    AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
//    when(asyncResult.succeeded()).thenReturn(false);

//    Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
//      @SuppressWarnings("unchecked")
//      @Override
//      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
//        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
//        return null;
//      }
//    }).when(cacheService).get(any());

   /* when(cacheService.get(any())).thenReturn(Future.failedFuture("failed"));
    
    JsonObject openIdJson=new JsonObject();
    openIdJson.put("type", CacheType.CATALOGUE_CACHE);
    openIdJson.put("key", openId);
    when(cacheService.get(openIdJson)).thenReturn(Future.succeededFuture(new JsonObject().put("accessPolicy", "OPEN")));
    
    JsonObject closedIdJson=new JsonObject();
    closedIdJson.put("type", CacheType.CATALOGUE_CACHE);
    closedIdJson.put("key", closeId);
    when(cacheService.get(closedIdJson)).thenReturn(Future.succeededFuture(new JsonObject().put("accessPolicy", "SECURE")));
    
    JsonObject invalidIdJson=new JsonObject();
    invalidIdJson.put("type", CacheType.CATALOGUE_CACHE);
    invalidIdJson.put("key", invalidId);
    when(cacheService.get(invalidIdJson)).thenReturn(Future.failedFuture("Failed future"));
    */
    LOGGER.info("Auth tests setup complete");
    testContext.completeNow();
  }

  @Test
  @Order(1)
  @DisplayName("Testing setup")
  public void shouldSucceed(VertxTestContext testContext) {
    LOGGER.info("Default test is passing");
    testContext.completeNow();
  }


  @Test
  @Order(5)
  @DisplayName("success - allow consumer access to /entities endpoint")
  public void success4ConsumerTokenEntitiesAPI(VertxTestContext testContext) {

    JsonObject request = new JsonObject();
    JsonObject authInfo = new JsonObject();
    authInfo.put("token", JwtTokenHelper.closedConsumerApiToken);
    authInfo.put("id", closeId);
    authInfo.put("apiEndpoint", apis.getEntitiesUrl());
    authInfo.put("method", Method.GET);
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject =
        new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name", "dummy_name")
            .put("resourceGroup", "5b7556b5-0779-4c47-9cf2-3f209779aa22")
            .put("value", "2021-09-09T12:52:37")
            .put("accessPolicy", "OPEN");

    JwtData jwtData = new JwtData();
    jwtData.setSub("valid_sub");
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid("rg:example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
    jwtData.setRole("provider");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api")));

    JsonObject revokedTokenRequest=new JsonObject();
    revokedTokenRequest.put("type", CacheType.REVOKED_CLIENT);
    revokedTokenRequest.put("key", jwtData.getSub());

    when(cacheService.get(revokedTokenRequest)).thenReturn(Future.succeededFuture(new JsonObject().put("value","2021-09-09T12:52:37")));

    when(cacheService.get(any())).thenReturn(Future.succeededFuture(jsonObject));
    jwtAuthenticationService.tokenIntrospect(authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Order(6)
  @DisplayName("success - allow consumer access to /subscription endpoint")
  public void success4ConsumerTokenSubsAPI(VertxTestContext testContext) {

    JsonObject request = new JsonObject();
    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.openConsumerSubsToken);
    authInfo.put("id", openId);
    authInfo.put("apiEndpoint", apis.getSubscriptionUrl());
    authInfo.put("method", Method.POST);
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22")
            .put("value", "2021-09-09T13:10:01")
            .put("accessPolicy","OPEN");

    JwtData jwtData = new JwtData();
    jwtData.setSub("valid_sub");
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid("rg:example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
    jwtData.setRole("provider");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api")));

    JsonObject revokedTokenRequest=new JsonObject();
    revokedTokenRequest.put("type", CacheType.REVOKED_CLIENT);
    revokedTokenRequest.put("key", jwtData.getSub());

    when(cacheService.get(revokedTokenRequest)).thenReturn(Future.succeededFuture(new JsonObject().put("value", "2021-09-09T13:10:01")));

    when(cacheService.get(any())).thenReturn(Future.succeededFuture(jsonObject));
    jwtAuthenticationService.tokenIntrospect(authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Order(7)
  @DisplayName("success - allow delegate access to /ingestion endpoint")
  public void allow4DelegateTokenIngestAPI(VertxTestContext testContext) {

    JsonObject request = new JsonObject();
    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.closedDelegateIngestToken);
    authInfo.put("id", closeId);
    authInfo.put("apiEndpoint", apis.getIngestionPath());
    authInfo.put("method", Method.POST);
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22")
            .put("value", "2021-09-09T14:04:07")
            .put("accessPolicy","OPEN");

    JwtData jwtData = new JwtData();
    jwtData.setSub("valid_sub");
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid("rg:example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
    jwtData.setRole("provider");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api")));

    JsonObject revokedTokenRequest=new JsonObject();
    revokedTokenRequest.put("type", CacheType.REVOKED_CLIENT);
    revokedTokenRequest.put("key", jwtData.getSub());

    when(cacheService.get(revokedTokenRequest)).thenReturn(Future.succeededFuture(new JsonObject().put("value", "2021-09-09T14:04:071")));

    when(cacheService.get(any())).thenReturn(Future.succeededFuture(jsonObject));
    jwtAuthenticationService.tokenIntrospect(authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Order(8)
  @DisplayName("failure - provider role -> subscription access")
  public void providerTokenSubsAPI(VertxTestContext testContext) {

    JsonObject request = new JsonObject();
    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.closedProviderSubsToken);
    authInfo.put("id", closeId);
    authInfo.put("apiEndpoint", apis.getSubscriptionUrl());
    authInfo.put("method", Method.POST);
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22")
            .put("value", "2021-09-09T13:00:39")
            .put("accessPolicy","OPEN");

    JwtData jwtData = new JwtData();
    jwtData.setSub("valid_sub");
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid("rg:example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
    jwtData.setRole("provider");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api")));

    JsonObject revokedTokenRequest=new JsonObject();
    revokedTokenRequest.put("type", CacheType.REVOKED_CLIENT);
    revokedTokenRequest.put("key", jwtData.getSub());

    when(cacheService.get(revokedTokenRequest)).thenReturn(Future.succeededFuture(new JsonObject().put("value", "2021-09-09T13:00:39")));
    when(cacheService.get(any())).thenReturn(Future.succeededFuture(jsonObject));
    jwtAuthenticationService.tokenIntrospect(authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Order(9)
  @DisplayName("success - consumer role -> subscription access")
  public void closedConsumerTokenSubsAPI(VertxTestContext testContext) {

    JsonObject request = new JsonObject();
    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.closedConsumerApiSubsToken);
    authInfo.put("id", closeId);
    authInfo.put("apiEndpoint",  apis.getSubscriptionUrl());
    authInfo.put("method", Method.GET);
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22")
            .put("value", "2021-09-09T12:52:37")
            .put("accessPolicy","OPEN");

    JwtData jwtData = new JwtData();
    jwtData.setSub("valid_sub");
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid("rg:example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
    jwtData.setRole("provider");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api")));

    JsonObject revokedTokenRequest=new JsonObject();
    revokedTokenRequest.put("type", CacheType.REVOKED_CLIENT);
    revokedTokenRequest.put("key", jwtData.getSub());

    when(cacheService.get(revokedTokenRequest)).thenReturn(Future.succeededFuture(new JsonObject().put("value", "2021-09-09T12:52:37")));
    when(cacheService.get(any())).thenReturn(Future.succeededFuture(jsonObject));
    jwtAuthenticationService.tokenIntrospect(authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Order(10)
  @DisplayName("success - consumer role -> subscription access")
  public void openConsumerTokenSubsAPI(VertxTestContext testContext) {

    JsonObject request = new JsonObject();
    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.openConsumerSubsToken);
    authInfo.put("id", openId);
    authInfo.put("apiEndpoint", apis.getSubscriptionUrl());
    authInfo.put("method", Method.POST);

    jwtAuthenticationService.tokenIntrospect(authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Order(12)
  @DisplayName("decode valid jwt")
  public void decodeJwtProviderSuccess(VertxTestContext testContext) {
    jwtAuthenticationService.decodeJwt(JwtTokenHelper.closedProviderApiToken)
            .onComplete(handler -> {
              if (handler.succeeded()) {
                assertEquals("provider", handler.result().getRole());
                testContext.completeNow();
              } else {
                testContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @Order(13)
  @DisplayName("decode valid jwt - delegate")
  public void decodeJwtDelegateSuccess(VertxTestContext testContext) {
    jwtAuthenticationService.decodeJwt(JwtTokenHelper.closedDelegateApiToken)
            .onComplete(handler -> {
              if (handler.succeeded()) {
                assertEquals("delegate", handler.result().getRole());
                testContext.completeNow();
              } else {
                testContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @Order(14)
  @DisplayName("decode valid jwt - consumer")
  public void decodeJwtConsumerSuccess(VertxTestContext testContext) {
    jwtAuthenticationService.decodeJwt(JwtTokenHelper.closedConsumerApiSubsToken)
            .onComplete(handler -> {
              if (handler.succeeded()) {
                assertEquals("consumer", handler.result().getRole());
                testContext.completeNow();
              } else {
                testContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @Order(15)
  @DisplayName("decode invalid jwt")
  public void decodeJwtFailure(VertxTestContext testContext) {
    String jwt =
            "eyJ0eXAiOiJKV1QiLCJbGciOiJFUzI1NiJ9.eyJzdWIiOiJhM2U3ZTM0Yy00NGJmLTQxZmYtYWQ4Ni0yZWUwNGE5NTQ0MTgiLCJpc3MiOiJhdXRoLnRlc3QuY29tIiwiYXVkIjoiZm9vYmFyLml1ZHguaW8iLCJleHAiOjE2Mjc2ODk5NDAsImlhdCI6MTYyNzY0Njc0MCwiaWlkIjoicmc6ZXhhbXBsZS5jb20vNzllN2JmYTYyZmFkNmM3NjViYWM2OTE1NGMyZjI0Yzk0Yzk1MjIwYS9yZXNvdXJjZS1ncm91cCIsInJvbGUiOiJkZWxlZ2F0ZSIsImNvbnMiOnt9fQ.eJjCUvWuGD3L3Dn2fKj8Ydl1byGoyRS59VfL6ZJcdKR3_eIhm6SOY-CW3p5XDSYVhRTlWvlPLjfXYo9t_PxgnA";
    jwtAuthenticationService.decodeJwt(jwt).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.failNow(handler.cause());
      } else {
        testContext.completeNow();

      }
    });
  }


}
