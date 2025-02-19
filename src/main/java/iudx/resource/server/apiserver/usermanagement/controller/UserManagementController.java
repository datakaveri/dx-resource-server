package iudx.resource.server.apiserver.usermanagement.controller;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.Constants.AUTH_SERVICE_ADDRESS;
import static iudx.resource.server.common.Constants.CACHE_SERVICE_ADDRESS;
import static iudx.resource.server.databroker.util.Constants.DATA_BROKER_SERVICE_ADDRESS;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.handler.FailureHandler;
import iudx.resource.server.apiserver.usermanagement.service.UserManagementServiceImpl;
import iudx.resource.server.authenticator.AuthenticationService;
import iudx.resource.server.authenticator.handler.authentication.AuthHandler;
import iudx.resource.server.authenticator.handler.authorization.AuthValidationHandler;
import iudx.resource.server.authenticator.handler.authorization.AuthorizationHandler;
import iudx.resource.server.authenticator.handler.authorization.GetIdHandler;
import iudx.resource.server.authenticator.model.DxRole;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.common.Api;
import iudx.resource.server.common.CatalogueService;
import iudx.resource.server.common.ResultModel;
import iudx.resource.server.common.RoutingContextHelper;
import iudx.resource.server.databroker.service.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UserManagementController {
  private static final Logger LOGGER = LogManager.getLogger(UserManagementController.class);

  private final Router router;
  private JsonObject config;
  private Api api;
  private AuthenticationService authenticator;
  private CacheService cacheService;
  private Vertx vertx;
  private String audience;
  private DataBrokerService dataBrokerService;
  private UserManagementServiceImpl userManagementService;

  public UserManagementController(Router router, Vertx vertx, Api api, JsonObject config) {
    this.config = config;
    this.router = router;
    this.api = api;
    this.vertx = vertx;
    this.audience = config.getString("audience");
  }

  public void init() {
    createProxy();
    CatalogueService catalogueService = new CatalogueService(cacheService, config, vertx);
    AuthHandler authHandler = new AuthHandler(api, authenticator);
    Handler<RoutingContext> getIdHandler =
        new GetIdHandler(api).withNormalisedPath(api.getManagementApiPath());
    Handler<RoutingContext> validateToken =
        new AuthValidationHandler(api, cacheService, audience, catalogueService);
    Handler<RoutingContext> adminAndUserAccessHandler =
        new AuthorizationHandler()
            .setUserRolesForEndpoint(
                DxRole.DELEGATE, DxRole.CONSUMER, DxRole.PROVIDER, DxRole.ADMIN);
    FailureHandler validationsFailureHandler = new FailureHandler();

    router
        .post(api.getManagementApiPath())
        .handler(getIdHandler)
        .handler(authHandler)
        .handler(validateToken)
        .handler(adminAndUserAccessHandler)
        .handler(this::resetPassword)
        .failureHandler(validationsFailureHandler);

    userManagementService = new UserManagementServiceImpl(dataBrokerService);
  }

  private void resetPassword(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    String userId = RoutingContextHelper.getJwtData(routingContext).getSub();
    userManagementService
        .resetPassword(userId)
        .onSuccess(
            successResponse ->
                routingContext
                    .response()
                    .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .setStatusCode(200)
                    .end(successResponse.toString()))
        .onFailure(
            failureHandler -> {
              LOGGER.error("Error while resetting password for user");
              ResultModel rs = new ResultModel(failureHandler.getMessage(), response);
              response
                  .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                  .setStatusCode(rs.getStatusCode())
                  .end(rs.toJson().toString());
            });
  }

  void createProxy() {
    authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    cacheService = CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);
    dataBrokerService = DataBrokerService.createProxy(vertx, DATA_BROKER_SERVICE_ADDRESS);
  }
}
