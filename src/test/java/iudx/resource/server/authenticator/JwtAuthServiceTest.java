package iudx.resource.server.authenticator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import iudx.resource.server.common.Api;
import iudx.resource.server.database.postgres.PostgresService;
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
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.metering.MeteringService;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class JwtAuthServiceTest {
    @Mock
    JwtData jwtData;
    @Mock
    JWTAuth jwtAuth1;
    @Mock
    WebClient webClient;
    @Mock
    JsonObject config;
    @Mock
    CacheService cacheService;
    @Mock
    MeteringService meteringService;
    @Mock
    Throwable throwable;
    @Mock
    AsyncResult<JsonObject> asyncResult;
    @Mock
    PostgresService pgService;
    JwtAuthenticationServiceImpl jwtAuthenticationService;
    private Api apis;
    private String dxApiBasePath;
    private String dxCatalogueBasePath;
    private String dxAuthBasePath;

    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext)
    {
        JWTAuthOptions jwtAuthOptions = new JWTAuthOptions();
//        jwtAuthenticationService.jwtDecodeFuture = mock(Future.class);
        dxApiBasePath = "/ngsi-ld/v1";
        dxCatalogueBasePath = "/iudx/cat/v1";
        dxAuthBasePath = "/auth/v1";
        apis = Api.getInstance(dxApiBasePath);
        jwtAuthOptions.addPubSecKey(
                new PubSecKeyOptions()
                        .setAlgorithm("ES256")
                        .setBuffer("-----BEGIN PUBLIC KEY-----\n" +
                                "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE8BKf2HZ3wt6wNf30SIsbyjYPkkTS\n" +
                                "GGyyM2/MGF/zYTZV9Z28hHwvZgSfnbsrF36BBKnWszlOYW0AieyAUKaKdg==\n" +
                                "-----END PUBLIC KEY-----\n" +
                                ""));
        jwtAuthOptions.getJWTOptions().setIgnoreExpiration(true);// ignore token expiration only

        JWTAuth jwtAuth = JWTAuth.create(Vertx.vertx(), jwtAuthOptions);
        when(config.getString(anyString())).thenReturn("Dummy String");
        lenient().when(config.getInteger(anyString())).thenReturn(8443);
        jwtAuthenticationService = new JwtAuthenticationServiceImpl(Vertx.vertx(),jwtAuth,config,cacheService,meteringService,pgService,apis);
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test isRevokedClientToken method : Failure")
    public void testRevokedClientToken(VertxTestContext vertxTestContext)
    {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("value", "2022-06-13T17:20:00.330");
        when(jwtData.getSub()).thenReturn("Dummy ID");
        when(jwtData.getIat()).thenReturn(3000);

        when(cacheService.get(any())).thenReturn(Future.succeededFuture(jsonObject));
        jwtAuthenticationService
                .isRevokedClientToken(jwtData)
                .onComplete(
                        handler -> {
                            if (handler.succeeded()) {
                                vertxTestContext.failNow(handler.cause());
                            } else {
                                /*assertEquals("{\"401\":\"revoked token passes\"}", handler.cause().getMessage());*/
                                assertEquals(
                                        "{\"type\":\"urn:dx:rs:invalidAuthorizationToken\",\"status\":401,\"title\":\"Not Authorized\",\"detail\":\"revoked token passes\"}", handler.cause().getMessage());
                                vertxTestContext.completeNow();
                            }
                        });
    }

    @Test
    @DisplayName("Test isRevokedClientToken method : Success")
    public void testRevokedClientTokenSuccess(VertxTestContext vertxTestContext)
    {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("value", "1022-06-13T17:20:00.330");
        when(jwtData.getSub()).thenReturn("Dummy ID");
//        when(asyncResult.succeeded()).thenReturn(true);
//        when(asyncResult.result()).thenReturn(jsonObject);
        when(jwtData.getIat()).thenReturn(952097820);

//        doAnswer(new Answer<AsyncResult<JsonObject>>() {
//            @Override
//            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
//                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
//                return null;
//            }
//        }).when(jwtAuthenticationService.cache).get(any());
        when(cacheService.get(any())).thenReturn(Future.succeededFuture(jsonObject));
        jwtAuthenticationService.isRevokedClientToken(jwtData).onComplete(handler -> {
            if(handler.succeeded())
            {
                assertTrue(handler.result());
                vertxTestContext.completeNow();
            }
            else
            {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }


}
