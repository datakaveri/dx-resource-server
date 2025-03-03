package iudx.resource.server.apiserver.ingestion.controller;

import static iudx.resource.server.apiserver.subscription.util.Constants.RESULTS;
import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.Constants.AUTH_SERVICE_ADDRESS;
import static iudx.resource.server.common.Constants.CACHE_SERVICE_ADDRESS;
import static iudx.resource.server.common.HttpStatusCode.*;
import static iudx.resource.server.common.ResponseUrn.*;
import static iudx.resource.server.database.postgres.util.Constants.PG_SERVICE_ADDRESS;
import static iudx.resource.server.databroker.util.Constants.*;
import static iudx.resource.server.databroker.util.Util.getResponseJson;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.exception.FailureHandler;
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
import iudx.resource.server.database.postgres.model.PostgresResultModel;
import iudx.resource.server.database.postgres.service.PostgresService;
import iudx.resource.server.databroker.service.DataBrokerService;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IngestionController {
  private static final Logger LOGGER = LogManager.getLogger(IngestionController.class);
  private final Router router;
  private final Vertx vertx;
  private final Api api;
  private final JsonObject config;
  private final String audience;
  private AuthenticationService authenticator;
  private CacheService cacheService;
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
    String entities = requestJson.getJsonArray("entities").getString(0);
    ingestionService
        .registerAdapter(entities, instanceId, userId)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.info("Success: Registering adapter");
                routingContext.data().put(RESPONSE_SIZE, 0);
                response
                    .setStatusCode(201)
                    .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .end(handler.result().constructSuccessResponse().toString());
                /*routingContext.next();*/
              } else {
                LOGGER.error("Fail: " + handler.cause());
                routingContext.fail(handler.cause());
              }
            });
  }

  private void deleteAdapter(RoutingContext routingContext) {
    LOGGER.trace("Info: deleteAdapter method starts;");
    Map<String, String> pathParams = routingContext.pathParams();
    String adaptorId = pathParams.get("*");

    String userId = RoutingContextHelper.getJwtData(routingContext).getSub();
    HttpServerResponse response = routingContext.response();
    ingestionService
        .deleteAdapter(adaptorId, userId)
        .onSuccess(
            brokerResultHandler -> {
              LOGGER.info("Success: Deleting adapter");
              routingContext.data().put(RESPONSE_SIZE, 0);
              JsonObject deleteResponse = new JsonObject();
              deleteResponse.put(TYPE, ResponseUrn.SUCCESS_URN.getUrn());
              deleteResponse.put(TITLE, "Success");
              deleteResponse.put(RESULTS, "Adapter deleted");
              response
                  .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                  .setStatusCode(200)
                  .end(deleteResponse.toString());
              /*routingContext.next();*/
            })
        .onFailure(
            failure -> {
              LOGGER.error("Fail: Deleting adapter");
              routingContext.fail(failure);
            });
  }

  private void getAdapterDetails(RoutingContext routingContext) {
    LOGGER.trace("getAdapterDetails method starts");
    Map<String, String> pathParams = routingContext.pathParams();
    HttpServerResponse response = routingContext.response();
    String id = pathParams.get("UUID");
    ingestionService
        .getAdapterDetails(id)
        .onSuccess(
            brokerResultHandler -> {
              routingContext.data().put(RESPONSE_SIZE, 0);
              response
                  .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                  .setStatusCode(200)
                  .end(brokerResultHandler.constructSuccessResponse().toString());
              /*routingContext.next();*/
            })
        .onFailure(
            failure -> {
              routingContext.fail(failure);
            });
  }

  private void publishDataFromAdapter(RoutingContext routingContext) {
    LOGGER.trace("Info: publishDataFromAdapter method started;");
    HttpServerResponse response = routingContext.response();
    try {
      JsonArray requestJson = routingContext.body().asJsonArray();
      ingestionService
          .publishDataFromAdapter(requestJson)
          .onComplete(
              brokerResultHandler -> {
                if (brokerResultHandler.succeeded()) {
                  LOGGER.debug("Success: publishing data from adapter");
                  routingContext.data().put(RESPONSE_SIZE, 0);
                  response
                      .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                      .setStatusCode(200)
                      .end(brokerResultHandler.result().constructSuccessResponse().toString());
                  /*routingContext.next();*/
                } else {
                  LOGGER.debug("Fail: Bad request");
                  routingContext.fail(brokerResultHandler.cause());
                }
              });
    } catch (Exception e) {
      LOGGER.error("Fail: Bad request;" + e.getMessage());
      response
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(500)
          .end(
              getResponseJson(
                      INTERNAL_SERVER_ERROR.getUrn(),
                      INTERNAL_SERVER_ERROR.getDescription(),
                      "Error occurred while parsing")
                  .toString());
    }
  }

  private void getAllAdaptersForUsers(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    String iid = RoutingContextHelper.getJwtData(routingContext).getIid().split(":")[1];
    LOGGER.debug("Getting all adapters for user : " + iid);
    if (iid != null) {
      Future<PostgresResultModel> allAdapterForUser =
          ingestionService.getAllAdapterDetailsForUser(iid);
      allAdapterForUser.onComplete(
          handler -> {
            if (handler.succeeded()) {
              LOGGER.debug("Successful");
              if (handler.result().getResult().isEmpty()) {
                response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(204).end();
              } else {
                response
                    .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .setStatusCode(200)
                    .end(handler.result().toJson().toString());
              }
            } else {
              LOGGER.error("failed to complete request");
              routingContext.fail(handler.cause());
            }
          });
    } else {
      LOGGER.error("Fail: Bad request");
      response
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(400)
          .end(
              getResponseJson(
                      BAD_REQUEST.getUrn(),
                      HttpStatusCode.BAD_REQUEST.getDescription(),
                      "iid not found")
                  .toString());
    }
  }

  void createProxy() {
    this.authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    this.cacheService = CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);
    this.dataBrokerService = DataBrokerService.createProxy(vertx, DATA_BROKER_SERVICE_ADDRESS);
    this.postgresService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);
  }
}
