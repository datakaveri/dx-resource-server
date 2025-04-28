package org.cdpg.dx.auth.authentication.handler;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.auth.authentication.client.SecretKeyClient;
import org.cdpg.dx.auth.authentication.exception.AuthenticationException;
import org.cdpg.dx.auth.authentication.service.JwtAuthProvider;
import org.cdpg.dx.auth.authentication.service.JwtAuthenticatorServiceImpl;
import org.cdpg.dx.util.RoutingContextHelper;

public class TokenAuthenticationHandler implements AuthenticationHandler {
  private static final Logger LOGGER = LogManager.getLogger(TokenAuthenticationHandler.class);

  private final JsonObject config;
  private final Vertx vertx;
  SecretKeyClient secretKeyClient;
  private JWTAuth jwtAuth;

  public TokenAuthenticationHandler(
      JsonObject config, SecretKeyClient secretKeyClient, Vertx vertx) {
    this.config = config;
    this.vertx = vertx;
    this.secretKeyClient = secretKeyClient;

    JwtAuthProvider.init(vertx, config, secretKeyClient)
        .onSuccess(
            jwtAuth -> {
              this.jwtAuth = jwtAuth;
              LOGGER.info("JWT Auth initialized successfully");
            })
        .onFailure(
            err -> {
              LOGGER.error("Failed to initialize JWT Auth: {}", err.getMessage());
            });
  }

  @Override
  public void handle(RoutingContext context) {
    LOGGER.info("TokenAuthenticationHandler: handle method called");
    String token = context.request().getHeader("token");

    JwtAuthenticatorServiceImpl jwtAuthenticator = new JwtAuthenticatorServiceImpl(jwtAuth);
    jwtAuthenticator
        .authenticate(token)
        .onSuccess(
            jwtData -> {
              LOGGER.info("Token decoded successfully: {}", jwtData);
              RoutingContextHelper.setJwtData(context, jwtData);
              context.next();
            })
        .onFailure(
            err -> {
              context.fail(401, new AuthenticationException(err.getMessage()));
            });
  }
}
