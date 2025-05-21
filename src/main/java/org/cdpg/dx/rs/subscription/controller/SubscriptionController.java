package org.cdpg.dx.rs.subscription.controller;

import static org.cdpg.dx.util.Constants.*;

import io.vertx.core.Future;
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
import org.cdpg.dx.auditing.helper.AuditLogConstructor;
import org.cdpg.dx.auth.authorization.exception.AuthorizationException;
import org.cdpg.dx.auth.authorization.handler.ClientRevocationValidationHandler;
import org.cdpg.dx.common.HttpStatusCode;
import org.cdpg.dx.common.exception.BaseDxException;
import org.cdpg.dx.common.models.JwtData;
import org.cdpg.dx.common.response.ResponseBuilder;
import org.cdpg.dx.rs.apiserver.ApiController;
import org.cdpg.dx.rs.authorization.handler.ResourcePolicyAuthorizationHandler;
import org.cdpg.dx.rs.subscription.model.PostSubscriptionModel;
import org.cdpg.dx.rs.subscription.service.SubscriptionService;
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
      SubscriptionService subscriptionService,
      AuditingHandler auditingHandler,
      ClientRevocationValidationHandler clientRevocationValidationHandler,
      ResourcePolicyAuthorizationHandler resourcePolicyAuthorizationHandler) {
    this.subscriptionService = subscriptionService;
    this.auditingHandler = auditingHandler;
    this.clientRevocationValidationHandler = clientRevocationValidationHandler;
    this.resourcePolicyAuthorizationHandler = resourcePolicyAuthorizationHandler;
  }

  private static String getExpiry(Optional<JwtData> jwtData) {
    String expiryTime =
        LocalDateTime.ofInstant(
                Instant.ofEpochSecond(Long.parseLong(jwtData.get().exp().toString())),
                ZoneId.systemDefault())
            .toString();
    LOGGER.info("expiry time :: {}", expiryTime);
    return expiryTime;
  }

  @Override
  public void register(RouterBuilder builder) {
    GetIdFromBodyHandler getIdFromBodyHandler = new GetIdFromBodyHandler();
    builder
        .operation(GET_SUBSCRIBER_BY_ID)
        .handler(auditingHandler::handleApiAudit)
        .handler(clientRevocationValidationHandler)
        .handler(this::roleAccessValidation)
        .handler(this::handleGetSubscriberById);

    builder
        .operation(GET_LIST_OF_SUBSCRIBERS)
        .handler(clientRevocationValidationHandler)
        .handler(this::roleAccessValidation)
        .handler(this::handleGetListOfSubscribers);

    builder
        .operation(POST_SUBSCRIPTION)
        .handler(auditingHandler::handleApiAudit)
        .handler(getIdFromBodyHandler)
        .handler(clientRevocationValidationHandler)
        .handler(resourcePolicyAuthorizationHandler)
        .handler(this::roleAccessValidation)
        .handler(this::handlePostSubscription);

    builder
        .operation(UPDATE_SUBSCRIPTION)
        .handler(auditingHandler::handleApiAudit)
        .handler(getIdFromBodyHandler)
        .handler(clientRevocationValidationHandler)
        .handler(resourcePolicyAuthorizationHandler)
        .handler(this::roleAccessValidation)
        .handler(this::handleUpdateSubscription);

    builder
        .operation(APPEND_SUBSCRIPTION)
        .handler(auditingHandler::handleApiAudit)
        .handler(getIdFromBodyHandler)
        .handler(clientRevocationValidationHandler)
        .handler(resourcePolicyAuthorizationHandler)
        .handler(this::roleAccessValidation)
        .handler(this::handleAppendSubscription);

    builder
        .operation(DELETE_SUBSCRIBER_BY_ID)
        .handler(auditingHandler::handleApiAudit)
        .handler(clientRevocationValidationHandler)
        .handler(this::roleAccessValidation)
        .handler(this::handleDeleteSubscriberById);
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
              new AuditLogConstructor(routingContext);
              RoutingContextHelper.setId(routingContext, subscriber.entities());
              JsonObject result = new JsonObject();
              result.put("entities", new JsonArray(subscriber.listString()));
              ResponseBuilder.sendSuccess(routingContext, result);
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
              ResponseBuilder.sendSuccess(routingContext, subscriber);
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
              new AuditLogConstructor(routingContext);
              ResponseBuilder.send(
                  routingContext, HttpStatusCode.CREATED, null, subHandler.toJson());
            })
        .onFailure(routingContext::fail);
  }

  private void handleUpdateSubscription(RoutingContext routingContext) {
    LOGGER.trace("handleUpdateSubscription() started");
    Optional<JwtData> jwtData = RoutingContextHelper.getJwtData(routingContext);
    HttpServerRequest request = routingContext.request();
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
              new AuditLogConstructor(routingContext);
              List<String> resultEntities = new ArrayList<String>();
              resultEntities.add(entities);
              JsonObject result = new JsonObject();
              result.put("entities", new JsonArray(resultEntities));
              ResponseBuilder.send(routingContext, HttpStatusCode.CREATED, null, result);
            } else {
              LOGGER.error("Fail: Bad request");
              routingContext.fail(subsRequestHandler.cause());
            }
          });
    } else {
      LOGGER.error("Fail: Bad request");
      routingContext.fail(new BaseDxException(400, "Bad request"));
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
              new AuditLogConstructor(routingContext);
              List<String> resultEntities = new ArrayList<String>();
              resultEntities.add(entities);
              JsonObject result = new JsonObject();
              result.put("entities", new JsonArray(resultEntities));
              ResponseBuilder.send(routingContext, HttpStatusCode.CREATED, null, result);
            } else {
              LOGGER.error("Fail: Bad request");
              routingContext.fail(subsRequestHandler.cause());
            }
          });
    } else {
      LOGGER.error("Fail: Bad request");
      routingContext.fail(new BaseDxException(400, "Bad request"));
    }
  }

  private void handleDeleteSubscriberById(RoutingContext routingContext) {
    LOGGER.trace("handleDeleteSubscriberById() started");
    Optional<JwtData> jwtData = RoutingContextHelper.getJwtData(routingContext);
    HttpServerRequest request = routingContext.request();
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
            new AuditLogConstructor(routingContext);
            RoutingContextHelper.setId(routingContext, subHandler.result());
            ResponseBuilder.sendSuccess(routingContext, "Subscription deleted Successfully");
          } else {
            routingContext.fail(subHandler.cause());
          }
        });
  }

  public void roleAccessValidation(RoutingContext routingContext) {
    Optional<JwtData> jwtData = RoutingContextHelper.getJwtData(routingContext);
    if (!"consumer".equalsIgnoreCase(jwtData.get().role())) {
      routingContext.next();
    }
    boolean hasSubAccess =
        jwtData.get().cons().getJsonArray("access", new JsonArray()).contains("sub");
    if (!hasSubAccess) {
      LOGGER.error("Role validation failed");
      routingContext.fail(new AuthorizationException("Role validation failed"));
    }
    routingContext.next();
  }
}
