package iudx.resource.server.apiserver.handlers;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.authenticator.Constants.ROLE;
import static iudx.resource.server.common.Constants.*;
import static iudx.resource.server.common.ResponseUrn.*;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.authenticator.AuthenticationService;
import iudx.resource.server.common.Api;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.ResponseUrn;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** IUDX Authentication handler to authenticate token passed in HEADER */
public class AuthHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(AuthHandler.class);
  static AuthenticationService authenticator;
  private static Api api;
  private final String authInfo = "authInfo";
  private final List<String> noAuthRequired = bypassEndpoint;
  private HttpServerRequest request;

  public static AuthHandler create(Vertx vertx, Api apis) {
    authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    api = apis;
    return new AuthHandler();
  }

  @Override
  public void handle(RoutingContext context) {
    request = context.request();
    RequestBody requestBody = context.body();
    JsonObject requestJson = null;
    if (requestBody != null && requestBody.asJsonObject() != null) {
      requestJson = requestBody.asJsonObject().copy();
    }
    if (requestJson == null) {
      requestJson = new JsonObject();
    }

    LOGGER.debug("Info : path " + request.path());
    // bypassing auth for RDocs
    if (noAuthRequired.contains(request.path())) {
      context.next();
      return;
    }

    String token = request.headers().get(HEADER_TOKEN);
    final String path = getNormalizedPath(request.path());
    final String method = context.request().method().toString();

    if (token == null) {
      token = "public";
    }

    JsonObject authInfo =
        new JsonObject().put(API_ENDPOINT, path).put(HEADER_TOKEN, token).put(API_METHOD, method);

    LOGGER.debug("Info :" + context.request().path());
    String id = getId(context, path, method);
    authInfo.put(ID, id);

    JsonArray ids = new JsonArray();
    String[] idArray = id == null ? new String[0] : id.split(",");
    for (String i : idArray) {
      ids.add(i);
    }

    if (path.equals(IUDX_MANAGEMENT_URL + INGESTION_PATH)
        && HttpMethod.POST.name().equalsIgnoreCase(method)) {
      ids = requestJson.getJsonArray(JSON_ENTITIES);
    }
    requestJson.put(IDS, ids);

    authenticator.tokenInterospect(
        requestJson,
        authInfo,
        authHandler -> {
          if (authHandler.succeeded()) {
            authInfo.put(IID, authHandler.result().getValue(IID));
            authInfo.put(USER_ID, authHandler.result().getValue(USER_ID));
            authInfo.put(EXPIRY, authHandler.result().getValue(EXPIRY));
            authInfo.put(ROLE, authHandler.result().getValue(ROLE));
            authInfo.put(DID, authHandler.result().getValue(DID));
            authInfo.put(DRL, authHandler.result().getValue(DRL));
            context.data().put(this.authInfo, authInfo);
          } else {
            processAuthFailure(context, authHandler.cause().getMessage());
            return;
          }
          context.next();
          return;
        });
  }

  private void processAuthFailure(RoutingContext ctx, String result) {
    if (result.contains("Not Found")) {
      LOGGER.error("Error : Item Not Found");
      HttpStatusCode statusCode = HttpStatusCode.getByValue(404);
      ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(statusCode.getValue())
          .end(generateResponse(RESOURCE_NOT_FOUND_URN, statusCode).toString());
    } else {
      LOGGER.error("Error : Authentication Failure");
      HttpStatusCode statusCode = HttpStatusCode.getByValue(401);
      ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(statusCode.getValue())
          .end(generateResponse(INVALID_TOKEN_URN, statusCode).toString());
    }
  }

  private JsonObject generateResponse(ResponseUrn urn, HttpStatusCode statusCode) {
    return new JsonObject()
        .put(JSON_TYPE, urn.getUrn())
        .put(JSON_TITLE, statusCode.getDescription())
        .put(JSON_DETAIL, statusCode.getDescription());
  }

  /**
   * extract id from request (path/query or body )
   *
   * @param context current routing context
   * @param path endpoint called for
   * @return id extracted from path if present
   */
  private String getId(RoutingContext context, String path, String method) {

    String pathId = getId4rmPath(context);
    String paramId = getId4rmRequest();
    String bodyId = getId4rmBody(context, path);
    String id;
    if (pathId != null && !pathId.isBlank()) {
      id = pathId;
    } else {
      if (paramId != null && !paramId.isBlank()) {
        id = paramId;
      } else {
        id = bodyId;
      }
    }
    if (path.matches(api.getSubscriptionUrl())
        && (!method.equalsIgnoreCase("GET") || !method.equalsIgnoreCase("DELETE"))) {
      id = bodyId;
    }
    return id;
  }

  private String getId4rmPath(RoutingContext context) {
    StringBuilder id = null;
    Map<String, String> pathParams = context.pathParams();
    LOGGER.debug("path params :" + pathParams);
    if (pathParams != null && !pathParams.isEmpty()) {
      if (pathParams.containsKey("*")) {
        id = new StringBuilder(pathParams.get("*"));
        LOGGER.debug("id :" + id);
      } else if (pathParams.containsKey(USER_ID) && pathParams.containsKey(JSON_ALIAS)) {
        id = new StringBuilder();
        id.append(pathParams.get(USER_ID)).append("/").append(pathParams.get(JSON_ALIAS));
      }
    }
    return id != null ? id.toString() : null;
  }

  private String getId4rmRequest() {
    return request.getParam(ID);
  }

  private String getId4rmBody(RoutingContext context, String endpoint) {
    JsonObject body = context.body().asJsonObject();
    String id = null;
    if (body != null) {
      JsonArray array = body.getJsonArray(JSON_ENTITIES);
      if (array != null) {
        if (endpoint.matches(getpathRegex(api.getIngestionPath()))
            || endpoint.matches(getpathRegex(api.getSubscriptionUrl()))) {
          id = array.getString(0);
        } else {
          JsonObject json = array.getJsonObject(0);
          if (json != null) {
            id = json.getString(ID);
          }
        }
      }
    }
    return id;
  }

  /**
   * get normalized path without id as path param.
   *
   * @param url complete path from request
   * @return path without id.
   */
  private String getNormalizedPath(String url) {
    LOGGER.debug("URL : " + url);
    String path = null;
    if (url.matches(getpathRegex(api.getEntitiesUrl()))) {
      path = api.getEntitiesUrl();
    } else if (url.matches(getpathRegex(api.getTemporalUrl()))) {
      path = api.getTemporalUrl();
    } else if (url.matches(getpathRegex(api.getPostTemporalQueryPath()))) {
      path = api.getPostTemporalQueryPath();
    } else if (url.matches(getpathRegex(api.getPostEntitiesQueryPath()))) {
      path = api.getPostEntitiesQueryPath();
    } else if (url.matches(getpathRegex(api.getSubscriptionUrl()))) {
      path = api.getSubscriptionUrl();
    } else if (url.matches(api.getIngestionPathEntities())) {
      path = api.getIngestionPathEntities();
    } else if (url.matches(getpathRegex(api.getIngestionPath()))) {
      path = api.getIngestionPath();
    } else if (url.matches(getpathRegex(api.getMonthlyOverview()))) {
      path = api.getMonthlyOverview();
    } else if (url.matches(EXCHANGE_URL_REGEX)) {
      path = IUDX_MANAGEMENT_URL + EXCHANGE_PATH;
    } else if (url.matches(QUEUE_URL_REGEX)) {
      path = IUDX_MANAGEMENT_URL + QUEUE_PATH;
    } else if (url.matches(VHOST_URL_REGEX)) {
      path = IUDX_MANAGEMENT_URL + VHOST;
    } else if (url.matches(BIND_URL_REGEX)) {
      path = IUDX_MANAGEMENT_URL + BIND;
    } else if (url.matches(UNBIND_URL_REGEX)) {
      path = IUDX_MANAGEMENT_URL + UNBIND;
    } else if (url.matches(getpathRegex(api.getManagementBasePath()))) {
      path = api.getManagementBasePath();
    } else if (url.matches(REVOKE_TOKEN_REGEX)) {
      path = ADMIN + REVOKE_TOKEN;
    } else if (url.matches(UNIQUE_ATTR_REGEX)) {
      path = ADMIN + RESOURCE_ATTRIBS;
    } else if (url.matches(api.getIudxConsumerAuditUrl())) {
      path = api.getIudxConsumerAuditUrl();
    } else if (url.matches(api.getIudxProviderAuditUrl())) {
      path = api.getIudxProviderAuditUrl();
    } else if (url.matches(api.getIudxAsyncSearchApi())) {
      path = api.getIudxAsyncSearchApi();
    } else if (url.matches(IUDX_ASYNC_STATUS)) {
      path = api.getIudxAsyncStatusApi();
    } else if (url.matches(getpathRegex(api.getSummaryPath()))) {
      path = api.getSummaryPath();
    }
    return path;
  }

  private String getpathRegex(String path) {
    return path + "(.*)";
  }
}
