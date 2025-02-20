package iudx.resource.server.apiserver.metering.controller;

import static iudx.resource.server.common.Constants.AUTH_SERVICE_ADDRESS;
import static iudx.resource.server.common.Constants.CACHE_SERVICE_ADDRESS;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.exception.FailureHandler;
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

public class MeteringController {
  private static final Logger LOGGER = LogManager.getLogger(MeteringController.class);

  private final Router router;
  private Api api;
  private Vertx vertx;
  private JsonObject config;
  private CacheService cacheService;
  private AuthenticationService authenticator;
  private String audience;

  public MeteringController(Vertx vertx, Router router, Api api, JsonObject config) {
    this.api = api;
    this.router = router;
    this.vertx = vertx;
    this.config = config;
    this.audience = config.getString("audience");
  }

  public void init() {
    createProxy();
    CatalogueService catalogueService = new CatalogueService(cacheService, config, vertx);
    FailureHandler validationsFailureHandler = new FailureHandler();
    GetIdHandler getIdHandler = new GetIdHandler(api);
    AuthHandler authHandler = new AuthHandler(authenticator);
    Handler<RoutingContext> validateToken =
        new AuthValidationHandler(api, cacheService, catalogueService);
    Handler<RoutingContext> userAndAdminAccessHandler =
        new AuthorizationHandler()
            .setUserRolesForEndpoint(
                DxRole.DELEGATE, DxRole.CONSUMER, DxRole.PROVIDER, DxRole.ADMIN);
    Handler<RoutingContext> isTokenRevoked = new TokenRevokedHandler(cacheService).isTokenRevoked();
    Handler<RoutingContext> tokenIntrospectHandler =
        new TokenIntrospectHandler().validateTokenForRs(audience);

    Handler<RoutingContext> providerAndAdminAccessHandler =
        new AuthorizationHandler()
            .setUserRolesForEndpoint(DxRole.DELEGATE, DxRole.PROVIDER, DxRole.ADMIN);

    Handler<RoutingContext> apiConstraint =
        new ConstraintsHandlerForConsumer().consumerConstraintsForEndpoint(DxAccess.API);

    router
        .get(api.getMonthlyOverview())
        .handler(getIdHandler.withNormalisedPath(api.getMonthlyOverview()))
        .handler(authHandler)
        .handler(tokenIntrospectHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(apiConstraint)
        .handler(isTokenRevoked)
        .handler(this::getOverview)
        .failureHandler(validationsFailureHandler);
    router
        .get(api.getSummaryPath())
        .handler(getIdHandler.withNormalisedPath(api.getSummaryPath()))
        .handler(authHandler)
        .handler(tokenIntrospectHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(apiConstraint)
        .handler(isTokenRevoked)
        .handler(this::getSummary)
        .failureHandler(validationsFailureHandler);
    router
        .get(api.getIudxConsumerAuditUrl())
        .handler(getIdHandler.withNormalisedPath(api.getIudxConsumerAuditUrl()))
        .handler(authHandler)
        .handler(tokenIntrospectHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(apiConstraint)
        .handler(isTokenRevoked)
        .handler(this::getConsumerAuditDetail)
        .failureHandler(validationsFailureHandler);

    router
        .get(api.getIudxProviderAuditUrl())
        .handler(getIdHandler.withNormalisedPath(api.getIudxProviderAuditUrl()))
        .handler(authHandler)
        .handler(tokenIntrospectHandler)
        .handler(validateToken)
        .handler(providerAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::getProviderAuditDetail)
        .failureHandler(validationsFailureHandler);
  }

  private void getProviderAuditDetail(RoutingContext routingContext) {}

  private void getConsumerAuditDetail(RoutingContext routingContext) {}

  private void getSummary(RoutingContext routingContext) {}

  private void getOverview(RoutingContext routingContext) {}

  void createProxy() {
    this.authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    this.cacheService = CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);
  }
}
