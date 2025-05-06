package org.cdpg.dx.rs.subscription;

import static org.cdpg.dx.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.auditing.handler.AuditingHandler;
import org.cdpg.dx.auth.authorization.exception.AuthorizationException;
import org.cdpg.dx.auth.authorization.handler.ClientRevocationValidationHandler;
import org.cdpg.dx.catalogue.service.CatalogueService;
import org.cdpg.dx.common.models.JwtData;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.revoked.service.RevokedService;
import org.cdpg.dx.rs.apiserver.ApiController;
import org.cdpg.dx.rs.authorization.handler.ResourcePolicyAuthorizationHandler;
import org.cdpg.dx.rs.subscription.model.PostSubscriptionModel;
import org.cdpg.dx.rs.subscription.service.SubscriptionService;
import org.cdpg.dx.rs.subscription.service.SubscriptionServiceImpl;
import org.cdpg.dx.rs.subscription.util.SubsType;
import org.cdpg.dx.util.RoutingContextHelper;
import org.cdpg.dx.validations.idhandler.GetIdFromBodyHandler;

public class SubscriptionController implements ApiController {
  private static final Logger LOGGER = LogManager.getLogger(SubscriptionController.class);

  private final SubscriptionService subscriptionService;
  private final AuditingHandler auditingHandler;
  private final ClientRevocationValidationHandler clientRevocationValidationHandler;
  private final ResourcePolicyAuthorizationHandler resourcePolicyAuthorizationHandler;

  public SubscriptionController(
      Vertx vertx,
      PostgresService pgService,
      DataBrokerService brokerService,
      CatalogueService catalogueService,
      RevokedService revokedService) {
    this.subscriptionService =
        new SubscriptionServiceImpl(pgService, brokerService, catalogueService);
    this.auditingHandler = new AuditingHandler(vertx);
    this.resourcePolicyAuthorizationHandler =
        new ResourcePolicyAuthorizationHandler(catalogueService);
    this.clientRevocationValidationHandler = new ClientRevocationValidationHandler(revokedService);
  }

  private static String getExpiry(Optional<JwtData> jwtData) {
    return LocalDateTime.ofInstant(
            Instant.ofEpochSecond(Long.parseLong(jwtData.get().exp().toString())),
            ZoneId.systemDefault())
        .toString();
  }

  @Override
  public void register(RouterBuilder builder) {
    GetIdFromBodyHandler getIdFromBodyHandler = new GetIdFromBodyHandler();
    builder
        .operation(GET_SUBSCRIBER_BY_ID)
        .handler(auditingHandler::handleApiAudit)
        .handler(clientRevocationValidationHandler)
            .handler(this::roleAccessValidation)
        .handler(this::handleGetSubscriberById)
        .failureHandler(this::handleFailure);

    builder
        .operation(GET_LIST_OF_SUBSCRIBERS)
        .handler(clientRevocationValidationHandler)
            .handler(this::roleAccessValidation)
            .handler(this::handleGetListOfSubscribers)
        .failureHandler(this::handleFailure);

    builder
        .operation(POST_SUBSCRIPTION)
        .handler(auditingHandler::handleApiAudit)
            .handler(getIdFromBodyHandler)
        .handler(clientRevocationValidationHandler)
        .handler(resourcePolicyAuthorizationHandler)
            .handler(this::roleAccessValidation)
        .handler(this::handlePostSubscription)
        .failureHandler(this::handleFailure);

    builder
        .operation(UPDATE_SUBSCRIPTION)
        .handler(auditingHandler::handleApiAudit)
            .handler(getIdFromBodyHandler)
        .handler(clientRevocationValidationHandler)
        .handler(resourcePolicyAuthorizationHandler)
            .handler(this::roleAccessValidation)
        .handler(this::handleUpdateSubscription)
        .failureHandler(this::handleFailure);

    builder
        .operation(APPEND_SUBSCRIPTION)
        .handler(auditingHandler::handleApiAudit)
            .handler(getIdFromBodyHandler)
        .handler(clientRevocationValidationHandler)
        .handler(resourcePolicyAuthorizationHandler)
            .handler(this::roleAccessValidation)
        .handler(this::handleAppendSubscription)
        .failureHandler(this::handleFailure);

    builder
        .operation(DELETE_SUBSCRIBER_BY_ID)
        .handler(auditingHandler::handleApiAudit)
        .handler(clientRevocationValidationHandler)
            .handler(this::roleAccessValidation)
        .handler(this::handleDeleteSubscriberById)
        .failureHandler(this::handleFailure);
  }

  private void handleGetSubscriberById(RoutingContext routingContext) {
    LOGGER.trace("handleGetSubscriberById() started");

    Optional<JwtData> jwtData = RoutingContextHelper.getJwtData(routingContext);
    String userid = routingContext.pathParam(USER_ID);
    String name = routingContext.pathParam(NAME);
    String subscriptionId = userid + "/" + name;
    LOGGER.info("subscriptionId {}", subscriptionId);

    subscriptionService
        .getSubscription(subscriptionId)
        .onSuccess(
            subscriber -> {
              LOGGER.debug("subscriber details: {}", subscriber.listString().toString());
              RoutingContextHelper.setResponseSize(routingContext, 0);
              RoutingContextHelper.setId(routingContext, subscriber.entities());
              routingContext
                  .response()
                  .putHeader("Content-Type", "application/json")
                  .setStatusCode(200)
                  .end(new JsonObject().put("subscriber", subscriber.listString()).encode());
            })
        .onFailure(routingContext::fail);
  }

  private void handleGetListOfSubscribers(RoutingContext routingContext) {
    LOGGER.trace("handleGetListOfAllSubscribers() started");
    Optional<JwtData> jwtData = RoutingContextHelper.getJwtData(routingContext);
    subscriptionService
        .getAllSubscriptionQueueForUser(jwtData.get().sub())
        .onSuccess(
            subscriber -> {
              routingContext
                  .response()
                  .putHeader("Content-Type", "application/json")
                  .setStatusCode(200)
                  .end(new JsonObject().put("subscriber", subscriber).encode());
            })
        .onFailure(routingContext::fail);
  }

  private void handlePostSubscription(RoutingContext routingContext) {
    LOGGER.trace("handlePostSubscription() started");
    Optional<JwtData> jwtData = RoutingContextHelper.getJwtData(routingContext);
    HttpServerRequest request = routingContext.request();
    JsonObject requestBody = routingContext.body().asJsonObject();
    String instanceId = request.getHeader(HEADER_HOST);
    String subscriptionType = SubsType.STREAMING.type;

    HttpServerResponse response = routingContext.response();
    String entities = requestBody.getJsonArray("entities").getString(0);
    String userId = jwtData.get().sub();
    String role = jwtData.get().role();
    String drl = jwtData.get().drl();
    String delegatorId;
    if (role.equalsIgnoreCase("delegate") && drl != null) {
      delegatorId = jwtData.get().did();
    } else {
      delegatorId = userId;
    }
    String expiry = getExpiry(jwtData);

    PostSubscriptionModel postSubscriptionModel =
        new PostSubscriptionModel(
            userId,
            subscriptionType,
            instanceId,
            entities,
            requestBody.getString(NAME),
            expiry,
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

  private void handleUpdateSubscription(RoutingContext routingContext) {
    LOGGER.trace("handleUpdateSubscription() started");
    Optional<JwtData> jwtData = RoutingContextHelper.getJwtData(routingContext);
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String userid = request.getParam(USER_ID);
    String name = request.getParam(NAME);
    String subsId = userid + "/" + name;
    JsonObject requestJson = routingContext.body().asJsonObject();

    if (requestJson.getString(NAME).equalsIgnoreCase(name)) {
      String entities = requestJson.getJsonArray("entities").getString(0);
      Future<String> subsReq =
          subscriptionService.updateSubscription(entities, subsId, getExpiry(jwtData));
      subsReq.onComplete(
          subsRequestHandler -> {
            if (subsRequestHandler.succeeded()) {
              LOGGER.info("Updated subscription");
              RoutingContextHelper.setResponseSize(routingContext, 0);
              List<String> resultEntities = new ArrayList<String>();
              resultEntities.add(entities);
              /*JsonObject resultJson =
              new JsonObject()
                      .put("type", ResponseUrn.SUCCESS_URN.getUrn())
                      .put("title", ResponseUrn.SUCCESS_URN.getMessage().toLowerCase())
                      .put(
                              "results",
                              new JsonArray()
                                      .add(new JsonObject().put(ENTITIES, new JsonArray(resultEntities))));*/
              response
                  .setStatusCode(201)
                  .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                  .end(resultEntities.toString());
            } else {
              LOGGER.error("Fail: Bad request");
              routingContext.fail(subsRequestHandler.cause());
            }
          });
    } else {
      LOGGER.error("Fail: Bad request");
      /*response
      .putHeader(CONTENT_TYPE, APPLICATION_JSON)
      .setStatusCode(400)
      .end(
              getResponseJson(
                      INVALID_PARAM_URN.getUrn(),
                      HttpStatusCode.BAD_REQUEST.getDescription(),
                      MSG_INVALID_NAME)
                      .toString());*/
    }
  }

  private void handleAppendSubscription(RoutingContext routingContext) {
    LOGGER.trace("handleAppendSubscription() started");
    Optional<JwtData> jwtData = RoutingContextHelper.getJwtData(routingContext);
    HttpServerRequest request = routingContext.request();
    String userid = request.getParam(USER_ID);
    String name = request.getParam(NAME);
    String subsId = userid + "/" + name;
    JsonObject requestJson = routingContext.body().asJsonObject();
    String instanceId = request.getHeader(HEADER_HOST);
    String subscriptionType = SubsType.STREAMING.type;

    HttpServerResponse response = routingContext.response();

    String entities = requestJson.getJsonArray("entities").getString(0);
    String userId = jwtData.get().sub();
    String role = jwtData.get().role();
    String drl = jwtData.get().drl();
    String delegatorId;
    if (role.equalsIgnoreCase("delegate") && drl != null) {
      delegatorId = jwtData.get().did();
    } else {
      delegatorId = userId;
    }
    PostSubscriptionModel postSubscriptionModel =
        new PostSubscriptionModel(
            userId,
            subscriptionType,
            instanceId,
            entities,
            requestJson.getString(NAME),
            getExpiry(jwtData),
            delegatorId);
    if (requestJson.getString(NAME).equalsIgnoreCase(name)) {
      Future<String> subsReq =
          subscriptionService.appendSubscription(postSubscriptionModel, subsId);
      subsReq.onComplete(
          subsRequestHandler -> {
            if (subsRequestHandler.succeeded()) {
              LOGGER.info("appended subscription");
              RoutingContextHelper.setResponseSize(routingContext, 0);
              List<String> resultEntities = new ArrayList<String>();
              resultEntities.add(entities);
              /*JsonObject resultJson =
              new JsonObject()
                      .put("type", ResponseUrn.SUCCESS_URN.getUrn())
                      .put("title", ResponseUrn.SUCCESS_URN.getMessage().toLowerCase())
                      .put(
                              "results",
                              new JsonArray()
                                      .add(new JsonObject().put(ENTITIES, new JsonArray(resultEntities))));*/
              response
                  .setStatusCode(201)
                  .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                  .end(resultEntities.toString());
            } else {
              LOGGER.error("Fail: Bad request");
              routingContext.fail(subsRequestHandler.cause());
            }
          });
    } else {
      LOGGER.error("Fail: Bad request");
      /*response
      .putHeader(CONTENT_TYPE, APPLICATION_JSON)
      .setStatusCode(400)
      .end(
              getResponseJson(
                      INVALID_PARAM_URN.getUrn(),
                      HttpStatusCode.BAD_REQUEST.getDescription(),
                      MSG_INVALID_NAME)
                      .toString());*/
    }
  }

  private void handleDeleteSubscriberById(RoutingContext routingContext) {
    LOGGER.trace("handleDeleteSubscriberById() started");
    Optional<JwtData> jwtData = RoutingContextHelper.getJwtData(routingContext);
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String userid = request.getParam(USER_ID);
    String name = request.getParam(NAME);
    String subsId = userid + "/" + name;
    String userId = jwtData.get().sub();
    Future<String> subsReq = subscriptionService.deleteSubscription(subsId, userId);
    subsReq.onComplete(
        subHandler -> {
          if (subHandler.succeeded()) {
            LOGGER.info("Success: Deleting subscription");
            RoutingContextHelper.setResponseSize(routingContext, 0);
            RoutingContextHelper.setId(routingContext, subHandler.result());
            /*response
            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .setStatusCode(200)
            .end(
                    getResponseJson(
                            SUCCESS_URN.getUrn(), SUCCESS, "Subscription deleted Successfully")
                            .toString());*/
          } else {
            routingContext.fail(subHandler.cause());
          }
        });
  }

  private void handleFailure(RoutingContext ctx) {
    Throwable failure = ctx.failure();
    int statusCode = ctx.statusCode();

    if (statusCode < 400) {
      // Default to 500 if not set
      statusCode = 500;
    }

    String message = failure != null ? failure.getMessage() : "Unknown error occurred";

    ctx.response()
        .putHeader("Content-Type", "application/json")
        .setStatusCode(statusCode)
        .end(new JsonObject().put("error", message).put("status", statusCode).encode());
  }

  public void roleAccessValidation(RoutingContext routingContext) {
    Optional<JwtData> jwtData = RoutingContextHelper.getJwtData(routingContext);
    if (!"consumer".equalsIgnoreCase(jwtData.get().role())) {
      routingContext.next();
    }
    boolean hasSubAccess = jwtData.get().cons().getJsonArray("access", new JsonArray()).contains("sub");
    if (!hasSubAccess) {
      LOGGER.error("Role validation failed");
      routingContext.fail(new AuthorizationException("Role validation failed"));
    }
    routingContext.next();
  }
}
