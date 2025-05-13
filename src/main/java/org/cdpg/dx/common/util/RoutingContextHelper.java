//TODO: Need to correct and update it with out RCH

/*
package org.cdpg.dx.common.util;


import static org.cdpg.dx.apiserver.util.Constants.*;
import static org.cdpg.dx.common.ResponseUrn.INVALID_TOKEN_URN;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.models.User;
import org.cdpg.dx.auth.authentication.model.JwtData;
import org.cdpg.dx.common.HttpStatusCode;

public class RoutingContextHelper {
  private static final Logger LOGGER = LogManager.getLogger(RoutingContextHelper.class);
  private static final String JWT_DATA = "jwtData";

  public static void setUser(RoutingContext routingContext, User user) {
    routingContext.put(USER, user);
  }

  public static User getUser(RoutingContext routingContext) {
    return routingContext.get(USER);
  }

  public static JsonObject getAuthInfo(RoutingContext routingContext) {
    return new JsonObject()
        .put(API_ENDPOINT, getRequestPath(routingContext))
        .put(HEADER_TOKEN, getToken(routingContext))
        .put(API_METHOD, getMethod(routingContext));
  }

  public static String getToken(RoutingContext routingContext) {
    */
/* token would can be of the type : Bearer <JWT-Token> *//*

    */
/* Send Bearer <JWT-Token> if Authorization header is present *//*

    String token = routingContext.request().headers().get(HEADER_BEARER_AUTHORIZATION);
    boolean isValidBearerToken = token != null && token.trim().split(" ").length == 2;
    boolean isBearerAuthHeaderPresent = isValidBearerToken && (token.contains(HEADER_TOKEN_BEARER));
    String[] tokenWithoutBearer = new String[] {};
    if (isValidBearerToken) {
      if (isBearerAuthHeaderPresent) {
        tokenWithoutBearer = (token.split(HEADER_TOKEN_BEARER));
      }
      token = tokenWithoutBearer[1].replaceAll("\\s", "");
      return token;
    }
    throw new DxRuntimeException(HttpStatusCode.getByValue(401).getValue(), INVALID_TOKEN_URN);
  }

  public static JsonObject getVerifyAuthInfo(RoutingContext routingContext) {
    return new JsonObject()
        .put(API_ENDPOINT, getRequestPath(routingContext))
        .put(HEADER_TOKEN, getVerifyToken(routingContext))
        .put(API_METHOD, getMethod(routingContext));
  }

  private static String getVerifyToken(RoutingContext routingContext) {
    String token = routingContext.request().headers().get(AUTHORIZATION_KEY);
    if (token.trim().split(" ").length == 2) {
      token = token.trim().split(" ")[1];
      return token;
    } else {
      throw new DxRuntimeException(HttpStatusCode.getByValue(401).getValue(), INVALID_TOKEN_URN);
    }
  }

  public static String getMethod(RoutingContext routingContext) {
    return routingContext.request().method().toString();
  }

  public static String getRequestPath(RoutingContext routingContext) {
    return routingContext.request().path();
  }

  public static void setJwtData(RoutingContext routingContext, JwtData jwtData) {
    routingContext.put(JWT_DATA, jwtData);
  }

  public static JwtData getJwtData(RoutingContext routingContext) {
    return routingContext.get(JWT_DATA);
  }
}
*/
