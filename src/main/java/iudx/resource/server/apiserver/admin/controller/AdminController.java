package iudx.resource.server.apiserver.admin.controller;

import static iudx.resource.server.apiserver.admin.util.Constants.*;
import static iudx.resource.server.common.Constants.*;
import static iudx.resource.server.databroker.util.Constants.DATA_BROKER_SERVICE_ADDRESS;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.admin.service.AdminService;
import iudx.resource.server.apiserver.admin.service.AdminServiceImpl;
import iudx.resource.server.apiserver.exception.FailureHandler;
import iudx.resource.server.authenticator.AuthenticationService;
import iudx.resource.server.authenticator.handler.authentication.AuthHandler;
import iudx.resource.server.authenticator.handler.authentication.TokenIntrospectHandler;
import iudx.resource.server.authenticator.handler.authorization.GetIdHandler;
import iudx.resource.server.common.Api;
import iudx.resource.server.database.postgres.service.PostgresService;
import iudx.resource.server.databroker.service.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AdminController {
  private static final Logger LOGGER = LogManager.getLogger(AdminController.class);
  private final Router router;
  private Vertx vertx;
  private Api api;
  private AuthenticationService authenticator;
  private String rsUrl;
  private AdminService adminService;
  private PostgresService postgresService;
  private DataBrokerService dataBrokerService;

  // TODO: Need to add auditing

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

    adminService = new AdminServiceImpl(postgresService, dataBrokerService);
  }

  private void createUniqueAttribute(RoutingContext routingContext) {
    LOGGER.trace("createUniqueAttribute() started");
    HttpServerResponse response = routingContext.response();
    JsonObject requestBody = routingContext.body().asJsonObject();
    String id = requestBody.getString("id");
    String attribute = requestBody.getString("attribute");
    adminService.createUniqueAttribute(id, attribute, response);
  }

  private void updateUniqueAttribute(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject requestBody = routingContext.body().asJsonObject();

    String id = requestBody.getString("id");
    String attribute = requestBody.getString("attribute");
    adminService.updateUniqueAttribute(id, attribute, response);
  }

  private void deleteUniqueAttribute(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    String id = request.params().get("id");
    HttpServerResponse response = routingContext.response();
    adminService.deleteUniqueAttribute(id, response);
  }

  private void handleRevokeTokenRequest(RoutingContext routingContext) {
    JsonObject requestBody = routingContext.body().asJsonObject();
    String userid = requestBody.getString("sub");
    HttpServerResponse response = routingContext.response();
    adminService.revokedTokenRequest(userid, response);
  }

  void createProxy() {
    authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    postgresService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);
    dataBrokerService = DataBrokerService.createProxy(vertx, DATA_BROKER_SERVICE_ADDRESS);
  }
}
