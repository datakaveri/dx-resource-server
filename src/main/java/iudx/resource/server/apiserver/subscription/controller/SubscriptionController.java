package iudx.resource.server.apiserver.subscription.controller;

import static iudx.resource.server.apiserver.subscription.util.Constants.ENTITIES;
import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.cache.util.Constants.CACHE_SERVICE_ADDRESS;
import static iudx.resource.server.common.Constants.AUTH_SERVICE_ADDRESS;
import static iudx.resource.server.common.ResponseUrn.INVALID_PARAM_URN;
import static iudx.resource.server.common.ResponseUrn.SUCCESS_URN;
import static iudx.resource.server.database.postgres.util.Constants.PG_SERVICE_ADDRESS;
import static iudx.resource.server.databroker.util.Constants.DATA_BROKER_SERVICE_ADDRESS;
import static iudx.resource.server.databroker.util.Constants.SUCCESS;
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
import iudx.resource.server.apiserver.auditing.handler.AuditingHandler;
import iudx.resource.server.apiserver.exception.FailureHandler;
import iudx.resource.server.apiserver.subscription.model.GetSubscriptionResult;
import iudx.resource.server.apiserver.subscription.model.PostSubscriptionModel;
import iudx.resource.server.apiserver.subscription.model.SubscriberDetails;
import iudx.resource.server.apiserver.subscription.service.SubscriptionService;
import iudx.resource.server.apiserver.subscription.service.SubscriptionServiceImpl;
import iudx.resource.server.apiserver.subscription.util.SubsType;
import iudx.resource.server.apiserver.validation.id.handlers.GetIdFromBodyHandler;
import iudx.resource.server.authenticator.AuthenticationService;
import iudx.resource.server.authenticator.handler.authentication.AuthHandler;
import iudx.resource.server.authenticator.handler.authorization.AuthValidationHandler;
import iudx.resource.server.authenticator.handler.authorization.AuthorizationHandler;
import iudx.resource.server.authenticator.handler.authorization.GetIdHandler;
import iudx.resource.server.authenticator.handler.authorization.TokenRevokedHandler;
import iudx.resource.server.authenticator.model.DxRole;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.common.*;
import iudx.resource.server.common.validation.handler.ValidationHandler;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.databroker.service.DataBrokerService;

public class SubscriptionController {
  private static final Logger LOGGER = LogManager.getLogger(SubscriptionController.class);
  private final Router router;
  private final Vertx vertx;
  private final Api api;
  private final ValidationHandler subsValidationHandler;
  private final FailureHandler failureHandler;
  private final String audience;
  private final JsonObject config;
  private final AuditingHandler auditingHandler;
  private PostgresService postgresService;
  private SubscriptionService subscriptionService;
  private DataBrokerService dataBrokerService;
  private CacheService cacheService;
  private AuthenticationService authenticator;

  public SubscriptionController(Vertx vertx, Router router, Api api, JsonObject config) {
    this.vertx = vertx;
    this.router = router;
    this.api = api;
    /*TODO: update example config-dev and config-dev */
    this.audience = config.getString("audience");
    this.config = config;
    this.subsValidationHandler = new ValidationHandler(vertx, RequestType.SUBSCRIPTION);
    this.failureHandler = new FailureHandler();
    this.auditingHandler = new AuditingHandler(vertx);
  }

  public void init() {
    proxyRequired();
    CatalogueService catalogueService = new CatalogueService(cacheService, config, vertx);

    AuthHandler authHandler = new AuthHandler(api, authenticator);
    Handler<RoutingContext> getIdHandler =
        new GetIdHandler(api).withNormalisedPath(api.getSubscriptionUrl());

    Handler<RoutingContext> isTokenRevoked = new TokenRevokedHandler(cacheService).isTokenRevoked();
    Handler<RoutingContext> validateToken =
        new AuthValidationHandler(api, cacheService, audience, catalogueService);

    Handler<RoutingContext> userAndAdminAccessHandler =
        new AuthorizationHandler()
            .setUserRolesForEndpoint(
                DxRole.DELEGATE, DxRole.CONSUMER, DxRole.PROVIDER, DxRole.ADMIN);
    GetIdFromBodyHandler getIdFromBodyHandler = new GetIdFromBodyHandler();

    router
        .post(api.getSubscriptionUrl())
        .handler(auditingHandler::handleApiAudit)
        .handler(subsValidationHandler)
        .handler(/*getIdHandler*/ getIdFromBodyHandler)
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::postSubscriptions)
        .failureHandler(failureHandler);

    router
        .patch(api.getSubscriptionUrl() + "/:userid/:alias")
        .handler(auditingHandler::handleApiAudit)
        .handler(subsValidationHandler)
        .handler(/*getIdHandler*/ getIdFromBodyHandler)
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::appendSubscription)
        .failureHandler(failureHandler);

    router
        .put(api.getSubscriptionUrl() + "/:userid/:alias")
        .handler(auditingHandler::handleApiAudit)
        .handler(subsValidationHandler)
        .handler(/*getIdHandler*/ getIdFromBodyHandler)
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::updateSubscription)
        .failureHandler(failureHandler);

    router
        .get(api.getSubscriptionUrl() + "/:userid/:alias")
        .handler(auditingHandler::handleApiAudit)
        /*.handler(getIdHandler)*/
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::getSubscription)
        .failureHandler(failureHandler);

    router
        .get(api.getSubscriptionUrl())
        /*.handler(getIdHandler)*/
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::getAllSubscriptionForUser)
        .failureHandler(failureHandler);

    router
        .delete(api.getSubscriptionUrl() + "/:userid/:alias")
        .handler(auditingHandler::handleApiAudit)
        /*.handler(getIdHandler)*/
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::deleteSubscription)
        .failureHandler(failureHandler);

    subscriptionService =
        new SubscriptionServiceImpl(postgresService, dataBrokerService, cacheService);
  }

  private void appendSubscription(RoutingContext routingContext) {
    LOGGER.trace("Info: appendSubscription method started");
    JwtData jwtData = RoutingContextHelper.getJwtData(routingContext);
    HttpServerRequest request = routingContext.request();
    String userid = request.getParam(USER_ID);
    String alias = request.getParam(JSON_ALIAS);
    String subsId = userid + "/" + alias;
    JsonObject requestJson = routingContext.body().asJsonObject();
    String instanceId = request.getHeader(HEADER_HOST);
    String subscriptionType = SubsType.STREAMING.type;

    HttpServerResponse response = routingContext.response();

    String entities = requestJson.getJsonArray("entities").getString(0);
    String userId = jwtData.getSub();
    String role = jwtData.getRole();
    String drl = jwtData.getDrl();
    String delegatorId;
    if (role.equalsIgnoreCase("delegate") && drl != null) {
      delegatorId = jwtData.getDid();
    } else {
      delegatorId = userId;
    }
    PostSubscriptionModel postSubscriptionModel =
        new PostSubscriptionModel(
            userId,
            subscriptionType,
            instanceId,
            entities,
            requestJson.getString("name"),
            jwtData.getExpiry(),
            delegatorId);
    if (requestJson.getString(JSON_NAME).equalsIgnoreCase(alias)) {
      Future<String> subsReq =
          subscriptionService.appendSubscription(postSubscriptionModel, subsId);
      subsReq.onComplete(
          subsRequestHandler -> {
            if (subsRequestHandler.succeeded()) {
              LOGGER.info("appended subscription");
              RoutingContextHelper.setResponseSize(routingContext, 0);
              List<String> resultEntities = new ArrayList<String>();
              resultEntities.add(entities);
              JsonObject resultJson =
                  new JsonObject()
                      .put("type", ResponseUrn.SUCCESS_URN.getUrn())
                      .put("title", ResponseUrn.SUCCESS_URN.getMessage().toLowerCase())
                      .put(
                          "results",
                          new JsonArray()
                              .add(new JsonObject().put(ENTITIES, new JsonArray(resultEntities))));
              response
                  .setStatusCode(201)
                  .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                  .end(resultJson.toString());
            } else {
              LOGGER.error("Fail: Bad request");
              routingContext.fail(subsRequestHandler.cause());
            }
          });
    } else {
      LOGGER.error("Fail: Bad request");
      response
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(400)
          .end(
              getResponseJson(
                      INVALID_PARAM_URN.getUrn(),
                      HttpStatusCode.BAD_REQUEST.getDescription(),
                      MSG_INVALID_NAME)
                  .toString());
    }
  }

  private void updateSubscription(RoutingContext routingContext) {
    LOGGER.trace("Info: updateSubscription method started");
    JwtData jwtData = RoutingContextHelper.getJwtData(routingContext);
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String userid = request.getParam(USER_ID);
    String alias = request.getParam(JSON_ALIAS);
    String subsId = userid + "/" + alias;
    JsonObject requestJson = routingContext.body().asJsonObject();

    if (requestJson.getString(JSON_NAME).equalsIgnoreCase(alias)) {
      String entities = requestJson.getJsonArray("entities").getString(0);
      Future<String> subsReq =
          subscriptionService.updateSubscription(entities, subsId, jwtData.getExpiry());
      subsReq.onComplete(
          subsRequestHandler -> {
            if (subsRequestHandler.succeeded()) {
              LOGGER.info("Updated subscription");
              RoutingContextHelper.setResponseSize(routingContext, 0);
              List<String> resultEntities = new ArrayList<String>();
              resultEntities.add(entities);
              JsonObject resultJson =
                  new JsonObject()
                      .put("type", ResponseUrn.SUCCESS_URN.getUrn())
                      .put("title", ResponseUrn.SUCCESS_URN.getMessage().toLowerCase())
                      .put(
                          "results",
                          new JsonArray()
                              .add(new JsonObject().put(ENTITIES, new JsonArray(resultEntities))));
              response
                  .setStatusCode(201)
                  .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                  .end(resultJson.toString());
            } else {
              LOGGER.error("Fail: Bad request");
              routingContext.fail(subsRequestHandler.cause());
            }
          });
    } else {
      LOGGER.error("Fail: Bad request");
      response
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(400)
          .end(
              getResponseJson(
                      INVALID_PARAM_URN.getUrn(),
                      HttpStatusCode.BAD_REQUEST.getDescription(),
                      MSG_INVALID_NAME)
                  .toString());
    }
  }

  private void getSubscription(RoutingContext routingContext) {
    LOGGER.trace("Info: getSubscription method started");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String domain = request.getParam(USER_ID);
    String alias = request.getParam(JSON_ALIAS);
    String subsId = domain + "/" + alias;
    String subscriptionType = SubsType.STREAMING.type;

    Future<GetSubscriptionResult> subsReq =
        subscriptionService.getSubscription(subsId, subscriptionType);
    subsReq.onComplete(
        subHandler -> {
          if (subHandler.succeeded()) {
            LOGGER.info("Success: Getting subscription");
            RoutingContextHelper.setResponseSize(routingContext, 0);
            RoutingContextHelper.setId(routingContext, subHandler.result().entities());
            response.putHeader(CONTENT_TYPE, APPLICATION_JSON).end(subHandler.result().toString());
          } else {
            routingContext.fail(subHandler.cause());
          }
        });
  }

  private void getAllSubscriptionForUser(RoutingContext routingContext) {
    LOGGER.trace("Info: getAllSubscriptionForUser method started");
    JwtData jwtData = RoutingContextHelper.getJwtData(routingContext);
    HttpServerResponse response = routingContext.response();

    Future<List<SubscriberDetails>> subsReq =
        subscriptionService.getAllSubscriptionQueueForUser(jwtData.getSub());
    subsReq.onComplete(
        subHandler -> {
          if (subHandler.succeeded()) {
            LOGGER.info("Success: Getting subscription queue" + subHandler.result());
            response.putHeader(CONTENT_TYPE, APPLICATION_JSON).end(subHandler.result().toString());
          } else {
            routingContext.fail(subHandler.cause());
          }
        });
  }

  private void deleteSubscription(RoutingContext routingContext) {
    LOGGER.trace("Info: deleteSubscription() method started;");
    JwtData jwtData = RoutingContextHelper.getJwtData(routingContext);
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String userid = request.getParam(USER_ID);
    String alias = request.getParam(JSON_ALIAS);
    String subsId = userid + "/" + alias;
    String subscriptionType = SubsType.STREAMING.type;
    String userId = jwtData.getSub();
    Future<String> subsReq =
        subscriptionService.deleteSubscription(subsId, subscriptionType, userId);
    subsReq.onComplete(
        subHandler -> {
          if (subHandler.succeeded()) {
            LOGGER.info("Success: Deleting subscription");
            RoutingContextHelper.setResponseSize(routingContext, 0);
            RoutingContextHelper.setId(routingContext, subHandler.result());
            response
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setStatusCode(200)
                .end(
                    getResponseJson(
                            SUCCESS_URN.getUrn(), SUCCESS, "Subscription deleted Successfully")
                        .toString());
          } else {
            routingContext.fail(subHandler.cause());
          }
        });
  }

  private void postSubscriptions(RoutingContext routingContext) {
    LOGGER.trace("Info: postSubscriptions() method started");
    JwtData jwtData = RoutingContextHelper.getJwtData(routingContext);
    HttpServerRequest request = routingContext.request();
    JsonObject requestBody = routingContext.body().asJsonObject();
    String instanceId = request.getHeader(HEADER_HOST);
    String subscriptionType = SubsType.STREAMING.type;

    HttpServerResponse response = routingContext.response();
    String entities = requestBody.getJsonArray("entities").getString(0);
    String userId = jwtData.getSub();
    String role = jwtData.getRole();
    String drl = jwtData.getDrl();
    String delegatorId;
    if (role.equalsIgnoreCase("delegate") && drl != null) {
      delegatorId = jwtData.getDid();
    } else {
      delegatorId = userId;
    }
    PostSubscriptionModel postSubscriptionModel =
        new PostSubscriptionModel(
            userId,
            subscriptionType,
            instanceId,
            entities,
            requestBody.getString("name"),
            jwtData.getExpiry(),
            delegatorId);

    subscriptionService
        .createSubscription(postSubscriptionModel)
        .onSuccess(
            subHandler -> {
              LOGGER.info("Success: Handle Subscription request;");
              RoutingContextHelper.setResponseSize(routingContext, 0);
              response
                  .setStatusCode(201)
                  .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                  .end(subHandler.toJson().toString());
            })
        .onFailure(
            failure -> {
              routingContext.fail(failure);
            });
  }

  private void proxyRequired() {
    postgresService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);
    dataBrokerService = DataBrokerService.createProxy(vertx, DATA_BROKER_SERVICE_ADDRESS);
    cacheService = CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);
    authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
  }
}
