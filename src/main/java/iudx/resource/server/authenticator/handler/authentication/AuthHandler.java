package iudx.resource.server.authenticator.handler.authentication;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.ResponseUrn.*;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.authenticator.AuthenticationService;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.RoutingContextHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** DX Authentication handler to authenticate token passed in HEADER */
public class AuthHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(AuthHandler.class);
  public AuthenticationService authenticator;

  public AuthHandler(AuthenticationService authenticator) {
    this.authenticator = authenticator;
  }

  @Override
  public void handle(RoutingContext context) {
    String token = RoutingContextHelper.getToken(context);
    LOGGER.debug("Info :{}", context.request().path());
    Future<JwtData> jwtDataFuture = authenticator.decodeToken(token);
    jwtDataFuture
        .onSuccess(
            jwtData -> {
              RoutingContextHelper.setJwtData(context, jwtData);
              context.next();
            })
        .onFailure(
            failure -> {
              LOGGER.error("Token decode failed: {}", failure.getMessage());
              context.fail(
                  new DxRuntimeException(
                      HttpStatusCode.getByValue(401).getValue(), INVALID_TOKEN_URN));
            });
  }
}
