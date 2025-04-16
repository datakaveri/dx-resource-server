package org.cdpg.dx.util;

import static iudx.resource.server.apiserver.util.Constants.ID;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.auditing.model.AuditLog;
import org.cdpg.dx.common.models.JwtData;
import org.cdpg.dx.common.models.User;

public final class RoutingContextHelper {
  private static final Logger LOGGER = LogManager.getLogger(RoutingContextHelper.class);
  private static final String JWT_DATA = "jwtData";
  private static final String USER = "user";
  private static final String HEADER_AUTHORIZATION = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final String AUDITING_LOG = "auditingLog";
  private static final String API_ENDPOINT = "apiEndpoint";
  private static final String RESPONSE_SIZE = "responseSize";

  private RoutingContextHelper() {
    // Prevent instantiation
  }

  public static void setUser(RoutingContext routingContext, User user) {
    routingContext.put(USER, user);
  }

  public static Optional<User> getUser(RoutingContext routingContext) {
    return Optional.ofNullable(routingContext.get(USER));
  }

  public static JsonObject getAuthInfo(RoutingContext routingContext) {
    return new JsonObject()
        .put("api_endpoint", routingContext.request().path())
        .put("token", getToken(routingContext).orElse("No Token Provided"))
        .put("api_method", routingContext.request().method().toString());
  }

  public static Optional<String> getToken(RoutingContext routingContext) {
    return Optional.ofNullable(routingContext.request().getHeader(HEADER_AUTHORIZATION))
        .filter(authHeader -> authHeader.startsWith(BEARER_PREFIX))
        .map(authHeader -> authHeader.substring(BEARER_PREFIX.length()).trim());
  }

  public static void setJwtData(RoutingContext routingContext, JwtData jwtData) {
    routingContext.put(JWT_DATA, jwtData);
  }

  public static Optional<JwtData> getJwtData(RoutingContext routingContext) {
    return Optional.ofNullable(routingContext.get(JWT_DATA));
  }

  public static void setAuditingLog(RoutingContext routingContext, List<AuditLog> auditingLog) {
    routingContext.put(AUDITING_LOG, auditingLog);
  }

  public static Optional<List<AuditLog>> getAuditingLog(RoutingContext routingContext) {
    return Optional.ofNullable(routingContext.get(AUDITING_LOG));
  }

  public static void setEndPoint(RoutingContext event, String normalisedPath) {
    event.put(API_ENDPOINT, normalisedPath);
  }

  public static String getEndPoint(RoutingContext event) {
    return event.get(API_ENDPOINT);
  }

  public static void setResponseSize(RoutingContext event, long responseSize) {
    event.data().put(RESPONSE_SIZE, responseSize);
  }

  public static Long getResponseSize(RoutingContext event) {
    return (Long) event.data().get(RESPONSE_SIZE);
  }

  public static void setId(RoutingContext event, String id) {
    event.put(ID, id);
  }

  public static String getId(RoutingContext event) {
    return event.get(ID);
  }
  public static String getRequestPath(RoutingContext routingContext) {
    return routingContext.request().path();
  }

}
