package iudx.resource.server.apiserver.search.controller;

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
import iudx.resource.server.authenticator.handler.authorization.*;
import iudx.resource.server.authenticator.model.DxAccess;
import iudx.resource.server.authenticator.model.DxRole;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.common.Api;
import iudx.resource.server.common.CatalogueService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SearchController {
  private static final Logger LOGGER = LogManager.getLogger(SearchController.class);
  private final Router router;
  private Vertx vertx;
  private Api api;
  private JsonObject config;
  private AuthenticationService authenticator;
  private String audience;
  private CacheService cacheService;
  private CatalogueService catalogueService;

  public SearchController(Vertx vertx, Router router, Api api, JsonObject config) {
    this.vertx = vertx;
    this.router = router;
    this.api = api;
    this.config = config;
    this.audience = config.getString("audience");
  }

  public void init() {
    createProxy();
    catalogueService = new CatalogueService(cacheService, config, vertx);
    AuthHandler authHandler = new AuthHandler(api, authenticator);
    GetIdHandler getIdHandler = new GetIdHandler(api);
    Handler<RoutingContext> userAndAdminAccessHandler =
        new AuthorizationHandler()
            .setUserRolesForEndpoint(
                DxRole.DELEGATE, DxRole.CONSUMER, DxRole.PROVIDER, DxRole.ADMIN);
    Handler<RoutingContext> apiConstraint =
        new ConstraintsHandlerForConsumer().consumerConstraintsForEndpoint(DxAccess.API);
    FailureHandler validationsFailureHandler = new FailureHandler();
    Handler<RoutingContext> isTokenRevoked = new TokenRevokedHandler(cacheService).isTokenRevoked();

    Handler<RoutingContext> validateToken =
        new AuthValidationHandler(api, cacheService, audience, catalogueService);

    router
        .get(api.getEntitiesUrl())
        .handler(getIdHandler.withNormalisedPath(api.getEntitiesUrl()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(apiConstraint)
        .handler(isTokenRevoked)
        .handler(this::getEntitiesQuery)
        .handler(validationsFailureHandler);

    router
        .get(api.getEntitiesUrl() + "/*")
        .handler(getIdHandler.withNormalisedPath(api.getEntitiesUrl()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(apiConstraint)
        .handler(isTokenRevoked)
        .handler(this::getLatestEntitiesQuery)
        .handler(validationsFailureHandler);

    router
        .post(api.getPostTemporalQueryPath())
        .handler(getIdHandler.withNormalisedPath(api.getPostTemporalQueryPath()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(apiConstraint)
        .handler(isTokenRevoked)
        .handler(this::postEntitiesQuery)
        .handler(validationsFailureHandler);
    router
        .post(api.getPostEntitiesQueryPath())
        .handler(getIdHandler.withNormalisedPath(api.getPostEntitiesQueryPath()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(apiConstraint)
        .handler(isTokenRevoked)
        .handler(this::postEntitiesQuery)
        .handler(validationsFailureHandler);

    router
        .get(api.getTemporalUrl())
        .handler(getIdHandler.withNormalisedPath(api.getTemporalUrl()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(apiConstraint)
        .handler(isTokenRevoked)
        .handler(this::getTemporalQuery)
        .handler(validationsFailureHandler);
  }

  private void getTemporalQuery(RoutingContext routingContext) {}

  private void postEntitiesQuery(RoutingContext routingContext) {}

  private void getLatestEntitiesQuery(RoutingContext routingContext) {}

  private void getEntitiesQuery(RoutingContext routingContext) {}

  void createProxy() {
    this.cacheService = CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);
    this.authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
  }
}
