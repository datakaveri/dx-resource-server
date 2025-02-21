package iudx.resource.server.apiserver.metering.controller;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.Constants.*;
import static iudx.resource.server.databroker.util.Constants.DATA_BROKER_SERVICE_ADDRESS;

import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.exception.DxRuntimeException;
import iudx.resource.server.apiserver.exception.FailureHandler;
import iudx.resource.server.apiserver.metering.service.MeteringService;
import iudx.resource.server.apiserver.metering.service.MeteringServiceImpl;
import iudx.resource.server.authenticator.AuthenticationService;
import iudx.resource.server.authenticator.handler.authentication.AuthHandler;
import iudx.resource.server.authenticator.handler.authorization.*;
import iudx.resource.server.authenticator.model.DxAccess;
import iudx.resource.server.authenticator.model.DxRole;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.common.Api;
import iudx.resource.server.common.CatalogueService;
import iudx.resource.server.common.RoutingContextHelper;
import iudx.resource.server.database.postgres.service.PostgresService;
import iudx.resource.server.databroker.service.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MeteringController {
  private static final Logger LOGGER = LogManager.getLogger(MeteringController.class);

  private final Router router;
  private final Api api;
  private final Vertx vertx;
  private final JsonObject config;
  private final String audience;
  private CacheService cacheService;
  private AuthenticationService authenticator;
  private PostgresService postgresService;
  private DataBrokerService dataBrokerService;
  private MeteringService meteringService;
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
    AuthHandler authHandler = new AuthHandler(api, authenticator);
    Handler<RoutingContext> validateToken =
        new AuthValidationHandler(api, cacheService, audience, catalogueService);
    Handler<RoutingContext> userAndAdminAccessHandler =
        new AuthorizationHandler()
            .setUserRolesForEndpoint(
                DxRole.DELEGATE, DxRole.CONSUMER, DxRole.PROVIDER, DxRole.ADMIN);
    Handler<RoutingContext> isTokenRevoked = new TokenRevokedHandler(cacheService).isTokenRevoked();

    Handler<RoutingContext> providerAndAdminAccessHandler =
        new AuthorizationHandler()
            .setUserRolesForEndpoint(DxRole.DELEGATE, DxRole.PROVIDER, DxRole.ADMIN);

    Handler<RoutingContext> apiConstraint =
        new ConstraintsHandlerForConsumer().consumerConstraintsForEndpoint(DxAccess.API);

    router
        .get(api.getMonthlyOverview())
        .handler(getIdHandler.withNormalisedPath(api.getMonthlyOverview()))
        .handler(authHandler)
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
        .handler(validateToken)
        .handler(providerAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::getProviderAuditDetail)
        .failureHandler(validationsFailureHandler);

    meteringService = new MeteringServiceImpl(postgresService,cacheService,dataBrokerService);

  }

  private void getProviderAuditDetail(RoutingContext routingContext) {
    LOGGER.trace("Info: getProviderAuditDetail Started.");
    JsonObject entries = new JsonObject();
    JwtData jwtData = RoutingContextHelper.getJwtData(routingContext);
    HttpServerRequest request = routingContext.request();
    /*JsonObject provider = *//*(JsonObject) routingContext.data().get("authInfo")*/
    entries.put("endPoint", api.getIudxProviderAuditUrl());
    entries.put("time", request.getParam("time"));
    entries.put("endTime", request.getParam("endTime"));
    entries.put("timeRelation", request.getParam("timerel"));
    entries.put("providerID", request.getParam("providerID"));
    entries.put("consumerID", request.getParam("consumer"));
    entries.put("resourceId", request.getParam("id"));
    entries.put("api", request.getParam("api"));
    entries.put("options", request.headers().get("options"));
    entries.put("offset", request.getParam(OFFSETPARAM));
    entries.put("limit", request.getParam(LIMITPARAM));
    entries.put("iid",jwtData.getIid());
    entries.put("userid", jwtData.getSub());

    LOGGER.debug(entries);
    Promise<Void> promise = Promise.promise();
    HttpServerResponse response = routingContext.response();
      meteringService
          .executeReadQuery(entries)
          .onSuccess(
              successResult -> {
                response
                    .setStatusCode(200)
                    .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .end(successResult.toString());
              })
          .onFailure(
              failureResult -> {
                LOGGER.error("Fail");
                routingContext.fail(new DxRuntimeException(failureResult.getMessage()));
              });

  }

  private void getConsumerAuditDetail(RoutingContext routingContext) {}

  private void getSummary(RoutingContext routingContext) {}

  private void getOverview(RoutingContext routingContext) {}

  void createProxy() {
    this.authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    this.cacheService = CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);
    this.dataBrokerService = DataBrokerService.createProxy(vertx, DATA_BROKER_SERVICE_ADDRESS);
    this.postgresService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);
  }

}
