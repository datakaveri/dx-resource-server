package iudx.resource.server.apiserver.ingestion.controller;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.Constants.AUTH_SERVICE_ADDRESS;
import static iudx.resource.server.common.Constants.CACHE_SERVICE_ADDRESS;
import static iudx.resource.server.common.HttpStatusCode.BAD_REQUEST;
import static iudx.resource.server.common.HttpStatusCode.UNAUTHORIZED;
import static iudx.resource.server.common.ResponseUrn.*;
import static iudx.resource.server.common.ResponseUtil.generateResponse;
import static iudx.resource.server.database.postgres.util.Constants.PG_SERVICE_ADDRESS;
import static iudx.resource.server.databroker.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.handler.FailureHandler;
import iudx.resource.server.apiserver.ingestion.service.IngestionService;
import iudx.resource.server.apiserver.ingestion.service.IngestionServiceImpl;
import iudx.resource.server.authenticator.AuthenticationService;
import iudx.resource.server.authenticator.handler.authentication.AuthHandler;
import iudx.resource.server.authenticator.handler.authorization.AuthValidationHandler;
import iudx.resource.server.authenticator.handler.authorization.AuthorizationHandler;
import iudx.resource.server.authenticator.handler.authorization.GetIdHandler;
import iudx.resource.server.authenticator.handler.authorization.TokenRevokedHandler;
import iudx.resource.server.authenticator.model.DxRole;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.common.*;
import iudx.resource.server.database.postgres.service.PostgresService;
import iudx.resource.server.databroker.service.DataBrokerService;
import java.util.Map;
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
  private IngestionService ingestionService;
  private DataBrokerService dataBrokerService;
  private PostgresService postgresService;

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
    AuthHandler authHandler = new AuthHandler(api, authenticator);
    Handler<RoutingContext> validateToken =
        new AuthValidationHandler(api, cacheService, audience, catalogueService);
    Handler<RoutingContext> providerAndAdminAccessHandler =
        new AuthorizationHandler()
            .setUserRolesForEndpoint(DxRole.DELEGATE, DxRole.PROVIDER, DxRole.ADMIN);
    Handler<RoutingContext> isTokenRevoked = new TokenRevokedHandler(cacheService).isTokenRevoked();
    // TODO: Need to add auditing insert
    router
        .post(api.getIngestionPath())
        .handler(getIdHandler.withNormalisedPath(api.getIngestionPath()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(providerAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::registerAdapter)
        .failureHandler(validationsFailureHandler);

    router
        .delete(api.getIngestionPath() + "/*")
        .handler(getIdHandler.withNormalisedPath(api.getIngestionPath()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(providerAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::deleteAdapter)
        .failureHandler(validationsFailureHandler);

    router
        .get(api.getIngestionPath() + "/:UUID")
        .handler(getIdHandler.withNormalisedPath(api.getIngestionPath()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(providerAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::getAdapterDetails)
        .failureHandler(validationsFailureHandler);

    router
        .post(api.getIngestionPathEntities())
        .handler(getIdHandler.withNormalisedPath(api.getIngestionPathEntities()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(providerAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::publishDataFromAdapter)
        .failureHandler(validationsFailureHandler);

    router
        .get(api.getIngestionPath())
        .handler(getIdHandler.withNormalisedPath(api.getIngestionPath()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(providerAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::getAllAdaptersForUsers)
        .failureHandler(validationsFailureHandler);

    ingestionService = new IngestionServiceImpl(cacheService, dataBrokerService, postgresService);
  }

  private void registerAdapter(RoutingContext routingContext) {
    LOGGER.trace("Info: registerAdapter method started;");
    JsonObject requestJson = routingContext.body().asJsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceId = request.getHeader(HEADER_HOST);
    requestJson.put(JSON_INSTANCEID, instanceId);
    String userId = RoutingContextHelper.getJwtData(routingContext).getSub();
    requestJson.put(USER_ID, userId);

    Future<JsonObject> brokerResult = ingestionService.registerAdapter(requestJson);

    brokerResult.onComplete(
        handler -> {
          if (handler.succeeded()) {
            LOGGER.info("Success: Registering adapter");
            routingContext.data().put(RESPONSE_SIZE, 0);
            /*Future.future(fu -> updateAuditTable(routingContext));*/
            handleSuccessResponse(
                response, ResponseType.Created.getCode(), handler.result().toString());
          } else if (brokerResult.failed()) {
            LOGGER.error("Fail: Bad request" + handler.cause().getMessage());
            processBackendResponse(response, handler.cause().getMessage());
          }
        });
  }

  private void deleteAdapter(RoutingContext routingContext) {
    LOGGER.trace("Info: deleteAdapter method starts;");

    Map<String, String> pathParams = routingContext.pathParams();
    String id = pathParams.get("*");

    StringBuilder adapterIdBuilder = new StringBuilder();
    adapterIdBuilder.append(id);
    String userId = RoutingContextHelper.getJwtData(routingContext).getSub();
    Future<JsonObject> brokerResult =
        ingestionService.deleteAdapter(adapterIdBuilder.toString(), userId);
    HttpServerResponse response = routingContext.response();
    brokerResult.onComplete(
        brokerResultHandler -> {
          if (brokerResultHandler.succeeded()) {
            LOGGER.info("Success: Deleting adapter");
            routingContext.data().put(RESPONSE_SIZE, 0);
            /*Future.future(fu -> updateAuditTable(routingContext));*/
            handleSuccessResponse(
                response, ResponseType.Ok.getCode(), brokerResultHandler.result().toString());
          } else {
            LOGGER.error("Fail: Bad request;" + brokerResultHandler.cause().getMessage());
            processBackendResponse(response, brokerResultHandler.cause().getMessage());
          }
        });
  }

  private void getAdapterDetails(RoutingContext routingContext) {
    LOGGER.trace("getAdapterDetails method starts");

    Map<String, String> pathParams = routingContext.pathParams();
    String id = pathParams.get("UUID");
    Future<JsonObject> brokerResult = ingestionService.getAdapterDetails(id);
    HttpServerResponse response = routingContext.response();
    brokerResult.onComplete(
        brokerResultHandler -> {
          if (brokerResultHandler.succeeded()) {
            routingContext.data().put(RESPONSE_SIZE, 0);
            /*Future.future(fu -> updateAuditTable(routingContext));*/
            handleSuccessResponse(
                response, ResponseType.Ok.getCode(), brokerResultHandler.result().toString());
          } else {
            processBackendResponse(response, brokerResultHandler.cause().getMessage());
          }
        });
  }

  private void publishDataFromAdapter(RoutingContext routingContext) {
    LOGGER.trace("Info: publishDataFromAdapter method started;");
    JsonArray requestJson = routingContext.body().asJsonArray();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/iudx/v1/adapter");
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));

      Future<JsonObject> brokerResult = ingestionService.publishDataFromAdapter(requestJson);
      brokerResult.onComplete(
          brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              LOGGER.debug("Success: publishing data from adapter");
              routingContext.data().put(RESPONSE_SIZE, 0);
              /*Future.future(fu -> updateAuditTable(routingContext));*/
              handleSuccessResponse(
                  response, ResponseType.Ok.getCode(), brokerResultHandler.result().toString());
            } else {
              LOGGER.debug("Fail: Bad request;" + brokerResultHandler.cause().getMessage());
              processBackendResponse(response, brokerResultHandler.cause().getMessage());
            }
          });

    } else {
      LOGGER.debug("Fail: Unauthorized");
      handleResponse(response, UNAUTHORIZED, MISSING_TOKEN_URN);
    }
  }

  private void getAllAdaptersForUsers(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    String iid = RoutingContextHelper.getJwtData(routingContext).getIid().split(":")[1];
    LOGGER.debug("Getting all adapters for user : " + iid);
    Future<JsonObject> allAdapterForUser = ingestionService.getAllAdapterDetailsForUser(iid);
    allAdapterForUser.onComplete(
        handler -> {
          if (handler.succeeded()) {
            LOGGER.debug("Successful");
            if (handler.result().getJsonArray("result").isEmpty()) {
              handleSuccessResponse(
                  response, ResponseType.NoContent.getCode(), handler.result().toString());
            } else {
              handleSuccessResponse(
                  response, ResponseType.Ok.getCode(), handler.result().toString());
            }
          } else {
            LOGGER.debug(handler.cause());
            processBackendResponse(response, handler.cause().getMessage());
          }
        });
  }

  void createProxy() {
    this.authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    this.cacheService = CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);
    this.dataBrokerService = DataBrokerService.createProxy(vertx, DATA_BROKER_SERVICE_ADDRESS);
    this.postgresService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);
  }

  private void handleSuccessResponse(HttpServerResponse response, int statusCode, String result) {
    response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(statusCode).end(result);
  }

  private void processBackendResponse(HttpServerResponse response, String failureMessage) {
    LOGGER.debug("Info : " + failureMessage);
    try {
      JsonObject json = new JsonObject(failureMessage);
      int type = json.getInteger(JSON_TYPE);
      HttpStatusCode status = HttpStatusCode.getByValue(type);
      String urnTitle = json.getString(JSON_TITLE);
      ResponseUrn urn;
      if (urnTitle != null) {
        urn = fromCode(urnTitle);
      } else {
        urn = fromCode(String.valueOf(type));
      }
      // return urn in body
      if (json.getString("details") != null) {
        handleResponse(response, status, urn, json.getString("details"));
        return;
      }
      response
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(type)
          .end(generateResponse(status, urn).toString());
    } catch (DecodeException ex) {
      LOGGER.error("ERROR : Expecting Json from backend service [ jsonFormattingException ]");
      handleResponse(response, BAD_REQUEST, BACKING_SERVICE_FORMAT_URN);
    }
  }

  private void handleResponse(HttpServerResponse response, HttpStatusCode code, ResponseUrn urn) {
    handleResponse(response, code, urn, code.getDescription());
  }

  private void handleResponse(
      HttpServerResponse response, HttpStatusCode statusCode, ResponseUrn urn, String message) {
    response
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(statusCode.getValue())
        .end(generateResponse(statusCode, urn, message).toString());
  }
}
