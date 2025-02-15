package iudx.resource.server.authenticator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.vertx.core.Handler;
import iudx.resource.server.authenticator.service.JwtAuthenticationServiceImpl;
import iudx.resource.server.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.LocalDateTime;
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
            new JwtAuthenticationServiceImpl(vertx, jwtAuth, authConfig, cacheService,meteringService,apis);

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
  @Order(2)
  @DisplayName("success - allow access to all open endpoints")
  public void allow4OpenEndpoint(VertxTestContext testContext) {
    JsonObject authInfo = new JsonObject();
    authInfo.put("apiEndpoint", apis.getEntitiesUrl());
    authInfo.put("method", Method.GET);

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid("ri:foobar.iudx.io");
    jwtData.setRole("consumer");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api")));

    jwtAuthenticationService.validateAccess(jwtData, true, authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow("invalid access");
      }
    });
  }

  @Test
  @Order(3)
  @DisplayName("success - allow access to closed endpoint")
  public void allow4ClosedEndpoint(VertxTestContext testContext) {
    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.closedConsumerApiToken);
    authInfo.put("id", closeId);
    authInfo.put("apiEndpoint", apis.getEntitiesUrl());
    authInfo.put("method", Method.GET);

    JsonObject request = new JsonObject();
    
    when(cacheService.get(any())).thenReturn(Future.failedFuture(""));
    
    JsonObject closedIdJson=new JsonObject();
    closedIdJson.put("type", CacheType.CATALOGUE_CACHE);
    closedIdJson.put("key", closeId);
    when(cacheService.get(closedIdJson)).thenReturn(Future.succeededFuture(new JsonObject().put("accessPolicy", "SECURE")));

    jwtAuthenticationService.tokenInterospect(request, authInfo, handler -> {
      if (handler.failed()) {
        testContext.completeNow();
      } else {
        testContext.failNow("invalid access");
      }
    });
  }

  @Test
  @Order(4)
  @DisplayName("success - disallow access to closed endpoint for different id")
  public void disallow4ClosedEndpoint(VertxTestContext testContext) {
    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.closedConsumerApiToken);
    authInfo.put("id", invalidId);
    authInfo.put("apiEndpoint", apis.getEntitiesUrl());
    authInfo.put("method", Method.GET);
    JsonObject request = new JsonObject();

    jwtAuthenticationService.tokenInterospect(request, authInfo, handler -> {
      if (handler.succeeded()) {
        testContext.failNow("invalid access");
      } else {
        testContext.completeNow();
      }
    });
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
    jwtAuthenticationService.tokenInterospect(request, authInfo, handler -> {
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
    jwtAuthenticationService.tokenInterospect(request, authInfo, handler -> {
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
    jwtAuthenticationService.tokenInterospect(request, authInfo, handler -> {
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
    jwtAuthenticationService.tokenInterospect(request, authInfo, handler -> {
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
    jwtAuthenticationService.tokenInterospect(request, authInfo, handler -> {
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

    jwtAuthenticationService.tokenInterospect(request, authInfo, handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Order(11)
  @DisplayName("failure - provider role -> api access")
  public void closeProviderTokenApiAPI(VertxTestContext testContext) {

    JsonObject request = new JsonObject();
    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.openConsumerSubsToken);
    authInfo.put("id", closeId);
    authInfo.put("apiEndpoint", apis.getEntitiesUrl());
    authInfo.put("method", Method.GET);

    jwtAuthenticationService.tokenInterospect(request, authInfo, handler -> {
      if (handler.succeeded()) {
        testContext.failNow(handler.cause());
      } else {
        testContext.completeNow();
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

  @Test
  @Order(16)
  @DisplayName("success - allow consumer access to /entities endpoint for access [api,subs]")
  public void access4ConsumerTokenEntitiesAPI(VertxTestContext testContext) {

    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.openConsumerApiToken);
    authInfo.put("id",
            "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/entities");
    authInfo.put("method", "GET");

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid(
            "rg:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("consumer");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api").add("sub")));


    jwtAuthenticationService.validateAccess(jwtData, true, authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow("invalid access");
      }
    });
  }

  @Test
  @Order(17)
  @DisplayName("failure - consumer access to /entities endpoint for access [api]")
  public void access4ConsumerTokenEntitiesPostAPI(VertxTestContext testContext) {

    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.openConsumerApiToken);
    authInfo.put("id",
            "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/entities");
    authInfo.put("method", "POST");

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid(
            "rg:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("consumer");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api")));
    jwtAuthenticationService.validateAccess(jwtData, false, authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.failNow("invalid access provided");
      } else {
        testContext.completeNow();
      }
    });
  }

  @Test
  @Order(18)
  @DisplayName("success - consumer access to /subscription endpoint for access [api,subs]")
  public void access4ConsumerTokenSubsAPI(VertxTestContext testContext) {

    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.openConsumerApiToken);
    authInfo.put("id",
            "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/subscription");
    authInfo.put("method", "POST");

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid(
            "rg:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("consumer");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api").add("sub")));


    jwtAuthenticationService.validateAccess(jwtData, true, authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow("invalid access");
      }
    });
  }

  @Test
  @Order(19)
  @DisplayName("failure - consumer access to /subscription endpoint for access [api]")
  public void access4ConsumerTokenSubsAPIFailure(VertxTestContext testContext) {

    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.openConsumerSubsToken);
    authInfo.put("id",
            "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/subscription");
    authInfo.put("method", "POST");

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid(
            "rg:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("consumer");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api")));
    jwtAuthenticationService.validateAccess(jwtData, false, authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.failNow("invalid access provided");
      } else {
        testContext.completeNow();
      }
    });
  }

  @Test
  @Order(20)
  @DisplayName("failure - consumer access to /ingestion endpoint for access [api]")
  public void access4ConsumerTokenIngestAPI(VertxTestContext testContext) {

    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.openConsumerApiToken);
    authInfo.put("id",
            "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/ingestion");
    authInfo.put("method", "POST");

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid(
            "rg:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("consumer");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api")));

    jwtAuthenticationService.validateAccess(jwtData, false, authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.failNow("invalid access provided");
      } else {
        LOGGER.debug("failed access ");
        testContext.completeNow();
      }
    });
  }


  @Test
  @Order(21)
  @DisplayName("failure - provider access to /entities endpoint for access [api]")
  public void access4ProviderTokenEntitiesAPI(VertxTestContext testContext) {

    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.closedProviderApiToken);
    authInfo.put("id", "example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/entities");
    authInfo.put("method", "GET");

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid("rg:example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
    jwtData.setRole("provider");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api")));
    jwtAuthenticationService.validateAccess(jwtData, false, authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow("provider not provided access to API");
      }
    });
  }

  @Test
  @Order(22)
  @DisplayName("success - provider access to /entities endpoint for access [api]")
  public void access4ProviderTokenIngestionPostAPI(VertxTestContext testContext) {

    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.closedProviderApiToken);
    authInfo.put("id", "example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/ingestion");
    authInfo.put("method", "POST");

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid("rg:example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
    jwtData.setRole("provider");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("ingestion")));


    jwtAuthenticationService.validateAccess(jwtData, false, authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        LOGGER.debug("failed access ");
        testContext.failNow("failed for provider");

      }
    });
  }


  @Test
  @Order(23)
  @DisplayName("success - provider access to /entities endpoint for access [api]")
  public void access4ProviderTokenIngestionGetAPI(VertxTestContext testContext) {

    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.closedProviderApiToken);
    authInfo.put("id", "example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/ingestion");
    authInfo.put("method", "GET");

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid("rg:example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
    jwtData.setRole("provider");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("ingestion")));


    jwtAuthenticationService.validateAccess(jwtData, false, authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        LOGGER.debug("failed access ");
        testContext.failNow("failed for provider");
      }
    });
  }


  @Test
  @Order(24)
  @DisplayName("success - validId check")
  public void validIdCheck4JwtToken(VertxTestContext testContext) {
    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid(
            "rg:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("provider");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("ingest")));

    jwtAuthenticationService
            .isValidId(jwtData,
                    "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053")
            .onComplete(handler -> {
              if (handler.succeeded()) {
                testContext.completeNow();
              } else {
                testContext.failNow("fail");
              }
            });
  }


  @Test
  @Order(25)
  @DisplayName("failure - invalid validId check")
  public void invalidIdCheck4JwtToken(VertxTestContext testContext) {
    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid(
            "rg:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("provider");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("ingest")));

    jwtAuthenticationService
            .isValidId(jwtData,
                    "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR055")
            .onComplete(handler -> {
              if (handler.succeeded()) {
                testContext.failNow("fail");
              } else {
                testContext.completeNow();
              }
            });
  }

  @Test
  @Order(26)
  @DisplayName("failure - invalid audience")
  public void invalidAudienceCheck(VertxTestContext testContext) {
    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("abc.iudx.io1");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid(
            "rg:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("provider");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("ingest")));
    jwtAuthenticationService.isValidAudienceValue(jwtData).onComplete(handler -> {
      if (handler.failed()) {
        testContext.completeNow();
      } else {
        testContext.failNow("fail");

      }
    });
  }
  
  
  
  @Test
  @Order(0)
  @DisplayName("Revoked token passed")
  public void testRevokedTokenPassed(VertxTestContext testContext) {
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
    
    when(cacheService.get(revokedTokenRequest)).thenReturn(Future.succeededFuture(new JsonObject().put("value", LocalDateTime.now().minusDays(1).toString())));
    
    jwtAuthenticationService.isRevokedClientToken(jwtData).onComplete(handler->{
      if(handler.succeeded()) {
        testContext.failNow("access provided for revoked token");
      }else {
        testContext.completeNow();
      }
    });
    
  }
  
  
  @Test
  @Order(28)
  @DisplayName("correct unrevoked token passed")
  public void testCorrectUnrevokedTokenPassed(VertxTestContext testContext) {
    JwtData jwtData = new JwtData();
    jwtData.setSub("valid_sub");
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1676016492);
    jwtData.setIat(1676016492);
    jwtData.setIid("rg:example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
    jwtData.setRole("provider");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api")));
    
    JsonObject revokedTokenRequest=new JsonObject();
    revokedTokenRequest.put("type", CacheType.REVOKED_CLIENT);
    revokedTokenRequest.put("key", jwtData.getSub());
    String time="2023-02-08T12:37:26.796"; 
    when(cacheService.get(revokedTokenRequest)).thenReturn(Future.succeededFuture(new JsonObject().put("value", time)));
    
    
    jwtAuthenticationService.isRevokedClientToken(jwtData).onComplete(handler->{
      if(handler.succeeded()) {
        testContext.completeNow();
      }else {
        testContext.failNow("no access for correct token");
      }
    });
    
  }
  
  @Test
  @Order(29)
  @DisplayName("Test isOpenResource method for Cache miss for Valid Group ID")
  public void testIsOpenResourceGroupId(VertxTestContext vertxTestContext)
  {

    String id = "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053";
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject groupId = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");
    JsonObject openResourceIdJson=new JsonObject();
    openResourceIdJson.put("type", CacheType.CATALOGUE_CACHE);
    openResourceIdJson.put("key", id);
    when(cacheService.get(openResourceIdJson)).thenReturn(Future.succeededFuture(groupId));
    
    JsonObject openGroupIdJson=openResourceIdJson.copy();
    openGroupIdJson.put("key", "groupId");
    when(cacheService.get(any())).thenReturn(Future.succeededFuture(groupId)).thenReturn(Future.succeededFuture(new JsonObject().put("accessPolicy", "OPEN")));

    jwtAuthenticationService.isOpenResource(id).onComplete(handler -> {
      if (handler.succeeded()) {
        vertxTestContext.completeNow();
      } else {
        vertxTestContext.failNow(handler.cause());
      }
    });
  }
  
  
  @Test
  @Order(30)
  @DisplayName("Test isOpenResource method for Cache miss for Valid Group ID")
  public void testIsOpenResourceId(VertxTestContext vertxTestContext)
  {

    String id = "b58da193-23d9-43eb-b98a-a103d4b6103c";
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject groupId = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");
    JsonObject openResourceIdJson=new JsonObject();
    openResourceIdJson.put("type", CacheType.CATALOGUE_CACHE);
    openResourceIdJson.put("key", id);
    when(cacheService.get(any())).thenReturn(Future.succeededFuture(groupId)).thenReturn(Future.succeededFuture(new JsonObject().put("accessPolicy", "OPEN")));

    jwtAuthenticationService.isOpenResource(id).onComplete(handler -> {
      if (handler.succeeded()) {
        vertxTestContext.completeNow();
      } else {
        vertxTestContext.failNow(handler.cause());
      }
    });
  }
  
  
  @Test
  @Order(31)
  @DisplayName("Test isOpenResource method for Resource level ACL [No entry for resource Id]")
  public void testIsOpenResourceIdNoGroupId(VertxTestContext vertxTestContext)
  {
    String id = "b58da193-23d9-43eb-b98a-a103d4b6103c";
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject groupId = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");
    JsonObject openResourceIdJson=new JsonObject();
    openResourceIdJson.put("type", CacheType.CATALOGUE_CACHE);
    openResourceIdJson.put("key", id);
    when(cacheService.get(any())).thenReturn(Future.succeededFuture(groupId)).thenReturn(Future.failedFuture("failed for group id"));

    jwtAuthenticationService.isOpenResource(id).onComplete(handler -> {
      if (handler.failed()) {
        vertxTestContext.completeNow();
      } else {
        vertxTestContext.failed();
      }
    });
  }
  
  
  @Test
  @Order(32)
  @DisplayName("Test isOpenResource method for Group level ACL, but no resource id")
  public void testIsOpenResourceGroupNoResourceIdExist(VertxTestContext vertxTestContext)
  {

    String id = "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/non-existing";
    String[] idComponents = id.split("/");
    String groupId =(idComponents.length == 4)? id:String.join("/", Arrays.copyOfRange(idComponents, 0, 4));
    
    JsonObject openResourceIdJson=new JsonObject();
    openResourceIdJson.put("type", CacheType.CATALOGUE_CACHE);
    openResourceIdJson.put("key", id);
    when(cacheService.get(openResourceIdJson)).thenReturn(Future.failedFuture("failed for resource id"));
    
    JsonObject openGroupIdJson=openResourceIdJson.copy();
    openGroupIdJson.put("key", groupId);
    when(cacheService.get(openGroupIdJson)).thenReturn(Future.succeededFuture(new JsonObject().put("accessPolicy", "OPEN")));

    jwtAuthenticationService.isOpenResource(id).onComplete(handler -> {
      if (handler.succeeded()) {
        vertxTestContext.failNow(handler.cause());
      } else {
        vertxTestContext.completeNow();
      }
    });
  }
  
  @Test
  @Order(33)
  @DisplayName("Test isOpenResource method for Group level ACL")
  public void testIsOpenResourceGroupNoACL4ResourceId(VertxTestContext vertxTestContext)
  {

    String id = "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053";
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject groupId =
            new JsonObject()
                    .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
                    .put("type", list)
                    .put("name", "dummy_name")
                    .put("resourceGroup", "5b7556b5-0779-4c47-9cf2-3f209779aa22");

    JsonObject openResourceIdJson=new JsonObject();
    openResourceIdJson.put("type", CacheType.CATALOGUE_CACHE);
    openResourceIdJson.put("key", id);
    when(cacheService.get(openResourceIdJson)).thenReturn(Future.succeededFuture(new JsonObject()));
    
    JsonObject openGroupIdJson=openResourceIdJson.copy();
    openGroupIdJson.put("key", groupId);
    when(cacheService.get(any())).thenReturn(Future.succeededFuture(groupId)).thenReturn(Future.succeededFuture(new JsonObject().put("accessPolicy", "OPEN")));

    jwtAuthenticationService.isOpenResource(id).onComplete(handler -> {
      if (handler.succeeded()) {
        vertxTestContext.completeNow();
      } else {
        vertxTestContext.failNow(handler.cause());
      }
    });
  }
  
  @Test
  @Order(34)
  @DisplayName("Test No ACL at group and resource level")
  public void testNoACL(VertxTestContext vertxTestContext)
  {
    String id = "b58da193-23d9-43eb-b98a-a103d4b6103c";

    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject abc = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    when(cacheService.get(any())).thenReturn(Future.succeededFuture(abc));
    jwtAuthenticationService.isOpenResource(id).onComplete(handler -> {
      if (handler.succeeded()) {
        vertxTestContext.failNow(handler.cause());
      } else {
        vertxTestContext.completeNow();
        
      }
    });
  }

  @Test
  @Order(35)
  @DisplayName("Failed")
  public void failedValidateProviderUser(VertxTestContext testContext) {

    JsonObject request = new JsonObject();
    JsonObject authInfo = new JsonObject();
    authInfo.put("token", JwtTokenHelper.closedDelegateIngestToken);
    authInfo.put("id", closeId);
    authInfo.put("apiEndpoint", apis.getIngestionPathEntities());
    authInfo.put("method", Method.POST);
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
            .put("accessPolicy", "SECURED")
            .put("type", new JsonArray().add("ri:RESOURCEGROUP"))
                .put("providerUserId","b58da193-23d9-43eb-b98a-a103d4b6103c");

    JwtData jwtData = new JwtData();
    jwtData.setSub("valid_sub");
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid("rg:example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
    jwtData.setRole("provider");
    jwtData.setDid("b58da193-23d9-43eb-b98a-a103d4b6103c");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api")));

    JsonObject revokedTokenRequest=new JsonObject();
    revokedTokenRequest.put("type", CacheType.REVOKED_CLIENT);
    revokedTokenRequest.put("key", jwtData.getSub());

    JsonObject cacheRequest=new JsonObject();
    cacheRequest.put("type", CacheType.CATALOGUE_CACHE);
    cacheRequest.put("key", authInfo.getString("id"));

    when(cacheService.get(revokedTokenRequest)).thenReturn(Future.succeededFuture(new JsonObject().put("value","2021-09-09T12:52:37")));

    when(cacheService.get(any())).thenReturn(Future.succeededFuture(jsonObject));
    JsonObject jsonObject1 = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    JsonArray jsonArray1 = new JsonArray();
    jsonObject1.put("id", "abcd/abcd/abcd/abcd");
    jsonObject1.put("iudxResourceAPIs", jsonArray1);
    jsonObject1.put("providerUserId","abc");
    jsonArray.add(jsonObject1);
    jsonObject.put("results", jsonArray);
    JwtAuthenticationServiceImpl.catWebClient = mock(WebClient.class);
    when(JwtAuthenticationServiceImpl.catWebClient.get(anyInt(), anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.expect(any())).thenReturn(httpRequest);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(httpResponse);
    when(httpResponse.bodyAsJsonObject()).thenReturn(jsonObject);
    doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
      @Override
      public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {

        ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(asyncResult);
        return null;
      }
    }).when(httpRequest).send(any());


    jwtAuthenticationService.tokenInterospect(request, authInfo, handler -> {
      if (handler.failed()) {
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }
}
