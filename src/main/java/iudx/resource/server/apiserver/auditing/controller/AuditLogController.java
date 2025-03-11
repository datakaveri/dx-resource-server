package iudx.resource.server.apiserver.auditing.controller;

import static iudx.resource.server.apiserver.auditing.util.Constants.END_TIME;
import static iudx.resource.server.apiserver.auditing.util.Constants.START_TIME;
import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.Constants.*;
import static iudx.resource.server.common.HttpStatusCode.UNAUTHORIZED;
import static iudx.resource.server.common.ResponseUrn.SUCCESS_URN;
import static iudx.resource.server.common.ResponseUrn.UNAUTHORIZED_RESOURCE_URN;
import static iudx.resource.server.common.ResponseUtil.generateResponse;
import static iudx.resource.server.databroker.util.Constants.DATA_BROKER_SERVICE_ADDRESS;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.auditing.model.AuditLogSearchRequest;
import iudx.resource.server.apiserver.auditing.model.OverviewRequest;
import iudx.resource.server.apiserver.auditing.service.AuditLogService;
import iudx.resource.server.apiserver.auditing.service.AuditLogServiceImpl;
import iudx.resource.server.apiserver.auditing.util.DateValidation;
import iudx.resource.server.apiserver.exception.FailureHandler;
import iudx.resource.server.authenticator.AuthenticationService;
import iudx.resource.server.authenticator.handler.authentication.AuthHandler;
import iudx.resource.server.authenticator.handler.authorization.*;
import iudx.resource.server.authenticator.model.DxAccess;
import iudx.resource.server.authenticator.model.DxRole;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.common.Api;
import iudx.resource.server.common.CatalogueService;
import iudx.resource.server.common.Constants;
import iudx.resource.server.common.ResponseType;
import iudx.resource.server.database.postgres.service.PostgresService;
import iudx.resource.server.databroker.service.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AuditLogController {
  private static final Logger LOGGER = LogManager.getLogger(AuditLogController.class);
  private final Router router;
  private final Api api;
  private final JsonObject config;
  private final String audience;
  private final Vertx vertx;
  PostgresService postgresService;
  private DataBrokerService dataBrokerService;
  private CacheService cacheService;
  private AuditLogService auditLogService;
  private AuthenticationService authenticator;

  public AuditLogController(Vertx vertx,Router router, Api api, JsonObject config) {
    this.router = router;
    this.api = api;
    this.vertx = vertx;
    this.config = config;
    this.audience = config.getString("audience");
  }

  public void init() {
    createProxy();
    CatalogueService catalogueService = new CatalogueService(cacheService, config, vertx);
    FailureHandler failureHandler = new FailureHandler();
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
        .handler(this::ValidateDateTime)
        .handler(getIdHandler.withNormalisedPath(api.getMonthlyOverview()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(apiConstraint)
        .handler(isTokenRevoked)
        .handler(this::handleMonthlyOverview)
        .failureHandler(failureHandler);
    router
        .get(api.getSummaryPath())
        .handler(this::ValidateDateTime)
        .handler(getIdHandler.withNormalisedPath(api.getSummaryPath()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(apiConstraint)
        .handler(isTokenRevoked)
        .handler(this::handleSummaryRequest)
        .failureHandler(failureHandler);
    router
        .get(api.getIudxConsumerAuditUrl())
        .handler(getIdHandler.withNormalisedPath(api.getIudxConsumerAuditUrl()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(apiConstraint)
        .handler(isTokenRevoked)
        .handler(this::handleConsumerAuditDetailRequest)
        .failureHandler(failureHandler);

    router
        .get(api.getIudxProviderAuditUrl())
        .handler(getIdHandler.withNormalisedPath(api.getIudxProviderAuditUrl()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(providerAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::handleProviderAuditDetail)
        .failureHandler(failureHandler);

    auditLogService = new AuditLogServiceImpl(postgresService, cacheService, api);
  }

  void createProxy() {
    this.authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    this.cacheService = CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);
    this.dataBrokerService = DataBrokerService.createProxy(vertx, DATA_BROKER_SERVICE_ADDRESS);
    this.postgresService = PostgresService.createProxy(vertx, Constants.PG_SERVICE_ADDRESS);
  }

  private void handleConsumerAuditDetailRequest(RoutingContext routingContext) {
    LOGGER.debug("Info: handleConsumerAuditDetailRequest() Started.");
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    HttpServerRequest request = routingContext.request();

    AuditLogSearchRequest auditLogSearchRequest =
        AuditLogSearchRequest.fromHttpRequest(request, authInfo);
    HttpServerResponse response = routingContext.response();

    auditLogService
        .executeAuditingSearchQuery(auditLogSearchRequest)
        .onSuccess(
            result -> {
              String checkType = result.getString("type");
              if (checkType.equalsIgnoreCase("204")) {
                handleSuccessResponse(
                    response, ResponseType.NoContent.getCode(), result.getJsonArray("results"));
              } else {
                handleSuccessResponse(
                    response, ResponseType.Ok.getCode(), result.getJsonArray("results"));
              }
            })
        .onFailure(
            failure -> {
              LOGGER.error("Error during audit log: {}", failure.getMessage());
              routingContext.fail(failure);
            });
  }

  private void handleProviderAuditDetail(RoutingContext routingContext) {
    LOGGER.trace("Info: getProviderAuditDetail() Started.");
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    HttpServerRequest request = routingContext.request();

    AuditLogSearchRequest auditLogSearchRequest =
        AuditLogSearchRequest.fromHttpRequest(request, authInfo);
    LOGGER.debug("AuditLogSearchRequest: {}", auditLogSearchRequest.toJson());

    HttpServerResponse response = routingContext.response();

    auditLogService
        .executeAuditingSearchQuery(auditLogSearchRequest)
        .onSuccess(
            result -> {
              String checkType = result.getString("type");
              if (checkType.equalsIgnoreCase("204")) {
                handleSuccessResponse(
                    response, ResponseType.NoContent.getCode(), result.getJsonArray("results"));
              } else {
                handleSuccessResponse(
                    response, ResponseType.Ok.getCode(), result.getJsonArray("results"));
              }
            })
        .onFailure(
            failure -> {
              LOGGER.error("Error during audit log: {}", failure.getMessage());
              routingContext.fail(failure);
            });
  }

  private void handleMonthlyOverview(RoutingContext routingContext) {
    LOGGER.trace("Info: getMonthlyOverview Started.");
    HttpServerRequest request = routingContext.request();
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    authInfo.put(START_TIME, request.getParam(START_TIME));
    authInfo.put(END_TIME, request.getParam(END_TIME));
    HttpServerResponse response = routingContext.response();

    String iid = authInfo.getString("iid");
    String role = authInfo.getString("role");

    if (!VALIDATION_ID_PATTERN.matcher(iid).matches()
        && (role.equalsIgnoreCase("provider") || role.equalsIgnoreCase("delegate"))) {
      JsonObject jsonResponse =
          generateResponse(UNAUTHORIZED, UNAUTHORIZED_RESOURCE_URN, "Not Authorized");
      response
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(401)
          .end(jsonResponse.toString());
      return;
    }
    OverviewRequest monthlyOverviewRequest = OverviewRequest.fromJson(authInfo);
    auditLogService
        .monthlyOverview(monthlyOverviewRequest)
        .onSuccess(
            overviewResponseList -> {
              JsonArray result = new JsonArray();
              overviewResponseList.forEach(
                  overviewResponse -> result.add(overviewResponse.toJson()));
              handleSuccessResponse(response, ResponseType.Ok.getCode(), result);
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error(
                  "Error during get getMonthlyOverview data: {}", failureHandler.getMessage());
              routingContext.fail(failureHandler);
            });
  }

  private void handleSummaryRequest(RoutingContext routingContext) {
    LOGGER.trace("getAllSummaryHandler() started");
    HttpServerRequest request = routingContext.request();
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    authInfo.put(START_TIME, request.getParam(START_TIME));
    authInfo.put(END_TIME, request.getParam(END_TIME));
    HttpServerResponse response = routingContext.response();

    String iid = authInfo.getString("iid");
    String role = authInfo.getString("role");

    if (!VALIDATION_ID_PATTERN.matcher(iid).matches()
        && (role.equalsIgnoreCase("provider") || role.equalsIgnoreCase("delegate"))) {
      JsonObject jsonResponse =
          generateResponse(UNAUTHORIZED, UNAUTHORIZED_RESOURCE_URN, "Not Authorized");
      response
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(401)
          .end(jsonResponse.toString());
      return;
    }
    OverviewRequest summaryOverviewRequest = OverviewRequest.fromJson(authInfo);
    auditLogService
        .summaryOverview(summaryOverviewRequest)
        .onSuccess(
            result -> {
              String checkType = result.getString("type");
              if (checkType.equalsIgnoreCase("204")) {
                handleSuccessResponse(
                    response, ResponseType.NoContent.getCode(), result.getJsonArray("results"));
              } else {
                handleSuccessResponse(
                    response, ResponseType.Ok.getCode(), result.getJsonArray("results"));
              }
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error(
                  "Error during get summaryOverview data: {}", failureHandler.getMessage());
              routingContext.fail(failureHandler);
            });
  }

  private void handleSuccessResponse(
      HttpServerResponse response, int statusCode, JsonArray result) {
    JsonObject resultJson =
        new JsonObject()
            .put(JSON_TYPE, SUCCESS_URN.getUrn())
            .put(JSON_TITLE, SUCCESS_URN.getMessage())
            .put("result", result);
    response
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(statusCode)
        .end(resultJson.encode());
  }

  private void ValidateDateTime(RoutingContext routingContext) {
    LOGGER.debug("info: ValidateDateTime() started");
    HttpServerRequest request = routingContext.request();
    String startTime = request.getParam(START_TIME);
    String endTime = request.getParam(END_TIME);
    if (DateValidation.dateParamCheck(startTime, endTime)) {
      routingContext.next();
    }
  }
}
