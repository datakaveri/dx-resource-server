package iudx.resource.server.apiserver.admin.controller;

import static iudx.resource.server.apiserver.admin.util.Constants.*;
import static iudx.resource.server.common.Constants.AUTH_SERVICE_ADDRESS;
import static iudx.resource.server.common.Constants.CACHE_SERVICE_ADDRESS;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.handler.FailureHandler;
import iudx.resource.server.authenticator.AuthenticationService;
import iudx.resource.server.authenticator.handler.authentication.AuthHandler;
import iudx.resource.server.authenticator.handler.authentication.TokenIntrospectHandler;
import iudx.resource.server.authenticator.handler.authorization.GetIdHandler;
import iudx.resource.server.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AdminController {
  private static final Logger LOGGER = LogManager.getLogger(AdminController.class);
  private final Router router;
  private Vertx vertx;
  private Api api;
  private AuthenticationService authenticator;
  private String rsUrl;

  public AdminController(Vertx vertx, Router router, Api api, String audience) {
    this.vertx = vertx;
    this.router = router;
    this.api = api;
    this.rsUrl = audience;
  }

  public void init() {
    createProxy();
    AuthHandler authHandler = new AuthHandler(authenticator);
    GetIdHandler getIdHandler = new GetIdHandler(api);
    Handler<RoutingContext> tokenIntrospectHandler =
            new TokenIntrospectHandler().validateKeycloakToken(rsUrl);
    FailureHandler validationsFailureHandler = new FailureHandler();

    router
        .post(ADMIN + REVOKE_TOKEN)
        .handler(getIdHandler.withNormalisedPath(api.getAdminRevokeToken()))
        .handler(authHandler)
        .handler(tokenIntrospectHandler)
        .handler(this::handleRevokeTokenRequest)
        .failureHandler(validationsFailureHandler);

    router
        .post(ADMIN + RESOURCE_ATTRIBS)
        .handler(getIdHandler.withNormalisedPath(api.getAdminUniqueAttributeOfResource()))
        .handler(authHandler)
        .handler(tokenIntrospectHandler)
        .handler(this::createUniqueAttribute)
        .failureHandler(validationsFailureHandler);

    router
        .put(ADMIN + RESOURCE_ATTRIBS)
        .handler(getIdHandler.withNormalisedPath(api.getAdminUniqueAttributeOfResource()))
        .handler(authHandler)
        .handler(tokenIntrospectHandler)
        .handler(this::updateUniqueAttribute)
        .failureHandler(validationsFailureHandler);

    router
        .delete(ADMIN + RESOURCE_ATTRIBS)
        .handler(getIdHandler.withNormalisedPath(api.getAdminUniqueAttributeOfResource()))
        .handler(authHandler)
        .handler(tokenIntrospectHandler)
        .handler(this::deleteUniqueAttribute)
        .failureHandler(validationsFailureHandler);
  }

  private void createUniqueAttribute(RoutingContext routingContext) {}

  private void updateUniqueAttribute(RoutingContext routingContext) {}

  private void deleteUniqueAttribute(RoutingContext routingContext) {}

  private void handleRevokeTokenRequest(RoutingContext routingContext) {}

  void createProxy() {
    authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
  }
}
