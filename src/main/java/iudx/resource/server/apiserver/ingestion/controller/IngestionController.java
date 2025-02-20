package iudx.resource.server.apiserver.ingestion.controller;

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
import iudx.resource.server.authenticator.handler.authorization.AuthValidationHandler;
import iudx.resource.server.authenticator.handler.authorization.AuthorizationHandler;
import iudx.resource.server.authenticator.handler.authorization.GetIdHandler;
import iudx.resource.server.authenticator.handler.authorization.TokenRevokedHandler;
import iudx.resource.server.authenticator.model.DxRole;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.common.Api;
import iudx.resource.server.common.CatalogueService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IngestionController {
  private static final Logger LOGGER = LogManager.getLogger(IngestionController.class);
  private final Router router;
  private Vertx vertx;
  private Api api;
  private JsonObject config;
  private AuthenticationService authenticator;
  private CacheService cacheService;
  private String audience;

  public IngestionController(Vertx vertx, Router router, Api api, JsonObject config) {
    this.router = router;
    this.vertx = vertx;
    this.api = api;
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
    Handler<RoutingContext> providerAndAdminAccessHandler =
        new AuthorizationHandler()
            .setUserRolesForEndpoint(DxRole.DELEGATE, DxRole.PROVIDER, DxRole.ADMIN);
    Handler<RoutingContext> isTokenRevoked = new TokenRevokedHandler(cacheService).isTokenRevoked();
    Handler<RoutingContext> tokenIntrospectHandler =
        new TokenIntrospectHandler().validateTokenForRs(audience);

    router
        .post(api.getIngestionPath())
        .handler(getIdHandler.withNormalisedPath(api.getIngestionPath()))
        .handler(authHandler)
        .handler(tokenIntrospectHandler)
        .handler(validateToken)
        .handler(providerAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::registerAdapter)
        .failureHandler(validationsFailureHandler);

    router
        .delete(api.getIngestionPath() + "/*")
        .handler(getIdHandler.withNormalisedPath(api.getIngestionPath()))
        .handler(authHandler)
        .handler(tokenIntrospectHandler)
        .handler(validateToken)
        .handler(providerAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::deleteAdapter)
        .failureHandler(validationsFailureHandler);

    router
        .get(api.getIngestionPath() + "/:UUID")
        .handler(getIdHandler.withNormalisedPath(api.getIngestionPath()))
        .handler(authHandler)
        .handler(tokenIntrospectHandler)
        .handler(validateToken)
        .handler(providerAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::getAdapterDetails)
        .failureHandler(validationsFailureHandler);

    router
        .post(api.getIngestionPath() + "/heartbeat")
        .handler(getIdHandler.withNormalisedPath(api.getIngestionPath()))
        .handler(authHandler)
        .handler(tokenIntrospectHandler)
        .handler(validateToken)
        .handler(providerAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::publishHeartbeat)
        .failureHandler(validationsFailureHandler);

    router
        .post(api.getIngestionPathEntities())
        .handler(getIdHandler.withNormalisedPath(api.getIngestionPathEntities()))
        .handler(authHandler)
        .handler(tokenIntrospectHandler)
        .handler(validateToken)
        .handler(providerAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::publishDataFromAdapter)
        .failureHandler(validationsFailureHandler);

    router
        .get(api.getIngestionPath())
        .handler(getIdHandler.withNormalisedPath(api.getIngestionPath()))
        .handler(authHandler)
        .handler(tokenIntrospectHandler)
        .handler(validateToken)
        .handler(providerAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::getAllAdaptersForUsers)
        .failureHandler(validationsFailureHandler);
  }

  private void registerAdapter(RoutingContext routingContext) {}

  private void deleteAdapter(RoutingContext routingContext) {}

  private void publishHeartbeat(RoutingContext routingContext) {}

  private void getAdapterDetails(RoutingContext routingContext) {}

  private void publishDataFromAdapter(RoutingContext routingContext) {}

  private void getAllAdaptersForUsers(RoutingContext routingContext) {}

  void createProxy() {
    this.authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    this.cacheService = CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);
  }
}
