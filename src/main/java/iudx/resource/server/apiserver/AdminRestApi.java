package iudx.resource.server.apiserver;

import static iudx.resource.server.apiserver.response.ResponseUtil.generateResponse;
import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.apiserver.util.Constants.ID;
import static iudx.resource.server.apiserver.util.Constants.USER_ID;
import static iudx.resource.server.common.Constants.*;
import static iudx.resource.server.common.HttpStatusCode.*;
import static iudx.resource.server.common.HttpStatusCode.SUCCESS;
import static iudx.resource.server.common.ResponseUrn.*;
import static iudx.resource.server.database.postgres.Constants.*;
import static iudx.resource.server.metering.util.Constants.*;
import static iudx.resource.server.metering.util.Constants.API;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.handlers.*;
import iudx.resource.server.authenticator.AuthenticationService;
import iudx.resource.server.authenticator.handler.authentication.AuthHandler;
import iudx.resource.server.authenticator.handler.authorization.GetIdHandler;
import iudx.resource.server.authenticator.handler.authorization.TokenInterospectionForAdminApis;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.cache.cachelmpl.CacheType;
import iudx.resource.server.common.*;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.databroker.DataBrokerService;
import iudx.resource.server.metering.MeteringService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class AdminRestApi {

  private static final Logger LOGGER = LogManager.getLogger(AdminRestApi.class);

  private final Vertx vertx;
  private final Router router;
  private final DataBrokerService rmqBrokerService;
  private final PostgresService pgService;
  private final MeteringService auditService;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final CacheService cacheService;
  private Api api;
  private AuthenticationService authenticator;

  AdminRestApi(Vertx vertx, Router router, Api api) {
    this.vertx = vertx;
    this.router = router;
    this.rmqBrokerService = DataBrokerService.createProxy(vertx, BROKER_SERVICE_ADDRESS);
    this.auditService = MeteringService.createProxy(vertx, METERING_SERVICE_ADDRESS);
    this.pgService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);
    this.api = api;
    this.authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    cacheService = CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);
  }

  public Router init() {

    AuthHandler authHandler = new AuthHandler(api, authenticator);
    GetIdHandler getIdHandler = new GetIdHandler(api);
    Handler<RoutingContext> validateToken = new TokenInterospectionForAdminApis();
    FailureHandler validationsFailureHandler = new FailureHandler();

    router
        .post(REVOKE_TOKEN)
        .handler(getIdHandler.withNormalisedPath(api.getAdminRevokeToken()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(this::handleRevokeTokenRequest)
        .failureHandler(validationsFailureHandler);

    router
        .post(RESOURCE_ATTRIBS)
        .handler(getIdHandler.withNormalisedPath(api.getAdminUniqueAttributeOfResource()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(this::createUniqueAttribute)
        .failureHandler(validationsFailureHandler);

    router
        .put(RESOURCE_ATTRIBS)
        .handler(getIdHandler.withNormalisedPath(api.getAdminUniqueAttributeOfResource()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(this::updateUniqueAttribute)
        .failureHandler(validationsFailureHandler);

    router
        .delete(RESOURCE_ATTRIBS)
        .handler(getIdHandler.withNormalisedPath(api.getAdminUniqueAttributeOfResource()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(this::deleteUniqueAttribute)
        .failureHandler(validationsFailureHandler);

    return router;
  }

  private void handleRevokeTokenRequest(RoutingContext context) {

    JsonObject requestBody = context.body().asJsonObject();

    //    context.queryParam(ID).add("admin_op");

    String id = requestBody.getString("sub");
    StringBuilder query =
        new StringBuilder(
            INSERT_REVOKE_TOKEN_SQL
                .replace("$1", requestBody.getString("sub"))
                .replace("$2", LocalDateTime.now().toString()));

    JsonObject rmqMessage = new JsonObject();
    rmqMessage.put("sub", requestBody.getString("sub"));
    rmqMessage.put("expiry", LocalDateTime.now().toString());

    LOGGER.debug("query : " + query.toString());
    HttpServerResponse response = context.response();
    pgService.executeQuery(
        query.toString(),
        pgHandler -> {
          if (pgHandler.succeeded()) {
            rmqBrokerService.publishMessage(
                rmqMessage,
                TOKEN_INVALID_EX,
                TOKEN_INVALID_EX_ROUTING_KEY,
                rmqHandler -> {
                  if (rmqHandler.succeeded()) {
                    Future.future(fu -> updateAuditTable(context));
                    handleResponse(response, SUCCESS, SUCCESS_URN);
                  } else {
                    LOGGER.error(rmqHandler.cause());
                    try {
                      Response resp =
                          objectMapper.readValue(rmqHandler.cause().getMessage(), Response.class);
                      handleResponse(response, resp);
                    } catch (JsonProcessingException e) {
                      LOGGER.error("Failure message not in format [type,title,detail]");
                      handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
                    }
                  }
                });
          } else {
            try {
              Response resp =
                  objectMapper.readValue(pgHandler.cause().getMessage(), Response.class);
              handleResponse(response, resp);
            } catch (JsonProcessingException e) {
              handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
            }
          }
        });
  }

  private void createUniqueAttribute(RoutingContext context) {
    LOGGER.trace("createUniqueAttribute() started");
    HttpServerResponse response = context.response();
    JsonObject requestBody = context.body().asJsonObject();

    String id = requestBody.getString("id");
    String attribute = requestBody.getString("attribute");

    if (id == null || attribute == null) {
      handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
      return;
    }

    JsonObject rmqMessage = new JsonObject();
    rmqMessage.put("id", id);
    rmqMessage.put("unique-attribute", attribute);
    rmqMessage.put("eventType", BroadcastEventType.CREATE);

    StringBuilder query =
        new StringBuilder(INSERT_UNIQUE_ATTR_SQL.replace("$1", id).replace("$2", attribute));

    LOGGER.debug("query : " + query.toString());
    pgService.executeQuery(
        query.toString(),
        pghandler -> {
          if (pghandler.succeeded()) {
            rmqBrokerService.publishMessage(
                rmqMessage,
                UNIQUE_ATTR_EX,
                UNIQUE_ATTR_EX_ROUTING_KEY,
                rmqHandler -> {
                  if (rmqHandler.succeeded()) {
                    Future.future(fu -> updateAuditTable(context));
                    handleResponse(response, SUCCESS, SUCCESS_URN);
                  } else {
                    LOGGER.error(rmqHandler.cause());
                    try {
                      Response resp =
                          objectMapper.readValue(rmqHandler.cause().getMessage(), Response.class);
                      handleResponse(response, resp);
                    } catch (JsonProcessingException e) {
                      handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
                    }
                  }
                });
          } else {
            LOGGER.error(pghandler.cause());
            try {
              Response resp =
                  objectMapper.readValue(pghandler.cause().getMessage(), Response.class);
              handleResponse(response, resp);
            } catch (JsonProcessingException e) {
              handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
            }
          }
        });
  }

  private void updateUniqueAttribute(RoutingContext context) {
    HttpServerResponse response = context.response();
    JsonObject requestBody = context.body().asJsonObject();

    String id = requestBody.getString("id");
    String attribute = requestBody.getString("attribute");

    if (id == null || attribute == null) {
      handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
      return;
    }

    //    JsonObject queryparams = new JsonObject().put("attribute", attribute).put("id", id);

    JsonObject rmqMessage = new JsonObject();
    rmqMessage.put("id", id);
    rmqMessage.put("unique-attribute", attribute);
    rmqMessage.put("eventType", BroadcastEventType.UPDATE);
    String query = UPDATE_UNIQUE_ATTR_SQL.replace("$2", id).replace("$1", attribute);
    pgService.executePreparedQuery(
        query,
        new JsonObject(),
        pghandler -> {
          if (pghandler.succeeded()) {
            rmqBrokerService.publishMessage(
                rmqMessage,
                UNIQUE_ATTR_EX,
                UNIQUE_ATTR_EX_ROUTING_KEY,
                rmqHandler -> {
                  if (rmqHandler.succeeded()) {
                    Future.future(fu -> updateAuditTable(context));
                    handleResponse(response, SUCCESS, SUCCESS_URN);
                  } else {
                    LOGGER.error(rmqHandler.cause());
                    try {
                      Response resp =
                          objectMapper.readValue(rmqHandler.cause().getMessage(), Response.class);
                      handleResponse(response, resp);
                    } catch (JsonProcessingException e) {
                      handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
                    }
                  }
                });
          } else {
            LOGGER.error(pghandler.cause());
            try {
              Response resp =
                  objectMapper.readValue(pghandler.cause().getMessage(), Response.class);
              handleResponse(response, resp);
            } catch (JsonProcessingException e) {
              handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
            }
          }
        });
  }

  private void deleteUniqueAttribute(RoutingContext context) {

    HttpServerRequest request = context.request();
    String id = request.params().get("id");

    JsonObject rmqMessage = new JsonObject();
    rmqMessage.put("id", id);
    rmqMessage.put("unique-attribute", "dummy_attribute");
    rmqMessage.put("eventType", BroadcastEventType.DELETE);
    String query = DELETE_UNIQUE_ATTR_SQL.replace("$1", id);
    LOGGER.trace("query : " + query);
    HttpServerResponse response = context.response();
    JsonObject queryparams = new JsonObject();
    pgService.executePreparedQuery(
        query,
        queryparams,
        pghandler -> {
          if (pghandler.succeeded()) {
            rmqBrokerService.publishMessage(
                rmqMessage,
                UNIQUE_ATTR_EX,
                UNIQUE_ATTR_EX_ROUTING_KEY,
                rmqHandler -> {
                  if (rmqHandler.succeeded()) {
                    Future.future(fu -> updateAuditTable(context));
                    handleResponse(response, SUCCESS, SUCCESS_URN);
                  } else {
                    LOGGER.error(rmqHandler.cause());
                    try {
                      Response resp =
                          objectMapper.readValue(rmqHandler.cause().getMessage(), Response.class);
                      handleResponse(response, resp);
                    } catch (JsonProcessingException e) {
                      handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
                    }
                  }
                });
          } else {
            LOGGER.error(pghandler.cause());
            try {
              Response resp =
                  objectMapper.readValue(pghandler.cause().getMessage(), Response.class);
              handleResponse(response, resp);
            } catch (JsonProcessingException e) {
              handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
            }
          }
        });
  }

  private void handleResponse(HttpServerResponse response, Response respObject) {
    ResponseUrn urn = fromCode(respObject.getType());
    handleResponse(response, respObject, urn.getMessage());
  }

  private void handleResponse(HttpServerResponse response, Response respObject, String message) {
    HttpStatusCode httpCode = getByValue(respObject.getStatus());
    ResponseUrn urn = fromCode(respObject.getType());
    handleResponse(response, httpCode, urn, message);
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

  private Future<Void> updateAuditTable(RoutingContext context) {
    JwtData jwtData = RoutingContextHelper.getJwtData(context);
    String sub = jwtData.getSub();
    String role = jwtData.getRole();
    String did = jwtData.getDid();
    String drl = jwtData.getDrl();
    String id = RoutingContextHelper.getId(context);
    String endPoint = RoutingContextHelper.getEndPoint(context);

    JsonObject request = new JsonObject();
    if (StringUtils.isNotBlank(id)) {
      request.put(ID, id);
    } else {
      request.put(ID, RESOURCE_ID_DEFAULT);
    }
    JsonObject cacheRequest = new JsonObject();
    cacheRequest.put("type", CacheType.CATALOGUE_CACHE);
    cacheRequest.put("key", request.getValue(ID));
    Promise<Void> promise = Promise.promise();
    cacheService
        .get(cacheRequest)
        .onComplete(
            relHandler -> {
              if (relHandler.succeeded()) {
                JsonObject cacheResult = relHandler.result();
                String type =
                    cacheResult.containsKey(RESOURCE_GROUP) ? "RESOURCE" : "RESOURCE_GROUP";
                String resourceGroup =
                    cacheResult.containsKey(RESOURCE_GROUP)
                        ? cacheResult.getString(RESOURCE_GROUP)
                        : cacheResult.getString(ID);
                ZonedDateTime zst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
                if (role.equalsIgnoreCase("delegate") && drl != null) {
                  request.put(DELEGATOR_ID, did);
                } else {
                  request.put(DELEGATOR_ID, sub);
                }
                String providerId = cacheResult.getString("provider");
                long time = zst.toInstant().toEpochMilli();
                String isoTime = zst.truncatedTo(ChronoUnit.SECONDS).toString();
                request.put(RESOURCE_GROUP, resourceGroup);
                request.put(TYPE_KEY, type);
                request.put(PROVIDER_ID, providerId);
                request.put(EPOCH_TIME, time);
                request.put(ISO_TIME, isoTime);
                request.put(API, endPoint);
                request.put(RESPONSE_SIZE, 0);
                request.put(USER_ID, sub);
                LOGGER.debug("request : " + request.encode());
                auditService.insertMeteringValuesInRmq(
                    request,
                    handler -> {
                      if (handler.succeeded()) {
                        LOGGER.info("message published in RMQ.");
                        promise.complete();
                      } else {
                        LOGGER.error("failed to publish message in RMQ.");
                        promise.complete();
                      }
                    });
              } else {
                LOGGER.debug("Item not found and failed to call metering service");
              }
            });
    return promise.future();
  }
}
