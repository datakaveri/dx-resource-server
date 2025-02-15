package iudx.resource.server.authenticator.handler.authentication;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.ResponseUrn.*;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.authenticator.AuthenticationService;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.common.Api;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.common.RoutingContextHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** IUDX Authentication handler to authenticate token passed in HEADER */
public class AuthHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(AuthHandler.class);
  public AuthenticationService authenticator;
  Api api;

  public AuthHandler(Api api, AuthenticationService authenticator) {
    this.authenticator = authenticator;
    this.api = api;
  }

  @Override
  public void handle(RoutingContext context) {
    JsonObject authInfo = RoutingContextHelper.getAuthInfo(context);
    LOGGER.debug("Info :" + context.request().path());

    Future<JwtData> jwtDataFuture = authenticator.tokenIntrospect(authInfo);
    jwtDataFuture
        .onSuccess(
            jwtData -> {
              RoutingContextHelper.setJwtData(context, jwtData);
              context.next();
            })
        .onFailure(
            failure -> {
              processAuthFailure(context, failure.getMessage());
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
    } else if (result.contains("Entity IDs do not match")
        || result.contains("Error processing the request body")) {
      LOGGER.error("Entity IDs do not match");
      HttpStatusCode statusCode = HttpStatusCode.getByValue(400);
      ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(statusCode.getValue())
          .end(generateResponse(BAD_REQUEST_URN, statusCode).toString());
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
}