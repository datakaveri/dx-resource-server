package org.cdpg.dx.auth.authentication.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.common.models.JwtData;

public class JwtAuthenticatorServiceImpl implements TokenAuthenticatorService {
  private static final Logger LOGGER = LogManager.getLogger(JwtAuthenticatorServiceImpl.class);

  private final JWTAuth jwtAuth;

  public JwtAuthenticatorServiceImpl(JWTAuth jwtAuth) {
    this.jwtAuth = jwtAuth;
  }

  @Override
  public Future<JwtData> authenticate(String token) {
    LOGGER.info("JwtAuthenticator: authenticate method called");
    Promise<JwtData> promise = Promise.promise();
    TokenCredentials creds = new TokenCredentials(token);

    jwtAuth
        .authenticate(creds)
        .onSuccess(
            user -> {
              JsonObject json = user.principal();
              json.put("exp", user.get("exp"));
              json.put("iat", user.get("iat"));
              JwtData jwtData = new JwtData(json);
              promise.complete(jwtData);
            })
        .onFailure(
            err -> {
              LOGGER.error("failed to decode/validate jwt token : " + err.getMessage());
              promise.fail(err.getMessage());
            });

    return promise.future();
  }
}
