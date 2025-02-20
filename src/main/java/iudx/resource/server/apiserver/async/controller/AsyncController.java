package iudx.resource.server.apiserver.async.controller;

import static iudx.resource.server.common.Constants.*;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.async.service.AsyncService;
import iudx.resource.server.apiserver.handler.FailureHandler;
import iudx.resource.server.authenticator.AuthenticationService;
import iudx.resource.server.authenticator.handler.authentication.AuthHandler;
import iudx.resource.server.authenticator.handler.authentication.TokenIntrospectHandler;
import iudx.resource.server.authenticator.handler.authorization.*;
import iudx.resource.server.authenticator.model.DxAccess;
import iudx.resource.server.authenticator.model.DxRole;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.common.Api;
import iudx.resource.server.common.CatalogueService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AsyncController {
  private static final Logger LOGGER = LogManager.getLogger(AsyncController.class);
  private final Router router;
  private Vertx vertx;
  private Api api;
  private AsyncService asyncService;
  private AuthenticationService authenticator;
  private CacheService cacheService;
  private JsonObject config;
  private String audience;

  public AsyncController(Vertx vertx, Router router, Api api, JsonObject config) {
    this.vertx = vertx;
    this.router = router;
    this.api = api;
    this.config = config;
    this.audience = config.getString("audience");
  }

  public void init() {

    createProxy();
    CatalogueService catalogueService = new CatalogueService(cacheService, config, vertx);

    Handler<RoutingContext> asyncConstraint =
        new ConstraintsHandlerForConsumer().consumerConstraintsForEndpoint(DxAccess.ASYNC);
    FailureHandler validationsFailureHandler = new FailureHandler();

    asyncService = AsyncService.createProxy(vertx, ASYNC_SERVICE_ADDRESS);
    AuthHandler authHandler = new AuthHandler(authenticator);
    GetIdHandler getIdHandler = new GetIdHandler(api);
    Handler<RoutingContext> adminAndUserAccessHandler =
        new AuthorizationHandler()
            .setUserRolesForEndpoint(
                DxRole.DELEGATE, DxRole.CONSUMER, DxRole.PROVIDER, DxRole.ADMIN);
    Handler<RoutingContext> isTokenRevoked = new TokenRevokedHandler(cacheService).isTokenRevoked();
    Handler<RoutingContext> validateToken =
        new AuthValidationHandler(api, cacheService, catalogueService);
    Handler<RoutingContext> tokenIntrospectHandler =
        new TokenIntrospectHandler().validateTokenForRs(audience);

    router
        .get(api.getIudxAsyncSearchApi())
        .handler(getIdHandler.withNormalisedPath(api.getIudxAsyncSearchApi()))
        .handler(authHandler)
        .handler(tokenIntrospectHandler)
        .handler(validateToken)
        .handler(adminAndUserAccessHandler)
        .handler(asyncConstraint)
        .handler(isTokenRevoked)
        .handler(this::asyncSearchRequest)
        .failureHandler(validationsFailureHandler);

    router
        .get(api.getIudxAsyncStatusApi())
        .handler(getIdHandler.withNormalisedPath(api.getIudxAsyncStatusApi()))
        .handler(authHandler)
        .handler(tokenIntrospectHandler)
        .handler(validateToken)
        .handler(adminAndUserAccessHandler)
        .handler(asyncConstraint)
        .handler(isTokenRevoked)
        .handler(this::asyncStatusRequest)
        .failureHandler(validationsFailureHandler);
  }

  private void asyncStatusRequest(RoutingContext routingContext) {}

  private void asyncSearchRequest(RoutingContext routingContext) {}

  void createProxy() {
    this.cacheService = CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);
    this.authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
  }
}
