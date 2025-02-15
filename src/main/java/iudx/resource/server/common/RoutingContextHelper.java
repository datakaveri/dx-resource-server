package iudx.resource.server.common;

import static iudx.resource.server.apiserver.util.Constants.*;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.authenticator.model.JwtData;

public class RoutingContextHelper {
  //  private static final Logger LOGGER = LogManager.getLogger(RoutingContextHelper.class);
  private static final String JWT_DATA = "jwtData";

  public static String getRequestPath(RoutingContext routingContext) {
    return routingContext.request().path();
  }

  public static String getMethod(RoutingContext routingContext) {
    return routingContext.request().method().toString();
  }

  public static HttpServerRequest getRequest(RoutingContext routingContext) {
    return routingContext.request();
  }

  public static String getStartTime(RoutingContext event) {
    return getRequest(event).getParam(STARTT);
  }

  public static String getEndTime(RoutingContext event) {
    return getRequest(event).getParam(ENDT);
  }

  public static void setId(RoutingContext event, String id) {
    event.put(ID, id);
  }

  public static String getId(RoutingContext event) {
    return event.get(ID);
  }

  public static RequestBody getRequestBody(RoutingContext routingContext) {
    return routingContext.body();
  }

  public static String getToken(RoutingContext routingContext) {
    /* token would can be of the type : Bearer <JWT-Token>, <JWT-Token> */
    /* Send Bearer <JWT-Token> if Authorization header is present */
    /* allowing both the tokens to be authenticated for now */
    /* TODO: later, 401 error is thrown if the token does not contain Bearer keyword */
    String token = routingContext.request().headers().get(HEADER_AUTHORIZATION);
    boolean isBearerAuthHeaderPresent = token.contains(HEADER_BEARER_AUTHORIZATION);
    if (isBearerAuthHeaderPresent && token.trim().split(" ").length == 2) {
      String[] tokenWithoutBearer = token.split(HEADER_BEARER_AUTHORIZATION);
      token = tokenWithoutBearer[1].replaceAll("\\s", "");
      return token;
    }
    return routingContext.request().headers().get(HEADER_TOKEN);
  }

  public static JsonObject getAuthInfo(RoutingContext routingContext) {
    String normalisedEndpoint = getEndPoint(routingContext);
    /* endpoint not set using GetIdHandler */
    if (normalisedEndpoint == null) {
      /* endpoint is fetched from request path of routing context */
      normalisedEndpoint = getRequestPath(routingContext);
    }
    return new JsonObject()
        .put(API_ENDPOINT, normalisedEndpoint)
        .put(HEADER_TOKEN, getToken(routingContext))
        .put(API_METHOD, getMethod(routingContext));
  }

  public static void setJwtData(RoutingContext routingContext, JwtData jwtData) {
    routingContext.put(JWT_DATA, jwtData);
  }

  public static void setEndPoint(RoutingContext event, String normalisedPath) {
    event.put(API_ENDPOINT, normalisedPath);
  }

  public static String getEndPoint(RoutingContext event) {
    return event.get(API_ENDPOINT);
  }

  public static JwtData getJwtData(RoutingContext routingContext) {
    return routingContext.get(JWT_DATA);
  }
}
