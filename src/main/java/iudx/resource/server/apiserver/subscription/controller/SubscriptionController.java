package iudx.resource.server.apiserver.subscription.controller;

import static iudx.resource.server.apiserver.metering.util.Constant.METERING_SERVICE_ADDRESS;
import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.cache.util.Constants.CACHE_SERVICE_ADDRESS;
import static iudx.resource.server.common.Constants.AUTH_SERVICE_ADDRESS;
import static iudx.resource.server.common.ResponseUrn.INVALID_PARAM_URN;
import static iudx.resource.server.database.postgres.util.Constants.PG_SERVICE_ADDRESS;
import static iudx.resource.server.databroker.util.Constants.DATA_BROKER_SERVICE_ADDRESS;
import static iudx.resource.server.databroker.util.Util.getResponseJson;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.exception.FailureHandler;
import iudx.resource.server.apiserver.metering.handler.MeteringHandler;
import iudx.resource.server.apiserver.metering.service.MeteringService;
import iudx.resource.server.apiserver.subscription.model.DeleteSubsResultModel;
import iudx.resource.server.apiserver.subscription.model.GetResultModel;
import iudx.resource.server.apiserver.subscription.model.PostModelSubscription;
import iudx.resource.server.apiserver.subscription.service.SubscriptionService;
import iudx.resource.server.apiserver.subscription.service.SubscriptionServiceImpl;
import iudx.resource.server.apiserver.subscription.util.SubsType;
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
import iudx.resource.server.database.postgres.model.PostgresResultModel;
import iudx.resource.server.database.postgres.service.PostgresService;
import iudx.resource.server.databroker.service.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SubscriptionController {
  private static final Logger LOGGER = LogManager.getLogger(SubscriptionController.class);
  private final Router router;
  private final Vertx vertx;
  private final Api api;
  private ValidationHandler subsValidationHandler;
  private FailureHandler failureHandler;
  private PostgresService postgresService;
  private SubscriptionService subscriptionService;
  private DataBrokerService dataBrokerService;
  private CacheService cacheService;
  private AuthenticationService authenticator;
  private String audience;
  private JsonObject config;
  private MeteringService meteringService;

  public SubscriptionController(Vertx vertx, Router router, Api api, JsonObject config) {
    this.vertx = vertx;
    this.router = router;
    this.api = api;
    /*TODO: update example config-dev and config-dev */
    this.audience = config.getString("audience");
    this.config = config;
    this.subsValidationHandler = new ValidationHandler(vertx, RequestType.SUBSCRIPTION);
    this.failureHandler = new FailureHandler();
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

    /*MeteringHandler meteringHandler = new MeteringHandler(meteringService);*/

    // TODO: Need to add auditing insert

    router
        .post(api.getSubscriptionUrl())
        .handler(subsValidationHandler)
        .handler(getIdHandler)
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::postSubscriptions)
        /*.handler(meteringHandler)*/
        .failureHandler(failureHandler);

    router
        .patch(api.getSubscriptionUrl() + "/:userid/:alias")
        .handler(subsValidationHandler)
        .handler(getIdHandler)
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::appendSubscription)
        /*.handler(meteringHandler)*/
        .failureHandler(failureHandler);

    router
        .put(api.getSubscriptionUrl() + "/:userid/:alias")
        .handler(subsValidationHandler)
        .handler(getIdHandler)
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::updateSubscription)
        /*.handler(meteringHandler)*/
        .failureHandler(failureHandler);

    router
        .get(api.getSubscriptionUrl() + "/:userid/:alias")
        .handler(getIdHandler)
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::getSubscription)
        /*.handler(meteringHandler)*/
        .failureHandler(failureHandler);

    router
        .get(api.getSubscriptionUrl())
        .handler(getIdHandler)
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::getAllSubscriptionForUser)
        .failureHandler(failureHandler);

    router
        .delete(api.getSubscriptionUrl() + "/:userid/:alias")
        .handler(getIdHandler)
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::deleteSubscription)
        /*.handler(meteringHandler)*/
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
    PostModelSubscription postModelSubscription =
        new PostModelSubscription(
            userId,
            subscriptionType,
            instanceId,
            entities,
            requestJson.getString("name"),
            jwtData.getExpiry(),
            delegatorId);
    if (requestJson.getString(JSON_NAME).equalsIgnoreCase(alias)) {
      Future<GetResultModel> subsReq =
          subscriptionService.appendSubscription(postModelSubscription, subsId);
      subsReq.onComplete(
          subsRequestHandler -> {
            if (subsRequestHandler.succeeded()) {
              LOGGER.info("result : " + subsRequestHandler.result());
              routingContext.data().put(RESPONSE_SIZE, 0);
              response
                  .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                  .end(subsRequestHandler.result().constructSuccessResponse().toString());
              /*routingContext.next();*/
            } else {
              LOGGER.error("Fail: Bad request");
              ResultModel rs = new ResultModel(subsRequestHandler.cause().getMessage(), response);
              response
                  .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                  .setStatusCode(rs.getStatusCode())
                  .end(rs.toJson().toString());
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
      Future<GetResultModel> subsReq =
          subscriptionService.updateSubscription(entities, subsId, jwtData.getExpiry());
      subsReq.onComplete(
          subsRequestHandler -> {
            if (subsRequestHandler.succeeded()) {
              LOGGER.info("result : " + subsRequestHandler.result());
              routingContext.data().put(RESPONSE_SIZE, 0);
              response
                  .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                  .end(subsRequestHandler.result().constructSuccessResponse().toString());
              /*routingContext.next();*/
            } else {
              LOGGER.error("Fail: Bad request");
              ResultModel rs = new ResultModel(subsRequestHandler.cause().getMessage(), response);
              response
                  .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                  .setStatusCode(rs.getStatusCode())
                  .end(rs.toJson().toString());
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

    Future<GetResultModel> subsReq = subscriptionService.getSubscription(subsId, subscriptionType);
    subsReq.onComplete(
        subHandler -> {
          if (subHandler.succeeded()) {
            LOGGER.info("Success: Getting subscription");
            routingContext.data().put(RESPONSE_SIZE, 0);
            response
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .end(subHandler.result().constructSuccessResponse().toString());
            /*routingContext.next();*/
          } else {
            ResultModel rs = new ResultModel(subHandler.cause().getMessage(), response);
            response
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setStatusCode(rs.getStatusCode())
                .end(rs.toJson().toString());
          }
        });
  }

  private void getAllSubscriptionForUser(RoutingContext routingContext) {
    LOGGER.trace("Info: getAllSubscriptionForUser method started");
    JwtData jwtData = RoutingContextHelper.getJwtData(routingContext);
    HttpServerResponse response = routingContext.response();

    Future<PostgresResultModel> subsReq =
        subscriptionService.getAllSubscriptionQueueForUser(jwtData.getSub());
    subsReq.onComplete(
        subHandler -> {
          if (subHandler.succeeded()) {
            LOGGER.info("Success: Getting subscription queue" + subHandler.result());
            response
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .end(subHandler.result().toJson().toString());
          } else {
            ResultModel rs = new ResultModel(subHandler.cause().getMessage(), response);
            response
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setStatusCode(rs.getStatusCode())
                .end(rs.toJson().toString());
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
    Future<DeleteSubsResultModel> subsReq =
        subscriptionService.deleteSubscription(subsId, subscriptionType, userId);
    subsReq.onComplete(
        subHandler -> {
          if (subHandler.succeeded()) {
            routingContext.data().put(RESPONSE_SIZE, 0);
            response
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .end(subHandler.result().toJson().toString());
            /*routingContext.next();*/
          } else {
            ResultModel rs = new ResultModel(subHandler.cause().getMessage(), response);
            response
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setStatusCode(rs.getStatusCode())
                .end(rs.toJson().toString());
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
    PostModelSubscription postModelSubscription =
        new PostModelSubscription(
            userId,
            subscriptionType,
            instanceId,
            entities,
            requestBody.getString("name"),
            jwtData.getExpiry(),
            delegatorId);

    subscriptionService
        .createSubscription(postModelSubscription)
        .onSuccess(
            subHandler -> {
              LOGGER.info("Success: Handle Subscription request;");
              routingContext.data().put(RESPONSE_SIZE, 0);
              response
                  .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                  .end(subHandler.constructSuccessResponse().toString());
              /*routingContext.next();*/
            })
        .onFailure(
            failure -> {
              ResultModel rs = new ResultModel(failure.getMessage(), response);
              response
                  .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                  .setStatusCode(rs.getStatusCode())
                  .end(rs.toJson().toString());
              // routingContext.fail(new DxRuntimeException(failure.getMessage()));
            });
  }

  private void proxyRequired() {
    postgresService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);
    dataBrokerService = DataBrokerService.createProxy(vertx, DATA_BROKER_SERVICE_ADDRESS);
    cacheService = CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);
    authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    meteringService = MeteringService.createProxy(vertx, METERING_SERVICE_ADDRESS);
  }
}
